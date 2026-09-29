# Relatório para o backend — Home de primeiro acesso

Data: 28/09/2026

## Contexto

Após o login do motorista, o mobile agora exibe uma splash autenticada por 3 segundos e abre a tela “Home — Primeiro acesso”. Essa tela apresenta o botão “Verificar minha assinatura”, mas a API atualmente documentada não oferece um endpoint para consultar a situação da assinatura do usuário autenticado.

## Alteração necessária na API

Disponibilizar uma rota autenticada para consultar a assinatura do próprio usuário. Sugestão de contrato:

```http
GET /api/v1/assinaturas/minha
Authorization: Bearer <token>
Accept: application/json
```

Resposta sugerida:

```json
{
  "status": "ATIVA",
  "plano": "MOTORISTA",
  "inicioVigencia": "2026-09-01",
  "fimVigencia": "2027-09-01",
  "recursosLiberados": true
}
```

Estados esperados: `PENDENTE`, `ATIVA`, `BLOQUEADA` e `EXPIRADA`.

O usuário deve ser identificado pelo `sub`/ID presente no JWT. O mobile não deve enviar um ID de usuário livremente manipulável para consultar a assinatura.

## Respostas esperadas

- `200`: situação da assinatura retornada.
- `401`: token ausente, inválido ou expirado.
- `403`: usuário sem permissão para o recurso.
- `404`: assinatura ainda não cadastrada.
- `503`: serviço de assinatura temporariamente indisponível.

## Dados adicionais para a Home

O layout mostra cidade e estado do motorista. Atualmente o mobile utiliza o texto temporário “São Paulo, SP”. A API deve retornar a localização vinculada ao perfil autenticado, seja na resposta do login, em `GET /api/v1/usuarios/me` ou em outro contrato de perfil definido pelo time.

## Comportamento temporário no mobile

Até a API disponibilizar o endpoint, o botão exibe a mensagem “Verificação de assinatura será integrada em breve” e não libera recursos adicionais.

## Critérios de aceite do backend

1. A rota exige JWT válido.
2. O usuário é derivado do token.
3. A resposta utiliza um enum estável para o status.
4. Há testes para assinatura ativa, pendente, inexistente e token inválido.
5. O contrato é publicado no OpenAPI/Swagger.
