# Proposal

## Why

`patient-management` hoje só cobre leitura (listagem, detalhe e busca): não existe nenhum endpoint de escrita para `Patient`, decisão explicitamente adiada na change `add-patient-listing`. Isso significa que, na prática, nenhum paciente pode ser cadastrado via API — os únicos registros existentes vêm de inserção direta em banco/testes. O System Spec (`nutri-specs`, change `add-nutritionist-core-features`) já define os requisitos de Cadastro, Atualização e Desativação de Paciente para esta capacidade; esta change implementa essa fatia de escrita no backend, fechando `patient-management` por completo e desbloqueando as capacidades futuras (`diet-prescription`, `physical-assessment`) que dependem de pacientes reais existirem no sistema.

## What Changes

- Novo endpoint `POST /api/pacientes`: cadastra um paciente vinculado ao nutricionista autenticado, exigindo nome completo, data de nascimento e sexo; e-mail e telefone continuam opcionais.
- Novo endpoint `PUT /api/pacientes/{id}`: atualiza um paciente que o nutricionista autenticado possui, com payload completo (todos os campos, incluindo `active`). Não há endpoints dedicados de desativar/reativar — a mudança de status ativo/inativo acontece através deste mesmo endpoint.
- Validação de data de nascimento: rejeita datas futuras (HTTP 400) tanto no cadastro quanto na atualização.
- Regra de unicidade de e-mail de paciente por nutricionista (considerando pacientes ativos e inativos): ao detectar conflito no cadastro ou na atualização, o sistema responde HTTP 409 identificando o paciente já existente (em vez de criar um duplicado), permitindo ao cliente decidir reativá-lo através do próprio `PUT /api/pacientes/{id}`.
- Tratamento de erro padronizado (`ApiError`) para valor inválido do parâmetro `sexo`, aplicado tanto aos novos endpoints de escrita quanto ao endpoint de busca já existente (`GET /api/pacientes/busca`), que hoje não usa esse formato para esse caso (lacuna já registrada no relatório de conformidade da change anterior).
- Enum `Sex` permanece com os mesmos dois valores (`MASCULINO`, `FEMININO`); nenhuma mudança de valores aceitos.

## Capabilities

### New Capabilities
Nenhuma.

### Modified Capabilities
- `patient-management`: adiciona os requisitos "Cadastro de Paciente", "Atualização de Paciente" e "Desativação/Reativação de Paciente" (a versão do backend consolida desativação e reativação em um único requisito, já que ambas passam pelo mesmo endpoint de atualização); estende o requisito de busca existente para padronizar o erro de valor inválido de `sexo`.

## Impact

- **Novo código**: `PatientCreateRequest`/`PatientUpdateRequest` (DTOs), métodos `PatientService.create` e `PatientService.update`, novos endpoints em `PatientController`, exceção `PatientEmailAlreadyInUseException` (com dados do paciente conflitante) e seu handler em `GlobalExceptionHandler`, handler para `MethodArgumentTypeMismatchException` (valor inválido de `sexo`).
- **Banco de dados**: nova migração adicionando índice único composto em `patients` (`nutritionist_id`, `email`), permitindo múltiplos e-mails nulos.
- **API pública**: dois novos endpoints de escrita (`POST /api/pacientes`, `PUT /api/pacientes/{id}`), protegidos pelo mecanismo de autenticação já existente (`CurrentNutritionist`); nenhum endpoint existente é removido, e o comportamento de leitura (listagem/detalhe/busca) não muda, exceto pelo formato de erro de `sexo` inválido na busca.
- **Dependências**: nenhuma biblioteca nova.
