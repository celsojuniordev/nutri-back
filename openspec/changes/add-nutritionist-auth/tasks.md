# Tasks

## 1. Setup de Dependências e Infraestrutura

- [ ] 1.1 Adicionar dependência de JWT ao `build.gradle` (ex.: `io.jsonwebtoken:jjwt-api`/`jjwt-impl`/`jjwt-jackson`) e verificar que `./gradlew build` resolve as dependências sem erro
- [ ] 1.2 Adicionar dependência do Flyway (`org.flywaydb:flyway-core` + `flyway-mysql`) ao `build.gradle` e verificar que a aplicação sobe com `spring.jpa.hibernate.ddl-auto=validate` (schema gerenciado só pelo Flyway a partir daqui)
- [ ] 1.3 Adicionar dependência de validação de token Google (`com.google.api-client:google-api-client`) ao `build.gradle` e verificar que `./gradlew build` resolve a dependência sem erro
- [ ] 1.4 Decidir e configurar a estratégia de banco de dados de teste (H2 em memória ou Testcontainers MySQL, ver design.md - Riscos/Trade-offs) e verificar que um teste de contexto Spring (`@SpringBootTest`) sobe com sucesso usando essa estratégia
- [ ] 1.5 Adicionar propriedades de configuração da chave/segredo JWT e do tempo de expiração do token em `application.properties` (via variável de ambiente, sem valor hardcoded) e verificar que a aplicação falha ao subir se a variável obrigatória estiver ausente
- [ ] 1.6 Adicionar a propriedade de configuração `GOOGLE_CLIENT_ID` (via variável de ambiente) em `application.properties` e verificar que a aplicação falha ao subir se a variável obrigatória estiver ausente

## 2. Modelo de Dados e Migração

- [ ] 2.1 Criar a migração Flyway (`V1__create_nutricionistas.sql`) para a tabela `nutricionistas` (`id`, `nome`, `empresa` opcional, `email` único, `password_hash` opcional, `google_subject` opcional e único, `criado_em`) e verificar que a migração aplica sem erro em ambiente local
- [ ] 2.2 Criar a migração Flyway (`V2__create_revoked_tokens.sql`) para a tabela `revoked_tokens` (`id`, `jti` único, `expira_em`) e verificar que a migração aplica sem erro
- [ ] 2.3 Implementar a entidade JPA `Nutricionista` (campos `nome`, `empresa` nullable, `email`, `passwordHash` nullable, `googleSubject` nullable) e o repositório `NutricionistaRepository` (busca por e-mail case-insensitive e busca por `googleSubject`) e verificar com um teste de repositório (`@DataJpaTest`) que as constraints de unicidade de e-mail e de `google_subject` são respeitadas
- [ ] 2.4 Implementar a entidade JPA `RevokedToken` e o repositório `RevokedTokenRepository` e verificar com um teste de repositório que um `jti` inserido é encontrado por busca e que duplicatas são rejeitadas

## 3. Camada de Segurança

- [ ] 3.1 Configurar o bean `PasswordEncoder` (`BCryptPasswordEncoder`) e verificar com um teste unitário que uma senha em texto puro gera um hash diferente a cada chamada e que `matches` reconhece a senha correta
- [ ] 3.2 Implementar `JwtService` (emissão de token com `jti`, `sub` = id do nutricionista, expiração; validação e parse) e verificar com testes unitários a emissão, a validação de um token válido e a rejeição de um token expirado ou com assinatura inválida
- [ ] 3.3 Implementar `JwtAuthenticationFilter` que extrai o token do cabeçalho `Authorization: Bearer`, valida contra `JwtService` e `RevokedTokenRepository`, e popula o `SecurityContext` com a identidade do nutricionista e verificar com um teste de filtro que uma requisição com token válido popula o contexto de segurança
- [ ] 3.4 Configurar `SecurityConfig` (`SecurityFilterChain`) para exigir autenticação em todos os endpoints exceto cadastro, login e login via Google, registrar o `JwtAuthenticationFilter`, e verificar com um teste de integração que um endpoint protegido de teste retorna HTTP 401 sem token

## 4. Tratamento de Erros

- [ ] 4.1 Implementar o DTO `ApiError` no formato definido em design.md (status, error, message, details, timestamp) e verificar com um teste unitário de serialização que o JSON gerado segue exatamente essa estrutura
- [ ] 4.2 Implementar `GlobalExceptionHandler` (`@RestControllerAdvice`) mapeando erros de validação (`MethodArgumentNotValidException` → 400 `VALIDATION_ERROR`), autenticação (`AuthenticationException`/credenciais inválidas → 401 `INVALID_CREDENTIALS`/`UNAUTHORIZED`), conflito de e-mail (→ 409 `EMAIL_ALREADY_IN_USE`) e falha de validação de token Google (→ 401 `GOOGLE_TOKEN_INVALID`), e verificar com testes de integração que cada tipo de erro retorna o código HTTP e o corpo `ApiError` esperados

## 5. Endpoint de Cadastro de Nutricionista

- [ ] 5.1 Implementar os DTOs `RegisterRequest` (campos `nome` com `@NotBlank`, `email` com `@Email`, `senha` com validação de política de senha, `empresa` opcional sem `@NotBlank`) e `NutricionistaResponse` (nome, empresa, email — sem senha/hash) e verificar com testes unitários que payloads inválidos (nome vazio, e-mail malformado, senha fraca) disparam violação de validação, e que a ausência de empresa não dispara violação
- [ ] 5.2 Implementar `NutricionistaService.registrar` (normaliza e-mail para lowercase, verifica duplicidade, aplica hash de senha, persiste nome/empresa/email/senha) e verificar com um teste unitário que um e-mail já existente lança a exceção de conflito mapeada em 4.2
- [ ] 5.3 Implementar `NutricionistaController` com `POST /api/nutricionistas` e verificar com um teste de integração (`MockMvc`) os cenários de sucesso com e sem empresa (HTTP 201 + corpo sem senha) do spec `nutritionist-auth`
- [ ] 5.4 Verificar com testes de integração os cenários de rejeição do spec `nutritionist-auth` (e-mail duplicado → 409, campo obrigatório ausente → 400, e-mail inválido → 400, senha fraca → 400)

## 6. Endpoint de Login (E-mail/Senha)

- [ ] 6.1 Implementar os DTOs `LoginRequest` e `LoginResponse` (token de acesso) e verificar com um teste unitário a validação de campos obrigatórios
- [ ] 6.2 Implementar `AuthService.login` (busca por e-mail, verifica que `password_hash` não é nulo antes de comparar, valida senha com `PasswordEncoder`, emite token via `JwtService`) e verificar com testes unitários que credenciais corretas emitem token, que credenciais incorretas lançam a exceção mapeada em 4.2 sem revelar se o e-mail existe, e que uma conta sem `password_hash` (criada via Google) é tratada como credenciais inválidas
- [ ] 6.3 Implementar `AuthController` com `POST /api/auth/login` e verificar com um teste de integração o cenário de sucesso (HTTP 200 + token) do spec `nutritionist-auth`
- [ ] 6.4 Verificar com testes de integração os cenários de credenciais inválidas (HTTP 401, sem token emitido) e de login tradicional em conta sem senha local (HTTP 401) do spec `nutritionist-auth`

## 7. Login e Cadastro via Conta Google

- [ ] 7.1 Implementar o DTO `GoogleLoginRequest` (campo `idToken`) e verificar com um teste unitário que a ausência do campo dispara violação de validação
- [ ] 7.2 Implementar `GoogleTokenVerifierService`, encapsulando `GoogleIdTokenVerifier` configurado com o `GOOGLE_CLIENT_ID`, e verificar com testes unitários (usando um verificador mockado) a validação de token válido, e a rejeição de assinatura/emissor/audiência inválidos e de token expirado
- [ ] 7.3 Estender `AuthService` com `loginComGoogle` (valida o token via `GoogleTokenVerifierService`, rejeita se `email_verified` for falso, busca `Nutricionista` por `google_subject` e depois por e-mail, vincula a conta existente atualizando `google_subject` quando ausente, ou cria uma nova conta sem senha) e verificar com testes unitários os três fluxos: conta nova criada, conta existente vinculada, e-mail não verificado rejeitado
- [ ] 7.4 Implementar `POST /api/auth/google` em `AuthController`, sem exigir autenticação prévia, e verificar com testes de integração os cenários de sucesso (criação automática de conta e vinculação a conta existente, ambos retornando HTTP 200 + token) do spec `nutritionist-auth`
- [ ] 7.5 Verificar com testes de integração os cenários de rejeição do spec `nutritionist-auth` (token Google inválido → 401, e-mail do Google não verificado → 401)

## 8. Endpoint de Logout

- [ ] 8.1 Implementar `AuthService.logout` (extrai `jti` do token atual e insere em `RevokedTokenRepository`) e verificar com um teste unitário que um token revogado passa a ser rejeitado por `JwtService`/`JwtAuthenticationFilter`
- [ ] 8.2 Implementar `POST /api/auth/logout` em `AuthController`, exigindo autenticação, e verificar com um teste de integração o cenário de sucesso (HTTP 200) e o cenário de logout sem autenticação (HTTP 401) do spec `nutritionist-auth`
- [ ] 8.3 Verificar com um teste de integração que, após logout bem-sucedido, uma nova requisição a um endpoint protegido usando o mesmo token é rejeitada com HTTP 401, tanto para tokens emitidos por login tradicional quanto por login via Google

## 9. Verificação de Isolamento e Autorização

- [ ] 9.1 Implementar um mecanismo reutilizável (ex.: método utilitário ou anotação de argumento de controller) para resolver o nutricionista autenticado a partir do `SecurityContext`, para uso pelas capacidades futuras, e verificar com um teste unitário que retorna o id correto a partir de um contexto de segurança populado pelo filtro
- [ ] 9.2 Adicionar um endpoint de teste temporário (ou reutilizar `GET` do próprio perfil, se aplicável) protegido por autenticação e verificar com testes de integração os cenários "acesso não autenticado negado" e "acesso com token inválido ou adulterado negado" do spec `nutritionist-auth`

## 10. Validação da Especificação e Revisão Final

- [ ] 10.1 Executar `openspec validate add-nutritionist-auth --strict` e verificar que não reporta erros
- [ ] 10.2 Executar a suíte completa de testes (`./gradlew test`) e verificar que todos os testes unitários e de integração das seções 2 a 9 passam
- [ ] 10.3 Revisar se cada cenário do spec delta `nutritionist-auth` tem pelo menos um teste automatizado correspondente, registrando e corrigindo qualquer lacuna encontrada

## 11. Documentação

- [ ] 11.1 Documentar os quatro endpoints (cadastro, login, login via Google, logout) — método, path, payload de request/response e códigos de erro — em um arquivo `API.md` ou anotações OpenAPI/Springdoc, e verificar que cada endpoint documentado corresponde a um teste de integração existente
- [ ] 11.2 Atualizar o `README.md` do projeto com instruções de setup (variáveis de ambiente de banco de dados, segredo JWT e `GOOGLE_CLIENT_ID` necessárias, incluindo como obter um Client ID de teste no Google Cloud Console) e verificar que um novo desenvolvedor consegue subir a aplicação localmente seguindo apenas essas instruções
