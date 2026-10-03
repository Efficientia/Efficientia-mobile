# Relatório de integração — Feedback de rota no Firebase

Data: 03/10/2026
Card: EFFICIENTI-404

## Implementação atual no mobile

O formulário de feedback envia diretamente para a coleção `feedbacks_rota` do Cloud Firestore. O aplicativo realiza autenticação anônima no Firebase antes da gravação e as regras permitem somente a criação de documentos com o formato validado.

Campos enviados:

- `firebaseUid`
- `motoristaId`
- `motoristaNome`
- `localizacaoRota`
- `avaliacao`
- `motivos`
- `comentario`
- `origem`
- `criadoEm`

## Limitação da integração atual

O login principal do aplicativo utiliza o JWT da API Efficientia. O Firebase Authentication não reconhece esse JWT e cria uma identidade anônima separada. Por isso, as regras do Firestore conseguem validar o `firebaseUid`, mas não conseguem confirmar que `motoristaId`, `motoristaNome`, empresa e rota pertencem ao usuário autenticado na API.

A localização utilizada inicialmente também é a rota demonstrativa exibida na Home, pois a API ainda não fornece ao fluxo da tela um identificador da rota atual.

## Ajustes recomendados na API

Para produção, escolher uma das estratégias abaixo:

1. Criar `POST /api/v1/feedbacks-rota` na API. O backend valida o JWT Efficientia, identifica motorista, empresa e rota e grava no Firestore usando Firebase Admin SDK.
2. Gerar um Firebase Custom Token após o login da API, permitindo que o mobile autentique no Firebase com uma identidade vinculada ao motorista real.

O caminho recomendado é o endpoint na API, centralizando autorização, auditoria e vínculo dos dados.

Contrato sugerido:

```json
{
  "rotaId": "uuid-da-rota",
  "avaliacao": "OTIMA",
  "motivos": ["ENTRADA", "ESPERA_NA_FAZENDA"],
  "comentario": "Texto opcional"
}
```

O backend deve obter do JWT, sem confiar em valores enviados pelo aplicativo:

- `motoristaId`
- `empresaId`
- nome do motorista
- permissões e vínculo com a rota

## Segurança

- Não armazenar credenciais do Firebase Admin SDK no aplicativo ou no GitHub.
- Guardar a credencial administrativa somente nos secrets do Render.
- Manter leitura, atualização e exclusão bloqueadas para clientes mobile.
- Considerar Firebase App Check para reduzir requisições originadas por aplicativos não autorizados.
- Registrar data, usuário, rota e origem para auditoria.

## Alterações realizadas na API

N/A. Este relatório documenta as necessidades futuras; nenhuma mudança foi feita no repositório da API.
