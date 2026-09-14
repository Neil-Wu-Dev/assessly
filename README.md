# Assessly

Assessly is a React + Spring Boot PoC for cybersecurity assessment workflows.

The core boundary is intentional:

- AI translates natural-language Security Controls into constrained, reviewable Machine Rules.
- The Java Rule Engine performs deterministic PASS/FAIL assessment from confirmed rules and structured evidence.
- Evidence parsing is deterministic and only accepts data that can become column/value rows.
- Rule results remain traceable to Machine Rule and original Security Control text.
- BYOK is provider-neutral: users enter provider name, base URL, model name, and API key in the web UI. Only provider name/base URL/model are stored per user; API keys are never stored in the database and only live in a 3-hour server memory session.

## Structure

```text
assessly/
├── backend/   # Java Spring Boot API
├── frontend/  # React + TypeScript UI
├── release/   # Reserved for future release artifacts
└── database-init.sql
```

## Local IntelliJ Backend Configuration

For normal IntelliJ development, edit this local file directly:

```text
D:\dev\assessly\backend\src\main\resources\application-local.yml
```

It is intentionally ignored by Git because it can contain your local PostgreSQL password.

Example:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/assessly
    username: postgres
    password: your-local-postgres-password

assessly:
  api-session-ttl-minutes: 180
```

`application.yml` imports `application-local.yml` automatically, so you do not need to type environment variables in a terminal when running from IntelliJ.

`.env.example` is only a reference for command-line or deployment-style configuration. It is not required for IntelliJ local development.

## Local Database

PostgreSQL and pgvector are expected to be installed already.

Create the local database once:

```powershell
psql -h localhost -p 5432 -U postgres -f database-init.sql
```

Flyway migrations enable and use `vector` in the Assessly database.

## Backend

Open this folder in IntelliJ:

```text
D:\dev\assessly\backend
```

Then run:

```text
src/main/java/com/assessly/AssesslyBackendApplication.java
```

The backend defaults to:

```text
http://localhost:8080
```

## Frontend

Open this folder in VS Code:

```text
D:\dev\assessly\frontend
```

Run:

```powershell
npm install
npm run dev
```

The UI defaults to `http://localhost:8080/api/v1` for the backend.

## AI Provider BYOK

Assessly does not hardcode a specific AI provider.

Users enter these values in the web UI:

- Provider name
- Provider base URL
- Model name
- API key

The backend validates the key against:

```text
{baseUrl}/chat/completions
```

This expects an OpenAI-compatible chat completions API. Provider adapters can be added later for non-compatible APIs without storing user API keys.

Stored in PostgreSQL per user:

- Provider name
- Base URL
- Model name

Never stored in PostgreSQL:

- API key

## Rule AST

Rules are JSON AST, never arbitrary code.

Supported node types:

- `condition`
- `and`
- `or`
- `not`
- `if`

Example:

```json
{
  "rules": [
    {
      "id": "iam-04",
      "fieldsUsed": ["role", "mfa_enabled"],
      "sourceControl": {
        "controlId": "IAM-04",
        "text": "Administrative accounts must use MFA."
      },
      "explanation": "Identify admin accounts, then require MFA.",
      "ast": {
        "type": "if",
        "if": { "type": "condition", "field": "role", "operator": "=", "value": "admin" },
        "then": { "type": "condition", "field": "mfa_enabled", "operator": "=", "value": true }
      }
    }
  ]
}
```

