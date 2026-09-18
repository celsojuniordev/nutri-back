# API

Todas as respostas de erro seguem o mesmo formato JSON (`ApiError`):

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Descrição legível do problema",
  "details": [ { "field": "email", "message": "formato de e-mail inválido" } ],
  "timestamp": "2026-01-01T00:00:00Z"
}
```

`details` só aparece quando há erros de validação por campo.

## Cadastro, Login e Perfil de Nutricionista

Códigos de `error` usados nesta capacidade: `VALIDATION_ERROR`, `EMAIL_ALREADY_IN_USE`, `INVALID_CREDENTIALS`, `UNAUTHORIZED`, `GOOGLE_TOKEN_INVALID`.

### POST /api/nutricionistas

Cadastra uma nova conta de nutricionista. Não requer autenticação.

**Request body**
```json
{
  "name": "Ana Silva",
  "email": "ana@example.com",
  "password": "Senha123",
  "company": "Clínica Vida"
}
```
- `name`: obrigatório, até 255 caracteres, não pode ser vazio ou só espaços.
- `email`: obrigatório, formato de e-mail válido, até 255 caracteres.
- `password`: obrigatório, 8–72 caracteres, deve conter ao menos uma letra e um número.
- `company`: opcional, até 255 caracteres, não pode ser só espaços quando informado.

**Resposta de sucesso — `201 Created`**
```json
{ "id": 1, "name": "Ana Silva", "company": "Clínica Vida", "email": "ana@example.com" }
```
(`company` é omitido quando não informado.)

**Erros**: `400 VALIDATION_ERROR` (campo obrigatório ausente, formato inválido, tamanho excedido), `409 EMAIL_ALREADY_IN_USE` (e-mail já cadastrado).

### GET /api/nutricionistas/me

Retorna o perfil do nutricionista autenticado. Requer `Authorization: Bearer <token>`.

**Resposta de sucesso — `200 OK`**
```json
{ "id": 1, "name": "Ana Silva", "company": "Clínica Vida", "email": "ana@example.com" }
```

**Erros**: `401 UNAUTHORIZED` (sem token, token expirado, malformado ou revogado).

### POST /api/auth/login

Autentica por e-mail e senha. Não requer autenticação.

**Request body**
```json
{ "email": "ana@example.com", "password": "Senha123" }
```

**Resposta de sucesso — `200 OK`**
```json
{ "token": "<jwt>" }
```

**Erros**: `400 VALIDATION_ERROR` (campo ausente), `401 INVALID_CREDENTIALS` (e-mail/senha incorretos, e-mail inexistente, ou conta criada apenas via Google sem senha local — sem distinguir os casos na resposta).

### POST /api/auth/google

Autentica ou cadastra automaticamente via conta Google. Não requer autenticação.

**Request body**
```json
{ "idToken": "<token de identidade emitido pelo Google no cliente>" }
```

**Resposta de sucesso — `200 OK`**
```json
{ "token": "<jwt>", "accountCreated": true }
```
`accountCreated` indica se uma conta nova foi criada nesta chamada (`true`) ou se uma conta existente foi autenticada/vinculada (`false`). Ao vincular uma conta já existente, o nome e a empresa cadastrados não são sobrescritos pelos dados do Google.

**Erros**: `400 VALIDATION_ERROR` (`idToken` ausente), `401 GOOGLE_TOKEN_INVALID` (token inválido, expirado, emissor/audiência incorretos, ou e-mail do Google não verificado).

### POST /api/auth/logout

Invalida o token atual. Requer `Authorization: Bearer <token>`.

**Resposta de sucesso — `200 OK`**, sem corpo. Após o logout, o mesmo token passa a ser rejeitado (`401 UNAUTHORIZED`) em qualquer endpoint protegido.

**Erros**: `401 UNAUTHORIZED` (sem token válido).

## Listagem, Detalhe e Busca de Pacientes

Todos os endpoints desta capacidade requerem `Authorization: Bearer <token>` e retornam apenas dados dos pacientes do nutricionista autenticado. Códigos de `error` adicionais usados aqui (mesmo formato `ApiError` acima): `PATIENT_NOT_FOUND`, `INVALID_PAGE_SIZE`, `INVALID_DATE_RANGE`.

Cadastro, atualização e desativação de paciente não fazem parte desta versão da API (ver `openspec/changes/add-patient-listing/proposal.md`); os endpoints abaixo são somente leitura.

### GET /api/pacientes

Lista, de forma paginada, os pacientes ativos do nutricionista autenticado. É o conteúdo apresentado imediatamente após o login.

**Query params**
- `page`: opcional, número da página (0-indexado). Padrão: `0`.
- `size`: opcional. Valores aceitos: `10`, `25` ou `50`. Padrão: `25`.
- `sort`: opcional, formato padrão do Spring Data (ex.: `fullName,asc`).

**Resposta de sucesso — `200 OK`**
```json
{
  "content": [
    { "id": 1, "fullName": "Ana Silva", "birthDate": "1990-05-10", "sex": "FEMININO", "email": "ana@example.com", "phone": "11999990000", "active": true }
  ],
  "page": 0,
  "size": 25,
  "totalElements": 1,
  "totalPages": 1
}
```
(`email` e `phone` são omitidos quando não cadastrados.)

**Erros**: `400 INVALID_PAGE_SIZE` (`size` fora de `10`/`25`/`50`), `401 UNAUTHORIZED` (sem token válido).

### GET /api/pacientes/{id}

Retorna o detalhe de um paciente do nutricionista autenticado, incluindo pacientes inativos.

**Resposta de sucesso — `200 OK`**
```json
{ "id": 1, "fullName": "Ana Silva", "birthDate": "1990-05-10", "sex": "FEMININO", "email": "ana@example.com", "phone": "11999990000", "active": true }
```

**Erros**: `404 PATIENT_NOT_FOUND` (paciente inexistente ou pertencente a outro nutricionista — sem distinguir os dois casos), `401 UNAUTHORIZED` (sem token válido).

### GET /api/pacientes/busca

Pesquisa os pacientes ativos do nutricionista autenticado combinando, de forma cumulativa (E lógico), os critérios informados. Nenhum critério é obrigatório; sem nenhum, retorna o mesmo resultado que `GET /api/pacientes`.

**Query params** (todos opcionais, além de `page`/`size`/`sort` como em `GET /api/pacientes`)
- `nome`: correspondência parcial, sem diferenciar maiúsculas/minúsculas.
- `sexo`: correspondência exata (`MASCULINO` ou `FEMININO`).
- `dataNascimentoInicio` / `dataNascimentoFim`: intervalo de data de nascimento (formato `AAAA-MM-DD`), qualquer um dos dois pode ser informado isoladamente.
- `email`: correspondência parcial.
- `telefone`: correspondência parcial.

**Resposta de sucesso — `200 OK`**: mesmo formato de página de `GET /api/pacientes`.

**Erros**: `400 INVALID_DATE_RANGE` (`dataNascimentoInicio` posterior a `dataNascimentoFim`), `400 INVALID_PAGE_SIZE` (`size` fora de `10`/`25`/`50`), `401 UNAUTHORIZED` (sem token válido).
