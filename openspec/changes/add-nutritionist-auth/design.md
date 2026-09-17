# Design

## Context

`nutri-back` é um projeto Spring Boot greenfield: Java 25, Spring Boot 4.1.1, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-webmvc`, MySQL (`mysql-connector-j`). Não existe nenhum código de domínio ainda (apenas a classe `NutriApplication` gerada pelo Spring Initializr) e nenhuma tabela é usada em produção, então não há dado legado para migrar. O frontend é um repositório separado que consumirá esta API via HTTP, em outra origem (necessitando CORS); não há renderização server-side de páginas. Ver proposal.md - Why para a motivação e specs/nutritionist-auth/spec.md para o contrato de comportamento.

## Goals / Non-Goals

**Goals:**
- Definir um mecanismo de autenticação stateless adequado a uma API REST consumida por um frontend separado (SPA/mobile), sem exigir sessão fixada no servidor.
- Definir a estrutura de pacotes, entidade JPA e camadas (controller/service/repository) que as demais capacidades (`patient-management`, `diet-prescription`, `physical-assessment`) vão seguir e reutilizar (ex.: extrair o nutricionista autenticado do contexto de segurança).
- Definir o formato único de resposta de erro usado por toda a API, não apenas por esta capacidade.
- Definir como o login social via Google coexiste com o login por e-mail/senha, emitindo o mesmo tipo de token de acesso (JWT) independentemente do método de autenticação usado, para que o restante da API não precise diferenciar a origem da sessão.
- Definir explicitamente a configuração de CORS, CSRF e política de sessão necessária para uma API stateless consumida por um frontend em outra origem, e o tratamento de condições de corrida em cadastro/login que criam registros.

**Non-Goals:**
- Recuperação de senha, verificação de e-mail, MFA — fora de escopo também na especificação de sistema (`nutri-specs`).
- Refresh tokens de longa duração ou rotação de chaves — pode ser adicionado depois sem quebrar o contrato de API definido aqui.
- Rate limiting / proteção contra força bruta em login — sinalizado como risco abaixo, não implementado nesta change.
- Endpoint para adicionar senha local a uma conta criada via Google, ou para desvincular uma conta Google — sinalizado como risco/lacuna abaixo.

## Decisions

### Autenticação: JWT stateless em vez de sessão HTTP tradicional
Escolhido **JWT assinado (HS256)**, retornado no corpo da resposta de login e enviado pelo cliente no cabeçalho `Authorization: Bearer <token>`, em vez de sessão HTTP com cookie (`HttpSession` + `spring-boot-starter-security` baseado em cookie).

Justificativa:
- O backend serve uma API consumida por um frontend em outro repositório/origem; sessão baseada em cookie exigiria configuração de CORS com credenciais e SameSite mais delicada, e amarra o backend a manter estado de sessão em memória/servidor (ou store externo) desde o primeiro endpoint.
- JWT stateless evita depender de afinidade de sessão (sticky session) ou de replicar um store de sessão completo entre instâncias para escalar horizontalmente — o servidor não precisa manter o conteúdo da sessão (dados do usuário, atributos) em lugar nenhum, só validar a assinatura do token.
- O logout com JWT puro é normalmente "stateless" (o cliente descarta o token) e não invalida o token no servidor antes da expiração. Como o requisito de spec exige que o logout invalide o token corrente, este design adota uma **denylist de tokens revogados** (tabela `revoked_tokens` com o `jti` do token e sua expiração original) consultada pelo filtro de autenticação; entradas expiradas podem ser limpas por rotina futura (fora de escopo desta change, registrado como risco).
- **Trade-off reconhecido**: essa denylist reintroduz uma consulta ao banco em toda requisição autenticada — o ganho real de escolher JWT aqui não é "zero estado consultado por requisição", e sim evitar replicar o *conteúdo* da sessão entre instâncias e não depender de sticky sessions. Se essa consulta se tornar gargalo sob carga, uma extensão futura pode cachear a denylist (ex.: Redis, ou um filtro Bloom em memória) em vez de consultar o MySQL a cada requisição; sinalizado como risco abaixo, não implementado nesta change.
- Alternativa considerada: sessão HTTP tradicional com `Spring Session`. Rejeitada para esta fatia porque adiciona um requisito de infraestrutura (store de sessão compartilhado) sem necessidade imediata, e o time já sinalizou a API como stateless-first.

Expiração do token: configurável via variável de ambiente (ver tasks.md 1.5), sem valor hardcoded no código. Valor padrão **sugerido** para o ambiente configurado (não um default silencioso no código, já que a variável é obrigatória): 3600 segundos (1 hora). Um token mais longo aumenta a janela de exposição em caso de vazamento; como não há refresh token nesta change (ver Non-Goals), esse valor deve equilibrar segurança com a frequência aceitável de novo login — reavaliar se o feedback de uso mostrar re-login excessivo.

### Hash de senha: BCrypt via `PasswordEncoder` do Spring Security
Usa-se `BCryptPasswordEncoder` (já disponível via `spring-boot-starter-security`), custo padrão (10), sem dependência nova. A senha em texto puro nunca é logada nem persistida; apenas o hash é armazenado na coluna `password_hash`. Essa coluna passa a ser **opcional** (ver Modelo de dados) porque contas criadas via login Google não têm senha local.

A senha recebida no cadastro é limitada a 72 caracteres (ver Validações), alinhado ao limite de entrada efetiva do BCrypt (que trunca silenciosamente qualquer conteúdo além de 72 bytes) e como mitigação a payloads de senha artificialmente longos, que aumentariam o custo de CPU do hashing sem ganho de segurança (o BCrypt é deliberadamente lento; aceitar entradas sem limite de tamanho é um vetor de negação de serviço por exaustão de CPU).

### Configuração de CORS, CSRF e política de sessão
Como o frontend é servido de uma origem diferente e a API é stateless (sem cookie de sessão), o `SecurityConfig` precisa declarar explicitamente:
- **CORS**: lista de origens permitidas configurável via variável de ambiente `ALLOWED_ORIGINS` (uma ou mais origens, sem valor hardcoded), liberando os métodos usados pela API (`GET`, `POST`, `PUT`, `DELETE`) e incluindo o cabeçalho `Authorization` na lista de cabeçalhos permitidos — sem isso, o navegador do frontend bloqueia as chamadas antes mesmo de chegarem ao backend.
- **CSRF desabilitado** (`.csrf(csrf -> csrf.disable())`): proteção CSRF é uma defesa contra requisições autenticadas por cookie enviadas involuntariamente pelo navegador; como a autenticação é feita por token enviado explicitamente no cabeçalho `Authorization` (não por cookie), CSRF não se aplica e, se deixado habilitado por padrão, bloquearia `POST`s legítimos do frontend.
- **Política de sessão stateless** (`SessionCreationPolicy.STATELESS`): impede que o Spring Security crie ou dependa de `HttpSession`, reforçando que toda informação de autenticação vem do token a cada requisição.

### Tratamento de condição de corrida em cadastro e login via Google
Tanto o cadastro tradicional quanto o login via Google seguem o padrão "verifica se existe, senão cria" (check-then-act), que sozinho não é seguro contra duas requisições concorrentes com o mesmo e-mail (ou, no caso do Google, o mesmo e-mail vindo de duas chamadas simultâneas para uma conta ainda inexistente). A garantia final de unicidade vem da constraint `UNIQUE` no banco (`email`, e `google_subject` quando presente), não da checagem prévia em si. Por isso, `NutritionistService.register` e `AuthService.loginWithGoogle` DEVEM capturar a exceção de violação de constraint única na persistência (`DataIntegrityViolationException`) e:
- No cadastro tradicional: traduzir a exceção para o mesmo erro de conflito 409 `EMAIL_ALREADY_IN_USE` já usado quando a duplicidade é detectada pela checagem prévia.
- No login via Google: se a violação ocorreu porque outra requisição concorrente já criou a conta para o mesmo e-mail, reconsultar por e-mail e autenticar a conta que "venceu" a corrida, em vez de propagar erro ao cliente — do ponto de vista do usuário, ambas as chamadas devem terminar autenticadas com sucesso (ver spec - "Logins via Google concorrentes para conta nova").

### Login social: Google OAuth 2.0 / OpenID Connect coexistindo com login tradicional
Adiciona-se um novo endpoint `POST /api/auth/google`, que recebe `{ "idToken": "<token de identidade emitido pelo Google no cliente>" }` e retorna `{ "token": "<JWT>", "accountCreated": true|false }` (DTO `GoogleLoginResponse`) — do ponto de vista do restante da API, uma sessão iniciada via Google é indistinguível de uma sessão iniciada por e-mail/senha; o campo `accountCreated` existe só para o frontend eventualmente exibir uma mensagem diferente (ex.: "conta criada com sucesso" vs. "bem-vindo de volta"), sem que isso exija variar o código HTTP de resposta (sempre 200, ver spec).

Fluxo:
1. O backend valida o `idToken` usando a biblioteca oficial `com.google.api-client:google-api-client` (`GoogleIdTokenVerifier`), verificando assinatura contra as chaves públicas (JWKS) do Google, emissor (`accounts.google.com`), audiência (Client ID configurado via `GOOGLE_CLIENT_ID`) e a claim `email_verified`.
2. Se já existe `Nutritionist` com o e-mail do token (criado via cadastro tradicional ou por login Google anterior), autentica essa conta e grava/atualiza a claim `sub` do Google na coluna `google_subject` se ainda não estiver setada. Os campos de perfil já cadastrados (`name`, `company`) **não são sobrescritos** pelos dados vindos do Google nesse momento — o cadastro existente é preservado como fonte de verdade do perfil (ver spec - "Perfil existente preservado ao vincular conta Google").
3. Se não existe, cria uma nova conta com nome e e-mail do token, `password_hash` nulo e `google_subject` preenchido, sem exigir senha. Se a criação falhar por violação de constraint única (corrida concorrente, ver decisão acima), reconsulta por e-mail e autentica a conta resultante em vez de propagar erro.
4. Em ambos os casos, emite um token de acesso via o mesmo `JwtService` usado pelo login tradicional, e retorna `accountCreated=true` apenas no caminho 3.

O login tradicional (e-mail/senha) passa a verificar que `password_hash` não é nulo antes de comparar a senha; uma conta criada só via Google tentando logar com senha recebe a mesma resposta 401 genérica de credenciais inválidas (ver spec - "Login tradicional em conta sem senha local"), preservando a regra já especificada de não revelar se uma conta existe. Essa mesma conta, se tentar se cadastrar novamente pela rota tradicional para "ganhar" uma senha, recebe o erro genérico de e-mail duplicado (409) — não há, nesta change, um endpoint para adicionar senha local a uma conta só-Google (ver Riscos/Trade-offs).

Justificativa da biblioteca oficial em vez de validação manual do JWT contra o JWKS do Google: a biblioteca já trata rotação de chaves, cache e expiração corretamente; reimplementar isso manualmente adicionaria risco de segurança sem benefício.

Alternativa considerada: usar `spring-security-oauth2-client` com fluxo de redirecionamento completo (Authorization Code) gerenciado pelo backend. Rejeitada para esta fatia porque o frontend (SPA separado) já obtém o `id_token` diretamente do Google no cliente (fluxo "One Tap"/Google Identity Services), então o backend só precisa validar o token recebido, não orquestrar o fluxo de redirecionamento OAuth completo.

### Estrutura de pacotes
Segue convenção por camada dentro do pacote base `com.br.nutri`, preparando para novas capacidades adicionarem seus próprios subpacotes de domínio depois:
```
com.br.nutri
├── nutritionist/
│   ├── Nutritionist.java              (entidade JPA)
│   ├── NutritionistRepository.java
│   ├── NutritionistService.java
│   ├── NutritionistController.java    (POST /api/nutricionistas, GET /api/nutricionistas/me)
│   └── dto/ (RegisterRequest, NutritionistResponse)
├── auth/
│   ├── AuthController.java            (POST /api/auth/login, /api/auth/logout, /api/auth/google)
│   ├── AuthService.java
│   ├── GoogleTokenVerifierService.java (valida o idToken do Google via GoogleIdTokenVerifier)
│   ├── JwtService.java                (emissão/validação/parse do token)
│   ├── RevokedToken.java + RevokedTokenRepository.java
│   └── dto/ (LoginRequest, LoginResponse, GoogleLoginRequest, GoogleLoginResponse)
├── security/
│   ├── SecurityConfig.java            (SecurityFilterChain, CORS, CSRF, SessionCreationPolicy, PasswordEncoder bean)
│   └── JwtAuthenticationFilter.java    (resolve o token, popula o SecurityContext)
└── common/
    ├── ApiError.java                  (formato de erro padronizado)
    └── GlobalExceptionHandler.java     (@RestControllerAdvice)
```

### Modelo de dados: entidade `Nutritionist`
Tabela `nutritionists`:
| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | `BIGINT` (auto-increment) | PK |
| `name` | `VARCHAR(255)` | NOT NULL |
| `company` | `VARCHAR(255)` | NULL (opcional) |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE (armazenado em lowercase) |
| `password_hash` | `VARCHAR(255)` | NULL (nunca exposto em DTO de resposta; nulo para contas criadas exclusivamente via Google) |
| `google_subject` | `VARCHAR(255)` | NULL, UNIQUE quando presente (claim `sub` do token Google, usada para reconhecer a mesma conta Google em logins futuros mesmo se o e-mail mudar) |
| `created_at` | `TIMESTAMP` | NOT NULL, default now |

Constraint de aplicação (não apenas de banco): todo `Nutritionist` deve ter `password_hash` não nulo, `google_subject` não nulo, ou ambos — nunca os dois nulos ao mesmo tempo, já que a conta precisa de pelo menos um método de autenticação. Nesta change, essa invariante é satisfeita automaticamente pelos dois únicos caminhos de criação existentes (cadastro tradicional sempre define `password_hash`; login Google sempre define `google_subject`); se uma mudança futura permitir remover um dos dois (ex.: desvincular Google, ou "esquecer senha" com exclusão), essa invariante precisará virar uma verificação explícita antes de persistir.

Tabela auxiliar `revoked_tokens` (suporte ao logout stateless):
| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | `BIGINT` (auto-increment) | PK |
| `jti` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `expires_at` | `TIMESTAMP` | NOT NULL (usado para permitir limpeza futura) |

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
`error` é um código estável (`VALIDATION_ERROR`, `EMAIL_ALREADY_IN_USE`, `INVALID_CREDENTIALS`, `UNAUTHORIZED`, `GOOGLE_TOKEN_INVALID`) que o frontend pode usar para lógica condicional sem depender do texto de `message`. `details` é omitido quando não há múltiplos campos a reportar.

### Estratégia de testes
- **Testes unitários** (`spring-boot-starter-*-test`, JUnit 5): validação de regras de negócio no `NutritionistService` (rejeição de e-mail duplicado, hashing de senha, campo empresa opcional, tratamento de `DataIntegrityViolationException` na corrida de cadastro) e no `AuthService`/`JwtService` (emissão, expiração, rejeição de token revogado), usando mocks de repositório. `GoogleTokenVerifierService` é testado com um `GoogleIdTokenVerifier` mockado, cobrindo token válido, assinatura/emissor/audiência inválidos e e-mail não verificado, sem depender de rede.
- **Testes de integração**: `@SpringBootTest` + `MockMvc` (ou `@WebMvcTest` para os controllers com serviços mockados) cobrindo cada cenário do spec delta (cadastro sucesso/duplicado/validação/limites de tamanho, login sucesso/credenciais inválidas, login tradicional em conta só-Google, login e cadastro automático via Google, vinculação a conta existente via Google com preservação de perfil, token Google inválido/e-mail não verificado, consulta do próprio perfil, logout, acesso negado sem token/token inválido) contra um banco de teste (H2 ou Testcontainers MySQL — a decidir na tarefa de setup, ver tasks.md).

## Riscos / Trade-offs

- [Denylist de tokens revogados cresce indefinidamente sem rotina de limpeza] → Aceito nesta change porque o volume inicial é baixo; uma mudança futura deve adicionar expurgo agendado de registros com `expires_at` no passado.
- [Sem rate limiting no login, uma tentativa de força bruta de senha não é mitigada nesta change] → Aceito como não-objetivo explícito; sinalizado para uma mudança de segurança futura antes de produção.
- [Chave de assinatura JWT única (HS256) versus par de chaves assimétrico] → HS256 com chave simétrica escolhido por simplicidade, já que o mesmo backend emite e valida o token (não há terceiro validando o token de forma independente); migrar para RS256 é possível depois sem mudar o contrato de API se necessário.
- [Escolha entre H2 e Testcontainers para testes de integração ainda em aberto] → Não bloqueia a especificação nem a lista de tarefas; será decidido na tarefa de setup do ambiente de teste (tasks.md), pois qualquer uma das opções satisfaz os mesmos cenários de teste.
- [Conta criada apenas via Google não tem senha local, e não há endpoint para adicionar uma depois] → Aceito nesta change; se essa conta tentar se cadastrar novamente pela rota tradicional com o mesmo e-mail (na tentativa de "ganhar" uma senha), recebe o erro genérico de e-mail duplicado (409), sem orientação — comportamento intencional e conhecido desta change, não um bug; resolver isso (endpoint de "definir senha") fica para uma mudança futura.
- [Nome/e-mail exibidos pela conta Google podem divergir do que o nutricionista cadastraria manualmente] → Mitigado ao decidir que a vinculação nunca sobrescreve o perfil já existente (ver Decisions); para contas criadas automaticamente via Google, o nutricionista pode editar seu próprio perfil depois (fora do escopo desta change, que cobre apenas cadastro/login).
- [Dependência de disponibilidade dos servidores do Google para validar o token] → Se o Google estiver indisponível, apenas o login via Google fica indisponível; o login tradicional por e-mail/senha continua funcionando normalmente, pois os dois mecanismos são independentes.
- [Denylist consultada a cada requisição autenticada reduz o ganho de "stateless" da escolha de JWT] → Aceito nesta change dado o volume inicial esperado; se virar gargalo, uma mudança futura deve cachear a denylist (ex.: Redis) em vez de consultar o MySQL diretamente a cada requisição.
