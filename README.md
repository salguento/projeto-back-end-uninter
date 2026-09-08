# Projeto de Back-End - Raízes do Nordeste

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.15-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Maven](https://img.shields.io/badge/Maven-3.9+-red?logo=apache-maven)](https://maven.apache.org/)
[![CI](https://github.com/salguento/projeto-back-end-uninter/actions/workflows/ci.yml/badge.svg)](https://github.com/salguento/projeto-back-end-uninter/actions/workflows/ci.yml)

> **Atividade prática de Projeto Back-End** - Tecnologia em Análise e Desenvolvimento de Sistemas  
> **Centro Universitário Internacional UNINTER** - 2026  
> **Aluno:** Humberto S. (RU: 4819398)

---

## Sobre o Projeto

API RESTful desenvolvida para a atividade prática da disciplina de **Projeto Back-End** do curso de Tecnologia em Análise e Desenvolvimento de Sistemas da UNINTER.

O sistema atende à rede de restaurantes **"Raízes do Nordeste"**, oferecendo uma solução completa para:

- **Gestão Multicanal de Pedidos** (APP, TOTEM, BALCAO, PICKUP, WEB)
- **Controle de Estoque por Unidade** com entradas/saídas auditáveis, validação transacional e devolução conforme o estágio operacional do pedido
- **Simulação de Pagamento e Estorno** via gateway emulado (mock), com múltiplas tentativas e tolerância a indisponibilidade técnica
- **Programa de Fidelidade** com acúmulo e resgate de pontos
- **Campanhas e Cupons Promocionais** com desativação lógica e proteção concorrente contra períodos sobrepostos
- **Gestão de Unidades** com CRUD completo
- **Autenticação JWT** com controle de acesso baseado em perfis (RBAC)
- **Auditoria e Rastreabilidade** com logs estruturados
- **Documentos legais versionados** com aceite contratual rastreável e consentimento LGPD separado
- **CORS Configurável** para integração com clientes web

### Rastreabilidade de Requisições

Cada requisição recebe um identificador `X-Request-ID`, que também é devolvido no header da resposta,
incluído nas respostas de erro e inserido no contexto dos logs. Um identificador válido enviado pelo
cliente é propagado; valores ausentes ou inválidos são substituídos por UUID. Identificadores pessoais,
como e-mails, são pseudonimizados antes do registro em log.

---

## Tecnologias Utilizadas

| Tecnologia | Versão | Finalidade |
|------------|--------|------------|
| **Java** | 17 | Linguagem principal |
| **Spring Boot** | 3.5.15 | Framework web e IoC |
| **Spring Security** | 6.x | Autenticação e autorização |
| **Spring Data JPA** | 3.x | Persistência e ORM |
| **Hibernate Validator** | 8.x | Validação Bean Validation |
| **Flyway** | 11.7.2 | Migrations de banco de dados |
| **H2 Database** | 2.x | Banco de dados em memória (dev/test) |
| **PostgreSQL** | Conforme o ambiente | Banco de dados do perfil de produção |
| **JWT (jjwt)** | 0.12.5 | Tokens de autenticação |
| **Lombok** | 1.18.x | Redução de boilerplate |
| **Springdoc OpenAPI** | 2.8.17 | Documentação Swagger |
| **Maven Wrapper** | Maven 3.9.16 | Construção e gerenciamento de dependências reproduzíveis |
| **JUnit 5** | 5.12.x | Testes automatizados |
| **Mockito** | 5.x | Mocking em testes |

---

## Requisitos

Antes de começar, certifique-se de ter instalado:

- **JDK 17** ou superior ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Git** ([Download](https://git-scm.com/))
- **Insomnia** ou **Postman** (opcional, para testes)

O Maven não precisa ser instalado separadamente: o repositório inclui o Maven Wrapper, que baixa e
utiliza a versão configurada na primeira execução.

Verifique as instalações:

```bash
java -version
./mvnw -version
git --version
```

---

## Como Rodar o Projeto

### 1. Clonar o Repositório

```bash
git clone https://github.com/salguento/projeto-back-end-uninter.git
cd projeto-back-end-uninter
```

### 2. Configurar Variáveis de Ambiente

Copie o arquivo de exemplo e edite conforme necessário:

```bash
cp .env.example .env
set -a
source .env
set +a
```

**IMPORTANTE:** O arquivo `.env` **nunca** deve ser commitado no Git.
O Spring Boot não lê esse arquivo automaticamente; os comandos acima exportam suas variáveis para a
sessão atual do terminal. Como alternativa, configure-as diretamente na execução da IDE. O perfil
`dev` já possui valores locais seguros e pode ser iniciado sem personalizar o arquivo.

### 3. Compilar o Projeto

```bash
./mvnw clean install
```

### 4. Executar a Aplicação

#### Modo Desenvolvimento (H2 em memória)

```bash
# Via Maven Wrapper
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Ou com variável de ambiente
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

A aplicação estará disponível em: **http://localhost:8080/api**

### 5. Acessar o Console H2 (Apenas em Desenvolvimento)

- **URL:** http://localhost:8080/api/h2-console
- **JDBC URL:** `jdbc:h2:mem:raizesdb`
- **User:** `sa`
- **Password:** (deixe em branco)

---

## Perfis de Execução

O projeto possui dois perfis de execução e uma configuração isolada para testes:

### Profile `dev` (Desenvolvimento)

- H2 Database em memória
- Console H2 habilitado
- Logs detalhados (DEBUG)
- SQL queries visíveis
- Swagger ativo
- Pagamento forçado como APROVADO (determinístico)
- CORS permissivo (localhost)

### Profile `prod` (Produção)

- PostgreSQL com `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` obrigatórios
- `JWT_SECRET` obrigatório, sem valor padrão
- Swagger e console H2 desabilitados
- Detalhes internos de erros desabilitados
- `Flyway clean` bloqueado
- Header de simulação de pagamento ignorado
- CORS limitado às origens informadas em `CORS_ALLOWED_ORIGINS`

Exemplo de inicialização:

```bash
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET="$(openssl rand -base64 48)"
export DB_URL="jdbc:postgresql://localhost:5432/raizesdb"
export DB_USERNAME="raizes"
export DB_PASSWORD="troque-esta-senha"
export CORS_ALLOWED_ORIGINS="https://app.exemplo.com.br"
./mvnw spring-boot:run
```

Sem essas variáveis, o perfil de produção falha na inicialização, evitando credenciais inseguras.


---

## Variáveis de Ambiente

### Configuração Mínima (Desenvolvimento)

```env
SPRING_PROFILES_ACTIVE=dev
```

O segredo local do perfil `dev` pode ser substituído opcionalmente por `JWT_SECRET`.

### Configuração Completa

```env
# Profile ativo
SPRING_PROFILES_ACTIVE=dev

# JWT (mínimo 32 caracteres)
JWT_SECRET=SuaChaveSecretaComPeloMenos32CaracteresAqui

# Banco de Dados (apenas produção)
DB_URL=jdbc:postgresql://localhost:5432/raizesdb
DB_USERNAME=postgres
DB_PASSWORD=suaSenhaSegura

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
CORS_ALLOWED_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=Authorization,Content-Type,Idempotency-Key,X-Request-ID

# Pagamento (APROVADO, RECUSADO, INDISPONIVEL ou vazio para aleatório)
PAGAMENTO_FORCAR_STATUS=APROVADO

# Estorno (CONFIRMADO, INDISPONIVEL ou vazio)
PAGAMENTO_FORCAR_STATUS_ESTORNO=CONFIRMADO

# Versões vigentes dos documentos legais
TERMOS_USO_VERSAO=1.0
AVISO_PRIVACIDADE_VERSAO=1.0

# Server
SERVER_PORT=8080
```

**Veja o arquivo `.env.example` para lista completa de variáveis.**

---

## Migrations e Seed

### Migrations (Flyway)

O projeto utiliza **Flyway** para controle de versão do banco de dados:

```
src/main/resources/db/migration/
└── V1__schema_completo.sql
```

Ao iniciar a aplicação, o Flyway executa automaticamente todas as migrations pendentes.

### Seed (Dados Iniciais)

O **DataSeed** popula automaticamente o banco com dados de teste na primeira execução:

- **2 Unidades** (Centro, Zona Sul)
- **5 Usuários** (Cliente, Cozinha, Atendente, Gerente, Admin)
- **4 Produtos** (Tapioca, Cuscuz, Suco de Cajá, Carne de Sol)
- **8 Registros de Estoque** (produtos distribuídos nas unidades)
- **1 Campanha** (Terça da Tapioca)
- **2 Cupons** (NORDESTE10, PROMO5)

**Credenciais de Teste:**

| Perfil | E-mail | Senha |
|--------|--------|-------|
| Cliente | cliente@raizes.com | cliente123 |
| Cozinha | cozinha@raizes.com | cozinha123 |
| Atendente | atendente@raizes.com | atendente123 |
| Gerente | gerente@raizes.com | gerente123 |
| Admin | admin@raizes.com | admin123 |

---

## Endpoints da API

**Base URL:** `http://localhost:8080/api`

### Autenticação

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `POST` | `/auth/login` | Autenticar e obter token JWT | Público |

### Unidades

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `POST` | `/unidades` | Criar unidade | ADMIN |
| `GET` | `/unidades?page=0&limit=10` | Listar unidades ativas (paginado) | Autenticado |
| `GET` | `/unidades/{id}` | Buscar unidade por ID | Autenticado |
| `PUT` | `/unidades/{id}` | Atualizar unidade | ADMIN |
| `DELETE` | `/unidades/{id}` | Desativar unidade (exclusão lógica) | ADMIN |

### Pedidos

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `POST` | `/pedidos` | Criar novo pedido; aceita `Idempotency-Key` | CLIENTE, ATENDENTE |
| `GET` | `/pedidos?canalPedido=APP&status=AGUARDANDO_PAGAMENTO&unidadeId=1&page=0&limit=10` | Listar pedidos com filtros combináveis | Autenticado |
| `GET` | `/pedidos/{id}` | Buscar pedido por ID | Autenticado |
| `PATCH` | `/pedidos/{id}/status` | Atualizar status | COZINHA, ATENDENTE, GERENTE, ADMIN |
| `POST` | `/pedidos/{id}/cancelamento` | Cancelar o pedido e executar as compensações aplicáveis | CLIENTE proprietário, ATENDENTE da unidade, ADMIN |

Na criação de pedidos, o header opcional `Idempotency-Key` deve conter de 8 a 100 caracteres
alfanuméricos ou `.`, `_`, `:`, `-`. A chave é escopada ao usuário autenticado. Repetir a mesma
chave com o mesmo conteúdo devolve o pedido original sem nova reserva de estoque; reutilizá-la com
conteúdo diferente retorna `409 CHAVE_IDEMPOTENCIA_REUTILIZADA`.

### Pagamento

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `POST` | `/pedidos/{pedidoId}/pagamento` | Processar pagamento | CLIENTE proprietário, ATENDENTE da unidade |
| `GET` | `/pedidos/{pedidoId}/pagamento/tentativas` | Consultar o histórico de tentativas | Usuário com acesso ao pedido |

**Nota:** após pagamento recusado, o cliente pode tentar novamente com outro método. Se o gateway
estiver tecnicamente indisponível, a API retorna `503 GATEWAY_PAGAMENTO_INDISPONIVEL`, mantém o
pedido em `AGUARDANDO_PAGAMENTO` e não consome uma tentativa.

O cancelamento de um pedido pago solicita o estorno ao mesmo gateway simulado. O pedido somente é
cancelado depois da confirmação. Um bloqueio pessimista serializa cancelamentos concorrentes do mesmo
pedido, impedindo duas solicitações de estorno. Se o gateway estiver indisponível, a API retorna `503`, preserva o
pagamento e mantém o pedido no estado anterior para permitir nova solicitação.

### Produtos

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `GET` | `/produtos?page=0&limit=10` | Listar produtos ativos (paginado) | Público |
| `GET` | `/produtos/unidade/{unidadeId}?page=0&limit=10` | Listar disponibilidade por unidade (paginado) | Autenticado |
| `GET` | `/produtos/{id}` | Buscar produto por ID | Público |
| `POST` | `/produtos` | Criar produto | ADMIN, GERENTE |
| `PUT` | `/produtos/{id}` | Atualizar produto | ADMIN, GERENTE |
| `DELETE` | `/produtos/{id}` | Desativar produto (soft-delete) | ADMIN, GERENTE |

### Estoque

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `POST` | `/estoque` | Criar registro de estoque | ADMIN, GERENTE da unidade |
| `GET` | `/estoque/unidade/{unidadeId}?page=0&limit=10` | Listar estoque por unidade | ADMIN, GERENTE da unidade |
| `GET` | `/estoque/{id}` | Buscar estoque por ID | ADMIN, GERENTE da unidade |
| `PUT` | `/estoque/{id}` | Atualizar quantidade | ADMIN, GERENTE da unidade |
| `DELETE` | `/estoque/{id}` | Deletar registro | ADMIN, GERENTE da unidade |
| `POST` | `/estoque/{id}/movimentacoes` | Registrar entrada ou saída | ADMIN, GERENTE da unidade |
| `GET` | `/estoque/{id}/movimentacoes?page=0&limit=10` | Consultar histórico auditável | ADMIN, GERENTE da unidade |

**Nota:** saídas manuais nunca podem tornar o saldo negativo. Cada movimentação preserva quantidade,
motivo, saldo anterior, saldo posterior, data e responsável pseudonimizado. No cancelamento, o estoque
é devolvido quando o pedido ainda está em `AGUARDANDO_PAGAMENTO` ou `RECEBIDO`; após o início da
preparação, os itens são considerados consumidos e não retornam ao saldo.

### Fidelidade

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `GET` | `/fidelidade/saldo` | Consultar saldo de pontos | Autenticado |
| `GET` | `/fidelidade/consentimento` | Consultar situação e versão do consentimento | Autenticado |
| `POST` | `/fidelidade/consentimento` | Consentir explicitamente com o uso de pontos | Autenticado |
| `DELETE` | `/fidelidade/consentimento` | Revogar o consentimento | Autenticado |

O resgate de pontos somente é permitido quando o cliente possui consentimento ativo para a versão
vigente do termo (`app.lgpd.termo-fidelidade-versao`). Concessões e revogações são armazenadas como
eventos imutáveis, contendo finalidade, versão, data/hora e origem. Uma nova versão do termo exige
novo aceite. Funcionários podem registrar pedidos para clientes, mas não podem consentir em nome deles.

Se o cancelamento de um pedido exigir a remoção de pontos concedidos que já foram usados em outra
compra, o saldo passa a ser devedor. O cancelamento não é bloqueado: créditos futuros amortizam a
dívida, e nenhum ponto fica disponível para resgate enquanto o saldo não voltar a ser positivo.

### Campanhas

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `POST` | `/campanhas` | Criar campanha | ADMIN, GERENTE da unidade |
| `GET` | `/campanhas?page=0&limit=10` | Listar campanhas (paginado) | ADMIN, GERENTE no próprio escopo |
| `GET` | `/campanhas/{id}` | Buscar campanha por ID | ADMIN, GERENTE da unidade |
| `PUT` | `/campanhas/{id}` | Atualizar campanha | ADMIN, GERENTE das unidades envolvidas |
| `DELETE` | `/campanhas/{id}` | Desativar campanha | ADMIN, GERENTE da unidade |

### Cupons

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `POST` | `/cupons` | Criar cupom | ADMIN; GERENTE da unidade para cupom local |
| `GET` | `/cupons?page=0&limit=10` | Listar cupons (paginado) | ADMIN, GERENTE no próprio escopo |
| `GET` | `/cupons/{id}` | Buscar cupom por ID | ADMIN, GERENTE da unidade |
| `PUT` | `/cupons/{id}` | Atualizar cupom | ADMIN, GERENTE das unidades envolvidas |
| `DELETE` | `/cupons/{id}` | Desativar cupom | ADMIN, GERENTE da unidade |

### Usuários

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `POST` | `/usuarios` | Registrar cliente com aceite dos termos vigentes | Público |
| `GET` | `/usuarios?page=0&limit=10` | Listar usuários (paginado) | ADMIN |
| `GET` | `/usuarios/{id}` | Buscar usuário por ID | ADMIN |
| `PUT` | `/usuarios/{id}` | Atualizar usuário; alteração de perfil somente por ADMIN | Próprio usuário, ADMIN |
| `DELETE` | `/usuarios/{id}` | Anonimizar usuário (LGPD) | Próprio usuário, ADMIN |

### Documentos legais

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| `GET` | `/documentos-legais/termos-uso` | Consultar versão, hash SHA-256 e conteúdo dos termos vigentes | Público |
| `GET` | `/documentos-legais/aviso-privacidade` | Consultar o aviso de privacidade vigente | Público |
| `GET` | `/documentos-legais/termos-uso/aceite` | Consultar o aceite contratual do usuário | Autenticado |
| `PUT` | `/documentos-legais/termos-uso/aceite` | Registrar ou repetir de forma idempotente o aceite vigente | Autenticado |

O cadastro público exige `aceiteTermosUso: true`, a versão e o hash retornados pela consulta pública
dos termos. O servidor registra usuário, documento, versão, hash, data e origem do aceite. Quando a
versão vigente muda, operações autenticadas ficam bloqueadas com `409 TERMOS_USO_NAO_ACEITOS`, exceto
as rotas necessárias para consultar os documentos e renovar o aceite.

Esse aceite possui natureza contratual e não é revogado como um consentimento. O aviso de privacidade
cumpre função informativa. O consentimento LGPD permanece separado e é solicitado exclusivamente para
o uso opcional dos pontos de fidelidade.

---

## Autenticação JWT

### 1. Obter Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cliente@raizes.com",
    "senha": "cliente123"
  }'
```

**Resposta:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tipo": "Bearer"
}
```

### 2. Usar Token nas Requisições

```bash
curl -X GET http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**O token expira, por padrão, em 60 minutos.** Após esse período, obtenha um novo token. O prazo pode ser alterado por meio de `JWT_EXPIRATION_MINUTES`.

### Armazenamento Seguro de Credenciais

Senhas nunca são armazenadas em texto plano: o cadastro e a autenticação usam
`BCryptPasswordEncoder`, com hash e salt gerados automaticamente para cada senha. A senha e seu hash
não são retornados pela API. Conforme a operação e a autorização aplicada, o DTO de resposta do
usuário pode conter id, nome, e-mail, perfil, pontos, telefone e CPF.

---

## Pagamento Determinístico

Para testes consistentes, o sistema suporta **forçar o status do pagamento**:

### Via Header HTTP (Testes Manuais)

```bash
curl -X POST http://localhost:8080/api/pedidos/1/pagamento \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -H "X-Forcar-Status-Pagamento: APROVADO" \
  -d '{
    "formaPagamento": "PIX"
  }'
```

### Via Propriedade no Perfil de Desenvolvimento

```env
# application-dev.properties
PAGAMENTO_FORCAR_STATUS=APROVADO
PAGAMENTO_FORCAR_STATUS_ESTORNO=CONFIRMADO
```

**Valores aceitos:**
- `APROVADO` - Força aprovação
- `RECUSADO` - Força recusa
- `INDISPONIVEL` - Simula falha técnica e retorna `503`, sem alterar o pedido ou consumir tentativa
- (vazio) - Comportamento aleatório (80% aprovação)

O status `INDISPONIVEL` é distinto de uma recusa comercial. O pedido permanece em
`AGUARDANDO_PAGAMENTO`, nenhum pagamento é persistido e a operação pode ser repetida com segurança.

---

## Documentação Swagger

A API está documentada com **OpenAPI 3.1** e disponibiliza uma interface interativa via Swagger UI.
O contrato cobre as **49 operações REST**, com finalidade, permissões, corpos, exemplos,
respostas de sucesso e erros padronizados. As rotas públicas aparecem sem requisito de JWT;
as demais usam o esquema `bearerAuth`.

### Acessar Swagger UI

- **Swagger UI:** http://localhost:8080/api/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api/v3/api-docs
- **OpenAPI YAML:** http://localhost:8080/api/v3/api-docs.yaml

### Contrato estático público

Para permitir a consulta do contrato sem iniciar a aplicação, o repositório mantém cópias
versionadas geradas pelo Springdoc:

- [OpenAPI JSON](docs/openapi/openapi.json)
- [OpenAPI YAML](docs/openapi/openapi.yaml)

As duas representações correspondem ao contrato atual de 49 operações. A interface interativa
do Swagger UI continua disponível ao executar a API localmente com o perfil `dev`.

### Autenticar no Swagger

1. Clique no botão **"Authorize"** no canto superior direito
2. Insira o token JWT obtido em `/auth/login`
3. Clique em **"Authorize"** e depois em **"Close"**
4. Agora você pode testar todos os endpoints protegidos

### Coleção Postman

A coleção [raizes_do_nordeste.postman_collection.json](postman/raizes_do_nordeste.postman_collection.json)
reúne **35 chamadas encadeadas**, identificadas de **T01 a T35**. A seleção demonstra autenticação,
aceite contratual, formato inválido, catálogo e disponibilidade, autorização por perfil e por propriedade,
recurso inexistente, consentimento de fidelidade, criação idempotente de pedidos, estoque insuficiente,
pagamento aprovado ou recusado, histórico auditável correlacionado, transições operacionais,
indisponibilidade e repetição segura do pagamento e do estorno, além da validação do contrato OpenAPI.
A coleção deve ser executada no perfil `dev`, que habilita os dados de
demonstração, a documentação e os cabeçalhos usados para controlar o gateway simulado.

---

## Testes

### Executar Todos os Testes

```bash
./mvnw clean test
```

**Resultado esperado:** 255 testes passando

Os resultados da execução final, incluindo o resumo por classe do Surefire e o relatório JUnit da
coleção Newman, estão preservados em [`docs/evidencias/`](docs/evidencias/README.md).

### Executar Testes Específicos

```bash
# Testes de um service específico
./mvnw test -Dtest=PedidoServiceTest

# Testes de segurança
./mvnw test -Dtest=JwtTokenServiceTest
```

### Cobertura de Testes

O projeto possui **255 testes automatizados**, selecionados para cobrir comportamentos distintos sem
repetir combinações equivalentes de validação. A suíte abrange:

- Services (regras de negócio)
- Security (JWT, autenticação e exigência de aceite contratual vigente)
- Exceptions (tratamento de erros)
- Validações (Bean Validation)
- Integração da aplicação, configuração dos ambientes e migração Flyway
- Persistência e concorrência de estoque, cupons e campanhas
- Contrato OpenAPI (49 operações, segurança, respostas e exemplos)

---

## Estrutura do Projeto

```
projeto-back-end-uninter/
├── src/
│   ├── main/
│   │   ├── java/com/raizesdonordeste/backendapi/
│   │   │   ├── config/              # Seed, OpenAPI e correlação de requisições
│   │   │   ├── controller/          # Contrato HTTP e controllers REST
│   │   │   ├── dto/                 # Objetos de entrada e saída da API
│   │   │   ├── exception/           # Exceções e tratamento global
│   │   │   ├── gateway/             # Contrato e implementação do gateway simulado
│   │   │   ├── model/               # Entidades JPA e enumerações do domínio
│   │   │   ├── repository/          # Repositórios e operações concorrentes
│   │   │   ├── security/            # JWT, filtros e handlers de segurança
│   │   │   ├── service/             # Regras de negócio, autorização e reservas
│   │   │   └── util/                # Pseudonimização para logs
│   │   └── resources/
│   │       ├── db/migration/        # Migrations Flyway
│   │       ├── legal/               # Termos de uso e aviso de privacidade versionados
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── application-prod.properties
│   └── test/                        # Testes automatizados unitários e de integração
├── docs/
│   ├── diagramas-casos-de-uso/      # Figuras 1 a 4 e fontes PlantUML
│   ├── diagramas-dados/             # Figuras 5 e 6 e fontes PlantUML
│   ├── diagramas-fluxos/            # Figuras 7 a 9 e fontes PlantUML
│   ├── openapi/                      # Contrato estático em JSON e YAML
│   └── evidencias/                  # Resumos verificáveis das execuções de teste
├── postman/                         # Coleção com 35 chamadas encadeadas
├── .env.example                     # Template de variáveis de ambiente
├── .gitignore                       # Ignorados pelo Git
├── LICENSE                          # Licença GPL v3
├── pom.xml                          # Dependências Maven
└── README.md                        # Este arquivo
```

---

## Códigos de Status HTTP

A API segue o padrão REST e retorna códigos HTTP apropriados:

| Código | Significado | Quando é Retornado |
|--------|-------------|-------------------|
| `200 OK` | Sucesso | Operação concluída com corpo de resposta |
| `201 Created` | Criado | Recurso ou tentativa criado com sucesso |
| `204 No Content` | Sem conteúdo | Operação concluída sem corpo de resposta |
| `400 Bad Request` | Requisição inválida | Dados malformados, parâmetros inválidos |
| `401 Unauthorized` | Não autenticado | Token ausente, expirado ou inválido |
| `403 Forbidden` | Acesso negado | Usuário sem permissão |
| `404 Not Found` | Não encontrado | Recurso inexistente |
| `409 Conflict` | Conflito | Violação de regra de negócio |
| `422 Unprocessable Entity` | Erro de validação | Campos inválidos |
| `500 Internal Server Error` | Erro interno | Erro inesperado no servidor |
| `503 Service Unavailable` | Serviço indisponível | Indisponibilidade temporária do gateway simulado |

As falhas seguem o mesmo contrato de resposta. Erros da aplicação são tratados pelo
`GlobalExceptionHandler`; falhas de autenticação e autorização são produzidas pelos handlers de
segurança equivalentes, preservando o mesmo formato:

```json
{
  "error": "ESTOQUE_INSUFICIENTE",
  "message": "Não há quantidade suficiente para um ou mais itens.",
  "details": [
    { "field": "itens[0].quantidade", "issue": "Disponível: 1" }
  ],
  "timestamp": "2026-02-05T12:00:00Z",
  "path": "/api/pedidos",
  "requestId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab"
}
```

- `error`: código estável da falha ou regra violada.
- `details`: lista vazia quando não há campos específicos envolvidos.
- `requestId`: corresponde ao `X-Request-ID` e permite correlacionar a resposta com os logs.

---

## CORS (Cross-Origin Resource Sharing)

A API suporta CORS para integração com clientes executados em navegadores web.

### Configuração em Desenvolvimento

```env
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200,http://localhost:5173
CORS_ALLOWED_HEADERS=*
CORS_ALLOW_CREDENTIALS=true
```

### Configuração em Produção

```env
CORS_ALLOWED_ORIGINS=https://app.raizesdonordeste.com.br
CORS_ALLOWED_HEADERS=Authorization,Content-Type,Idempotency-Key,X-Request-ID
CORS_ALLOW_CREDENTIALS=true
```

### Testar CORS

```bash
curl -X OPTIONS http://localhost:8080/api/produtos \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -v
```


---

## Troubleshooting

### Erro 404 em Todas as Rotas

**Causa:** Context path não configurado corretamente.

**Solução:** Todas as rotas devem começar com `/api`:
```bash
# Correto
curl http://localhost:8080/api/produtos

# Incorreto
curl http://localhost:8080/produtos
```

### Erro 401 Unauthorized

**Causa:** Token JWT ausente ou expirado.

**Solução:**
1. Faça login em `/api/auth/login`
2. Copie o token retornado
3. Inclua no header: `Authorization: Bearer {token}`

### Erro 500 Internal Server Error

**Causa:** Erro interno no servidor.

**Solução:** Verifique os logs da aplicação para detalhes do erro.

### Swagger Não Carrega

**Causa:** Profile de produção ativo (Swagger desabilitado).

**Solução:** Execute com profile `dev`:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Autor

**Humberto S.**  
RU: 4819398  
Tecnologia em Análise e Desenvolvimento de Sistemas  
Centro Universitário Internacional UNINTER - 2026

---

## Licença

Este projeto está licenciado sob a **GNU General Public License v3.0** - veja o arquivo [LICENSE](LICENSE) para detalhes.

### Resumo da Licença

- Uso comercial permitido
- Modificação permitida
- Distribuição permitida
- Uso privado permitido
- Derivados devem ser GPL v3
- Código fonte deve ser disponibilizado
- O mesmo aviso de copyright e licença deve ser preservado


## Integração contínua (CI)

O workflow [CI](.github/workflows/ci.yml) executa em pushes, pull requests e acionamento manual pelo GitHub Actions. O ambiente usa Ubuntu 24.04, Java 17 (Temurin) e a versão do Maven definida no Wrapper do repositório, com cache de dependências.

Para reproduzir a verificação localmente:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

A execução compila a aplicação, executa os testes configurados e gera o pacote. Falhas de compilação ou de testes deixam o check vermelho. Os relatórios Surefire (e Failsafe, caso esse plugin seja configurado futuramente) são disponibilizados como artefatos por 14 dias, inclusive quando os testes falham. Se a compilação falhar antes de gerar relatórios, consulte os logs do job. O nome do artefato identifica o commit e a tentativa de execução.

Consulte o [histórico das execuções](https://github.com/salguento/projeto-back-end-uninter/actions/workflows/ci.yml) para verificar o resultado e baixar os relatórios. O badge mostra o estado do workflow; a contagem atual de testes deve ser consultada nos relatórios, não em um número fixo no README.

**Escopo:** a suíte atual usa H2. Este workflow não valida PostgreSQL, não executa a coleção Postman, não mede desempenho e não publica a aplicação. Os registros locais em `docs/evidencias` permanecem como evidência histórica. A configuração do workflow, isoladamente, não comprova uma execução bem-sucedida: confirme o resultado no histórico do Actions.
