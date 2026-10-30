# Sistema de Chamados do IFRN

API REST para registrar problemas de suporte no campus e acompanhar o atendimento. Um solicitante abre o chamado, um técnico do setor responsável assume e registra a solução. As interações ficam no histórico para que os dois possam acompanhar o que foi feito.

Projeto da disciplina de Desenvolvimento de Sistemas Corporativos, do curso de Análise e Desenvolvimento de Sistemas do IFRN, Campus Pau dos Ferros.

## Tecnologias

Java 21, Spring Boot 4.0.9, Maven, Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL 17 e Flyway. O Lombok é usado para os acessores das entidades e a injeção por construtor. Os testes usam JUnit, MockMvc e Testcontainers com PostgreSQL real.

## Executar com Docker

Com o Docker e o Docker Compose disponíveis, execute na raiz do projeto:

```sh
docker compose up --build
```

A API responde em `http://localhost:8080`. Na primeira execução, o Maven baixa as dependências e o Flyway cria o banco e insere os dados iniciais. Não precisa instalar Java ou PostgreSQL na máquina para essa opção. `docker compose up` também faz o build automaticamente quando a imagem ainda não existe.

Para conferir:

```sh
curl http://localhost:8080/setores
```

Para encerrar, use `docker compose down`. Os dados ficam no volume `dados_postgres` e continuam disponíveis na próxima execução.

O Compose já fornece os valores de desenvolvimento. Se quiser alterá-los, copie `.env.example` para `.env` e ajuste as variáveis antes da primeira execução. A configuração da aplicação recebe `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` pelo ambiente. A senha local de exemplo fica no Compose, sem senha fixa no `application.properties`.

Se a porta 5432 estiver ocupada, altere `DB_PORT` no `.env`. A comunicação entre os containers continua usando a porta interna 5432.

## Executar pela IDE ou pelo Maven

É necessário um **JDK 21**. Java 8 não compila este projeto. Configure `JAVA_HOME` para o JDK 21 e importe o `pom.xml` na IDE.

Suba apenas o banco:

```sh
docker compose up -d banco
```

No PowerShell, configure as variáveis e inicie a aplicação:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/chamados"
$env:DB_USERNAME = "chamados"
$env:DB_PASSWORD = "chamados_local"
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```sh
export DB_URL=jdbc:postgresql://localhost:5432/chamados
export DB_USERNAME=chamados
export DB_PASSWORD=chamados_local
sh mvnw spring-boot:run
```

O `.env` é lido pelo Docker Compose. Ao executar pela IDE, configure as variáveis na configuração de execução. Se tiver alterado o banco ou as credenciais, use os mesmos valores nos dois lugares. Para rodar localmente, encerre o container da API com `docker compose stop api` caso a porta 8080 já esteja ocupada.

## Dados iniciais

Os registros são fictícios e estão na migration `V2__inserir_dados_iniciais.sql`.

| Recurso | ID | Cadastro |
| --- | --- | --- |
| Setor | 1 | COTIC |
| Setor | 2 | Manutenção e Infraestrutura (MANUT) |
| Categoria | 1 | Rede e internet — COTIC |
| Categoria | 2 | Computadores e projetores — COTIC |
| Categoria | 3 | Instalações elétricas — MANUT |
| Categoria | 4 | Instalações hidráulicas — MANUT |
| Usuário | 1 | Ana Beatriz — solicitante |
| Usuário | 2 | Lucas Ferreira — solicitante |
| Usuário | 3 | Marcos Silva — técnico da COTIC |
| Usuário | 4 | Juliana Costa — técnica da MANUT |

## Experimentar a API

O arquivo [requisicoes.http](requisicoes.http) tem todas as rotas do enunciado e alguns exemplos de erro. Pode ser usado no REST Client do VS Code ou no cliente HTTP do IntelliJ. Execute as requisições individualmente, seguindo os comentários.

Para abrir um chamado:

```http
POST http://localhost:8080/chamados
Content-Type: application/json

{
  "titulo": "Wi-Fi do laboratório sem conexão",
  "descricao": "A rede aparece, mas os computadores não conseguem acessar a internet.",
  "local": "Laboratório de Informática 2",
  "solicitanteId": 1,
  "categoriaId": 1,
  "prioridade": "ALTA"
}
```

A resposta é `201 Created`, com o chamado e um cabeçalho `Location`. Sem a prioridade, o sistema usa `MEDIA`. O status inicial é sempre `ABERTO` e a data de abertura é preenchida pelo sistema.

Use o ID retornado para continuar:

```http
PATCH http://localhost:8080/chamados/1/assumir
Content-Type: application/json

{"tecnicoId": 3}
```

```http
POST http://localhost:8080/chamados/1/interacoes
Content-Type: application/json

{"autorId": 3, "texto": "Estou verificando o ponto de acesso do laboratório."}
```

```http
PATCH http://localhost:8080/chamados/1/resolver
Content-Type: application/json

{"tecnicoId": 3, "solucao": "Ponto de acesso reiniciado e conexão testada nos computadores."}
```

O detalhe em `GET /chamados/1` inclui as interações em ordem cronológica. As listagens trazem os dados do chamado sem carregar o histórico inteiro.

## Rotas

| Método | Rota | Resultado |
| --- | --- | --- |
| POST | `/setores` | Cadastra setor — 201 |
| GET | `/setores` | Lista setores — 200 |
| GET | `/setores/{id}` | Consulta setor — 200 |
| PUT | `/setores/{id}` | Atualiza setor — 200 |
| DELETE | `/setores/{id}` | Exclui setor sem vínculos — 204 |
| GET | `/setores/{id}/categorias` | Lista categorias do setor — 200 |
| POST | `/categorias` | Cadastra categoria — 201 |
| GET | `/categorias` | Lista categorias — 200 |
| GET | `/categorias/{id}` | Consulta categoria — 200 |
| PUT | `/categorias/{id}` | Atualiza categoria — 200 |
| DELETE | `/categorias/{id}` | Exclui categoria sem chamados — 204 |
| POST | `/usuarios` | Cadastra usuário — 201 |
| GET | `/usuarios` | Lista usuários — 200 |
| GET | `/usuarios/{id}` | Consulta usuário — 200 |
| PUT | `/usuarios/{id}` | Atualiza usuário — 200 |
| GET | `/usuarios/{id}/chamados` | Lista chamados abertos pelo solicitante ou atribuídos ao técnico — 200 |
| POST | `/chamados` | Abre chamado — 201 |
| GET | `/chamados` | Lista chamados — 200 |
| GET | `/chamados/{id}` | Detalha chamado e interações — 200 |
| PATCH | `/chamados/{id}/assumir` | Atribui o técnico — 200 |
| PATCH | `/chamados/{id}/resolver` | Registra solução e encerra — 200 |
| PATCH | `/chamados/{id}/cancelar` | Cancela um chamado aberto — 200 |
| POST | `/chamados/{id}/interacoes` | Registra interação — 201 |

O `PUT` recebe todos os campos do cadastro. Não há rota genérica de atualização ou exclusão de chamados: as mudanças passam pelas ações do ciclo de vida.

## Paginação e filtros

Todas as listas aceitam `page`, `size` e `sort`. A primeira página é `0`, o tamanho padrão é `10` e o máximo é `50`. Sem `sort`, a ordem é por `id`. Para ordenar por mais de um campo, repita o parâmetro: `sort=dataAbertura,desc&sort=id,asc`.

| Listagem | Filtros opcionais |
| --- | --- |
| `/setores` | `nome` |
| `/categorias` | `nome`, `setorId` |
| `/setores/{id}/categorias` | `nome` |
| `/usuarios` | `nome`, `tipo`, `setorId` |
| `/chamados` | `status`, `prioridade`, `setorId`, `categoriaId`, `solicitanteId`, `tecnicoId`, `dataInicio`, `dataFim` |
| `/usuarios/{id}/chamados` | `status` |

Os filtros de nome procuram parte do texto, sem diferenciar maiúsculas e minúsculas. Os filtros informados são combinados. `dataInicio` e `dataFim` usam `AAAA-MM-DD` e incluem os dois dias no fuso `America/Sao_Paulo`. As datas dos registros são devolvidas em ISO 8601, com offset.

Exemplo: `GET /chamados?status=ABERTO&setorId=1&prioridade=ALTA&page=0&size=10&sort=dataAbertura,desc`.

O formato de página é o mesmo em todas as listas:

```json
{
  "content": [],
  "number": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}
```

Os enums devem ser enviados em maiúsculas: tipos `SOLICITANTE` e `TECNICO`; prioridades `BAIXA`, `MEDIA` e `ALTA`; status `ABERTO`, `EM_ATENDIMENTO`, `RESOLVIDO` e `CANCELADO`.

## Regras do atendimento

O ciclo permitido é `ABERTO → EM_ATENDIMENTO → RESOLVIDO`, ou `ABERTO → CANCELADO`.

Somente solicitantes abrem chamados. O técnico precisa pertencer ao setor da categoria para assumir e somente o técnico atribuído pode resolver, informando uma solução. O cancelamento cabe ao próprio solicitante e só acontece enquanto o chamado está aberto. A data de fechamento é registrada tanto na resolução quanto no cancelamento.

O solicitante e o técnico atribuído podem registrar interações enquanto o chamado estiver aberto ou em atendimento. Chamados encerrados recusam qualquer nova ação. Setores e categorias com chamados vinculados não podem ser excluídos, mesmo quando os chamados já estiverem encerrados.

Algumas decisões complementam o enunciado: técnicos precisam ter setor e solicitantes não têm setor; email e sigla são únicos sem distinguir maiúsculas; setores com categorias ou técnicos também não podem ser excluídos. Para preservar os vínculos, uma categoria com chamados não muda de setor, um usuário com chamados não muda de tipo e um técnico com chamados atribuídos não muda de setor. Os demais dados do cadastro podem ser corrigidos.

As ações sobre um chamado usam bloqueio de linha dentro de uma transação. Assim, duas tentativas simultâneas de assumir não passam juntas, e uma interação não é registrada depois que outra requisição encerra o chamado.

Não há autenticação, conforme o enunciado. O usuário da ação é indicado por `solicitanteId`, `tecnicoId` ou `autorId` no corpo. Esses IDs representam a ação, mas não comprovam a identidade de quem enviou a requisição.

## Erros

Dados inválidos retornam `400`, recursos inexistentes retornam `404` e conflitos de negócio retornam `409`. Por exemplo, uma solução em branco é um dado inválido; um técnico de outro setor é um conflito de negócio. O `@RestControllerAdvice` mantém o mesmo formato:

```json
{
  "timestamp": "2026-10-30T20:00:00Z",
  "status": 409,
  "mensagem": "O técnico deve pertencer ao setor responsável pela categoria.",
  "caminho": "/chamados/1/assumir"
}
```

## Organização

```text
src/main/java/br/edu/ifrn/chamados/
  controller/   rotas e respostas HTTP
  service/      regras de negócio e transações
  repository/   consultas e persistência
  model/        entidades e enums
  dto/          contratos de entrada e saída
  exception/    exceções e tratamento dos erros
src/main/resources/db/migration/
  V1__criar_tabelas.sql
  V2__inserir_dados_iniciais.sql
```

Os controllers dependem dos services, e os services acessam os repositories. As entidades não são expostas diretamente no JSON, evitando ciclos nos relacionamentos e alterações de campos controlados pelo sistema.

## Migrations e validação do banco

O Flyway registra as migrations aplicadas na tabela `flyway_schema_history`, incluindo versão, arquivo, checksum e resultado. Na inicialização, ele valida esse histórico e executa as versões pendentes em ordem. Se o checksum de uma migration já aplicada mudar, a validação falha: o arquivo deixou de corresponder ao que foi executado. Por isso, uma correção entra em uma nova migration, como `V3__ajustar_algum_campo.sql`, preservando as anteriores. Essa é a orientação da [documentação do Flyway sobre migrations versionadas](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations).

O Hibernate usa `ddl-auto=validate`: confere se o mapeamento corresponde ao esquema, sem criar ou alterar tabelas. `ddl-auto=update` não é usado porque alterações automáticas não oferecem o mesmo histórico de scripts revisáveis e reproduzíveis entre ambientes. Neste projeto, mudanças no banco ficam explícitas nas migrations. A [documentação do Spring Boot sobre inicialização do banco](https://docs.spring.io/spring-boot/4.0/how-to/data-initialization.html) descreve essas opções.

## Testes

Com JDK 21 e Docker em execução:

```powershell
.\mvnw.cmd verify
```

No Linux/macOS, use `sh mvnw verify`. O Testcontainers sobe um PostgreSQL isolado em porta aleatória, aplica as migrations e remove o container ao terminar. Não precisa configurar credenciais de teste, nem usar o banco do Compose.

Os testes verificam o ciclo completo, as transições recusadas, as permissões de interação, as exclusões protegidas, os filtros combinados, a paginação de todas as listas, a unicidade de email e sigla, a padronização dos erros e a disputa entre duas requisições para assumir o mesmo chamado. Cada teste reinicia apenas os dados do seu banco isolado.
