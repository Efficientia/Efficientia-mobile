# Relatório para integração do diário de rota

O fluxo mobile possui seis etapas prontas no front-end. Para concluir a integração, a API deverá receber e devolver todos os dados do formulário em `POST /api/v1/relatorios-viagem` e nas rotas de consulta/edição.

## Campos necessários

- Documentos: GTA, nota fiscal, fazenda/propriedade de origem e unidade frigorífica de destino.
- Embarque e chegada: datas, horários, quilômetros de saída/chegada, horário de desembarque, curral e funcionamento da sirene de ré.
- Animais: totais por sexo/categoria, em pé, deitados, mortos e em emergência.
- Ocorrências: motivo e comentários de incidente, lista de paradas imprevistas com início/fim e anomalias com animais envolvidos.
- Assinaturas: pecuarista, motorista, manobrista e curraleiro, incluindo referência ao arquivo de imagem e responsável.
- Metadados: motorista, empresa, rota, status, data de criação/atualização e duração calculada no servidor.

## Recomendações para a API

- Relacionar motorista e empresa pelo JWT, sem confiar em identificadores enviados livremente pelo app.
- Aceitar salvamento como rascunho e submissão final, permitindo retomada do fluxo.
- Validar coerência de horários, quilometragem e totais de animais.
- Aceitar `Idempotency-Key` no lançamento final para evitar duplicidade em redes instáveis.
- Devolver erros por campo em formato estável para exibição no aplicativo.
- Manter as assinaturas fixas como atributo do motorista e uma cópia/referência imutável no relatório lançado.
- Documentar o contrato atualizado no OpenAPI/Swagger com exemplos de request e response.

Nenhuma alteração de backend foi realizada neste trabalho; este documento registra somente os requisitos identificados no front-end.
