# nutritionist-auth Specification

## Purpose

Fornecer, via API REST, o cadastro de conta e a autenticação de nutricionistas, permitindo que cada nutricionista acesse o sistema com segurança e que as demais capacidades do backend restrinjam o acesso aos dados de cada nutricionista apenas ao seu próprio dono.

## Requirements

### Requirement: Cadastro de Nutricionista via API
O sistema DEVE expor um endpoint público que permita criar uma conta de nutricionista informando, no mínimo, nome, e-mail e senha, e opcionalmente uma empresa, e DEVE persistir o e-mail em caixa baixa e de forma única, rejeitando qualquer tentativa de cadastro com um e-mail já existente (comparação case-insensitive), inclusive quando duas requisições de cadastro concorrentes usam o mesmo e-mail.

#### Scenario: Cadastro bem-sucedido sem empresa
- **WHEN** uma requisição de cadastro é enviada com nome, e-mail ainda não utilizado, senha válida, e sem informar empresa
- **THEN** o sistema cria a conta do nutricionista sem empresa associada, armazena a senha apenas em forma de hash e responde com HTTP 201 e os dados públicos do nutricionista (sem a senha ou seu hash)

#### Scenario: Cadastro bem-sucedido com empresa
- **WHEN** uma requisição de cadastro é enviada com nome, e-mail ainda não utilizado, senha válida, e uma empresa informada
- **THEN** o sistema cria a conta do nutricionista com a empresa associada e responde com HTTP 201 e os dados públicos do nutricionista, incluindo a empresa informada

#### Scenario: E-mail duplicado rejeitado
- **WHEN** uma requisição de cadastro é enviada com um e-mail que já pertence a uma conta existente, independentemente de diferenças de maiúsculas/minúsculas
- **THEN** o sistema rejeita a requisição com HTTP 409 e um corpo de erro indicando que o e-mail já está em uso, sem criar nova conta

#### Scenario: Cadastros concorrentes com o mesmo e-mail
- **WHEN** duas requisições de cadastro com o mesmo e-mail (ainda não cadastrado) são processadas de forma concorrente
- **THEN** exatamente uma delas cria a conta com sucesso (HTTP 201) e a outra é rejeitada como e-mail duplicado (HTTP 409), sem que duas contas com o mesmo e-mail sejam criadas

### Requirement: Validação dos Dados de Cadastro
O sistema DEVE validar os dados de cadastro antes de criar a conta: nome não pode ser vazio nem conter apenas espaços em branco, e não pode exceder 255 caracteres; e-mail deve estar em formato de e-mail válido; senha deve atender a uma política mínima de força (mínimo de 8 caracteres, contendo ao menos uma letra e um número) e não pode exceder 72 caracteres; e empresa, quando informada, não pode ser uma string vazia nem conter apenas espaços em branco, e não pode exceder 255 caracteres. A ausência de empresa nunca é motivo de rejeição.

#### Scenario: Campo obrigatório ausente
- **WHEN** uma requisição de cadastro é enviada sem nome, sem e-mail ou sem senha
- **THEN** o sistema rejeita a requisição com HTTP 400 e um corpo de erro identificando qual(is) campo(s) são inválidos

#### Scenario: E-mail em formato inválido
- **WHEN** uma requisição de cadastro é enviada com um valor de e-mail que não corresponde a um formato de e-mail válido
- **THEN** o sistema rejeita a requisição com HTTP 400 e não cria a conta

#### Scenario: Senha abaixo da política mínima
- **WHEN** uma requisição de cadastro é enviada com uma senha menor que 8 caracteres ou sem combinar letra e número
- **THEN** o sistema rejeita a requisição com HTTP 400 e não cria a conta

#### Scenario: Empresa ausente é aceita
- **WHEN** uma requisição de cadastro é enviada sem o campo empresa
- **THEN** o sistema não rejeita a requisição por causa desse campo e cria a conta normalmente

#### Scenario: Nome ou empresa somente com espaços em branco rejeitado
- **WHEN** uma requisição de cadastro é enviada com nome, ou com empresa, contendo apenas espaços em branco
- **THEN** o sistema rejeita a requisição com HTTP 400, tratando o campo como se estivesse vazio

#### Scenario: Campo excede o tamanho máximo permitido
- **WHEN** uma requisição de cadastro é enviada com nome ou empresa maiores que 255 caracteres, ou senha maior que 72 caracteres
- **THEN** o sistema rejeita a requisição com HTTP 400 e não cria a conta

### Requirement: Login do Nutricionista via API
O sistema DEVE expor um endpoint público que autentique um nutricionista existente por e-mail e senha e, em caso de sucesso, retorne um token de acesso que o cliente deve enviar nas requisições subsequentes a endpoints protegidos.

#### Scenario: Login bem-sucedido
- **WHEN** uma requisição de login é enviada com o e-mail e a senha corretos de uma conta de nutricionista existente
- **THEN** o sistema responde com HTTP 200 e um token de acesso válido associado a esse nutricionista

#### Scenario: Credenciais inválidas rejeitadas
- **WHEN** uma requisição de login é enviada com um e-mail não cadastrado, ou com uma senha que não corresponde ao e-mail informado
- **THEN** o sistema rejeita a requisição com HTTP 401, sem emitir token, e sem indicar se o e-mail existe ou não

#### Scenario: Login tradicional em conta sem senha local
- **WHEN** uma requisição de login por e-mail/senha é enviada para uma conta que foi criada exclusivamente via login com Google e não possui senha local cadastrada
- **THEN** o sistema rejeita a requisição com HTTP 401, com o mesmo formato de erro de credenciais inválidas, sem indicar que a conta existe apenas via Google

### Requirement: Login e Cadastro via Conta Google
O sistema DEVE permitir que um nutricionista se autentique usando uma conta Google (OAuth 2.0/OpenID Connect) como alternativa ao login por e-mail/senha, aceitando um token de identidade emitido pelo Google, validando sua autenticidade, emissor e verificação de e-mail antes de conceder acesso. Quando não existir conta de nutricionista com o e-mail da conta Google, o sistema DEVE criar uma automaticamente a partir do nome e e-mail informados pelo token, sem senha local. Quando já existir conta de nutricionista com esse e-mail (criada via cadastro tradicional ou via Google anteriormente), o sistema DEVE autenticar essa conta existente em vez de criar uma duplicata, preservando os dados de perfil (nome, empresa) já cadastrados nela. O sistema DEVE responder sempre com o mesmo código HTTP de sucesso (200) em ambos os casos, distinguindo-os apenas por um indicador no corpo da resposta, e DEVE garantir que logins com Google concorrentes para o mesmo e-mail ainda inexistente resultem em uma única conta criada.

#### Scenario: Login via Google com criação automática de conta
- **WHEN** um token de identidade Google válido, com e-mail verificado, é enviado e nenhuma conta de nutricionista existe com esse e-mail
- **THEN** o sistema cria uma conta de nutricionista com nome e e-mail obtidos do token, sem senha local, e responde com HTTP 200, um token de acesso válido para essa conta, e um indicador no corpo informando que a conta foi criada nesta chamada

#### Scenario: Login via Google vinculado a conta existente
- **WHEN** um token de identidade Google válido é enviado e já existe uma conta de nutricionista com o mesmo e-mail
- **THEN** o sistema concede um token de acesso válido para essa conta existente, responde com HTTP 200 e um indicador no corpo informando que nenhuma conta nova foi criada, sem criar uma conta duplicada

#### Scenario: Perfil existente preservado ao vincular conta Google
- **WHEN** uma conta de nutricionista já existente (com nome e/ou empresa diferentes dos informados pela conta Google) é vinculada por login via Google
- **THEN** o sistema mantém o nome e a empresa já cadastrados nessa conta, sem sobrescrevê-los com os dados vindos do Google

#### Scenario: Token do Google inválido rejeitado
- **WHEN** o token de identidade enviado não pode ser validado (assinatura inválida, emissor diferente do Google, audiência incorreta, ou token expirado)
- **THEN** o sistema rejeita a requisição com HTTP 401 e não concede acesso nem cria conta

#### Scenario: E-mail do Google não verificado rejeitado
- **WHEN** o token de identidade do Google indica que o e-mail associado não foi verificado
- **THEN** o sistema rejeita a requisição com HTTP 401 e não concede acesso nem cria conta

#### Scenario: Logins via Google concorrentes para conta nova
- **WHEN** dois logins via Google com token válido e o mesmo e-mail ainda não cadastrado são processados de forma concorrente
- **THEN** exatamente uma conta de nutricionista é criada para esse e-mail, e ambas as chamadas terminam autenticadas nessa mesma conta

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

### Requirement: Consulta do Próprio Perfil
O sistema DEVE expor um endpoint protegido que retorne os dados públicos (nome, empresa, e-mail) do nutricionista autenticado, resolvidos a partir do token de acesso, sem exigir que o cliente informe um identificador.

#### Scenario: Perfil retornado com sucesso
- **WHEN** um nutricionista autenticado solicita seus próprios dados com um token de acesso válido
- **THEN** o sistema responde com HTTP 200 contendo nome, empresa (quando cadastrada) e e-mail desse nutricionista, sem incluir senha ou hash de senha

#### Scenario: Consulta de perfil sem autenticação negada
- **WHEN** a consulta ao próprio perfil é feita sem um token de acesso válido
- **THEN** o sistema rejeita a requisição com HTTP 401

### Requirement: Formato Padronizado de Resposta de Erro
O sistema DEVE responder a toda falha de validação, autenticação ou autorização nos endpoints desta capacidade com um corpo de erro em formato JSON consistente, contendo ao menos um código/tipo de erro e uma mensagem descritiva, além do código de status HTTP apropriado.

#### Scenario: Corpo de erro consistente entre falhas
- **WHEN** qualquer requisição a um endpoint desta capacidade falha por validação (HTTP 400), autenticação (HTTP 401) ou conflito de e-mail (HTTP 409)
- **THEN** o corpo da resposta segue a mesma estrutura JSON de erro em todos os casos, permitindo que o cliente trate falhas de forma uniforme
