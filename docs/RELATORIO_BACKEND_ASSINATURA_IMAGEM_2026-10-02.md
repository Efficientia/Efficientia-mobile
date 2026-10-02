# Relatório para o backend — Assinatura do motorista como imagem

Data: 02/10/2026

## Contexto

No primeiro acesso, o motorista pode criar a assinatura de duas formas:

1. desenhando manualmente no aplicativo; ou
2. digitando o nome completo e confirmando a prévia estilizada.

Nos dois casos, o mobile deverá gerar um arquivo PNG e enviá-lo à API. Essa assinatura será reutilizada futuramente nos formulários e relatórios exibidos na visão web do administrador.

Além das assinaturas vinculadas a cada formulário, existe uma **assinatura fixa do motorista**. Ela faz parte do cadastro do motorista e pode ser criada pelo administrador antes mesmo do primeiro login no aplicativo. A autenticação não cria esse dado; ela apenas permite ao motorista consultar ou, conforme a regra de negócio definida, atualizar sua própria assinatura.

Os dois conceitos não devem ser confundidos:

- `assinatura_motorista`: assinatura fixa/atual do perfil do motorista;
- assinatura do formulário: evidência imutável utilizada em uma viagem ou documento específico.

Uma alteração futura na assinatura fixa não pode modificar retroativamente formulários já finalizados.

## Situação encontrada na API

O repositório `Efficientia-API` já possui `POST /api/v1/documentos` com `multipart/form-data`, mas o contrato atual não atende ao fluxo de primeiro acesso:

- assinaturas com arquivo aceitam somente PNG;
- o documento exige `viagemId`, porém a assinatura de perfil é criada antes da primeira viagem;
- a assinatura desenhada usa `origem=DESENHO` e `modalidadeAssinatura=DESENHO`;
- a assinatura digitada usa atualmente JSON, `origem=TEXTO` e `modalidadeAssinatura=TEXTO`, sem imagem;
- a migration `V2__create_documento.sql` registra somente metadados no PostgreSQL e mantém o conteúdo binário em storage privado por meio de `storage_key`;
- o enum atual não distingue uma imagem gerada a partir de nome digitado.

Portanto, não é seguro integrar o mobile ao endpoint atual usando um `viagemId` fictício ou classificando a assinatura digitada como desenho.

## Alteração necessária na API

Criar um recurso próprio para a assinatura fixa do motorista. O administrador poderá cadastrá-la antes do primeiro login, enquanto o motorista autenticado poderá operar somente sobre o próprio perfil, conforme a permissão definida pelo produto.

Cadastro administrativo sugerido:

```http
PUT /api/v1/usuarios/{motoristaId}/assinatura
Authorization: Bearer <token-administrador>
Idempotency-Key: <uuid>
Content-Type: multipart/form-data
```

Cadastro ou atualização pelo próprio motorista:

```http
PUT /api/v1/usuarios/me/assinatura
Authorization: Bearer <token>
Idempotency-Key: <uuid>
Content-Type: multipart/form-data

arquivo: assinatura.png
metadados: {
  "modalidade": "DESENHO" | "NOME_DIGITADO",
  "textoOrigem": "Nome completo opcional para acessibilidade"
}
```

Na rota `/me`, o usuário deve ser obtido do JWT. O mobile não deve enviar um `usuarioId` manipulável. A rota com `{motoristaId}` deve aceitar somente administradores autorizados e validar que o usuário informado possui o papel de motorista.

Não deve existir upload público sem autenticação. “Antes do primeiro login” significa que o administrador consegue cadastrar a assinatura junto ao motorista; não significa permitir que um cliente anônimo altere esse dado.

Resposta sugerida:

```json
{
  "id": "uuid",
  "usuarioId": 5,
  "modalidade": "NOME_DIGITADO",
  "mimeType": "image/png",
  "tamanhoBytes": 18342,
  "sha256": "hash-sha-256",
  "conteudoUrl": "/api/v1/usuarios/me/assinatura/conteudo",
  "atualizadoEm": "2026-10-02T12:00:00Z",
  "versao": 0
}
```

Rotas complementares:

```http
GET /api/v1/usuarios/me/assinatura
GET /api/v1/usuarios/me/assinatura/conteudo
GET /api/v1/usuarios/{usuarioId}/assinatura
GET /api/v1/usuarios/{usuarioId}/assinatura/conteudo
```

As rotas com `{usuarioId}` devem ser restritas aos perfis administrativos autorizados.

## Reflexo no login e no primeiro acesso

O cadastro da assinatura pode acontecer antes do primeiro login, mas sua consulta pelo aplicativo continua autenticada. Depois que o login retornar sucesso, o mobile deve consultar `GET /api/v1/usuarios/me/assinatura`:

- `200`: o motorista já possui assinatura fixa; o app pode mostrar a prévia e seguir o fluxo definido pelo produto;
- `404`: ainda não existe assinatura; o app abre o fluxo de desenho ou nome digitado;
- `401`: sessão inválida ou expirada;
- `403`: o usuário autenticado não é um motorista autorizado para esse fluxo.

Como alternativa para reduzir uma chamada, a resposta de login pode incluir apenas metadados, nunca o PNG em Base64:

```json
{
  "token": "jwt",
  "tokenType": "Bearer",
  "usuario": {
    "id": 5,
    "nome": "Motorista",
    "tipo": "motorista",
    "assinaturaFixaCadastrada": true,
    "assinaturaFixaId": "uuid"
  }
}
```

Mesmo nessa alternativa, o conteúdo deve ser carregado separadamente pelo endpoint autenticado quando a interface realmente precisar exibi-lo.

## Migração Flyway sugerida

Como o requisito é armazenar a imagem no banco, criar uma nova migration, atualmente `V7__create_assinatura_motorista.sql`, sem alterar migrations já aplicadas. A tabela deve permitir versionamento, preservando as assinaturas anteriormente usadas em formulários.

Estrutura sugerida:

```sql
CREATE TABLE public.assinatura_motorista (
    id UUID PRIMARY KEY,
    motorista_id INTEGER NOT NULL,
    modalidade VARCHAR(30) NOT NULL,
    texto_origem VARCHAR(150),
    mime_type VARCHAR(50) NOT NULL,
    conteudo BYTEA NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    idempotency_key UUID NOT NULL UNIQUE,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por INTEGER NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_assinatura_motorista
        FOREIGN KEY (motorista_id) REFERENCES public.usuario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_assinatura_motorista_criado_por
        FOREIGN KEY (criado_por) REFERENCES public.usuario(id) ON DELETE RESTRICT,
    CONSTRAINT chk_assinatura_modalidade
        CHECK (modalidade IN ('DESENHO', 'NOME_DIGITADO')),
    CONSTRAINT chk_assinatura_mime_type
        CHECK (mime_type = 'image/png'),
    CONSTRAINT chk_assinatura_tamanho
        CHECK (tamanho_bytes > 0),
    CONSTRAINT chk_assinatura_sha256
        CHECK (sha256 ~ '^[0-9A-Fa-f]{64}$')
);

CREATE UNIQUE INDEX uq_assinatura_motorista_ativa
    ON public.assinatura_motorista(motorista_id)
    WHERE ativa = TRUE;
```

Ao trocar a assinatura, a API deve desativar a versão atual e inserir uma nova linha em uma única transação. O conteúdo de uma versão já utilizada não deve ser sobrescrito.

## Relação com formulários e relatórios

Quando um formulário utilizar a assinatura fixa do motorista, ele deve apontar para uma versão imutável por `assinatura_motorista_id` ou copiar essa versão para o documento final. A referência nunca deve ser apenas `motorista_id`, pois isso faria um formulário antigo passar a exibir a assinatura nova depois de uma atualização de perfil.

Sugestão para o vínculo:

```sql
ALTER TABLE public.documento
    ADD COLUMN assinatura_motorista_id UUID NULL,
    ADD CONSTRAINT fk_documento_assinatura_motorista
        FOREIGN KEY (assinatura_motorista_id)
        REFERENCES public.assinatura_motorista(id)
        ON DELETE RESTRICT;
```

Se o formulário assinado for materializado em outro agregado, esse campo deve ser adicionado à tabela correspondente. Para documentos com exigência jurídica ou de auditoria mais forte, a opção mais segura é gerar um snapshot imutável do formulário e da assinatura no momento da finalização.

Se o time decidir manter o binário fora do PostgreSQL, o contrato pode conservar `storage_key`, mas isso diverge do requisito atual de persistência da imagem no banco e precisa ser validado com o produto.

## Regras de validação

- aceitar somente `image/png` neste fluxo;
- validar a assinatura real do arquivo, e não apenas o `Content-Type` informado pelo cliente;
- limitar o tamanho do PNG, com sugestão inicial de 1 MB;
- calcular `SHA-256` no servidor;
- rejeitar imagem vazia ou com dimensões inválidas;
- substituir a assinatura anterior do próprio usuário de forma transacional;
- manter versões anteriores imutáveis enquanto forem referenciadas por formulários;
- respeitar `Idempotency-Key` para evitar duplicidade em redes móveis instáveis;
- nunca retornar o campo `BYTEA` em JSON ou Base64;
- transmitir o conteúdo pelo endpoint autenticado com `Content-Type: image/png`, `Content-Disposition: inline` e `Cache-Control: private, no-store`.

## Ajustes necessários no mobile após o contrato estar disponível

- gerar PNG tanto para o desenho quanto para a prévia do nome;
- usar fundo transparente e traço escuro para funcionar no formulário web claro;
- enviar `modalidade=DESENHO` ou `modalidade=NOME_DIGITADO`;
- incluir JWT e um UUID em `Idempotency-Key`;
- só abrir a `MainADMActivity` após o upload retornar sucesso;
- apresentar opção de tentar novamente em caso de falha de rede;
- não transportar o PNG em `Intent`, evitando o limite de tamanho do Binder.

## Critérios de aceite do backend

1. Uma assinatura de primeiro acesso pode ser criada sem `viagemId`.
2. Um administrador pode cadastrar a assinatura fixa no perfil do motorista antes do primeiro login.
3. Na rota `/me`, o usuário é derivado do JWT.
4. Desenho e nome digitado são recebidos como PNG.
5. O conteúdo fica persistido no PostgreSQL em `BYTEA`, conforme o requisito atual.
6. O administrador autorizado consegue consultar e visualizar a assinatura no formulário web.
7. Usuários comuns não conseguem consultar assinaturas de terceiros.
8. Uma troca da assinatura fixa não altera formulários antigos.
9. Repetir a mesma requisição com a mesma `Idempotency-Key` não cria duplicidade.
10. O contrato e os códigos `400`, `401`, `403`, `413`, `415` e `422` estão documentados no OpenAPI.
11. Existem testes de integração para upload administrativo, upload próprio, substituição, histórico, consulta, autorização, idempotência e PNG inválido.
