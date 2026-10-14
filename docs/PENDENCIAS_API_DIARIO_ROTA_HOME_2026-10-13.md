# Pendências da API para concluir diário de rota e home mobile

Em 13/10/2026, os endpoints do diário e a migration V15 foram incorporados à API. A entrada livre de origem e destino requer um complemento de validação na API antes de funcionar no envio final.

## 1. Seleção de origem e destino

O formulário permite digitar os nomes de origem e destino ou buscar locais cadastrados. Os IDs são enviados apenas quando houve seleção no catálogo.

Implementado: endpoints autenticados e paginados em `/api/v1/locais-diario/fazendas` e `/unidades-frigorificas`, com `id`, `nome`, `cidade` e `uf`. O complemento da API aceita `nomeOrigem`/`nomeDestino` sem IDs no envio final; nomes livres ficam no relatório, sem criar cadastro mestre. A modelagem ainda não vincula necessariamente fazendas à empresa transportadora, portanto o catálogo não é filtrado por empresa.

## 2. Listagem de relatórios da conta

`GET /api/v1/relatorios-viagem` usa `repository.findAll(pageable)` e retorna relatórios de todos os motoristas aos perfis autorizados. Filtrar só no Android ainda transferiria dados de outras contas para o aparelho.

Implementado: `/api/v1/relatorios-viagem/me`, listagem geral filtrada para `ROLE_MOTORISTA` e detalhe que devolve 404 para relatório de outro motorista. A home consulta `/me` para exibir os quatro mais recentes, inclusive em outro dispositivo após implantação.

## 3. Paradas e anomalias

A tela atual aceita motivo e descrição livres. A API exige códigos específicos em `paradasImprevistas[].motivo` e `anomaliasEmbarque/Desembarque[].anomalia`; paradas exigem data e hora completas, mas a tela só coleta horários. O mobile não deve eliminar silenciosamente esses dados.

Implementado: `descricaoMotivo` nas paradas; o mobile envia `motivo: outro` com o texto, data e horários completos. Anomalia livre de embarque usa `outro`, `descricaoOutros` e `quantidadeAnimais`. A tela atual admite uma parada e uma anomalia; múltiplas ocorrências exigem expansão posterior da interface.

## Aceite conjunto

1. Motorista pode digitar origem/destino ou selecioná-los na busca sem digitar IDs e consegue salvar rascunho após a primeira etapa.
2. Etapas seguintes atualizam o mesmo relatório, sem duplicar GTA. Pela home, o motorista pode reabrir um rascunho da API em outro aparelho e editar as informações disponíveis nas telas atuais.
3. As três assinaturas participantes chegam como PNG e a assinatura fixa do motorista é vinculada pela API.
4. `Idempotency-Key` repetida não cria segundo relatório; erro de campo não apresenta splash de sucesso.
5. A home de outro dispositivo mostra apenas relatórios do motorista autenticado, sem receber registros de terceiros.
