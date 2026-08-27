# API de votação

Implementação simples e segura do [desafio técnico original](README_DESAFIO.md): cadastro de pautas, abertura de sessão, recebimento de votos, elegibilidade por CPF e contabilização do resultado.

## Considerações
Para implementação deste desafio, foi utilizada a metodologia de SDD (Spec Driven Design) com o GPT Codex.
- Eu tomei as decisões
- A IA sugeriu um plano
- Eu ajustei as especificações
- A IA implementou
- Eu revisei e aprovei

Para atender ao requisito de avaliação de commits, dividi a implementação em etapas e criei tasks "ficticias" para simular o Jira, as quais chamei de "DES" para poder utilizar nos comentários dos commits.

Criei markdowns (CHECKLIST_DES-0001, CHECKLIST_DES-0002, CHECKLIST_DES-0003) para representar um checklist e status do que cada task/commit representou.

## Escolhas principais

- Java 21 LTS e Spring Boot 3.5.16: versões estáveis, maduras e com suporte amplo no ecossistema.
- PostgreSQL 17: integridade transacional e restrições no banco para proteger concorrência.
- Flyway: esquema versionado; o Hibernate apenas valida as tabelas.
- JUnit 5, Mockito e Testcontainers: testes unitários, web e integrados contra PostgreSQL real.
- JaCoCo e SonarQube: mínimo de 90% de cobertura de linhas e branches e quality gate sem pendências.

## Executar toda a aplicação com Docker

O único requisito é Docker com Compose. Opcionalmente copie `.env.example` para `.env` e altere as credenciais locais.

```bash
docker compose up --build -d app
docker compose ps
docker compose logs -f app
```

A aplicação estará em `http://localhost:8080`:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Saúde: `http://localhost:8080/actuator/health`

Para encerrar:

```bash
docker compose down
```

Use `docker compose down -v` somente quando também quiser apagar os dados locais.

## Usar a API

Criar pauta:

```bash
curl -i -X POST http://localhost:8080/api/v1/pautas \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Aprovar orçamento?","descricao":"Orçamento anual"}'
```

Abra a sessão usando o `id` retornado. Sem corpo, a duração padrão é 60 segundos.

```bash
curl -i -X POST http://localhost:8080/api/v1/pautas/PAUTA_ID/sessoes \
  -H 'Content-Type: application/json' \
  -d '{"duracaoSegundos":120}'
```

Registrar um voto (`SIM` ou `NAO`):

```bash
curl -i -X POST http://localhost:8080/api/v1/pautas/PAUTA_ID/votos \
  -H 'Content-Type: application/json' \
  -d '{"associadoId":"associado-123","opcao":"SIM"}'
```

O registro V2 interpreta `associadoId` como CPF, aceita o valor com ou sem pontuação, valida os dígitos verificadores e consulta a elegibilidade aleatória antes de registrar o voto:

```bash
curl -i -X POST http://localhost:8080/api/v2/pautas/PAUTA_ID/votos \
  -H 'Content-Type: application/json' \
  -d '{"associadoId":"529.982.247-25","opcao":"SIM"}'
```

CPF inválido retorna `404` com o código `CPF_INVALIDO`. Um CPF válido sorteado como não habilitado retorna `403` com `UNABLE_TO_VOTE`. Quando habilitado, o CPF é persistido normalizado, contendo somente os 11 dígitos. A V1 foi preservada para compatibilidade e continua tratando `associadoId` como identificador textual.

Consultar resultado parcial ou final:

```bash
curl http://localhost:8080/api/v1/pautas/PAUTA_ID/resultado
```

## Regras implementadas

- Uma pauta possui no máximo uma sessão.
- A sessão abre imediatamente e aceita duração positiva; o padrão é 60 segundos.
- O intervalo de votação é `abertaEm <= instante < fechaEm`.
- Cada `associadoId` vota uma única vez por pauta.
- Na V2, `associadoId` deve ser um CPF válido e o associado precisa ser sorteado como `ABLE_TO_VOTE`.
- Restrições `UNIQUE` no PostgreSQL garantem as regras mesmo sob requisições concorrentes.
- O resultado é `EM_ANDAMENTO` enquanto a sessão está aberta e, depois, `APROVADA`, `REJEITADA` ou `EMPATE`.
- Erros usam o formato padronizado RFC 9457 (`application/problem+json`).

## Testes e cobertura

Os testes `*Test` são unitários/web; `*IT` são integrados e sobem um PostgreSQL descartável com Testcontainers. Ambos são testes JUnit 5. Para executar sem Java instalado na máquina:

```bash
docker compose --profile test build tests
docker compose --profile test run --rm tests
```

Com Java 21 local:

```bash
./mvnw clean verify
```

O build falha se linhas ou branches ficarem abaixo de 90%. O relatório HTML fica em `target/site/jacoco/index.html`.

## Testes de performance

Os testes usam k6 2.2.0 e um Compose exclusivo (`compose.performance.yaml`). A aplicação e o PostgreSQL de performance não compartilham containers nem volume com o ambiente normal. Cada execução começa com um banco vazio, gera evidências locais e remove somente o ambiente de performance ao terminar.

Perfis disponíveis:

| Perfil | Objetivo | Configuração padrão |
|---|---|---|
| `smoke` | Confirmar rapidamente contrato, persistência e instrumentação | 10 votos, 1 VU |
| `load` | Sustentar uma taxa conhecida sem perder requisições | 100 votos/s por 2 minutos |
| `volume` | Validar integridade e latência com uma tabela grande | 200.000 votos, 100 VUs |

### Por que usamos k6

k6, Apache JMeter e Gatling são ferramentas maduras. A escolha não significa que k6 seja sempre superior; ele foi o melhor encaixe para este desafio pequeno, orientado a API REST e com exigência explícita de evitar overengineering.

| Critério deste projeto | k6 | JMeter | Gatling |
|---|---|---|---|
| Teste versionável e fácil de revisar | Um arquivo JavaScript curto | Plano JMX/XML, normalmente preparado pela interface gráfica | Simulação em Java, Kotlin ou Scala |
| Execução neste repositório | Imagem Docker oficial, sem runtime local | Requer ambiente JMeter/Java ou imagem adicional | Requer JVM e integração Maven/Gradle ou imagem adicional |
| Critério automático de aprovação | Thresholds nativos alteram o exit code | Possível, porém tende a exigir configuração adicional sobre listeners/assertions | Assertions nativas |
| Evidência visual | Dashboard web nativo e exportação HTML | Relatório HTML completo | Relatório HTML completo |
| Adequação ao escopo | Alta: HTTP JSON, poucos cenários e Compose | Alta, mas com mais configuração para este caso | Alta, mas adicionaria outra DSL/estrutura de build |

Os fatores decisivos foram:

- teste como código em JavaScript, pequeno e legível mesmo para quem não conhece a ferramenta;
- executores próprios para taxa constante e volume fixo;
- checks funcionais e thresholds de latência, erros, descartes e contagem no mesmo arquivo;
- exit code não zero quando um threshold falha, adequado para automação e CI;
- dashboard ao vivo e relatório HTML sem instalar Grafana, Prometheus ou plugins;
- imagem Docker oficial, mantendo a regra de que somente Docker é necessário na máquina.

JMeter seria uma escolha forte se o projeto precisasse do seu ecossistema amplo de protocolos, plugins ou criação visual de planos. A própria documentação recomenda usar a GUI para criação/depuração e o modo CLI para carga. Gatling seria especialmente atraente para uma equipe que queira manter os cenários na JVM e já use sua DSL e build. Aqui, ambos aumentariam a quantidade de configuração sem melhorar a evidência exigida pelo desafio.

Referências oficiais: [documentação do k6](https://grafana.com/docs/k6/latest/), [boas práticas do JMeter](https://jmeter.apache.org/usermanual/best-practices.html) e [simulações do Gatling](https://docs.gatling.io/concepts/simulation/).

### Passo a passo recomendado

O único requisito é Docker Desktop, ou Docker Engine com o plugin Compose. Java, Maven, PostgreSQL e k6 não precisam estar instalados na máquina.

1. Abra o Docker Desktop e aguarde o engine ficar disponível.
2. Abra um terminal na raiz deste repositório.
3. Confirme que Docker e Compose estão acessíveis:

```bash
docker version
docker compose version
```

4. Execute um perfil. No Windows/PowerShell:

```powershell
.\performance\run.ps1 -Profile smoke
.\performance\run.ps1 -Profile load
.\performance\run.ps1 -Profile volume
```

No Linux/macOS:

```bash
chmod +x performance/run.sh
./performance/run.sh smoke
./performance/run.sh load
./performance/run.sh volume
```

Não é necessário executar `docker compose up` antes. Cada runner realiza automaticamente este fluxo:

1. remove apenas containers e volume de uma execução de performance anterior;
2. constrói a imagem atual da aplicação;
3. sobe um PostgreSQL vazio e a aplicação em containers isolados;
4. aguarda os health checks do banco e da aplicação;
5. sobe o k6, executa o perfil e publica o dashboard na porta `5665`;
6. salva os relatórios em `performance-results/<data-perfil>/`;
7. derruba os containers e apaga somente o banco de performance, mesmo quando o teste falha.

O banco normal de desenvolvimento, definido em `compose.yaml`, não é alterado.

### Visualizar durante e depois do teste

Enquanto o comando estiver executando, abra `http://localhost:5665` no navegador para acompanhar taxa, latência, VUs e erros em tempo real. O smoke dura cerca de dez segundos; para observar o dashboard com calma, use o perfil `load`, que dura dois minutos.

Ao terminar, o dashboard ao vivo sai do ar porque o ambiente é limpo. Abra então o `report.html` gerado. No PowerShell:

```powershell
$ultimaExecucao = Get-ChildItem .\performance-results -Directory |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
Start-Process (Join-Path $ultimaExecucao.FullName "report.html")
```

No Linux:

```bash
ultima_execucao=$(ls -dt performance-results/*/ | head -n 1)
xdg-open "${ultima_execucao}report.html"
```

No macOS, substitua `xdg-open` por `open`.

Cada pasta da execução contém:

- `report.html`: dashboard navegável;
- `summary.json`: métricas e resultado dos thresholds;
- `console.log`: saída completa do k6;
- `metadata.json`: data, commit, parâmetros e versões do Docker.

O terminal termina com `Teste aprovado` quando todos os thresholds passam. Em caso de falha, ele retorna código diferente de zero e informa a pasta que deve ser analisada.

### Personalizar a execução

No PowerShell, é possível alterar os parâmetros sem editar arquivos:

```powershell
.\performance\run.ps1 -Profile load -Rate 200 -Duration 5m -PreAllocatedVUs 200
.\performance\run.ps1 -Profile volume -TotalVotes 300000 -VUs 150
```

No shell Unix, personalize com variáveis de ambiente, por exemplo:

```bash
PERF_RATE=200 PERF_DURATION=5m PERF_PREALLOCATED_VUS=200 ./performance/run.sh load
PERF_TOTAL_VOTES=300000 PERF_VUS=150 ./performance/run.sh volume
```

### Subir e executar manualmente

Os runners são a opção recomendada porque criam metadados, registram o console, validam exit codes e sempre limpam o ambiente. Para inspecionar cada etapa manualmente no PowerShell:

```powershell
$env:PERF_PROFILE = "load"
$env:PERF_RUN_ID = "$(Get-Date -Format 'yyyyMMdd-HHmmss')-load-manual"
New-Item -ItemType Directory -Force ".\performance-results\$env:PERF_RUN_ID"

docker compose -f compose.performance.yaml down -v --remove-orphans
docker compose -f compose.performance.yaml build perf-app
docker compose -f compose.performance.yaml up --no-build -d perf-app
docker compose -f compose.performance.yaml ps
docker compose -f compose.performance.yaml logs --tail 100 perf-app

docker compose -f compose.performance.yaml run --rm --service-ports k6

docker compose -f compose.performance.yaml down -v --remove-orphans
```

No Linux/macOS, o mesmo fluxo manual é:

```bash
export PERF_PROFILE=load
export PERF_RUN_ID="$(date +%Y%m%d-%H%M%S)-load-manual"
mkdir -p "performance-results/$PERF_RUN_ID"

docker compose -f compose.performance.yaml down -v --remove-orphans
docker compose -f compose.performance.yaml build perf-app
docker compose -f compose.performance.yaml up --no-build -d perf-app
docker compose -f compose.performance.yaml ps
docker compose -f compose.performance.yaml logs --tail 100 perf-app

docker compose -f compose.performance.yaml run --rm --service-ports k6

docker compose -f compose.performance.yaml down -v --remove-orphans
```

O `up` sobe `perf-db` e `perf-app`; o comando `run` sobe o k6 e disponibiliza o dashboard. No modo manual, execute o último `down -v` depois de consultar o relatório. Esse comando atua somente no projeto Docker `desafio-votacao-performance`.

`performance-results/` é ignorada pelo Git porque os números dependem da máquina e da carga concorrente. Para manter uma evidência específica, arquive a pasta da execução fora do repositório. O processo retorna código diferente de zero se houver voto perdido, erro HTTP, iteração descartada, p95 igual ou superior a 500 ms ou p99 igual ou superior a 1 segundo.

O fluxo mede o endpoint V1 de voto porque cada iteração precisa resultar em uma gravação para validar throughput e contagem exata. Usar a V2 misturaria desempenho com o sorteio de elegibilidade, no qual aproximadamente metade das chamadas pode terminar legitimamente em `403`. A validação de CPF e os dois resultados desse sorteio são cobertos de forma determinística pelos testes unitários e integrados.

Baseline local executado em 27/08/2026 com Docker Desktop:

| Perfil | Resultado | Latência dos votos |
|---|---|---|
| Smoke | 10 votos, 0 falhas | p95 23,65 ms; p99 29,69 ms |
| Load | 12.001 votos, 0 falhas, 0 descartados, ~99,73 votos/s | média 4,34 ms; p95 6,97 ms; p99 10,40 ms |
| Volume | 200.000 votos, 0 falhas, ~3.203,85 votos/s | média 30,87 ms; p95 55,51 ms; p99 94,57 ms; máximo 437,67 ms |

Os thresholds são critérios técnicos iniciais, não um SLA de produção. Stress, spike e soak não foram necessários neste ciclo: smoke, load e volume ficaram com ampla margem, sem falhas ou descartes. Esses perfis devem ser adicionados quando houver uma meta de capacidade máxima, tráfego em rajadas ou estabilidade de longa duração a comprovar; executá-los sem essa pergunta aumentaria tempo e custo sem um critério objetivo de aprovação.

### Como a aleatoriedade é testada

A V2 depende da interface `ConsultaElegibilidade`, não diretamente do gerador aleatório. Em produção, `ConsultaElegibilidadeFake` usa `SecureRandom`. Nos testes unitários, o gerador e a interface são mocks com respostas controladas; no teste integrado, uma implementação `@Primary` sempre retorna `ABLE_TO_VOTE`. Assim os dois resultados do sorteio são testados sem repetição, seed global ou possibilidade de falha intermitente.

A validação dos dígitos verificadores foi implementada em uma classe pequena de domínio (`Cpf`). Uma biblioteca adicional não traria benefício proporcional para esse algoritmo estável e aumentaria dependências, superfície de atualização e tempo de build.

## SonarQube

Inicie a instância local:

```bash
docker compose --profile quality up -d sonarqube
```

Acesse `http://localhost:9000`, entre inicialmente com `admin`/`admin`, altere a senha e gere um token. Crie e associe ao projeto `desafio-votacao` um quality gate com:

- zero issues novas;
- cobertura e cobertura de branches de pelo menos 90%;
- 100% dos security hotspots revisados;
- duplicação em código novo de no máximo 3%.

Execute a análise pelo mesmo ambiente de testes:

```bash
docker compose --profile test --profile quality run --rm tests \
  mvn -B clean verify sonar:sonar \
  -Dsonar.host.url=http://sonarqube:9000 \
  -Dsonar.token=SEU_TOKEN
```

O scanner aguarda o quality gate e retorna erro se ele não for aprovado.

## Decisões e limites intencionais

- Na V1, `associadoId` permanece textual e opaco; na V2, ele representa um CPF, conforme o bônus de integração externa.
- Resultados parciais são retornados para tornar a API observável; o estado informa claramente `EM_ANDAMENTO`.
- Autenticação e mensageria foram deixadas fora. A elegibilidade é uma porta local com adapter fake aleatório, pronta para receber um client externo real sem alterar o fluxo da V2.
- As entidades ficam dentro de cada funcionalidade e controllers dependem de services, que dependem de repositories. Essa separação aplica responsabilidade única e inversão de dependência sem criar camadas artificiais.
- Repository substitui DAOs manuais; DTOs separam o contrato HTTP das entidades; injeção de `Clock` torna regras temporais determinísticas nos testes.
- Virtual threads não foram habilitadas no servidor. Os testes com até 100 usuários virtuais não mostraram esgotamento das threads tradicionais do Tomcat, e o limite mais provável em concorrência maior seria o pool de conexões JDBC/PostgreSQL. Virtual threads não tornam consultas ou transações mais rápidas; elas ajudam principalmente quando muitas requisições ficam bloqueadas aguardando I/O. Neste projeto, ativá-las sem evidência de gargalo acrescentaria uma otimização sem ganho demonstrado. Elas foram usadas somente no teste integrado para coordenar requisições concorrentes e seriam reconsideradas por meio de um benchmark A/B caso surgissem milhares de conexões simultâneas ou integrações externas lentas.
