# Design

## Context

`nutri-back` hoje só implementa `nutritionist-auth`: cadastro, login (e-mail/senha e Google), logout e resolução do nutricionista autenticado via JWT (`CurrentNutritionist`). Não existe nenhuma tabela, entidade ou endpoint de paciente. O System Spec (`nutri-specs`) define, para a capacidade `patient-management`, que paciente tem no mínimo nome completo, data de nascimento e sexo, que a listagem padrão mostra apenas pacientes ativos de um nutricionista, e que cada nutricionista só vê os próprios pacientes — mas não detalha campos de contato, apesar do `Purpose` da capacidade mencionar "informações de contato" pesquisáveis. Ver `proposal.md` - Why para a motivação (login sem destino, ausência de base de pacientes).

Esta change soma a essa base o requisito trazido pelo usuário: um endpoint de busca de pacientes, além da listagem simples.

## Goals / Non-Goals

**Goals:**
- Decidir os campos de contato do paciente que o System Spec deixa em aberto, de forma mínima e coerente com o `Purpose` de `patient-management`.
- Decidir a forma do endpoint de busca (path dedicado vs. query params na listagem) e quais campos do paciente são buscáveis/filtráveis, com justificativa.
- Definir o formato de paginação e de resposta dos três endpoints novos, reaproveitando os padrões já usados por `nutritionist-auth` (DTOs de resposta, `ApiError`, autenticação via `CurrentNutritionist`).
- Definir o modelo de persistência mínimo de `Patient` que sustenta listagem, detalhe e busca, incluindo o campo de status ativo/inativo mesmo sem endpoint de desativação nesta change.

**Non-Goals:**
- Desenhar o endpoint de cadastro, atualização, desativação ou reativação de paciente (fora de escopo desta change, conforme `proposal.md`).
- Desenhar prescrição de dieta ou avaliação física.
- Full-text search (ex.: Elasticsearch) ou busca fonética/fuzzy — o volume de pacientes por nutricionista não justifica isso agora.

## Decisions

### Campos de contato do paciente: e-mail e telefone, ambos opcionais
O `Purpose` de `patient-management` no System Spec cita "informações de contato" pesquisáveis, mas os `Requirements` só exigem nome completo, data de nascimento e sexo no cadastro. Como esta change não inclui o endpoint de cadastro, ela precisa apenas decidir o formato de persistência/exposição desses campos, não sua obrigatoriedade no cadastro (isso fica para a change de cadastro). Opta-se por `email` e `phone`, ambos opcionais (nullable), pois são os dois canais de contato universais em um consultório de nutrição e não há, no System Spec ou no domínio, indicação de outros campos de contato relevantes (ex.: endereço postal não é mencionado em nenhum requisito). Alternativa considerada: não incluir nenhum campo de contato nesta change e deixá-los inteiramente para a change de cadastro. Rejeitada porque a busca por contato foi meta explícita desta change (sugestão do usuário) e a entidade `Patient` já precisa existir aqui; adicionar as duas colunas nullable agora evita uma migração adicional quando o cadastro for implementado.

### Campos buscáveis: nome completo (texto parcial), sexo (filtro exato), faixa de data de nascimento (filtro por intervalo), e-mail e telefone (texto parcial)
Avaliação campo a campo, dado o conjunto de campos do paciente (nome completo, data de nascimento, sexo, e-mail, telefone):
- **Nome completo**: campo primário de busca. Um nutricionista lembra o nome do paciente com muito mais frequência do que qualquer outro dado; busca textual parcial e case-insensitive (`LIKE %termo%` via `LOWER(full_name) LIKE ...` ou `Containing`/`IgnoreCase` do Spring Data) é o caso de uso dominante.
- **Sexo**: cardinalidade baixa e valores fixos, não faz sentido como busca textual; expõe-se como filtro de igualdade exata (ex.: `?sexo=FEMININO`), combinável com os demais filtros.
- **Data de nascimento**: não é um bom campo de busca textual (nutricionista raramente busca por data exata) nem um bom candidato a igualdade exata isolada; expõe-se como filtro por intervalo (`dataNascimentoInicio`/`dataNascimentoFim`), útil para localizar pacientes por faixa etária.
- **E-mail e telefone**: exceção ao caso "não incluir contato na busca" — como esses campos existem justamente para contato, é razoável que o nutricionista busque um paciente pelo e-mail ou telefone que tem em mãos (ex.: veio de um WhatsApp ou e-mail recebido). Busca por correspondência parcial, mesmo tratamento de nome.
Todos os filtros/parâmetros são combináveis via AND (ex.: nome parcial + sexo exato); nenhum parâmetro é obrigatório - ausência de todos os parâmetros de busca equivale à listagem simples (mesmo resultado que `GET /api/pacientes`, já paginado e já restrito aos pacientes ativos do nutricionista autenticado).

### Endpoint de busca como path dedicado (`GET /api/pacientes/busca`), não como query params em `GET /api/pacientes`
Alternativas consideradas:
1. **Path dedicado** (`GET /api/pacientes/busca?nome=&sexo=&dataNascimentoInicio=&dataNascimentoFim=&email=&telefone=`) - escolhida. Mantém `GET /api/pacientes` como o contrato simples e estável exigido pelo pós-login (sem parâmetros de filtro, apenas paginação), e isola a lógica de busca (mais parâmetros, mais casos de validação) em seu próprio controller/serviço, sem arriscar comportamento surpresa em quem chama a listagem simples sem saber que ela aceita filtros.
2. **Query params opcionais em `GET /api/pacientes`** - rejeitada por esta change, mas não descartada permanentemente; documentada aqui como alternativa válida caso, no futuro, o frontend prefira um único endpoint parametrizável. Custo de mudar depois é baixo (mesma camada de serviço).
Ambos os endpoints reaproveitam o mesmo `PatientService`/`PatientRepository` por trás; a diferença é só de contrato HTTP.

### Paginação via Spring Data `Pageable`, parâmetros padrão `page`/`size`/`sort`
Reaproveita o suporte nativo do Spring Data (`PagingAndSortingRepository`), evitando código de paginação manual. Resposta usa um DTO de página (conteúdo + metadados: página atual, tamanho, total de elementos, total de páginas) em vez de expor `org.springframework.data.domain.Page` diretamente, para não acoplar o contrato HTTP à biblioteca. Tamanho de página padrão: 25, aplicado quando o parâmetro `size` não é informado. Valores aceitos para `size`: apenas 10, 25 ou 50 - o mesmo conjunto de opções que o frontend exibirá em um campo de seleção no final da tela de listagem; qualquer outro valor é rejeitado com HTTP 400. Alternativa considerada: aceitar qualquer valor até um máximo (ex.: 100), sem restringir a um conjunto fechado - rejeitada porque o produto já define um conjunto fixo de opções para o usuário final, e validar exatamente esse conjunto no backend evita que o cliente envie um tamanho que a interface nunca ofereceria; alargar o conjunto aceito no futuro é uma mudança compatível (basta ampliar a lista de valores válidos).

### Isolamento por nutricionista aplicado na camada de repositório
Toda consulta de `Patient` (listagem, detalhe, busca) filtra por `nutritionistId = CurrentNutritionist.id()` diretamente na query (não em memória após buscar tudo), replicando o padrão que `nutritionist-auth` já estabelece para autorização. Um paciente de outro nutricionista solicitado por `GET /api/pacientes/{id}` deve responder HTTP 404 (não 403), para não confirmar a um nutricionista que um determinado `id` existe na base de outro dono.

### Status ativo/inativo modelado agora, mesmo sem endpoint de desativação
A coluna `active` (boolean, default `true`) é necessária para que a listagem e a busca já apliquem corretamente a regra do System Spec ("listagem padrão exclui pacientes desativados"), mesmo que nenhum endpoint desta change permita desativar um paciente. Os poucos registros usados em teste são inseridos com `active = true` por padrão via repositório/seed; testes de listagem cobrem também o caso de um paciente inativo inserido diretamente no banco para verificar que ele não aparece na listagem nem na busca.

## Risks / Trade-offs

- [Ausência de endpoint de cadastro nesta change] → Mitigação: dados de teste são inseridos via repositório JPA diretamente nos testes de integração (não via API); a lacuna é explícita em `proposal.md` e a change de cadastro é responsabilidade de uma change futura que reaproveita a mesma entidade `Patient`.
- [Path dedicado de busca duplica parcialmente a query de listagem] → Mitigação: ambos os endpoints compartilham o mesmo `PatientService`/`PatientRepository`; a duplicação fica restrita ao controller/DTO de parâmetros, não à lógica de consulta.
- [Busca por e-mail/telefone parcial pode ter custo de índice em bases grandes] → Mitigação: fora de escopo otimizar agora (volume por nutricionista é baixo); se necessário, uma change futura pode adicionar índice ou migrar para busca dedicada (ex.: Elasticsearch) sem alterar o contrato HTTP.
