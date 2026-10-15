# HTTP 400 no fluxo completo do diário de rota

**Para:** Mateus (Efficientia-API)
**Data:** 15/10/2026
**Ambiente:** Mobile Android → `https://efficientia-api.onrender.com/`

## Problema

O **HTTP 400 não ocorre só na criação**. Foi relatado ao avançar etapas e ao editar diários existentes. O fluxo mobile usa:

- `POST /api/v1/relatorios-viagem` com `status: "rascunho"` quando a rota ainda não tem ID no servidor;
- `PUT /api/v1/relatorios-viagem/{id}` a cada avanço após a criação, para atualizar o mesmo rascunho;
- o mesmo `PUT` ao editar um diário aberto pela home; nessa situação, o mobile pode enviar `status: "pendente"` e não envia `Idempotency-Key` no salvamento intermediário;
- `PUT /api/v1/relatorios-viagem/{id}` com `status: "pendente"` e `Idempotency-Key` no lançamento final de um diário novo.

É necessário validar **criação, avanço entre todas as etapas, retomada e edição**, sem tratar o erro como apenas do primeiro POST. Nenhuma etapa deve ser considerada sincronizada antes da resposta de sucesso da API.

Exemplo dos dados informados no teste: GTA `6767a`, nota fiscal `56767`, origem `Fazenda Do Osmar`, destino `Fazenda Do Noé`, embarque `11/09/2026 08:44`, saída `10:12`, km de saída `67`. Origem e destino foram **digitados**, então o mobile envia `nomeOrigem` e `nomeDestino`, sem `fazendaId` e `unidadeFrigorificaId`. A data vai em ISO (`2026-09-11`) e os horários com segundos (`08:44:00`, `10:12:00`).

## Evidência da divergência

- Em 14/10, o OpenAPI **publicado no Render** respondeu HTTP 200, mas o schema `CriarRelatorioViagemRequest` não continha `nomeOrigem`/`nomeDestino`. Também não havia `GET /api/v1/relatorios-viagem/me` nem `/api/v1/locais-diario/fazendas`.
- A `main` da Efficientia-API, commit `d938d17` (merge da PR #43), contém esses campos, aceita nomes digitados sem IDs e permite rascunhos parciais. A migration `V15__rascunhos_diario_rota_parciais.sql` remove `NOT NULL` dos campos ainda não preenchidos e adiciona `nome_origem`/`nome_destino`.
- O serviço publicado foi consultado novamente em 15/10, mas `/v3/api-docs` não respondeu dentro de 55 segundos. Portanto, a versão **atual neste instante** ainda precisa ser confirmada.
- Pela validação da `main`, GTA alfanumérica e os horários acima não são, por si, causa de rejeição. O contrato antigo publicado é uma explicação provável para a falha inicial com locais digitados, **mas não comprova a causa de todos os 400 nas atualizações e edições**.
- Há também uma divergência a analisar no mobile: ao editar um diário marcado como `pendente`, o salvamento intermediário envia `status: "pendente"` sem `Idempotency-Key`. A API exige essa chave para submissão final, salvo quando o relatório já possui uma chave vinculada. O corpo completo do 400 e o estado do relatório são necessários para confirmar se é esse o caso. Esse ajuste, se confirmado, é responsabilidade do mobile.

## Ação solicitada

1. Conferir no Render qual commit está implantado e publicar a `main` da API contendo a PR #43 e a migration V15. Verificar que o Flyway concluiu sem erro.
2. Conferir em `GET /v3/api-docs` que `CriarRelatorioViagemRequest` inclui `nomeOrigem` e `nomeDestino`, e que existem `/api/v1/relatorios-viagem/me` e `/api/v1/locais-diario/fazendas`.
3. Com um JWT de motorista de teste, reproduzir o `POST /api/v1/relatorios-viagem` como rascunho parcial, com os dois nomes digitados e sem IDs. Esperado: **HTTP 201** e resposta com `id` e `status: "rascunho"`.
4. Atualizar esse mesmo ID por `PUT` com os dados de cada etapa, mantendo `status: "rascunho"`. Esperado: sucesso em cada avanço, sem criar outro relatório e sem exigir dados das etapas futuras.
5. Reabrir o diário pela listagem, editar dados e salvar por `PUT`. Testar separadamente um diário `rascunho` e um `pendente`, documentando a regra de `Idempotency-Key` e se a edição de `pendente` é permitida sem nova submissão.
6. Se algum passo retornar 400, enviar **método, URL, status anterior do diário, corpo da requisição sem credenciais, corpo completo da resposta, log correspondente no Render e commit implantado**. Verificar validação, schema e constraints do banco. Evitar criar múltiplos relatórios de teste com a mesma GTA.

## Mobile

O app já envia `nomeOrigem`/`nomeDestino` e não envia IDs quando os locais são digitados. O tratamento de erro foi ajustado para exibir mais detalhes da resposta e **não avançar** até a API confirmar o salvamento. A edição de `pendente` e o envio de `Idempotency-Key` nos `PUT` intermediários precisam ser revisados no mobile em conjunto com o contrato confirmado pela API. Build e testes unitários passaram; essas alterações do mobile ainda estão locais, sem commit.
