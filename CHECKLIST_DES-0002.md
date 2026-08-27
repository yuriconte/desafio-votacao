# Checklist V2 — desafio de votação

Legenda: ✅ atendido · 🟡 parcial ou depende de confirmação · ❌ não realizado

## Checklist dos requisitos obrigatórios

| Requisito | Estado | Observação |
|---|:---:|---|
| Implementação em Java e Spring Boot | ✅ | Java 21 LTS e Spring Boot 3.5.16. |
| API REST com comunicação JSON | ✅ | API disponível sob o prefixo `/api/v1`. |
| Cadastrar uma pauta | ✅ | `POST /api/v1/pautas`. |
| Abrir sessão com duração informada | ✅ | `POST /api/v1/pautas/{pautaId}/sessoes`. |
| Usar duração padrão de um minuto | ✅ | São usados 60 segundos quando a duração é omitida. |
| Receber votos `Sim` ou `Não` | ✅ | Valores aceitos: `SIM` e `NAO`. |
| Permitir somente um voto por associado e pauta | ✅ | Protegido pela regra de negócio e por constraint `UNIQUE` no PostgreSQL. |
| Contabilizar votos e apresentar resultado | ✅ | `GET /api/v1/pautas/{pautaId}/resultado`. |
| Persistir pautas e votos após restart da aplicação | ✅ | PostgreSQL, Flyway e volume Docker nomeado. |
| Deixar segurança das interfaces abstraída | ✅ | Autenticação não foi adicionada, conforme permitido pelo enunciado. |
| Documentar como executar e testar | ✅ | Instruções e exemplos estão no `README.md`. |
| Executar a solução na nuvem | ❌ | A aplicação está containerizada, mas não foi publicada em um provedor cloud. |
| Mensagens JSON server-driven para o aplicativo mobile | 🟡 | A API retorna JSON convencional; o Anexo 1 não fornece o schema completo das telas. |

## Critérios de avaliação

| Critério | Estado | Observação |
|---|:---:|---|
| Simplicidade e ausência de overengineering | ✅ | Monólito modular, projeto Maven único e arquitetura direta. |
| Organização e arquitetura | ✅ | Pacotes por funcionalidade e fluxo Controller → Service → Repository. |
| Clean Code e manutenibilidade | ✅ | DTOs, injeção por construtor, nomes de domínio e métodos coesos. |
| Tratamento de erros e exceções | ✅ | Bean Validation e Problem Details com respostas `400`, `404` e `409`. |
| Explicação das escolhas | ✅ | Decisões técnicas foram documentadas localmente para estudo. |
| Testes automatizados | ✅ | 45 testes unitários/web e 5 testes integrados aprovados. |
| Qualidade e cobertura | ✅ | JaCoCo e SonarQube; 99,2% de linhas, 93,5% de branches e Quality Gate aprovado. |
| Documentação do código e da API | ✅ | README, OpenAPI e Swagger UI. |
| Logs da aplicação | ✅ | Operações de escrita possuem logs sem expor identificadores de associados. |
| Revisão de possíveis bugs | ✅ | Foi executado um ciclo de revisão destrutiva e correções. |

## Tarefas bônus

### Bônus 1 — elegibilidade por CPF

✅ Implementado na V2 do endpoint de voto: `POST /api/v2/pautas/{pautaId}/votos`.

- `associadoId` representa o CPF e aceita 11 dígitos ou a máscara `000.000.000-00`;
- os dígitos verificadores são validados e CPF inválido retorna `404` com `CPF_INVALIDO`;
- o CPF é normalizado para 11 dígitos antes de ser persistido;
- `ConsultaElegibilidade` isola a regra externa e `ConsultaElegibilidadeFake` sorteia `ABLE_TO_VOTE` ou `UNABLE_TO_VOTE`;
- `UNABLE_TO_VOTE` interrompe o registro e retorna `403 Forbidden`;
- os testes substituem a aleatoriedade por mocks ou por uma implementação determinística, evitando falhas intermitentes;
- a V1 foi preservada e continua aceitando um identificador textual.

### Bônus 2 — performance

🟡 Parcial.

Existem índice, agregação no PostgreSQL, transações curtas e teste de concorrência para voto duplicado. Não existe teste de carga com centenas de milhares de votos nem relatório de latência e throughput.

Antes de implementar, definir volume, concorrência, latência esperada e infraestrutura do teste.

### Bônus 3 — versionamento

✅ Implementado por URL com o prefixo `/api/v1`.

## Mobile

🟡 Não foi implementado um contrato server-driven de telas `FORMULARIO` e `SELECAO` porque o anexo não define seu schema completo.

Precisamos confirmar:

- o JSON oficial de cada tipo de tela;
- campos obrigatórios de itens, botões, opções e ações;
- quais endpoints retornam telas em vez de recursos de domínio;
- se as URLs de ação são absolutas ou relativas.

## Callbacks

❌ Não implementados porque o enunciado não define nenhum fluxo de callback.

Precisamos confirmar quais eventos geram callbacks, destino, payload, autenticação, timeout e política de retry. Também é necessário esclarecer se a referência a “domínio das URLs de callback” trata de callbacks do backend ou apenas das URLs de ação enviadas ao aplicativo.
