# Design

## Context

`nutri-back` é um projeto Spring Boot greenfield: Java 25, Spring Boot 4.1.1, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-webmvc`, MySQL (`mysql-connector-j`). Não existe nenhum código de domínio ainda (apenas a classe `NutriApplication` gerada pelo Spring Initializr) e nenhuma tabela é usada em produção, então não há dado legado para migrar. O frontend é um repositório separado que consumirá esta API via HTTP; não há renderização server-side de páginas. Ver proposal.md - Why para a motivação e specs/nutritionist-auth/spec.md para o contrato de comportamento.

## Goals / Non-Goals

**Goals:**
- Definir um mecanismo de autenticação stateless adequado a uma API REST consumida por um frontend separado (SPA/mobile), sem exigir sessão fixada no servidor.
- Definir a estrutura de pacotes, entidade JPA e camadas (controller/service/repository) que as demais capacidades (`patient-management`, `diet-prescription`, `physical-assessment`) vão seguir e reutilizar (ex.: extrair o nutricionista autenticado do contexto de segurança).
- Definir o formato único de resposta de erro usado por toda a API, não apenas por esta capacidade.

**Non-Goals:**
- Recuperação de senha, verificação de e-mail, MFA — fora de escopo também na especificação de sistema (`nutri-specs`).
- Refresh tokens de longa duração ou rotação de chaves — pode ser adicionado depois sem quebrar o contrato de API definido aqui.
- Rate limiting / proteção contra força bruta em login — sinalizado como risco abaixo, não implementado nesta change.

## Decisions

### Autenticação: JWT stateless em vez de sessão HTTP tradicional
Escolhido **JWT assinado (HS256)**, retornado no corpo da resposta de login e enviado pelo cliente no cabeçalho `Authorization: Bearer <token>`, em vez de sessão HTTP com cookie (`HttpSession` + `spring-boot-starter-security` baseado em cookie).

Justificativa:
- O backend serve uma API consumida por um frontend em outro repositório/origem; sessão baseada em cookie exigiria configuração de CORS com credenciais e SameSite mais delicada, e amarra o backend a manter estado de sessão em memória/servidor (ou store externo) desde o primeiro endpoint.
- JWT stateless mantém o servidor sem estado de sessão, o que simplifica escalar horizontalmente o backend mais tarde, sem depender de sticky sessions ou store de sessão compartilhado.
- O logout com JWT puro é normalmente "stateless" (o cliente descarta o token) e não invalida o token no servidor antes da expiração. Como o requisito de spec exige que o logout invalide o token corrente, este design adota uma **denylist de tokens revogados** (tabela `revoked_tokens` com o `jti` do token e sua expiração original) consultada pelo filtro de autenticação; entradas expiradas podem ser limpas por rotina futura (fora de escopo desta change, registrado como risco).
- Alternativa considerada: sessão HTTP tradicional com `Spring Session`. Rejeitada para esta fatia porque adiciona um requisito de infraestrutura (store de sessão compartilhado) sem necessidade imediata, e o time já sinalizou a API como stateless-first.

### Hash de senha: BCrypt via `PasswordEncoder` do Spring Security
Usa-se `BCryptPasswordEncoder` (já disponível via `spring-boot-starter-security`), custo padrão (10), sem dependência nova. A senha em texto puro nunca é logada nem persistida; apenas o hash é armazenado na coluna `password_hash`.

### Estrutura de pacotes
Segue convenção por camada dentro do pacote base `com.br.nutri`, preparando para novas capacidades adicionarem seus próprios subpacotes de domínio depois:
```
com.br.nutri
├── nutritionist/
│   ├── Nutricionista.java            (entidade JPA)
│   ├── NutricionistaRepository.java
│   ├── NutricionistaService.java
│   ├── NutricionistaController.java   (POST /api/nutricionistas)
│   └── dto/ (RegisterRequest, NutricionistaResponse)
├── auth/
│   ├── AuthController.java            (POST /api/auth/login, /api/auth/logout)
│   ├── AuthService.java
│   ├── JwtService.java                (emissão/validação/parse do token)
│   ├── RevokedToken.java + RevokedTokenRepository.java
│   └── dto/ (LoginRequest, LoginResponse)
├── security/
│   ├── SecurityConfig.java            (SecurityFilterChain, PasswordEncoder bean)
│   └── JwtAuthenticationFilter.java    (resolve o token, popula o SecurityContext)
└── common/
    ├── ApiError.java                  (formato de erro padronizado)
    └── GlobalExceptionHandler.java     (@RestControllerAdvice)
```

### Modelo de dados: entidade `Nutricionista`
Tabela `nutricionistas`:
| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | `BIGINT` (auto-increment) | PK |
| `nome_completo` | `VARCHAR(255)` | NOT NULL |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE (armazenado em lowercase) |
| `password_hash` | `VARCHAR(255)` | NOT NULL (nunca exposto em DTO de resposta) |
| `criado_em` | `TIMESTAMP` | NOT NULL, default now |

Tabela auxiliar `revoked_tokens` (suporte ao logout stateless):
| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | `BIGINT` (auto-increment) | PK |
| `jti` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `expira_em` | `TIMESTAMP` | NOT NULL (usado para permitir limpeza futura) |

Migração via **Flyway** (`spring-boot-starter-data-jpa` + script SQL versionado em `src/main/resources/db/migration`), em vez de `spring.jpa.hibernate.ddl-auto=update`, para que o schema seja explícito e versionado desde a primeira tabela do projeto.

### Formato de resposta de erro
Corpo JSON único para toda a API, retornado por um `@RestControllerAdvice` global:
```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Descrição legível do problema",
  "details": [ { "field": "email", "message": "formato de e-mail inválido" } ],
  "timestamp": "2026-09-17T12:00:00Z"
}
```
`error` é um código estável (`VALIDATION_ERROR`, `EMAIL_ALREADY_IN_USE`, `INVALID_CREDENTIALS`, `UNAUTHORIZED`) que o frontend pode usar para lógica condicional sem depender do texto de `message`. `details` é omitido quando não há múltiplos campos a reportar.

### Estratégia de testes
- **Testes unitários** (`spring-boot-starter-*-test`, JUnit 5): validação de regras de negócio no `NutricionistaService` (rejeição de e-mail duplicado, hashing de senha) e no `AuthService`/`JwtService` (emissão, expiração, rejeição de token revogado), usando mocks de repositório.
- **Testes de integração**: `@SpringBootTest` + `MockMvc` (ou `@WebMvcTest` para os controllers com serviços mockados) cobrindo cada cenário do spec delta (cadastro sucesso/duplicado/validação, login sucesso/credenciais inválidas, logout, acesso negado sem token/token inválido) contra um banco de teste (H2 ou Testcontainers MySQL — a decidir na tarefa de setup, ver tasks.md).

## Riscos / Trade-offs

- [Denylist de tokens revogados cresce indefinidamente sem rotina de limpeza] → Aceito nesta change porque o volume inicial é baixo; uma mudança futura deve adicionar expurgo agendado de registros com `expira_em` no passado.
- [Sem rate limiting no login, uma tentativa de força bruta de senha não é mitigada nesta change] → Aceito como não-objetivo explícito; sinalizado para uma mudança de segurança futura antes de produção.
- [Chave de assinatura JWT única (HS256) versus par de chaves assimétrico] → HS256 com chave simétrica escolhido por simplicidade, já que o mesmo backend emite e valida o token (não há terceiro validando o token de forma independente); migrar para RS256 é possível depois sem mudar o contrato de API se necessário.
- [Escolha entre H2 e Testcontainers para testes de integração ainda em aberto] → Não bloqueia a especificação nem a lista de tarefas; será decidido na tarefa de setup do ambiente de teste (tasks.md), pois qualquer uma das opções satisfaz os mesmos cenários de teste.
