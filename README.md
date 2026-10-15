# A.1 Instruções para execução do backend

## Passo 1 - Clonar o repositório

git clone git@github.com:0110k011/backend-gestao-estoque-tcc.git
ou
git clone https://github.com/0110k011/backend-gestao-estoque-tcc.git

## Passo 2 -  Executar a aplicação Spring Boot

./mvnw spring-boot:run

Na primeira execução, o Maven baixará as dependências e o Flyway aplicará automaticamente as migrações V1, V2 e V3, criando o esquema do banco e inserindo os dados iniciais (categoria padrão e usuário administrador).
Ao final da inicialização, o backend estará disponível em http://localhost:8080. A documentação interativa da API (Swagger UI) pode ser acessada em http://localhost:8080/docs.

# A.3 Execução da suíte de testes automatizados

O backend contempla uma suíte de 93 testes automatizados distribuídos entre testes unitários, de integração, de controller, de segurança e de concorrência. Para executar a suíte completa:

./mvnw clean test

## Execução de testes específicos:

Para executar apenas os testes de uma classe específica:

./mvnw test -Dtest=MovimentacaoConcorrenciaTest

## Para executar apenas os testes de serviço:

./mvnw test -Dtest="*ServiceTest"

## Para executar apenas os testes de controller:

./mvnw test -Dtest="*ControllerTest"

## Geração do relatório de cobertura (JaCoCo):

./mvnw clean test jacoco:report

Após a execução, o relatório HTML de cobertura estará disponível em:

target/site/jacoco/index.html

O relatório apresenta, por pacote e por classe, o percentual de instruções, branches, linhas e métodos cobertos pelos testes.

# A.4 Execução do benchmark de desempenho

O benchmark foi conduzido com a ferramenta k6, que deve ser instalada previamente.

## Passo 1 - Obter um token JWT

Antes de executar os cenários, é necessário obter um token JWT válido via autenticação, pela url /api/v1/auth/login no swagger é possível pegar o token. O token deve ser copiado para uso nos comandos seguintes.

## Passo 2 - Executar cada cenário

### Cenário 1 - Listagem de produtos:

k6 run -e TOKEN=<token-jwt> benchmark/listagem-produtos.js

### Cenário 2 - Registro de movimentação:

k6 run -e TOKEN=<token-jwt> benchmark/movimentacao-entrada.js

### Cenário 3 - Busca com filtro:

k6 run -e TOKEN=<token-jwt> benchmark/busca-produtos.js

Cada execução produz um relatório no terminal com métricas de latência (média, p(90), p(95), p(99), máxima), throughput, taxa de erro e validação dos thresholds estabelecidos no RNF001. O código para executar o benchmark se encontra no repositório frontend-gestao-estoque-tcc dentro do diretório benchmark.

