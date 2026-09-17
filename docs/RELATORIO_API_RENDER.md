# Relatório de integração do mobile com a API no Render

Data da validação: 15/09/2026

API analisada: `https://efficientia-api.onrender.com/`

Repositório analisado: `https://github.com/Efficientia/Efficientia-API`

## Resultado dos testes públicos

- `GET /actuator/health`: `200 OK`, banco PostgreSQL, storage, liveness e readiness com status `UP`.
- `GET /api/v1/status`: `200 OK`, serviço `efficientia`, versão `1.0.0`.
- `GET /swagger-ui.html`: `302` para `/swagger-ui/index.html`.
- `GET /v3/api-docs`: `200 OK`.
- `POST /api/v1/auth/login` com usuário inexistente: `401` e `application/problem+json`, corretamente interpretado pelo mobile.
- `GET /api/v1/documentos` sem JWT: `200 OK` (falha de segurança).
- `GET /api/v1/relatorios-viagem` sem JWT: `200 OK` (falha de segurança).

Não foi criado usuário de teste no banco público. Portanto, o caminho de sucesso do login ainda precisa ser validado com uma credencial controlada do ambiente Render.

## Contrato usado pelo mobile

O app envia `POST /api/v1/auth/login` em JSON com:

```json
{
  "cpf": "somente 11 dígitos",
  "email": "email válido",
  "senha": "senha do usuário",
  "codigoEmpresa": "código informado na tela"
}
```

O app espera `token`, `tokenType` e `usuario`. O contrato publicado no OpenAPI e os DTOs do repositório possuem esses mesmos campos.

## Alterações necessárias na API — Mateus

### P0 — Ativar proteção das rotas no Render

O Render está executando com `SECURITY_ENABLED=false`, pois `/api/v1/documentos` e `/api/v1/relatorios-viagem` responderam sem `Authorization`. Configurar produção para negar por padrão e liberar apenas health, status, Swagger e autenticação.

Ao ativar, incluir explicitamente `/api/v1/status` em `permitAll()`, pois hoje ele cai em `anyRequest().authenticated()`.

### P0 — Unificar emissão e validação do JWT

`JwtTokenService` emite tokens `HS256` com segredo local, mas `SecurityConfig` usa `NimbusJwtDecoder.withJwkSetUri(...)`, voltado ao JWKS de outro emissor. O token devolvido por `/auth/login` não será aceito pelas rotas protegidas quando a segurança for ligada.

Escolher uma única estratégia:

1. manter HS256 e validar com `NimbusJwtDecoder.withSecretKey(...)`, usando `JWT_SECRET` forte no Render; ou
2. emitir RS256 com chave privada e publicar/configurar o JWKS correspondente.

Também alinhar `iss` e `aud`: o token atual define `iss=efficientia-api`, mas o resource server pode exigir `JWT_ISSUER_URI` e sempre valida `JWT_AUDIENCE`.

Remover o segredo padrão versionado. Em produção, a aplicação deve falhar ao iniciar quando `JWT_SECRET` não estiver definido.

### P0 — Corrigir as roles

O domínio permite `motorista`, `manobrista`, `analista`, `pecuarista` e `curraleiro`. O `JwtRoleConverter` aceita apenas `MOTORISTA`, `FUNCIONARIO_FRIBOI` e `ADMIN`. Depois de ativar segurança, somente motorista terá alguma role reconhecida; as demais serão descartadas.

Definir uma matriz de autorização coerente com `TipoUsuario` e cobri-la com testes de integração usando os tokens realmente emitidos pelo login.

### P1 — Proteger todos os cadastros e relatórios

Além da segurança estar desligada no ambiente, a configuração atual usa `anyRequest().authenticated()` para cadastros e relatórios, sem autorização por papel. Definir quem pode criar usuários, endereços, fazendas, veículos e relatórios. Cadastro de usuário não deve permitir elevação de privilégio ou criação irrestrita de perfis operacionais.

### P1 — Modelar corretamente o código da empresa

O login recebe `codigoEmpresa`, mas compara esse valor com `usuario.codigoInterno`. Se `codigoInterno` estiver vazio, qualquer código de empresa é aceito. Criar vínculo explícito usuário–empresa e validar o código contra a empresa ativa do usuário.

### P1 — Evitar enumeração de usuários

O `401` diferencia usuário inexistente, CPF divergente, e-mail divergente, código incorreto, usuário inativo e senha incorreta. Para o cliente, responder uma mensagem genérica de credenciais inválidas e registrar o motivo detalhado apenas nos logs internos.

### P2 — Melhorar o contrato OpenAPI

O OpenAPI marca documentos/exportações com JWT, mas não reflete de forma consistente a segurança efetiva de cadastros e relatórios. Documentar respostas `401` e `403`, esquemas de erro `application/problem+json`, roles permitidas e exemplos de `Idempotency-Key`.

### P2 — Teste de integração obrigatório no CI

Adicionar um teste que execute o fluxo completo: signup controlado, login, acesso com Bearer válido, acesso sem token (`401`), role insuficiente (`403`) e token expirado/inválido (`401`). Isso detectará automaticamente a incompatibilidade HS256/JWKS.

## Ajustes aplicados no mobile

- Base URL padrão HTTPS alterada para `https://efficientia-api.onrender.com/`.
- Removida a configuração de cleartext/localhost.
- Timeouts ampliados para suportar inicialização fria do Render.
- Respostas `502`, `503`, `504`, timeout e resposta de login sem token agora geram mensagens específicas.
- Token e dados básicos do usuário são persistidos; foi incluído gerador do cabeçalho `Authorization` para as próximas telas.
- Backup do app foi desativado para não incluir a sessão persistida em backups do Android.
