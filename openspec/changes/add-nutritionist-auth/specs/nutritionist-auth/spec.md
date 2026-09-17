# Spec Delta

## Purpose

Fornecer, via API REST, o cadastro de conta e a autenticação de nutricionistas, permitindo que cada nutricionista acesse o sistema com segurança e que as demais capacidades do backend restrinjam o acesso aos dados de cada nutricionista apenas ao seu próprio dono.

## ADDED Requirements

### Requirement: Cadastro de Nutricionista via API
O sistema DEVE expor um endpoint público que permita criar uma conta de nutricionista informando, no mínimo, nome completo, e-mail e senha, e DEVE persistir o e-mail em caixa baixa e de forma única, rejeitando qualquer tentativa de cadastro com um e-mail já existente (comparação case-insensitive).

#### Scenario: Cadastro bem-sucedido
- **WHEN** uma requisição de cadastro é enviada com nome completo, e-mail ainda não utilizado e senha válida
- **THEN** o sistema cria a conta do nutricionista, armazena a senha apenas em forma de hash e responde com HTTP 201 e os dados públicos do nutricionista (sem a senha ou seu hash)

#### Scenario: E-mail duplicado rejeitado
- **WHEN** uma requisição de cadastro é enviada com um e-mail que já pertence a uma conta existente, independentemente de diferenças de maiúsculas/minúsculas
- **THEN** o sistema rejeita a requisição com HTTP 409 e um corpo de erro indicando que o e-mail já está em uso, sem criar nova conta

### Requirement: Validação dos Dados de Cadastro
O sistema DEVE validar os dados de cadastro antes de criar a conta: nome completo não pode ser vazio, e-mail deve estar em formato de e-mail válido, e senha deve atender a uma política mínima de força (mínimo de 8 caracteres, contendo ao menos uma letra e um número).

#### Scenario: Campo obrigatório ausente
- **WHEN** uma requisição de cadastro é enviada sem nome, sem e-mail ou sem senha
- **THEN** o sistema rejeita a requisição com HTTP 400 e um corpo de erro identificando qual(is) campo(s) são inválidos

#### Scenario: E-mail em formato inválido
- **WHEN** uma requisição de cadastro é enviada com um valor de e-mail que não corresponde a um formato de e-mail válido
- **THEN** o sistema rejeita a requisição com HTTP 400 e não cria a conta

#### Scenario: Senha abaixo da política mínima
- **WHEN** uma requisição de cadastro é enviada com uma senha menor que 8 caracteres ou sem combinar letra e número
- **THEN** o sistema rejeita a requisição com HTTP 400 e não cria a conta

### Requirement: Login do Nutricionista via API
O sistema DEVE expor um endpoint público que autentique um nutricionista existente por e-mail e senha e, em caso de sucesso, retorne um token de acesso que o cliente deve enviar nas requisições subsequentes a endpoints protegidos.

#### Scenario: Login bem-sucedido
- **WHEN** uma requisição de login é enviada com o e-mail e a senha corretos de uma conta de nutricionista existente
- **THEN** o sistema responde com HTTP 200 e um token de acesso válido associado a esse nutricionista

#### Scenario: Credenciais inválidas rejeitadas
- **WHEN** uma requisição de login é enviada com um e-mail não cadastrado, ou com uma senha que não corresponde ao e-mail informado
- **THEN** o sistema rejeita a requisição com HTTP 401, sem emitir token, e sem indicar se o e-mail existe ou não

### Requirement: Logout do Nutricionista
O sistema DEVE expor um endpoint que permita a um nutricionista autenticado encerrar sua sessão atual, de forma que o token usado nessa sessão deixe de ser aceito para autenticar novas requisições.

#### Scenario: Logout bem-sucedido
- **WHEN** um nutricionista autenticado envia uma requisição de logout com um token válido
- **THEN** o sistema invalida esse token e responde com HTTP 200; requisições subsequentes com o mesmo token a endpoints protegidos são negadas

#### Scenario: Logout sem autenticação
- **WHEN** uma requisição de logout é enviada sem um token válido
- **THEN** o sistema rejeita a requisição com HTTP 401

### Requirement: Autenticação Obrigatória em Endpoints Protegidos
O sistema DEVE negar acesso a qualquer endpoint que exponha ou modifique dados vinculados a um nutricionista (paciente, plano alimentar, avaliação física, ou o próprio perfil do nutricionista) quando a requisição não apresentar um token de acesso válido e não expirado.

#### Scenario: Acesso não autenticado negado
- **WHEN** uma requisição a um endpoint protegido é feita sem token, com token ausente do cabeçalho esperado, ou com token expirado
- **THEN** o sistema responde com HTTP 401 e não executa a operação solicitada

#### Scenario: Acesso com token inválido ou adulterado negado
- **WHEN** uma requisição a um endpoint protegido é feita com um token malformado, assinado incorretamente, ou que não corresponde a nenhuma sessão válida
- **THEN** o sistema responde com HTTP 401 e não executa a operação solicitada

### Requirement: Identidade do Nutricionista Autenticado Disponível para Autorização
O sistema DEVE resolver, a partir de um token válido, a identidade única do nutricionista autenticado, disponibilizando-a para que os endpoints de outras capacidades (paciente, plano alimentar, avaliação física) apliquem a regra de que um nutricionista só acessa registros que ele próprio possui.

#### Scenario: Identidade resolvida corretamente
- **WHEN** um endpoint protegido recebe uma requisição com um token válido emitido para um nutricionista específico
- **THEN** o sistema disponibiliza o identificador desse nutricionista para a lógica de autorização do endpoint, sem exigir que o cliente informe o identificador separadamente

### Requirement: Formato Padronizado de Resposta de Erro
O sistema DEVE responder a toda falha de validação, autenticação ou autorização nos endpoints desta capacidade com um corpo de erro em formato JSON consistente, contendo ao menos um código/tipo de erro e uma mensagem descritiva, além do código de status HTTP apropriado.

#### Scenario: Corpo de erro consistente entre falhas
- **WHEN** qualquer requisição a um endpoint desta capacidade falha por validação (HTTP 400), autenticação (HTTP 401) ou conflito de e-mail (HTTP 409)
- **THEN** o corpo da resposta segue a mesma estrutura JSON de erro em todos os casos, permitindo que o cliente trate falhas de forma uniforme
