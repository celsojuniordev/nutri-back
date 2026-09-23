# Design

## Context

`PatientController`/`PatientService`/`PatientRepository` já existem, cobrindo apenas leitura (`GET /api/pacientes`, `/{id}`, `/busca`), com isolamento por `CurrentNutritionist.id()`. A tabela `patients` (migração `V3__create_patients.sql`) já tem todas as colunas necessárias (`full_name`, `birth_date`, `sex`, `email` nullable, `phone` nullable, `active`, `nutritionist_id`), então nenhuma coluna nova é necessária — apenas um índice.

O padrão de erro (`ApiError`, `GlobalExceptionHandler`) e o padrão de conflito de e-mail já existem para `Nutritionist` (`EmailAlreadyInUseException` + fallback em `DataIntegrityViolationException`, ver `NutritionistService.register`), e servem de modelo direto para o conflito de e-mail de paciente, com uma diferença: aqui o corpo de erro precisa carregar o paciente já existente (ver proposal.md e a decisão de UX de "sugerir reativação" em vez de rejeitar silenciosamente).

## Goals / Non-Goals

**Goals:**
- Definir o contrato exato dos dois novos endpoints de escrita (`POST`/`PUT`), incluindo forma do payload e do corpo de resposta em cada cenário (sucesso, validação, conflito de e-mail, não encontrado).
- Definir como a unicidade de e-mail por nutricionista (ativos + inativos) é garantida em concorrência, reaproveitando o padrão já usado em `NutritionistService`.
- Definir o formato do corpo de erro de conflito de e-mail, que precisa carregar mais informação (o paciente já existente) do que o `ApiError` padrão hoje carrega.
- Definir como o handler de `sexo` inválido é adicionado sem quebrar o contrato de erro já documentado em `API.md`.

**Non-Goals:**
- Migrar dados existentes: não há e-mails duplicados hoje porque não havia cadastro via API; a constraint nova não precisa de passo de limpeza de dados.
- Cascata de efeitos de desativação sobre planos alimentares ou avaliações físicas — essas capacidades ainda não existem neste backend.
- Qualquer endpoint dedicado de desativar/reativar — decisão do produto foi consolidar tudo em `PUT`.

## Decisions

### Um único `PatientWriteRequest` (record) reaproveitado por cadastro e atualização
Ambos os endpoints exigem o mesmo conjunto de campos, com a mesma validação (`@NotBlank`/`@Size`/`@Past-or-present` em `birthDate`, etc.), diferindo apenas em `active` (cadastro sempre cria como ativo, ignorando ou rejeitando esse campo no payload de entrada; atualização exige `active` explicitamente). Decisão: dois records de request separados — `PatientCreateRequest` (sem campo `active`) e `PatientUpdateRequest` (com campo `active` obrigatório) — em vez de um único record com `active` opcional. Alternativa considerada: um único DTO com `Boolean active` opcional (ignorado no cadastro). Rejeitada porque um campo aceito e silenciosamente ignorado em um dos dois endpoints é uma armadilha de API (cliente manda `active: false` no cadastro e o paciente é criado ativo mesmo assim, sem nenhum aviso) — dois DTOs deixam o contrato de cada endpoint explícito e a ausência do campo em `PatientCreateRequest` já é auto-documentada pelo Bean Validation.

### Unicidade de e-mail via índice único composto `(nutritionist_id, email)` + verificação otimista no serviço
Nova migração `V4__add_patient_email_unique_index.sql` cria `CONSTRAINT uq_patients_nutritionist_email UNIQUE (nutritionist_id, email)`. MySQL trata cada valor `NULL` em uma unique key como distinto dos demais, então múltiplos pacientes do mesmo nutricionista sem e-mail continuam permitidos (consistente com o cenário "ausência de e-mail nunca gera conflito"). `PatientService.create`/`update` primeiro consulta `PatientRepository.findByNutritionistIdAndEmailIgnoreCase` (novo método) para construir o erro 409 com os dados do paciente conflitante antes de tentar salvar; em caso de corrida (duas requisições concorrentes), o `DataIntegrityViolationException` da constraint é capturado e a mesma consulta é refeita para montar o corpo de erro — mesmo padrão de dupla checagem já usado em `NutritionistService.register`. Comparação de e-mail é case-insensitive, consistente com a busca (`PatientSpecifications`) e com o cadastro de nutricionista.
Alternativa considerada: checar unicidade só na aplicação, sem constraint de banco. Rejeitada pela mesma razão que `Nutritionist` usa as duas camadas: sem a constraint, uma corrida de duas requisições concorrentes com o mesmo e-mail poderia criar dois pacientes duplicados.

### Corpo de erro de conflito de e-mail: `ApiError` padrão + paciente existente aninhado
Novo record `PatientEmailConflictError` que estende o formato de erro atual: mesmos campos de `ApiError` (`status`, `error`, `message`, `timestamp`) mais um campo `existingPatient: PatientResponse` com os dados públicos do paciente já cadastrado com aquele e-mail (reaproveitando o `PatientResponse` já existente, incluindo seu `id` e seu `active`). O código de erro é `PATIENT_EMAIL_ALREADY_IN_USE`. Isso permite ao cliente decidir chamar `PUT /api/pacientes/{existingPatient.id}` com `active: true` para reativar, sem uma segunda requisição de consulta.
Alternativa considerada: colocar o `id` do paciente existente dentro de `ApiError.details` (como um `FieldError` de propósito geral). Rejeitada porque `FieldError` carrega só `field`/`message` (strings), insuficiente para expressar um paciente inteiro sem gambiarra (ex. serializar JSON dentro de uma string); um campo adicional tipado é mais direto e não quebra nenhum client existente, já que `ApiError` já usa `@JsonInclude(NON_NULL)` e nenhum client depende da ausência desse campo extra em outros tipos de erro.

### Handler de `sexo` inválido via `MethodArgumentTypeMismatchException`
Novo `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` em `GlobalExceptionHandler`, escopado a converter qualquer falha de conversão de `@RequestParam`/`@PathVariable` em `ApiError` (`INVALID_PARAMETER`, HTTP 400), citando o nome do parâmetro problemático em `details`. Como o handler é genérico (não específico de `sexo`), ele automaticamente cobre tanto o `sexo` do endpoint de busca quanto qualquer parâmetro de tipo incompatível futuro nesta ou em outras capacidades — não é um retrabalho isolado desta change, mas o mesmo mecanismo que a spec de busca já pede.
Alternativa considerada: um handler específico só para `sexo` (verificando o nome do parâmetro dentro do handler). Rejeitada por ser mais frágil (acopla o handler a um nome de parâmetro específico) sem nenhum ganho sobre o handler genérico.

### Atualização não usa `PATCH` nem aceita payload parcial
Já decidido no discovery: `PUT` com payload completo, sem suporte a atualização parcial. Isso significa que o cliente é responsável por reenviar todos os campos, inclusive os que não mudaram (incluindo o próprio e-mail atual, coberto pelo cenário "manter o próprio e-mail não gera conflito").

## Risks / Trade-offs

- [Cliente esquece de reenviar `active: true` em uma atualização de rotina e desativa o paciente sem querer] → Mitigação: fora de escopo de backend; é um risco inerente à decisão de produto de usar `PUT` completo em vez de endpoints dedicados de status, e cabe ao frontend proteger com uma tela de edição que sempre carrega o estado atual antes de enviar.
- [Índice único novo falhar ao aplicar caso já existam e-mails duplicados em ambiente com dados de teste/seed antigos] → Mitigação: a migração `V4` é adicionada agora, cedo no ciclo do projeto (sem dados de produção); se algum ambiente de desenvolvimento tiver duplicatas de teste, a migração falha de forma visível (Flyway) em vez de silenciosamente, e pode ser resolvida limpando o dado de teste antes de reaplicar.
- [Handler genérico de `MethodArgumentTypeMismatchException` capturar casos não previstos, ex. `id` de paciente inválido em `/api/pacientes/{id}`] → Mitigação: comportamento aceitável — um `id` não numérico também é um parâmetro inválido e merece HTTP 400 com `ApiError`, em vez do 500 genérico do Spring que ocorre hoje; nenhum cenário do spec depende do comportamento atual (não testado).
