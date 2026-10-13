# Pendências da API para concluir diário de rota e home mobile

Em 13/10/2026, conferimos o contrato da `main` da Efficientia-API (commit `88b4b4c`). Os ajustes descritos abaixo foram implementados na branch de trabalho `feat/dev/EFFICIENTI-63/api-diario-mobile` da API e no mobile local, mas dependem de merge e implantação para funcionar no aparelho.

## 1. Seleção de origem e destino

O formulário recebe nomes de fazenda e destino como texto. `POST/PUT /api/v1/relatorios-viagem` recebe `fazendaId` e `unidadeFrigorificaId`; a tabela `relatorio_viagem.fazenda_id` é `NOT NULL`. A API atual não expõe uma consulta de fazendas e unidades frigoríficas disponíveis para o motorista/empresa.

Implementado: endpoints autenticados e paginados em `/api/v1/locais-diario/fazendas` e `/unidades-frigorificas`, com `id`, `nome`, `cidade` e `uf`. O mobile usa um seletor e guarda os IDs reais. V15 permite rascunhos sem IDs e preserva `nomeOrigem`/`nomeDestino`; o envio final continua exigindo IDs. A modelagem ainda não vincula necessariamente fazendas à empresa transportadora, portanto o catálogo não é filtrado por empresa.

## 2. Listagem de relatórios da conta

`GET /api/v1/relatorios-viagem` usa `repository.findAll(pageable)` e retorna relatórios de todos os motoristas aos perfis autorizados. Filtrar só no Android ainda transferiria dados de outras contas para o aparelho.

Implementado: `/api/v1/relatorios-viagem/me`, listagem geral filtrada para `ROLE_MOTORISTA` e detalhe que devolve 404 para relatório de outro motorista. A home consulta `/me` para exibir os quatro mais recentes, inclusive em outro dispositivo após implantação.

## 3. Paradas e anomalias

A tela atual aceita motivo e descrição livres. A API exige códigos específicos em `paradasImprevistas[].motivo` e `anomaliasEmbarque/Desembarque[].anomalia`; paradas exigem data e hora completas, mas a tela só coleta horários. O mobile não deve eliminar silenciosamente esses dados.

Implementado: `descricaoMotivo` nas paradas; o mobile envia `motivo: outro` com o texto, data e horários completos. Anomalia livre de embarque usa `outro`, `descricaoOutros` e `quantidadeAnimais`. A tela atual admite uma parada e uma anomalia; múltiplas ocorrências exigem expansão posterior da interface.

## Aceite conjunto

1. Motorista seleciona fazenda/destino sem digitar IDs e consegue salvar rascunho após a primeira etapa.
2. Etapas seguintes atualizam o mesmo relatório, sem duplicar GTA. A retomada de edição em outro dispositivo ainda exige reconstruir as telas a partir do GET do rascunho; nesta entrega, o histórico é visível na home, mas a edição cruzada não foi implementada.
3. As três assinaturas participantes chegam como PNG e a assinatura fixa do motorista é vinculada pela API.
4. `Idempotency-Key` repetida não cria segundo relatório; erro de campo não apresenta splash de sucesso.
5. A home de outro dispositivo mostra apenas relatórios do motorista autenticado, sem receber registros de terceiros.
