# Proposal

## Why

O backend `nutri-back` é um projeto greenfield (Spring Boot) que ainda não possui nenhuma capacidade implementada. A especificação de sistema do repositório `nutri-specs` (capacidade `nutritionist-auth`, change `add-nutritionist-core-features`) define que o acesso ao sistema depende de contas de nutricionista autenticadas e que todo dado de paciente é isolado por nutricionista. Sem cadastro e login implementados, nenhuma outra capacidade do backend (gestão de pacientes, prescrição de dieta, avaliação física) pode ser construída, pois todas dependem de um nutricionista autenticado como dono dos dados. Esta change traduz a fatia de autenticação dessa especificação de sistema em uma especificação de implementação concreta para este backend.

## What Changes

- Adiciona a entidade `Nutricionista` persistida via Spring Data JPA/MySQL, com e-mail único e senha armazenada com hash (BCrypt).
- Adiciona endpoint de cadastro de nutricionista (`POST /api/nutricionistas`), com campos nome (obrigatório), e-mail (obrigatório), senha (obrigatória) e empresa (opcional), e rejeição de e-mail duplicado.
- Adiciona endpoint de login (`POST /api/auth/login`), autenticando por e-mail/senha e emitindo um token JWT stateless (ver design.md para a justificativa da escolha entre sessão HTTP e JWT).
- Adiciona endpoint de login/cadastro via conta Google (`POST /api/auth/google`), como método alternativo que coexiste com o login por e-mail/senha: valida o token de identidade do Google, vincula a conta existente pelo e-mail ou cria uma conta automaticamente (sem senha local) quando ainda não existir.
- Adiciona endpoint de logout (`POST /api/auth/logout`) compatível com o mecanismo de autenticação escolhido, válido para sessões iniciadas por qualquer um dos dois métodos de login.
- Adiciona configuração de segurança (Spring Security) que exige um token válido para qualquer endpoint autenticado e nega acesso a requisições sem autenticação.
- Adiciona a base do isolamento de dados por nutricionista: o nutricionista autenticado é resolvido a partir do token em cada requisição e disponibilizado para uso por capacidades futuras (`patient-management`, `diet-prescription`, `physical-assessment`) que vincularão seus registros a esse nutricionista.
- Adiciona tratamento de erros padronizado (formato de resposta de erro e códigos HTTP) para falhas de validação, autenticação e autorização.
- **Fora de escopo nesta change**: recuperação de senha, verificação de e-mail, autenticação multifator (também fora de escopo na especificação de sistema) e as capacidades de `patient-management`, `diet-prescription` e `physical-assessment` em si — apenas o mecanismo de autenticação/isolamento que elas vão consumir.

## Capabilities

### New Capabilities
- `nutritionist-auth`: Cadastro de conta de nutricionista, login/logout via JWT, e o mecanismo de resolução do nutricionista autenticado que sustenta o isolamento de dados multi-tenant usado pelas demais capacidades do backend.

### Modified Capabilities
Nenhuma — este é o primeiro conjunto de specs de implementação deste backend; não há capacidades existentes em `openspec/specs/`.

## Impact

- **Novo código**: entidade JPA `Nutricionista`, repositório, serviço de autenticação, serviço de validação de token Google, controllers REST (`NutricionistaController`, `AuthController`), configuração `Spring Security` (filtro JWT, `PasswordEncoder`), DTOs de request/response, tratador global de exceções (`@ControllerAdvice`).
- **Banco de dados**: nova tabela `nutricionistas` (via Flyway/migração ou `ddl-auto`, a decidir em design.md) com colunas `nome`, `empresa` (opcional), `email` (único), `password_hash` (opcional) e `google_subject` (opcional, único).
- **Dependências**: adiciona biblioteca de JWT (ex.: `jjwt` ou `spring-security-oauth2-jose`) e biblioteca de validação de token Google (`com.google.api-client:google-api-client`) ao `build.gradle`; já usa `spring-boot-starter-security`, `spring-boot-starter-data-jpa` e `mysql-connector-j` existentes.
- **Configuração**: requer uma variável de ambiente `GOOGLE_CLIENT_ID` (Client ID do projeto no Google Cloud/Google Identity Services) além do segredo JWT já previsto.
- **API pública**: introduz os quatro primeiros endpoints públicos do backend (`/api/nutricionistas` para cadastro, `/api/auth/login`, `/api/auth/google`, `/api/auth/logout`); nenhum endpoint existente é afetado pois não há endpoints hoje.
- **Capacidades futuras**: estabelece o contrato (nutricionista autenticado disponível via `SecurityContext`) que `patient-management`, `diet-prescription` e `physical-assessment` vão depender para vincular seus registros ao nutricionista dono.
