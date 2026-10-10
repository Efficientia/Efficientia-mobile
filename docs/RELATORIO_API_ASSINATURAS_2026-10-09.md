# Integração de assinaturas — 09/10/2026

## Implementado no mobile

- A assinatura fixa do motorista (desenhada ou nome digitado convertido em PNG) é enviada ao Render por `PUT /api/v1/usuarios/me/assinatura` com JWT, `Idempotency-Key`, `arquivo` e `metadados`.
- O login lê `usuario.assinaturaFixaCadastrada`: após a splash, quando a flag é `true`, o app confirma a existência do PNG com `GET /api/v1/usuarios/me/assinatura/conteudo` antes de ir à home principal. Sem PNG confirmado (inclusive erro/timeout), continua no primeiro acesso para não pular a assinatura por uma flag inconsistente. A flag local é atualizada após upload confirmado.
- O app só confirma o cadastro e avança para a home depois de receber sucesso da API. Em falha, mantém a tela aberta para nova tentativa com a mesma chave de idempotência para o mesmo conteúdo.
- A etapa de assinaturas do diário pode recuperar o PNG fixo do motorista em `GET /api/v1/usuarios/me/assinatura/conteudo` quando não existe cópia local.
- Para testar com o banco, o login do motorista agora exige os dados reais também no build de desenvolvimento; o atalho sem JWT foi removido.

## Pendência para a API — assinaturas dos demais participantes

Após um `500` no upload da assinatura fixa, o login de homologação passou a encaminhar o motorista diretamente à home porque `assinaturaFixaCadastrada` veio como `true`. O mobile agora confirma o PNG via `GET /api/v1/usuarios/me/assinatura/conteudo` antes de aceitar essa flag. Se o backend retornar `true` no login, mas o conteúdo retornar `404` ou falhar, Mateus deve conferir a transação do upload, a assinatura ativa do usuário e a consistência entre a flag e o endpoint de conteúdo. Um `500` não deve deixar persistência parcial sem resposta clara ao cliente.

No commit `bf46f39735f4d5add5d8bd9c2b09a9d82f9bdc9c` da Efficientia-API, `PATCH /api/v1/relatorios-viagem/{id}/assinaturas` recebe apenas strings `urlAssinaturaPecuarista`, `urlAssinaturaManobrista` e `urlAssinaturaCurraleiro`. O serviço persiste essas strings; ele não recebe nem armazena os bytes das imagens. O app hoje tem essas assinaturas apenas em arquivos privados locais, cujos caminhos não são URLs acessíveis ao servidor ou ao administrador web. Enviar esses caminhos ao relatório criaria referências inválidas.

Solicitamos ao Mateus um contrato para upload binário dessas três assinaturas, vinculado ao relatório e ao papel: por exemplo `PUT /api/v1/relatorios-viagem/{id}/assinaturas/{papel}` com JWT, `Idempotency-Key`, multipart `arquivo` PNG e metadados da modalidade/nome digitado. A API deve validar permissão do motorista vinculado, PNG e tamanho, salvar os bytes no banco/storage gerenciado, devolver ID/URL de conteúdo autenticado e expor consulta/download para o administrador web. A finalização do relatório deve validar essas assinaturas persistidas, não apenas URLs fornecidas pelo cliente.

Até esse contrato existir, **as assinaturas de pecuarista, manobrista e curraleiro não estão salvas no banco** e o lançamento do diário ainda não deve ser apresentado como persistência completa. O fluxo de relatório do app segue visual/local nesta branch.

## Atualização: PR #37 da API

A [PR #37](https://github.com/Efficientia/Efficientia-API/pull/37) foi mesclada à `main` da API em 09/10/2026 e resolve a lacuna de upload descrita acima no código da API. Ela disponibiliza `PUT /api/v1/relatorios-viagem/{id}/assinaturas/{papel}` para `pecuarista`, `motorista`, `manobrista` e `curraleiro`, com JWT e multipart `arquivo` (PNG autêntico, até 1 MB) e `metadados` opcionais. `Idempotency-Key` UUID é recomendado. O PNG pode ser consultado em `GET /api/v1/relatorios-viagem/{id}/assinaturas/{papel}/conteudo`. O merge não comprova que a versão já está implantada no Render.

No mobile, os três participantes da viagem ainda ficam somente em arquivos locais e o botão de lançamento ainda exibe sucesso sem persistência. A sequência necessária é: criar o relatório como rascunho e guardar seu ID; fazer upload dos três PNGs por papel; submeter o relatório com `Idempotency-Key` estável; exibir sucesso apenas após confirmação da API. O corpo do relatório precisa mapear as etapas do formulário e resolver os IDs de fazenda e unidade frigorífica exigidos pelo contrato. Não enviar caminhos locais como URLs.

## Validação pendente

### HTTP 409 ao substituir a assinatura fixa

Em 09/10/2026, o teste no app recebeu `409` no `PUT /api/v1/usuarios/me/assinatura` após desenhar uma nova assinatura. A documentação da API afirma que o endpoint cadastra **ou substitui** a assinatura; o serviço em `main` consulta a chave de idempotência, desativa a versão anterior e insere uma nova versão ativa. Portanto, uma assinatura preexistente, por si só, não explica o `409`. O mobile passou a exibir o `detail`/`message` da resposta e a usar uma nova UUID na próxima ação do usuário após `409`. É preciso repetir o teste e registrar a mensagem detalhada, além de conferir os logs do Render. Se houver conflito de índice único da versão ativa, verificar a ordem de flush entre desativação e inserção no serviço da API. Não tratar `409` como upload concluído.

Resposta completa reproduzida no aparelho: `API não salvou a assinatura (HTTP 409). O cadastro viola uma referência ou valor único existente.` Essa mensagem é genérica para violação de integridade e não identifica a constraint. No Render, correlacionar o horário da requisição com a exceção SQL/constraint exata; conferir especialmente (1) índice único da assinatura ativa durante a troca de versão, (2) chave de idempotência já persistida e (3) FK de `assinatura_motorista` para `sc_corporativo.tb_usuario` após a migration V13. Não é seguro converter esse `409` em sucesso no mobile. Após corrigir a API, testar nova assinatura, `GET /api/v1/usuarios/me/assinatura/conteudo` e avanço à home.

Testar com uma conta real de motorista no Render: login, envio das duas modalidades, repetição após falha de rede, `GET` do PNG e conferência da versão/auditoria no banco. Não há credenciais de teste neste repositório, portanto o teste fim a fim não foi executado aqui.
