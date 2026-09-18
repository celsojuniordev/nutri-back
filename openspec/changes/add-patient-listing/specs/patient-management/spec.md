# Spec Delta

## Purpose

Permitir que um nutricionista autenticado liste, visualize o detalhe e pesquise os pacientes sob seus próprios cuidados, restringindo o acesso a apenas os pacientes que ele possui.

## ADDED Requirements

### Requirement: Listagem Paginada de Pacientes
O sistema DEVE expor um endpoint protegido que retorne, de forma paginada, apenas os pacientes ativos pertencentes ao nutricionista autenticado, ordenáveis, sem exigir nenhum parâmetro de filtro. Este é o conteúdo apresentado ao nutricionista imediatamente após um login bem-sucedido (conforme já definido na capacidade `nutritionist-auth`). Quando o tamanho de página não for informado, o sistema DEVE utilizar 25 itens por página; quando informado, o sistema DEVE aceitar apenas os tamanhos 10, 25 ou 50 (as mesmas opções apresentadas ao nutricionista em um campo de seleção no final da tela de listagem), rejeitando qualquer outro valor.

#### Scenario: Listagem retorna apenas pacientes ativos do próprio nutricionista
- **WHEN** um nutricionista autenticado solicita sua lista de pacientes
- **THEN** o sistema responde com HTTP 200 contendo somente os pacientes ativos vinculados a esse nutricionista, junto com metadados de paginação (página atual, tamanho, total de elementos, total de páginas)

#### Scenario: Pacientes de outro nutricionista não aparecem
- **WHEN** um nutricionista autenticado solicita sua lista de pacientes e existem pacientes ativos pertencentes a outros nutricionistas
- **THEN** o sistema não inclui, na resposta, nenhum paciente que não pertença ao nutricionista autenticado

#### Scenario: Pacientes inativos não aparecem na listagem padrão
- **WHEN** um nutricionista autenticado possui ao menos um paciente marcado como inativo
- **THEN** o sistema exclui esse paciente da listagem padrão

#### Scenario: Listagem sem pacientes cadastrados
- **WHEN** um nutricionista autenticado sem nenhum paciente ativo solicita sua lista de pacientes
- **THEN** o sistema responde com HTTP 200 e uma lista vazia, sem erro

#### Scenario: Paginação respeitada
- **WHEN** um nutricionista autenticado com mais pacientes ativos do que o tamanho de uma página solicita uma página específica
- **THEN** o sistema retorna somente os pacientes correspondentes àquela página, com os metadados de paginação refletindo o total real de pacientes ativos desse nutricionista

#### Scenario: Tamanho de página padrão aplicado quando não informado
- **WHEN** um nutricionista autenticado solicita sua lista de pacientes sem informar o tamanho de página
- **THEN** o sistema retorna a listagem usando 25 itens por página

#### Scenario: Tamanho de página fora das opções permitidas rejeitado
- **WHEN** uma requisição de listagem informa um tamanho de página diferente de 10, 25 ou 50
- **THEN** o sistema rejeita a requisição com HTTP 400 e um corpo de erro identificando o parâmetro inválido

#### Scenario: Listagem sem autenticação negada
- **WHEN** a listagem de pacientes é solicitada sem um token de acesso válido
- **THEN** o sistema rejeita a requisição com HTTP 401

### Requirement: Visualização de Detalhe do Paciente
O sistema DEVE expor um endpoint protegido que retorne os dados completos de um paciente específico, desde que esse paciente pertença ao nutricionista autenticado.

#### Scenario: Detalhe retornado com sucesso
- **WHEN** um nutricionista autenticado solicita o detalhe de um paciente que possui
- **THEN** o sistema responde com HTTP 200 contendo os dados cadastrados desse paciente

#### Scenario: Detalhe de paciente de outro nutricionista negado
- **WHEN** um nutricionista autenticado solicita o detalhe de um paciente que pertence a outro nutricionista
- **THEN** o sistema responde com HTTP 404, sem indicar se o paciente existe na base de outro nutricionista

#### Scenario: Detalhe de paciente inexistente
- **WHEN** um nutricionista autenticado solicita o detalhe de um paciente cujo identificador não existe
- **THEN** o sistema responde com HTTP 404

#### Scenario: Detalhe de paciente inativo ainda acessível
- **WHEN** um nutricionista autenticado solicita o detalhe de um paciente inativo que possui
- **THEN** o sistema responde com HTTP 200 contendo os dados desse paciente, mesmo que ele não apareça na listagem padrão

#### Scenario: Consulta de detalhe sem autenticação negada
- **WHEN** o detalhe de um paciente é solicitado sem um token de acesso válido
- **THEN** o sistema rejeita a requisição com HTTP 401

### Requirement: Busca de Pacientes por Critérios
O sistema DEVE expor um endpoint protegido de busca que permita ao nutricionista autenticado localizar, entre os próprios pacientes ativos, aqueles que atendem a uma combinação opcional dos seguintes critérios: nome completo (correspondência parcial, sem diferenciar maiúsculas/minúsculas), sexo (correspondência exata), faixa de data de nascimento (intervalo com data inicial e/ou final), e-mail (correspondência parcial) e telefone (correspondência parcial). Os critérios informados DEVEM ser combinados de forma cumulativa (E lógico); a ausência de qualquer critério equivale a não filtrar por ele.

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

#### Scenario: Busca sem autenticação negada
- **WHEN** a busca de pacientes é solicitada sem um token de acesso válido
- **THEN** o sistema rejeita a requisição com HTTP 401
