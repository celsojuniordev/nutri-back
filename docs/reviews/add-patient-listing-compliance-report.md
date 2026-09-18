# Relatório de Conformidade — `add-patient-listing`

**Data da revisão**: 2026-09-18
**Fonte de verdade**: `openspec/changes/add-patient-listing/specs/patient-management/spec.md` (spec delta), complementado por `proposal.md` e `design.md`.
**Código revisado**: `src/main/java/com/br/nutri/patient/**`, `src/main/java/com/br/nutri/common/{PageResponse,PageSizePolicy,PatientNotFoundException,InvalidDateRangeException,InvalidPageSizeException,GlobalExceptionHandler}.java`, `src/main/resources/db/migration/V3__create_patients.sql`, `API.md`, e todos os testes em `src/test/java/com/br/nutri/patient/**` e os novos/relacionados em `src/test/java/com/br/nutri/common/**`.
**Comandos executados**: `openspec validate add-patient-listing --strict` (só warnings de estilo RFC 2119, esperados por a spec estar em PT-BR) e `./gradlew test --rerun` (BUILD SUCCESSFUL; 10 classes de teste relevantes, 57 testes, 0 falhas/erros).

---

## Requisitos atendidos

### Requirement: Listagem Paginada de Pacientes
Todos os 8 cenários têm implementação e teste de integração correspondente em `PatientListingIntegrationTest.java`:
- Retorna apenas ativos do próprio nutricionista (`returnsOnlyActivePatientsOfAuthenticatedNutritionist`), isolamento por `nutritionistId` aplicado em `PatientRepository.findByNutritionistIdAndActiveTrue` (repositório, não em memória — conforme design.md).
- Pacientes de outro nutricionista excluídos (mesmo teste, paciente extra de `otherNutritionist()`).
- Inativos excluídos da listagem padrão (`Carla Dias` inativa no mesmo teste).
- Lista vazia sem pacientes (`returnsEmptyListWhenNutritionistHasNoPatients`).
- Paginação respeitada (`respectsPagination`, 12 pacientes, página 1 de tamanho 10 → 2 itens, `totalElements=12`, `totalPages=2`).
- Tamanho padrão 25 quando ausente (`appliesDefaultPageSizeOfTwentyFiveWhenNotInformed`) — implementado via `@PageableDefault(size = 25)` em `PatientController.list` e reforçado por `PageSizePolicy.DEFAULT_SIZE`.
- Tamanho fora de `{10,25,50}` rejeitado com 400 (`rejectsPageSizeOutsideAllowedSet`) — `PageSizePolicy.validate` chamado em `PatientService.listActive`.
- Sem autenticação → 401 (`rejectsRequestWithoutAuthentication`) — coberto pelo `SecurityConfig` (`anyRequest().authenticated()`) e `JwtAuthenticationEntryPoint`.

### Requirement: Visualização de Detalhe do Paciente
Todos os 5 cenários cobertos em `PatientDetailIntegrationTest.java` e reforçados em `PatientServiceTest.java`:
- Detalhe com sucesso, 404 para paciente de outro nutricionista, 404 para inexistente (mesmo código HTTP e mesmo `error: PATIENT_NOT_FOUND`, sem distinguir os dois casos — exatamente como o spec exige em "Detalhe de paciente de outro nutricionista negado": *"sem indicar se o paciente existe na base de outro nutricionista"*).
- Detalhe de paciente inativo ainda acessível (`returnsInactivePatientDetail`) — `PatientService.getOwnedById` não filtra por `active`, correto.
- Sem autenticação → 401 (`rejectsDetailRequestWithoutAuthentication`).

### Requirement: Busca de Pacientes por Critérios
Todos os 11 cenários cobertos em `PatientSearchIntegrationTest.java` e em `PatientRepositoryTest.java`/`PatientSearchCriteriaTest.java`:
- Busca por nome parcial case-insensitive, sexo exato, faixa de data de nascimento, e-mail/telefone parcial, combinação com E lógico, ausência de critérios = listagem padrão, sem resultados, exclusão de outros nutricionistas e de inativos, faixa de data invertida rejeitada com 400 `INVALID_DATE_RANGE`, sem autenticação → 401.
- Implementação via `PatientSpecifications.matching` (Specification cumulativa com `AND`), reaproveitada tanto por `PatientService.search` quanto pelos testes de repositório — consistente com a decisão de design de path dedicado compartilhando `PatientService`/`PatientRepository`.

### Modelo de dados e infraestrutura
- `V3__create_patients.sql` cria a tabela `patients` com exatamente as colunas descritas em `proposal.md`/`tasks.md` (`full_name`, `birth_date`, `sex`, `email` nullable, `phone` nullable, `active boolean not null default true`, `nutritionist_id` com FK para `nutritionists`). Migração aplicada com sucesso nos testes (Flyway).
- Entidade `Patient` com campos em inglês (`fullName`, `birthDate`, `sex`, `email`, `phone`, `active`, `nutritionistId`) — em conformidade com a convenção de nomenclatura do projeto (identificadores em inglês).
- `PageResponse<T>` e `PageSizePolicy` implementados e testados isoladamente (`PageResponseTest`, `PageSizePolicyTest`), reutilizáveis entre listagem e busca.
- `GlobalExceptionHandler` ganhou os três novos mapeamentos (`PatientNotFoundException`→404, `InvalidPageSizeException`→400, `InvalidDateRangeException`→400), todos testados em `GlobalExceptionHandlerTest` com um `ProbeController` dedicado, seguindo o padrão já usado para as exceções de `nutritionist-auth`.
- Mensagens de exceção voltadas ao usuário final (`PatientNotFoundException`, `InvalidPageSizeException`, `InvalidDateRangeException`) estão em PT-BR, consistente com a convenção do projeto.
- `API.md` documenta os três endpoints (query params, payloads de sucesso, códigos de erro), com uma seção dedicada "Listagem, Detalhe e Busca de Pacientes" que remete corretamente ao `proposal.md` para justificar a ausência de endpoints de escrita.

### Tasks.md
As 26 tarefas marcadas como concluídas refletem a realidade do código: cada uma tem artefato de produção e teste correspondente identificável (migração, entidade, repositório, DTOs, serviço, controller, tratamento de erro, dados de teste via `PatientTestFactory`, e documentação em `API.md`). `openspec validate --strict` não reporta erros (apenas 3 warnings de estilo RFC 2119 esperados, já que a spec é intencionalmente escrita em PT-BR).

## Requisitos não atendidos

Nenhum requisito do spec delta `patient-management` ficou sem implementação ou sem teste correspondente.

*Observação de escopo* (não é requisito não atendido): não há endpoints de escrita (`POST`/`PUT`/`DELETE`) para `Patient`. Isso é esperado — está explicitamente fora do escopo desta change, conforme `proposal.md` ("Fora de escopo nesta change") e `design.md` ("Non-Goals").

## Comportamentos contraditórios

Nenhum comportamento implementado contradiz diretamente o spec delta. Dois pontos limítrofes que vale registrar (não são contradições, mas merecem atenção):

1. **Busca por telefone não usa `LOWER()`, busca por e-mail e nome sim** (`PatientSpecifications.matching`, linhas do bloco `telefone`): o spec exige apenas que a busca por telefone seja "correspondência parcial" (sem exigir explicitamente case-insensitive, ao contrário do nome, que o spec pede *"sem diferenciar maiúsculas/minúsculas"*). Como telefone tipicamente não contém letras, a ausência de `LOWER()` não deve gerar bug prático, mas é uma assimetria de tratamento entre os três campos de texto parcial (nome e e-mail usam `LOWER()`, telefone não) que vale documentar como decisão intencional, já que não está registrada em `design.md`.
2. **Nenhuma validação para valor de `sexo` fora do enum** — ver seção "Riscos/edge cases" abaixo.

## Cobertura de testes

Cobertura é ampla e, em geral, bem alinhada aos cenários do spec. Um ponto de qualidade de teste a destacar:

- **`PatientServiceTest.getOwnedByIdThrowsWhenPatientBelongsToAnotherNutritionist`** (linhas 70–75) é **idêntico** ao teste anterior `getOwnedByIdThrowsWhenPatientDoesNotExist` (linhas 63–68): ambos fazem `mock(repository.findByIdAndNutritionistId(10L, 1L)).thenReturn(Optional.empty())` e verificam a mesma exceção. Como `PatientService.getOwnedById` delega inteiramente ao repositório sem lógica adicional para distinguir "não existe" de "pertence a outro nutricionista", os dois testes exercitam exatamente o mesmo caminho de código e a mesma asserção — o segundo teste não adiciona cobertura real, apesar de a tarefa 4.1 pedir explicitamente a verificação dos "três desfechos" (paciente encontrado, inexistente, de outro nutricionista) como testes distintos.
  - **Severidade**: baixa (não é um bug de comportamento — o comportamento em si está correto e é exatamente o que o spec pede: "sem distinguir os dois casos" — HTTP 404 em ambos). É um problema de nomenclatura/cobertura de teste: o nome do teste sugere uma verificação que ele não faz de fato no nível de unidade (a diferenciação real só existe no dado de entrada do banco, testada em `PatientRepositoryTest.doesNotFindPatientOwnedByAnotherNutritionist`, que é onde essa distinção efetivamente é exercitada).
  - **Fonte de verdade**: `tasks.md`, item 4.1: *"verificar com testes unitários os três desfechos: paciente encontrado, paciente inexistente, paciente de outro nutricionista"*.

Fora esse ponto, todos os cenários do spec delta têm pelo menos um teste automatizado direto:
- Listagem: 8/8 cenários cobertos em `PatientListingIntegrationTest`.
- Detalhe: 5/5 cenários cobertos em `PatientDetailIntegrationTest`.
- Busca: 11/11 cenários cobertos em `PatientSearchIntegrationTest`.
- Regras de persistência/isolamento reforçadas em `PatientRepositoryTest` (11 testes) e regras de serviço em `PatientServiceTest` (6 testes).
- Serialização do DTO de resposta e validação da faixa de datas testadas isoladamente (`PatientResponseTest`, `PatientSearchCriteriaTest`).
- Novos mapeamentos de exceção testados em `GlobalExceptionHandlerTest` (`PATIENT_NOT_FOUND`, `INVALID_PAGE_SIZE`, `INVALID_DATE_RANGE`).

Nenhum teste encontrado valida um comportamento incorreto (i.e., nenhum teste "codifica" um bug como se fosse comportamento esperado).

## Riscos/edge cases

1. **Valor inválido de `sexo` na busca não gera erro `ApiError` padronizado (alta/média)**: `GET /api/pacientes/busca?sexo=OUTRO` (um valor fora do enum `Sex { MASCULINO, FEMININO }`) faz o Spring falhar na conversão do `@RequestParam Sex sexo` com `MethodArgumentTypeMismatchException`, que **não** tem handler em `GlobalExceptionHandler`. O resultado é a resposta de erro padrão do Spring Boot (formato `{"timestamp":...,"status":500,"error":"Internal Server Error",...}` ou 400 dependendo da configuração default), não o formato `ApiError` usado em todo o resto da API. Isso não é um cenário do spec delta (que só define os valores válidos de sexo como filtro exato), mas é um edge case plausível de uso real (erro de digitação do cliente) sem teste e sem tratamento padronizado. **Severidade**: média — não quebra nenhum requisito do spec, mas foge do padrão de erro da API documentado em `API.md` ("Todas as respostas de erro seguem o mesmo formato JSON").
2. **`size`/`page` não numéricos ou negativos**: mesma situação do item acima — não há teste nem handler específico para `size=abc` ou `page=-1`; o comportamento depende do `PageableHandlerMethodArgumentResolver` padrão do Spring Data, que pode silenciosamente normalizar `page` negativo para `0` (não gera 400) ou lançar exceção de binding para valores não numéricos, novamente sem o formato `ApiError`. Não é exigido explicitamente pelo spec delta, mas é um edge case não coberto por teste.
3. **Enum `Sex` com apenas dois valores (`MASCULINO`, `FEMININO`)**: nem o spec delta nem `design.md` listam os valores válidos de sexo (o spec só fala em "sexo... correspondência exata"). A escolha binária não é discutida como uma "Decision" em `design.md` (ao contrário de outras decisões de campo, como contatos e paginação, que têm justificativa e alternativas registradas). Não é um erro de implementação — apenas uma decisão de modelagem que não foi documentada nem confirmada com o produto, o que pode gerar retrabalho de migração se uma change futura de cadastro precisar de mais valores.
4. **Ordenação (`sort`) mencionada no requisito ("ordenáveis") mas sem teste dedicado**: o requisito de listagem diz que o resultado deve ser "ordenável", e a implementação de fato aceita o parâmetro `sort` nativo do Spring Data `Pageable` (documentado em `API.md`), mas não há nenhum teste de integração que exercite `?sort=fullName,asc` (ou qualquer variação) verificando a ordem retornada. Não é um requisito não atendido — a capacidade existe via `Pageable` — mas é uma lacuna de cobertura de teste para uma característica explicitamente citada no requisito.
5. **Assimetria `LOWER()` em telefone vs. nome/e-mail** — já discutida na seção "Comportamentos contraditórios", item 1.

## Ambiguidades na Spec

1. **Valores válidos de `sexo`**: o spec delta e o `design.md` nunca enumeram os valores aceitos para o campo/filtro `sexo` (o `design.md` do System Spec, citado em `proposal.md`, é mencionado como não detalhando "sexo" também). A implementação optou por um enum binário (`MASCULINO`, `FEMININO`). Isso é uma decisão razoável dado o que existe, mas como o Purpose menciona apenas "sexo" sem detalhar, sinalizo como ambiguidade a ser resolvida explicitamente (mesmo que retroativamente) em `design.md`, e não como erro de implementação.
2. **"Ordenável" no requisito de listagem**: o requisito diz que a listagem deve ser "ordenável" mas não especifica: (a) quais campos podem ser usados para ordenação, (b) qual é a ordenação padrão quando `sort` não é informado. A implementação delega inteiramente ao comportamento padrão do Spring Data (`Pageable`/`@PageableDefault`), o que tecnicamente satisfaz "ordenável" mas deixa a ordenação padrão indefinida (na prática, a ordem do banco, não determinística sem `ORDER BY` explícito). Não é uma falha de implementação — é uma lacuna do spec que pode levar a comportamento não determinístico de listagem em produção (ex.: paginação instável entre duas chamadas se registros forem inseridos entre elas). Vale considerar registrar uma ordenação padrão explícita (ex.: por `fullName` ou `id`) em uma revisão futura da spec/design.
3. **Mecanismo de validação da faixa de datas ("dispara violação de validação")**: a tarefa 5.1 usa a expressão "dispara violação de validação", que poderia sugerir uma `ConstraintViolationException` do Bean Validation. A implementação optou por uma exceção de domínio customizada (`InvalidDateRangeException`) lançada no construtor compacto do record `PatientSearchCriteria`, mapeada manualmente no `GlobalExceptionHandler`. O efeito observável (400 com `ApiError`) é idêntico e a tarefa/teste (`PatientSearchCriteriaTest.rejectsStartDateAfterEndDate`) confirma a exceção lançada — não considero isso uma implementação incorreta, apenas assinalo que o texto da tarefa é ambíguo quanto ao mecanismo esperado.

---

## Resumo executivo

Implementação em conformidade com a spec delta `patient-management`: todos os 24 cenários (8 listagem + 5 detalhe + 11 busca) têm teste automatizado correspondente e passam (`./gradlew test --rerun` = BUILD SUCCESSFUL, 0 falhas). Isolamento multi-tenant, regra de "apenas ativos" na listagem/busca, contrato de erro padronizado e paginação com tamanho fixo (10/25/50, padrão 25) estão todos corretos e testados. Não foram encontrados requisitos não implementados nem comportamentos que contradigam o spec. Achados relevantes ficam nos níveis de teste redundante (baixa severidade) e edge cases de validação de parâmetros não padronizados no formato `ApiError` (média severidade, fora do escopo formal do spec delta, mas relevante para consistência da API).
