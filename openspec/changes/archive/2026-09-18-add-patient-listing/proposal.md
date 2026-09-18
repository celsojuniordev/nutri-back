# Proposal

## Why

O System Spec (`nutri-specs`) já define que, após um login bem-sucedido, o nutricionista deve ser direcionado à listagem de seus próprios pacientes ativos (capacidade `nutritionist-auth`, requisito "Destino Padrão Após Login", que depende da capacidade `patient-management`). Hoje o backend `nutri-back` só implementa `nutritionist-auth`: não existe entidade, endpoint ou tabela de paciente. Sem essa listagem, o login não tem para onde levar o nutricionista e nenhuma outra capacidade de paciente (prescrição de dieta, avaliação física) tem uma base de dados para se apoiar. Esta change traduz a fatia de listagem e visualização de pacientes do System Spec em uma API concreta para este backend, e soma a ela uma capacidade de busca de pacientes.

## What Changes

- Adiciona a entidade `Patient` persistida via Spring Data JPA/MySQL, com nome completo, data de nascimento, sexo, e-mail e telefone (campos de contato, decisão técnica registrada em `design.md`), um indicador de status ativo/inativo, e vínculo obrigatório com o nutricionista dono (isolamento multi-tenant).
- Adiciona endpoint `GET /api/pacientes`, protegido por autenticação, que retorna a lista paginada dos pacientes ativos do nutricionista autenticado — este é o conteúdo apresentado imediatamente após o login, conforme já definido em `nutritionist-auth`.
- Adiciona endpoint `GET /api/pacientes/{id}`, protegido por autenticação, que retorna o detalhe de um paciente pertencente ao nutricionista autenticado, negando acesso a pacientes de outros nutricionistas.
- Adiciona endpoint `GET /api/pacientes/busca`, protegido por autenticação, que permite pesquisar os pacientes do nutricionista autenticado por nome (busca textual parcial), sexo (filtro exato) e faixa de data de nascimento (filtro por intervalo) — ver `design.md` para a avaliação dos campos buscáveis e a justificativa de um path dedicado em vez de sobrecarregar `GET /api/pacientes`.
- Adiciona tratamento de erros e validação de parâmetros de paginação/busca reaproveitando o formato padronizado `ApiError` e o `GlobalExceptionHandler` já existentes.
- **Fora de escopo nesta change**: cadastro, atualização e desativação/reativação de paciente (ficam para uma change futura); qualquer interface de frontend. Como consequência, esta change não expõe nenhum endpoint de escrita para `Patient` — os registros usados para exercitar listagem, detalhe e busca são inseridos diretamente via repositório/seed nos testes, não via API (lacuna assumida e registrada em `design.md`).

## Capabilities

### New Capabilities
- `patient-management`: Listagem paginada, visualização de detalhe e busca dos pacientes pertencentes ao nutricionista autenticado, com isolamento de dados por nutricionista dono. Mesmo nome de capacidade usado no System Spec (`nutri-specs`, change `add-nutritionist-core-features`), aqui restrito à fatia de leitura (listagem/visualização/busca).

### Modified Capabilities
Nenhuma — `nutritionist-auth` já prevê, no System Spec, que a listagem de pacientes é o destino pós-login; esta change apenas passa a existir como capacidade consumível, sem alterar nenhum requisito já implementado de `nutritionist-auth` neste backend.

## Impact

- **Novo código**: entidade JPA `Patient`, repositório (`PatientRepository`, com suporte a `Pageable` e especificações/queries de busca), serviço (`PatientService`), controller REST (`PatientController`), DTOs de resposta (`PatientResponse`, `PatientPageResponse` ou equivalente) e de parâmetros de busca.
- **Banco de dados**: nova tabela `patients` com colunas `full_name`, `birth_date`, `sex`, `email` (opcional), `phone` (opcional), `active` (default true) e `nutritionist_id` (chave estrangeira para `nutritionists`), via a mesma estratégia de migração já usada por `nutritionists`.
- **API pública**: introduz os três primeiros endpoints de paciente (`GET /api/pacientes`, `GET /api/pacientes/{id}`, `GET /api/pacientes/busca`), todos protegidos pelo mecanismo de autenticação já existente (`CurrentNutritionist`); nenhum endpoint existente é alterado.
- **Dependências**: nenhuma biblioteca nova; reaproveita `spring-boot-starter-data-jpa`, `spring-boot-starter-web`, `spring-boot-starter-security` e `mysql-connector-j` já presentes.
- **Capacidades futuras**: estabelece a entidade `Patient` e o padrão de isolamento por `nutritionistId` que as changes futuras de cadastro/atualização/desativação de paciente, prescrição de dieta e avaliação física vão reaproveitar.
