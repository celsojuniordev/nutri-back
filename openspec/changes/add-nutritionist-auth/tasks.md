# Tasks

## 1. Setup de Dependências e Infraestrutura

- [ ] 1.1 Adicionar dependência de JWT ao `build.gradle` (ex.: `io.jsonwebtoken:jjwt-api`/`jjwt-impl`/`jjwt-jackson`) e verificar que `./gradlew build` resolve as dependências sem erro
- [ ] 1.2 Adicionar dependência do Flyway (`org.flywaydb:flyway-core` + `flyway-mysql`) ao `build.gradle` e verificar que a aplicação sobe com `spring.jpa.hibernate.ddl-auto=validate` (schema gerenciado só pelo Flyway a partir daqui)
- [ ] 1.3 Decidir e configurar a estratégia de banco de dados de teste (H2 em memória ou Testcontainers MySQL, ver design.md - Riscos/Trade-offs) e verificar que um teste de contexto Spring (`@SpringBootTest`) sobe com sucesso usando essa estratégia
- [ ] 1.4 Adicionar propriedades de configuração da chave/segredo JWT e do tempo de expiração do token em `application.properties` (via variável de ambiente, sem valor hardcoded) e verificar que a aplicação falha ao subir se a variável obrigatória estiver ausente

## 2. Modelo de Dados e Migração

- [ ] 2.1 Criar a migração Flyway (`V1__create_nutricionistas.sql`) para a tabela `nutricionistas` (`id`, `nome_completo`, `email` único, `password_hash`, `criado_em`) e verificar que a migração aplica sem erro em ambiente local
- [ ] 2.2 Criar a migração Flyway (`V2__create_revoked_tokens.sql`) para a tabela `revoked_tokens` (`id`, `jti` único, `expira_em`) e verificar que a migração aplica sem erro
- [ ] 2.3 Implementar a entidade JPA `Nutricionista` e o repositório `NutricionistaRepository` (com busca por e-mail case-insensitive) e verificar com um teste de repositório (`@DataJpaTest`) que a constraint de e-mail único é respeitada
- [ ] 2.4 Implementar a entidade JPA `RevokedToken` e o repositório `RevokedTokenRepository` e verificar com um teste de repositório que um `jti` inserido é encontrado por busca e que duplicatas são rejeitadas

## 3. Camada de Segurança

- [ ] 3.1 Configurar o bean `PasswordEncoder` (`BCryptPasswordEncoder`) e verificar com um teste unitário que uma senha em texto puro gera um hash diferente a cada chamada e que `matches` reconhece a senha correta
- [ ] 3.2 Implementar `JwtService` (emissão de token com `jti`, `sub` = id do nutricionista, expiração; validação e parse) e verificar com testes unitários a emissão, a validação de um token válido e a rejeição de um token expirado ou com assinatura inválida
- [ ] 3.3 Implementar `JwtAuthenticationFilter` que extrai o token do cabeçalho `Authorization: Bearer`, valida contra `JwtService` e `RevokedTokenRepository`, e popula o `SecurityContext` com a identidade do nutricionista e verificar com um teste de filtro que uma requisição com token válido popula o contexto de segurança
- [ ] 3.4 Configurar `SecurityConfig` (`SecurityFilterChain`) para exigir autenticação em todos os endpoints exceto cadastro e login, registrar o `JwtAuthenticationFilter`, e verificar com um teste de integração que um endpoint protegido de teste retorna HTTP 401 sem token

## 4. Tratamento de Erros

- [ ] 4.1 Implementar o DTO `ApiError` no formato definido em design.md (status, error, message, details, timestamp) e verificar com um teste unitário de serialização que o JSON gerado segue exatamente essa estrutura
- [ ] 4.2 Implementar `GlobalExceptionHandler` (`@RestControllerAdvice`) mapeando erros de validação (`MethodArgumentNotValidException` → 400 `VALIDATION_ERROR`), autenticação (`AuthenticationException`/credenciais inválidas → 401 `INVALID_CREDENTIALS`/`UNAUTHORIZED`) e conflito de e-mail (→ 409 `EMAIL_ALREADY_IN_USE`), e verificar com testes de integração que cada tipo de erro retorna o código HTTP e o corpo `ApiError` esperados

## 5. Endpoint de Cadastro de Nutricionista

- [ ] 5.1 Implementar os DTOs `RegisterRequest` (com `@NotBlank`, `@Email`, e validação de política de senha) e `NutricionistaResponse` (sem senha/hash) e verificar com testes unitários que payloads inválidos (nome vazio, e-mail malformado, senha fraca) disparam violação de validação
- [ ] 5.2 Implementar `NutricionistaService.registrar` (normaliza e-mail para lowercase, verifica duplicidade, aplica hash de senha, persiste) e verificar com um teste unitário que um e-mail já existente lança a exceção de conflito mapeada em 4.2
- [ ] 5.3 Implementar `NutricionistaController` com `POST /api/nutricionistas` e verificar com um teste de integração (`MockMvc`) o cenário de sucesso (HTTP 201 + corpo sem senha) do spec `nutritionist-auth`
- [ ] 5.4 Verificar com testes de integração os cenários de rejeição do spec `nutritionist-auth` (e-mail duplicado → 409, campo obrigatório ausente → 400, e-mail inválido → 400, senha fraca → 400)

## 6. Endpoint de Login

- [ ] 6.1 Implementar os DTOs `LoginRequest` e `LoginResponse` (token de acesso) e verificar com um teste unitário a validação de campos obrigatórios
- [ ] 6.2 Implementar `AuthService.login` (busca por e-mail, valida senha com `PasswordEncoder`, emite token via `JwtService`) e verificar com um teste unitário que credenciais corretas emitem token e que credenciais incorretas lançam a exceção mapeada em 4.2, sem revelar se o e-mail existe
- [ ] 6.3 Implementar `AuthController` com `POST /api/auth/login` e verificar com um teste de integração o cenário de sucesso (HTTP 200 + token) do spec `nutritionist-auth`
- [ ] 6.4 Verificar com um teste de integração o cenário de credenciais inválidas (HTTP 401, sem token emitido) do spec `nutritionist-auth`

## 7. Endpoint de Logout

- [ ] 7.1 Implementar `AuthService.logout` (extrai `jti` do token atual e insere em `RevokedTokenRepository`) e verificar com um teste unitário que um token revogado passa a ser rejeitado por `JwtService`/`JwtAuthenticationFilter`
- [ ] 7.2 Implementar `POST /api/auth/logout` em `AuthController`, exigindo autenticação, e verificar com um teste de integração o cenário de sucesso (HTTP 200) e o cenário de logout sem autenticação (HTTP 401) do spec `nutritionist-auth`
- [ ] 7.3 Verificar com um teste de integração que, após logout bem-sucedido, uma nova requisição a um endpoint protegido usando o mesmo token é rejeitada com HTTP 401

## 8. Verificação de Isolamento e Autorização

- [ ] 8.1 Implementar um mecanismo reutilizável (ex.: método utilitário ou anotação de argumento de controller) para resolver o nutricionista autenticado a partir do `SecurityContext`, para uso pelas capacidades futuras, e verificar com um teste unitário que retorna o id correto a partir de um contexto de segurança populado pelo filtro
- [ ] 8.2 Adicionar um endpoint de teste temporário (ou reutilizar `GET` do próprio perfil, se aplicável) protegido por autenticação e verificar com testes de integração os cenários "acesso não autenticado negado" e "acesso com token inválido ou adulterado negado" do spec `nutritionist-auth`

## 9. Validação da Especificação e Revisão Final

- [ ] 9.1 Executar `openspec validate add-nutritionist-auth --strict` e verificar que não reporta erros
- [ ] 9.2 Executar a suíte completa de testes (`./gradlew test`) e verificar que todos os testes unitários e de integração das seções 2 a 8 passam
- [ ] 9.3 Revisar se cada cenário do spec delta `nutritionist-auth` tem pelo menos um teste automatizado correspondente, registrando e corrigindo qualquer lacuna encontrada

## 10. Documentação

- [ ] 10.1 Documentar os três endpoints (cadastro, login, logout) — método, path, payload de request/response e códigos de erro — em um arquivo `API.md` ou anotações OpenAPI/Springdoc, e verificar que cada endpoint documentado corresponde a um teste de integração existente
- [ ] 10.2 Atualizar o `README.md` do projeto com instruções de setup (variáveis de ambiente de banco de dados e segredo JWT necessárias) e verificar que um novo desenvolvedor consegue subir a aplicação localmente seguindo apenas essas instruções
