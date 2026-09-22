# Relatório de trabalho — 22/09/2026

## Objetivo

Validar o login de motorista do aplicativo Android usando exclusivamente a API publicada no Render e preparar dados de homologação para teste manual no Android Studio.

## Ambiente validado

- Mobile: `Efficientia-mobile`
- API: `https://efficientia-api.onrender.com/`
- Login: `POST /api/v1/auth/login`
- Cadastro usado para homologação: `POST /api/v1/auth/signup`

Nenhuma API foi executada em localhost durante este trabalho.

## Atividades realizadas

1. Confirmado que o mobile usa a URL do Render definida em `app/build.gradle.kts`.
2. Executado o health check da API no Render.
3. Criado um usuário sintético do tipo `motorista` no ambiente público de homologação.
4. Executado o login real com esse usuário.
5. Confirmado o retorno de um JWT com `tokenType=Bearer`.
6. Confirmado no mobile que o sucesso do login:
   - persiste o token e os dados do usuário com `SessionManager`;
   - limpa a pilha das telas de autenticação;
   - abre a `MainActivity`.
7. Aumentado o timeout de leitura do cliente de 60 para 120 segundos devido à inicialização fria observada no Render.
8. Atualizado o relatório técnico destinado ao responsável pela API.

## Resultado dos testes

| Teste | Resultado |
|---|---|
| Health check do Render | `200 OK`, status `UP` |
| Cadastro do motorista de homologação | Criado com sucesso, ID `5` |
| Login com CPF, e-mail, senha e código da empresa | `200 OK` |
| Tipo do token | `Bearer` |
| JWT retornado | Presente |
| Tipo de usuário retornado | `motorista` |
| Código interno retornado | `EMP-MOBILE-001` |
| Destino após sucesso no app | `MainActivity` |

## Dados para testar no Android Studio

- CPF: `529.982.247-25`
- E-mail: `motorista.mobile.teste@efficientia.app`
- Senha: `TesteMobile@123`
- Código da empresa: `EMP-MOBILE-001`

Esse cadastro contém somente dados sintéticos e deve ser usado apenas para homologação. Depois dos testes, o responsável pela API deve remover ou desativar o usuário ID `5` no banco do ambiente.

## Arquivos atualizados hoje

- `app/src/main/java/com/inter/efficientia_mobile/network/ApiClient.java`
  - timeout de leitura alterado para 120 segundos.
- `docs/RELATORIO_API_RENDER.md`
  - registrado o teste de login bem-sucedido;
  - registrada a inicialização fria superior a 90 segundos;
  - incluída recomendação de infraestrutura para o Render.
- `docs/RELATORIO_TRABALHO_2026-09-22.md`
  - criado este relatório diário.

## Observações para o responsável pela API

Durante o primeiro acesso, o Render levou mais de 90 segundos para responder. Após a instância ficar ativa, signup e login responderam em poucos segundos. O mobile agora tolera até 120 segundos, mas a solução de produção recomendada é evitar que o serviço entre em suspensão.

As demais correções necessárias no backend — segurança desativada, incompatibilidade entre JWT HS256 e JWKS, matriz de roles e validação do código da empresa — continuam documentadas em `docs/RELATORIO_API_RENDER.md`.

## Situação final

O login foi validado diretamente contra o Render. O projeto pode ser executado no Android Studio e testado com as credenciais acima; ao receber a resposta válida, deve navegar para a `MainActivity`.
