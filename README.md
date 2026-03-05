# Golden Raspberry Awards API

API RESTful desenvolvida em Spring Boot para leitura de indicados e vencedores da categoria "Pior Filme" do Golden Raspberry Awards.

## Descrição

Esta aplicação permite:
- Carregar automaticamente dados de filmes de um arquivo CSV ao iniciar
- Consultar produtores com maior e menor intervalo entre prêmios consecutivos
- Fornecer dados através de uma API RESTful seguindo o nível 2 de maturidade de Richardson

## Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **H2 Database** (banco em memória)
- **Apache Commons CSV**
- **Lombok**
- **Maven**

## Pré-requisitos

- Java 21 ou superior
- Maven 3.6 ou superior

## Como Executar

### 1. Compilar o projeto

```bash
mvn clean install
```

### 2. Executar a aplicação

```bash
mvn spring-boot:run
```

Ou executar o JAR gerado:

```bash
java -jar target/awards-api-1.0.0.jar
```

A aplicação estará disponível em: `http://localhost:8080`

## Endpoints da API

### GET /api/awards/intervals

Retorna os produtores com menor e maior intervalo entre prêmios consecutivos.

**Resposta de Exemplo:**

```json
{
  "min": [
    {
      "producer": "Producer 1",
      "interval": 1,
      "previousWin": 2008,
      "followingWin": 2009
    },
    {
      "producer": "Producer 2",
      "interval": 1,
      "previousWin": 2018,
      "followingWin": 2019
    }
  ],
  "max": [
    {
      "producer": "Producer 1",
      "interval": 99,
      "previousWin": 1900,
      "followingWin": 1999
    },
    {
      "producer": "Producer 2",
      "interval": 99,
      "previousWin": 2000,
      "followingWin": 2099
    }
  ]
}
```

## Executar Testes

Para executar os testes de integração:

```bash
mvn test
```

Os testes garantem que:
- Os dados são carregados corretamente do CSV
- O endpoint retorna dados no formato esperado
- Os cálculos de intervalos estão corretos

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/goldenraspberry/awards/
│   │   ├── controller/        # Controllers REST
│   │   ├── dto/               # Data Transfer Objects
│   │   ├── model/             # Entidades JPA
│   │   ├── repository/        # Repositórios JPA
│   │   ├── service/           # Lógica de negócio
│   │   └── AwardsApplication.java
│   └── resources/
│       ├── application.properties
│       └── movielist.csv      # Arquivo CSV com dados dos filmes
└── test/
    └── java/com/goldenraspberry/awards/
        ├── controller/        # Testes de integração dos controllers
        └── service/           # Testes de integração dos serviços
```

## Carregamento de Dados

Ao iniciar a aplicação, o arquivo `movielist.csv` localizado em `src/main/resources/` é automaticamente lido e os dados são inseridos no banco de dados H2 em memória.

O arquivo CSV deve ter o seguinte formato:
- Delimitador: `;` (ponto e vírgula)
- Colunas: `year`, `title`, `studios`, `producers`, `winner`
- O campo `winner` deve conter `yes` para filmes vencedores

## Banco de Dados

A aplicação utiliza H2 Database em memória. Nenhuma instalação externa é necessária. O banco é criado automaticamente ao iniciar a aplicação e os dados são perdidos ao encerrar.

Para acessar o console H2 durante a execução:
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:awardsdb`
- Usuário: `sa`
- Senha: (vazio)

## Observações

- A aplicação segue o nível 2 de maturidade de Richardson para REST APIs
- Apenas testes de integração foram implementados
- O banco de dados é em memória (H2), não requer instalação externa
- Os produtores são parseados corretamente, incluindo casos com múltiplos produtores separados por vírgula ou "and"

## Autor

Desenvolvido como parte de avaliação técnica.

