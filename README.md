# nutri-back

Backend do sistema de nutrição (Java 25, Spring Boot, MySQL). Esta versão implementa a capacidade `nutritionist-auth`: cadastro, login (e-mail/senha e Google), consulta de perfil e logout de nutricionistas. Veja [API.md](API.md) para o contrato dos endpoints e `openspec/changes/add-nutritionist-auth/` para a especificação completa.

## Pré-requisitos

- JDK 25 (o Gradle Wrapper baixa o toolchain automaticamente via `foojay-resolver`, desde que haja acesso à internet).
- MySQL 8+ rodando localmente (ou acessível pela rede) para o ambiente de desenvolvimento/produção. Os testes automatizados usam H2 em memória e não precisam de MySQL.

## Variáveis de ambiente obrigatórias

A aplicação **falha ao subir** se qualquer uma destas variáveis estiver ausente:

| Variável | Descrição |
|---|---|
| `JWT_SECRET` | Segredo usado para assinar os tokens JWT (HS256). Deve ter pelo menos 32 caracteres. Nunca reutilize o valor de exemplo abaixo em produção. |
| `JWT_EXPIRATION_SECONDS` | Tempo de expiração do token de acesso, em segundos. Valor sugerido: `3600` (1 hora). |
| `GOOGLE_CLIENT_ID` | Client ID do OAuth 2.0/OpenID Connect do Google usado para validar o login social. Veja abaixo como obter um para testes. |
| `ALLOWED_ORIGINS` | Uma ou mais origens (separadas por vírgula) autorizadas via CORS a chamar esta API (ex.: a URL do frontend). |

Além disso, o datasource é configurado pelas variáveis padrão do Spring Boot (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) apontando para o MySQL.

Exemplo de `.env` (ou exporte as variáveis no shell antes de rodar):

```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/nutri"
export SPRING_DATASOURCE_USERNAME="nutri"
export SPRING_DATASOURCE_PASSWORD="troque-esta-senha"
export JWT_SECRET="troque-este-segredo-por-um-valor-aleatorio-com-32-ou-mais-caracteres"
export JWT_EXPIRATION_SECONDS=3600
export GOOGLE_CLIENT_ID="seu-client-id.apps.googleusercontent.com"
export ALLOWED_ORIGINS="http://localhost:5173"
```

### Obtendo um Google Client ID de teste

1. Acesse o [Google Cloud Console](https://console.cloud.google.com/) e crie (ou selecione) um projeto.
2. Vá em **APIs & Services > Credentials** e crie uma credencial do tipo **OAuth client ID**, tipo de aplicação **Web application**.
3. Adicione a origem do seu frontend em **Authorized JavaScript origins** (ex.: `http://localhost:5173`).
4. Copie o **Client ID** gerado e use como valor de `GOOGLE_CLIENT_ID`. O frontend usa esse mesmo Client ID para obter o `idToken` (via Google Identity Services / "One Tap") que é enviado a `POST /api/auth/google`.

## Rodando localmente

```bash
./gradlew bootRun
```

## Rodando os testes

```bash
./gradlew test
```

Os testes usam um banco H2 em memória (modo de compatibilidade MySQL) configurado em `src/test/resources/application.properties`, com as migrações Flyway reais aplicadas — não é necessário MySQL nem as variáveis de ambiente acima para rodar a suíte de testes.
