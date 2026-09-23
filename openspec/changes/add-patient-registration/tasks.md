# Tasks

## 1. Migração e Acesso a Dados

- [ ] 1.1 Criar a migração Flyway `V4__add_patient_email_unique_index.sql` adicionando `CONSTRAINT uq_patients_nutritionist_email UNIQUE (nutritionist_id, email)` à tabela `patients`, e verificar que a migração aplica sem erro em ambiente local (nenhuma limpeza de dado é necessária, ver design.md - Non-Goals)
- [ ] 1.2 Implementar `PatientRepository.findByNutritionistIdAndEmailIgnoreCase(Long, String)` e verificar com um teste de repositório que encontra um paciente existente (ativo ou inativo) pelo e-mail dentro do mesmo nutricionista, e que não encontra nada para outro nutricionista ou e-mail diferente

## 2. DTOs de Escrita

- [ ] 2.1 Implementar `PatientCreateRequest` (fullName obrigatório não-branco ≤255, birthDate obrigatório não futuro, sex obrigatório, email opcional ≤255 em formato válido quando informado, phone opcional ≤30) e verificar com testes unitários de validação cada regra isoladamente (campo ausente, tamanho excedido, nome só com espaços, data futura)
- [ ] 2.2 Implementar `PatientUpdateRequest` (mesmos campos de 2.1 mais `active` obrigatório) e verificar com testes unitários de validação que as mesmas regras de 2.1 se aplicam e que a ausência de `active` é rejeitada
- [ ] 2.3 Implementar `PatientEmailConflictError` (status, error, message, timestamp, `existingPatient: PatientResponse`) e verificar com um teste unitário de serialização que o JSON gerado contém os dados públicos do paciente conflitante, incluindo seu `id`

## 3. Cadastro de Paciente

- [ ] 3.1 Implementar `PatientService.create(nutritionistId, PatientCreateRequest)`: verifica conflito de e-mail via 1.2 antes de salvar, cria o paciente como ativo vinculado ao nutricionista, e trata `DataIntegrityViolationException` da constraint de 1.1 como o mesmo conflito de e-mail (mesma dupla checagem usada em `NutritionistService.register`, ver design.md - Decisões); verificar com testes unitários os desfechos de sucesso (com e sem e-mail) e de conflito de e-mail
- [ ] 3.2 Mapear a exceção de conflito de e-mail de paciente para HTTP 409 com `PatientEmailConflictError` no `GlobalExceptionHandler`, código `PATIENT_EMAIL_ALREADY_IN_USE`, e verificar com um teste unitário/integração que o corpo de erro contém o paciente já existente
- [ ] 3.3 Implementar `POST /api/pacientes` em `PatientController`, protegido por autenticação, retornando HTTP 201 com `PatientResponse`, e verificar com testes de integração os cenários "Cadastro bem-sucedido com campos mínimos" e "Cadastro bem-sucedido com todos os campos" do spec `patient-management`
- [ ] 3.4 Verificar com testes de integração os cenários "Campo obrigatório ausente rejeitado", "Nome apenas com espaços em branco rejeitado" e "Campo excede o tamanho máximo permitido" do spec `patient-management` para o cadastro
- [ ] 3.5 Verificar com testes de integração os cenários "Data de nascimento futura rejeitada", "E-mail já usado por outro paciente do mesmo nutricionista rejeitado", "E-mail repetido entre nutricionistas diferentes aceito" e "Ausência de e-mail nunca gera conflito" do spec `patient-management` para o cadastro
- [ ] 3.6 Verificar com um teste de integração o cenário "Cadastro sem autenticação negado" (HTTP 401) do spec `patient-management`

## 4. Atualização de Paciente

- [ ] 4.1 Implementar `PatientService.update(nutritionistId, patientId, PatientUpdateRequest)`: reutiliza a resolução de posse já existente (mesma regra de `getOwnedById`, HTTP 404 sem distinguir inexistente de outro nutricionista), aplica a mesma verificação de conflito de e-mail de 3.1 mas excluindo o próprio paciente da checagem, substitui todos os campos (incluindo `active`) e salva; verificar com testes unitários os desfechos de sucesso, desativação, reativação, paciente de outro nutricionista, paciente inexistente e conflito de e-mail com outro paciente
- [ ] 4.2 Implementar `PUT /api/pacientes/{id}` em `PatientController`, protegido por autenticação, retornando HTTP 200 com `PatientResponse`, e verificar com testes de integração os cenários "Atualização bem-sucedida", "Desativação de paciente via atualização" e "Reativação de paciente via atualização" do spec `patient-management`
- [ ] 4.3 Verificar com testes de integração os cenários "Atualização de paciente de outro nutricionista negada" (HTTP 404) e "Atualização de paciente inexistente" (HTTP 404) do spec `patient-management`
- [ ] 4.4 Verificar com testes de integração os cenários "Campo obrigatório ausente na atualização rejeitado" e "Data de nascimento futura rejeitada na atualização" do spec `patient-management`
- [ ] 4.5 Verificar com testes de integração os cenários "Manter o próprio e-mail não gera conflito" e "E-mail já usado por outro paciente do mesmo nutricionista rejeitado na atualização" do spec `patient-management`
- [ ] 4.6 Verificar com um teste de integração o cenário "Atualização sem autenticação negada" (HTTP 401) do spec `patient-management`

## 5. Erro Padronizado para Parâmetro Inválido

- [ ] 5.1 Implementar `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` em `GlobalExceptionHandler`, retornando HTTP 400 com `ApiError` (código `INVALID_PARAMETER`, `details` identificando o parâmetro), e verificar com um teste unitário/integração usando um `ProbeController` (mesmo padrão já usado para as demais exceções)
- [ ] 5.2 Verificar com um teste de integração o cenário "Valor de sexo inválido rejeitado com erro padronizado" do spec `patient-management` contra `GET /api/pacientes/busca?sexo=<valor inválido>`

## 6. Validação da Especificação e Revisão Final

- [ ] 6.1 Executar `openspec validate add-patient-registration --strict` e verificar que não reporta erros
- [ ] 6.2 Executar a suíte completa de testes (`./gradlew test`) e verificar que todos os testes das seções 1 a 5 passam
- [ ] 6.3 Revisar se cada cenário ADICIONADO e MODIFICADO do spec delta `patient-management` tem pelo menos um teste automatizado correspondente, registrando e corrigindo qualquer lacuna encontrada

## 7. Documentação

- [ ] 7.1 Documentar os dois novos endpoints (cadastro, atualização) — método, path, payload de requisição, payload de resposta e códigos de erro, incluindo o formato de `PatientEmailConflictError` — em `API.md`, e atualizar a documentação do endpoint de busca já existente para refletir o novo formato de erro de `sexo` inválido; verificar que cada endpoint documentado corresponde a um teste de integração existente
