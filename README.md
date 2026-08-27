# API de votação

Implementação simples e segura do [desafio técnico original](README_DESAFIO.md): cadastro de pautas, abertura de sessão, recebimento de votos e contabilização do resultado.

## Documentação para estudo

- [Plano de implementação executado](docs/PLANO_IMPLEMENTACAO.md)
- [Decisões técnicas e guia de estudo](docs/DECISOES_TECNICAS_E_GUIA_DE_ESTUDO.md)

Esses documentos registram o que foi implementado, as alternativas consideradas, os resultados das validações e como a IA apoiou o processo sem substituir a revisão técnica.

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

Consultar resultado parcial ou final:

```bash
curl http://localhost:8080/api/v1/pautas/PAUTA_ID/resultado
```

## Regras implementadas

- Uma pauta possui no máximo uma sessão.
- A sessão abre imediatamente e aceita duração positiva; o padrão é 60 segundos.
- O intervalo de votação é `abertaEm <= instante < fechaEm`.
- Cada `associadoId` vota uma única vez por pauta.
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

- `associadoId` é um identificador textual opaco. O enunciado não define CPF nem um cadastro de associados, portanto essa regra não foi inventada.
- Resultados parciais são retornados para tornar a API observável; o estado informa claramente `EM_ANDAMENTO`.
- Autenticação, mensageria e serviços externos foram deixados fora: não são exigidos pelo contrato e aumentariam a complexidade sem resolver uma regra do desafio.
- As entidades ficam dentro de cada funcionalidade e controllers dependem de services, que dependem de repositories. Essa separação aplica responsabilidade única e inversão de dependência sem criar camadas artificiais.
- Repository substitui DAOs manuais; DTOs separam o contrato HTTP das entidades; injeção de `Clock` torna regras temporais determinísticas nos testes.
