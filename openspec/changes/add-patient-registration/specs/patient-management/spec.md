# Spec Delta

## ADDED Requirements

### Requirement: Cadastro de Paciente
O sistema DEVE expor um endpoint protegido que permita ao nutricionista autenticado cadastrar um novo paciente vinculado a si próprio, exigindo no mínimo nome completo, data de nascimento e sexo; e-mail e telefone são opcionais. O nome completo não pode ser vazio nem conter apenas espaços em branco, e não pode exceder 255 caracteres; e-mail e telefone, quando informados, não podem exceder 255 e 30 caracteres respectivamente; a data de nascimento não pode ser posterior à data atual. O paciente recém-cadastrado é criado como ativo.

#### Scenario: Cadastro bem-sucedido com campos mínimos
- **WHEN** um nutricionista autenticado envia nome completo, data de nascimento e sexo de um novo paciente, sem informar e-mail nem telefone
- **THEN** o sistema cria o paciente vinculado a esse nutricionista, marcado como ativo, e responde com HTTP 201 contendo os dados cadastrados

#### Scenario: Cadastro bem-sucedido com todos os campos
- **WHEN** um nutricionista autenticado envia nome completo, data de nascimento, sexo, e-mail e telefone de um novo paciente
- **THEN** o sistema cria o paciente vinculado a esse nutricionista com todos os dados informados e responde com HTTP 201

#### Scenario: Campo obrigatório ausente rejeitado
- **WHEN** uma requisição de cadastro de paciente é enviada sem nome completo, sem data de nascimento ou sem sexo
- **THEN** o sistema rejeita a requisição com HTTP 400 e um corpo de erro identificando qual(is) campo(s) são inválidos, sem criar o paciente

#### Scenario: Nome apenas com espaços em branco rejeitado
- **WHEN** uma requisição de cadastro de paciente é enviada com o nome completo contendo apenas espaços em branco
- **THEN** o sistema rejeita a requisição com HTTP 400, tratando o campo como se estivesse vazio

#### Scenario: Campo excede o tamanho máximo permitido
- **WHEN** uma requisição de cadastro de paciente é enviada com nome completo maior que 255 caracteres, e-mail maior que 255 caracteres, ou telefone maior que 30 caracteres
- **THEN** o sistema rejeita a requisição com HTTP 400 e não cria o paciente

#### Scenario: Data de nascimento futura rejeitada
- **WHEN** uma requisição de cadastro de paciente informa uma data de nascimento posterior à data atual
- **THEN** o sistema rejeita a requisição com HTTP 400 e um corpo de erro identificando o parâmetro inválido, sem criar o paciente

#### Scenario: E-mail já usado por outro paciente do mesmo nutricionista rejeitado
- **WHEN** um nutricionista autenticado cadastra um paciente informando um e-mail que já pertence a outro paciente seu (ativo ou inativo)
- **THEN** o sistema rejeita a requisição com HTTP 409, identifica no corpo da resposta o paciente já existente com esse e-mail, e não cria um novo paciente

#### Scenario: E-mail repetido entre nutricionistas diferentes aceito
- **WHEN** um nutricionista autenticado cadastra um paciente com um e-mail que já pertence a um paciente de outro nutricionista
- **THEN** o sistema cria o paciente normalmente, já que a unicidade de e-mail de paciente é restrita a cada nutricionista

#### Scenario: Ausência de e-mail nunca gera conflito
- **WHEN** um nutricionista autenticado cadastra mais de um paciente seu sem informar e-mail em nenhum deles
- **THEN** o sistema cria todos os pacientes normalmente, sem tratar a ausência de e-mail como um valor conflitante

#### Scenario: Cadastro sem autenticação negado
- **WHEN** o cadastro de um paciente é solicitado sem um token de acesso válido
- **THEN** o sistema rejeita a requisição com HTTP 401 e não cria o paciente

### Requirement: Atualização de Paciente
O sistema DEVE expor um endpoint protegido que permita ao nutricionista autenticado atualizar todos os dados de um paciente que possui — nome completo, data de nascimento, sexo, e-mail, telefone e status ativo/inativo — substituindo integralmente o registro anterior a partir de um payload completo. Não existe um endpoint separado para alterar apenas o status; a desativação e a reativação de um paciente acontecem através desta mesma atualização. As mesmas regras de validação do cadastro (campos obrigatórios, tamanho máximo, data de nascimento não futura, e-mail não duplicado entre pacientes do mesmo nutricionista) se aplicam à atualização, exceto que um paciente não conflita consigo mesmo ao manter seu próprio e-mail atual.

#### Scenario: Atualização bem-sucedida
- **WHEN** um nutricionista autenticado envia um payload completo com dados atualizados para um paciente que possui
- **THEN** o sistema substitui os dados desse paciente pelos valores informados e responde com HTTP 200 contendo o registro atualizado

#### Scenario: Desativação de paciente via atualização
- **WHEN** um nutricionista autenticado envia um payload de atualização para um paciente ativo que possui, com o status marcado como inativo
- **THEN** o sistema marca o paciente como inativo, preservando os demais dados cadastrados, e o exclui da listagem padrão de pacientes ativos

#### Scenario: Reativação de paciente via atualização
- **WHEN** um nutricionista autenticado envia um payload de atualização para um paciente inativo que possui, com o status marcado como ativo
- **THEN** o sistema marca o paciente como ativo novamente e volta a incluí-lo na listagem padrão de pacientes ativos

#### Scenario: Atualização de paciente de outro nutricionista negada
- **WHEN** um nutricionista autenticado tenta atualizar um paciente que pertence a outro nutricionista
- **THEN** o sistema responde com HTTP 404, sem indicar se o paciente existe na base de outro nutricionista, e não altera nenhum dado

#### Scenario: Atualização de paciente inexistente
- **WHEN** um nutricionista autenticado tenta atualizar um paciente cujo identificador não existe
- **THEN** o sistema responde com HTTP 404

#### Scenario: Campo obrigatório ausente na atualização rejeitado
- **WHEN** uma requisição de atualização de paciente é enviada sem nome completo, sem data de nascimento ou sem sexo
- **THEN** o sistema rejeita a requisição com HTTP 400 e um corpo de erro identificando qual(is) campo(s) são inválidos, sem alterar o paciente

#### Scenario: Data de nascimento futura rejeitada na atualização
- **WHEN** uma requisição de atualização de paciente informa uma data de nascimento posterior à data atual
- **THEN** o sistema rejeita a requisição com HTTP 400 e não altera o paciente

#### Scenario: Manter o próprio e-mail não gera conflito
- **WHEN** um nutricionista autenticado atualiza um paciente que possui reenviando o mesmo e-mail que esse paciente já tinha
- **THEN** o sistema aceita a atualização normalmente, sem tratá-la como conflito de e-mail duplicado

#### Scenario: E-mail já usado por outro paciente do mesmo nutricionista rejeitado na atualização
- **WHEN** um nutricionista autenticado atualiza um paciente informando um e-mail que já pertence a um paciente diferente seu (ativo ou inativo)
- **THEN** o sistema rejeita a requisição com HTTP 409, identifica no corpo da resposta o paciente já existente com esse e-mail, e não altera o paciente

#### Scenario: Atualização sem autenticação negada
- **WHEN** a atualização de um paciente é solicitada sem um token de acesso válido
- **THEN** o sistema rejeita a requisição com HTTP 401 e não altera o paciente

## MODIFIED Requirements

### Requirement: Busca de Pacientes por Critérios
O sistema DEVE expor um endpoint protegido de busca que permita ao nutricionista autenticado localizar, entre os próprios pacientes ativos, aqueles que atendem a uma combinação opcional dos seguintes critérios: nome completo (correspondência parcial, sem diferenciar maiúsculas/minúsculas), sexo (correspondência exata), faixa de data de nascimento (intervalo com data inicial e/ou final), e-mail (correspondência parcial) e telefone (correspondência parcial). Os critérios informados DEVEM ser combinados de forma cumulativa (E lógico); a ausência de qualquer critério equivale a não filtrar por ele. Um valor de sexo fora dos valores aceitos DEVE ser rejeitado com o mesmo formato padronizado de erro usado pelas demais falhas de validação desta capacidade.

#### Scenario: Busca por nome parcial
- **WHEN** um nutricionista autenticado busca seus pacientes informando um trecho do nome, em qualquer combinação de maiúsculas/minúsculas
- **THEN** o sistema responde com HTTP 200 contendo apenas os pacientes ativos desse nutricionista cujo nome completo contém o trecho informado

#### Scenario: Busca por sexo
- **WHEN** um nutricionista autenticado busca seus pacientes informando um valor de sexo
- **THEN** o sistema responde com HTTP 200 contendo apenas os pacientes ativos desse nutricionista cujo sexo corresponde exatamente ao valor informado

#### Scenario: Busca por faixa de data de nascimento
- **WHEN** um nutricionista autenticado busca seus pacientes informando uma data de nascimento inicial e/ou final
- **THEN** o sistema responde com HTTP 200 contendo apenas os pacientes ativos desse nutricionista cuja data de nascimento está dentro do intervalo informado

#### Scenario: Busca por e-mail ou telefone parcial
- **WHEN** um nutricionista autenticado busca seus pacientes informando um trecho de e-mail ou de telefone
- **THEN** o sistema responde com HTTP 200 contendo apenas os pacientes ativos desse nutricionista cujo e-mail ou telefone (respectivamente) contém o trecho informado

#### Scenario: Combinação de múltiplos critérios
- **WHEN** um nutricionista autenticado busca seus pacientes informando mais de um critério simultaneamente (por exemplo, trecho de nome e sexo)
- **THEN** o sistema responde com HTTP 200 contendo apenas os pacientes ativos desse nutricionista que atendem a todos os critérios informados

#### Scenario: Busca sem nenhum critério informado
- **WHEN** um nutricionista autenticado chama o endpoint de busca sem informar nenhum critério
- **THEN** o sistema responde com HTTP 200 contendo o mesmo resultado paginado que a listagem padrão retornaria para esse nutricionista

#### Scenario: Busca sem resultados
- **WHEN** um nutricionista autenticado busca seus pacientes com critérios que não correspondem a nenhum paciente ativo seu
- **THEN** o sistema responde com HTTP 200 e uma lista vazia, sem erro

#### Scenario: Busca não retorna pacientes de outro nutricionista
- **WHEN** um nutricionista autenticado busca pacientes com critérios que também correspondem a pacientes ativos de outro nutricionista
- **THEN** o sistema não inclui, na resposta, nenhum paciente que não pertença ao nutricionista autenticado

#### Scenario: Busca não retorna pacientes inativos
- **WHEN** um nutricionista autenticado busca pacientes com critérios que também correspondem a um paciente inativo seu
- **THEN** o sistema não inclui esse paciente inativo na resposta

#### Scenario: Faixa de data de nascimento inválida rejeitada
- **WHEN** uma requisição de busca informa uma data de nascimento inicial posterior à data de nascimento final
- **THEN** o sistema rejeita a requisição com HTTP 400 e um corpo de erro identificando o parâmetro inválido

#### Scenario: Valor de sexo inválido rejeitado com erro padronizado
- **WHEN** uma requisição de busca informa um valor de sexo fora dos valores aceitos
- **THEN** o sistema rejeita a requisição com HTTP 400 e o mesmo formato de corpo de erro (`ApiError`) usado pelas demais validações desta capacidade, identificando o parâmetro inválido

#### Scenario: Busca sem autenticação negada
- **WHEN** a busca de pacientes é solicitada sem um token de acesso válido
- **THEN** o sistema rejeita a requisição com HTTP 401
