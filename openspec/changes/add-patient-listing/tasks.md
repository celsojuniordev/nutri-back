# Tasks

## 1. Modelo de Dados e Migração

- [ ] 1.1 Criar a migração Flyway (`V3__create_patients.sql`) para a tabela `patients` (`id`, `full_name` obrigatório, `birth_date` obrigatório, `sex` obrigatório, `email` opcional, `phone` opcional, `active` boolean não nulo com default `true`, `nutritionist_id` obrigatório com chave estrangeira para `nutritionists`) e verificar que a migração aplica sem erro em ambiente local
- [ ] 1.2 Implementar a entidade JPA `Patient` (campos `fullName`, `birthDate`, `sex`, `email` nullable, `phone` nullable, `active` com default `true`, `nutritionistId`) e verificar com um teste de repositório (`@DataJpaTest`) que um paciente persiste corretamente com todos os campos, incluindo os opcionais ausentes
- [ ] 1.3 Implementar `PatientRepository` (Spring Data JPA) com um método paginado que retorna apenas pacientes ativos de um `nutritionistId` (ex.: `findByNutritionistIdAndActiveTrue(Long, Pageable)`) e verificar com um teste de repositório que pacientes inativos e de outros nutricionistas são excluídos do resultado
- [ ] 1.4 Implementar no `PatientRepository` (via `Specification`/query derivada) a consulta de busca combinando, de forma cumulativa e todos opcionais, nome parcial case-insensitive, sexo exato, faixa de data de nascimento, e e-mail/telefone parciais, restrita a pacientes ativos de um `nutritionistId`, e verificar com testes de repositório cada critério isoladamente e em combinação (ver design.md - Decisões: campos buscáveis)

## 2. DTOs e Formato de Resposta Paginada

- [ ] 2.1 Implementar o DTO `PatientResponse` (id, fullName, birthDate, sex, email, phone, active) e verificar com um teste unitário de serialização que o JSON gerado contém exatamente esses campos
- [ ] 2.2 Implementar um DTO de página (ex.: `PageResponse<T>` com content, page, size, totalElements, totalPages) reutilizável pelos endpoints de listagem e busca, e verificar com um teste unitário que ele é construído corretamente a partir de um `org.springframework.data.domain.Page`
- [ ] 2.3 Implementar validação de parâmetros de paginação (tamanho padrão 20, máximo 100) e verificar com um teste unitário que um `size` acima de 100 é rejeitado

## 3. Endpoint de Listagem

- [ ] 3.1 Implementar `PatientService.listActive(nutritionistId, pageable)` reutilizando o repositório de 1.3 e verificar com um teste unitário que delega corretamente ao repositório com o `nutritionistId` do nutricionista autenticado
- [ ] 3.2 Implementar `PatientController` com `GET /api/pacientes`, protegido por autenticação, resolvendo o nutricionista autenticado via `CurrentNutritionist`, e verificar com testes de integração (`MockMvc`) os cenários "Listagem retorna apenas pacientes ativos do próprio nutricionista", "Pacientes de outro nutricionista não aparecem", "Pacientes inativos não aparecem na listagem padrão" e "Listagem sem pacientes cadastrados" do spec `patient-management`
- [ ] 3.3 Verificar com um teste de integração o cenário "Paginação respeitada" do spec `patient-management` (inserindo mais pacientes do que uma página e solicitando páginas específicas)
- [ ] 3.4 Verificar com um teste de integração o cenário "Tamanho de página acima do limite rejeitado" (HTTP 400) e o cenário "Listagem sem autenticação negada" (HTTP 401) do spec `patient-management`

## 4. Endpoint de Detalhe

- [ ] 4.1 Implementar `PatientService.getOwnedById(nutritionistId, patientId)` (lança exceção mapeada para HTTP 404 quando o paciente não existe ou não pertence ao nutricionista, sem distinguir os dois casos) e verificar com testes unitários os três desfechos: paciente encontrado, paciente inexistente, paciente de outro nutricionista
- [ ] 4.2 Mapear a exceção de 4.1 no `GlobalExceptionHandler` para HTTP 404 com o formato `ApiError` já existente, e verificar com um teste unitário/integração que o corpo de erro segue essa estrutura
- [ ] 4.3 Implementar `GET /api/pacientes/{id}` em `PatientController`, protegido por autenticação, e verificar com testes de integração os cenários "Detalhe retornado com sucesso", "Detalhe de paciente de outro nutricionista negado" (HTTP 404), "Detalhe de paciente inexistente" (HTTP 404) e "Detalhe de paciente inativo ainda acessível" do spec `patient-management`
- [ ] 4.4 Verificar com um teste de integração o cenário "Consulta de detalhe sem autenticação negada" (HTTP 401) do spec `patient-management`

## 5. Endpoint de Busca

- [ ] 5.1 Implementar o DTO de parâmetros de busca (`nome`, `sexo`, `dataNascimentoInicio`, `dataNascimentoFim`, `email`, `telefone`, todos opcionais, mais os parâmetros de paginação) e verificar com um teste unitário que uma faixa de data de nascimento com `dataNascimentoInicio` posterior a `dataNascimentoFim` dispara violação de validação
- [ ] 5.2 Implementar `PatientService.search(nutritionistId, criteria, pageable)` reutilizando a consulta de 1.4 e verificar com testes unitários que critérios ausentes não filtram e que múltiplos critérios são combinados com E lógico
- [ ] 5.3 Implementar `GET /api/pacientes/busca` em `PatientController`, protegido por autenticação, e verificar com testes de integração os cenários "Busca por nome parcial", "Busca por sexo", "Busca por faixa de data de nascimento" e "Busca por e-mail ou telefone parcial" do spec `patient-management`
- [ ] 5.4 Verificar com testes de integração os cenários "Combinação de múltiplos critérios", "Busca sem nenhum critério informado" (mesmo resultado da listagem padrão) e "Busca sem resultados" do spec `patient-management`
- [ ] 5.5 Verificar com testes de integração os cenários "Busca não retorna pacientes de outro nutricionista" e "Busca não retorna pacientes inativos" do spec `patient-management`
- [ ] 5.6 Verificar com testes de integração os cenários "Faixa de data de nascimento inválida rejeitada" (HTTP 400) e "Busca sem autenticação negada" (HTTP 401) do spec `patient-management`

## 6. Dados de Teste

- [ ] 6.1 Implementar um mecanismo de apoio a testes (ex.: builder/factory de `Patient` usado via `PatientRepository` diretamente nos testes de integração) para inserir pacientes ativos/inativos e de diferentes nutricionistas sem depender de um endpoint de cadastro (ver design.md - Riscos/Trade-offs), e verificar que os testes das seções 3 a 5 o utilizam de forma consistente

## 7. Validação da Especificação e Revisão Final

- [ ] 7.1 Executar `openspec validate add-patient-listing --strict` e verificar que não reporta erros
- [ ] 7.2 Executar a suíte completa de testes (`./gradlew test`) e verificar que todos os testes das seções 1 a 6 passam
- [ ] 7.3 Revisar se cada cenário do spec delta `patient-management` tem pelo menos um teste automatizado correspondente, registrando e corrigindo qualquer lacuna encontrada

## 8. Documentação

- [ ] 8.1 Documentar os três endpoints (listagem, detalhe, busca) — método, path, parâmetros, payload de resposta e códigos de erro — em `API.md` ou anotações OpenAPI/Springdoc, e verificar que cada endpoint documentado corresponde a um teste de integração existente
