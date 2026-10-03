# Relatório para o backend — Ações dos relatórios na Home

Data: 02/10/2026

## Contexto

A Home do motorista apresenta os relatórios e rotas recentes. Cada card possui um menu contextual com as ações:

- salvar o formulário como PDF;
- editar as informações;
- excluir o relatório.

O layout e o comportamento visual foram implementados no mobile, mas os cards ainda utilizam dados demonstrativos e não possuem identificadores retornados pela API.

## Dados necessários na listagem

O endpoint de relatórios recentes deve retornar, no mínimo:

```json
{
  "id": 123,
  "versao": 2,
  "dataHora": "2026-05-10T11:36:00-03:00",
  "origem": "Uberaba",
  "ufOrigem": "MG",
  "destino": "Cascavel",
  "ufDestino": "PR",
  "status": "RASCUNHO",
  "podeEditar": true,
  "podeExcluir": true,
  "pdfDisponivel": true
}
```

As permissões devem ser calculadas no servidor. O mobile não deve decidir sozinho se um relatório pode ser editado ou excluído.

## PDF do formulário

Sugestão de endpoint síncrono para relatórios pequenos:

```http
GET /api/v1/relatorios-viagem/{id}/pdf
Authorization: Bearer <token>
Accept: application/pdf
```

Resposta esperada:

- `200 application/pdf` com `Content-Disposition: attachment`;
- `202` caso a geração seja assíncrona, retornando o ID da exportação;
- `404` para relatório inexistente;
- `409` se o relatório ainda não puder gerar um documento final;
- `403` quando o usuário não possuir acesso.

Se o PDF já for tratado pelo domínio de documentos/exportações, a resposta do relatório deve informar o `documentoId` ou a URL autenticada correta, evitando que o mobile tente descobrir essa associação.

## Edição

Sugestão:

```http
PATCH /api/v1/relatorios-viagem/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

O corpo deve conter somente os campos editáveis e a propriedade `versao` para controle de concorrência otimista. Relatórios finalizados, auditados ou bloqueados devem retornar `409` ou `422`, conforme a convenção da API.

## Exclusão

Sugestão:

```http
DELETE /api/v1/relatorios-viagem/{id}
Authorization: Bearer <token>
```

Regras recomendadas:

- permitir exclusão somente para rascunhos, salvo regra administrativa explícita;
- verificar se o relatório pertence ao motorista autenticado ou se o usuário possui perfil administrativo autorizado;
- preferir exclusão lógica quando o registro participar de auditoria, documentos ou assinaturas;
- excluir ou preservar documentos relacionados conforme regra transacional documentada;
- retornar `204` somente após a operação persistir com sucesso.

## Segurança e auditoria

- Derivar o usuário do JWT.
- Impedir acesso por enumeração de IDs.
- Auditar geração de PDF, edição e exclusão.
- Não expor URLs públicas permanentes para documentos privados.
- Manter as assinaturas e versões utilizadas em formulários finalizados imutáveis.

## Critérios de aceite

1. A listagem retorna IDs, versão, status e permissões por relatório.
2. O motorista acessa somente relatórios autorizados.
3. O PDF é baixado com tipo e nome de arquivo corretos.
4. A edição valida versão e estado do relatório.
5. A exclusão possui autorização e regra de integridade.
6. As respostas `401`, `403`, `404`, `409` e `422` estão documentadas no OpenAPI.
7. Existem testes de integração para propriedade, permissões, concorrência, exclusão e download.
