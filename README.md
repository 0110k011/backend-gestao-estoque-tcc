#A.1 Instruções para execução do backend

##Passo 1 - Clonar o repositório

git clone git@github.com:0110k011/backend-gestao-estoque-tcc.git
ou
git clone https://github.com/0110k011/backend-gestao-estoque-tcc.git

##Passo 2 -  Executar a aplicação Spring Boot

./mvnw spring-boot:run

Na primeira execução, o Maven baixará as dependências e o Flyway aplicará automaticamente as migrações V1, V2 e V3, criando o esquema do banco e inserindo os dados iniciais (categoria padrão e usuário administrador).
Ao final da inicialização, o backend estará disponível em http://localhost:8080. A documentação interativa da API (Swagger UI) pode ser acessada em http://localhost:8080/docs.
