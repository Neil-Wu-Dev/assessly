# Assessly

Assessly is a React + Spring Boot PoC for cybersecurity assessment workflows.

The core boundary is intentional:

- AI translates natural-language Security Controls into constrained, reviewable Machine Rules.
- The Java Rule Engine performs deterministic PASS/FAIL assessment from confirmed rules and structured evidence.
- Evidence parsing is deterministic and only accepts data that can become column/value rows.
- Rule results remain traceable to Machine Rule and original Security Control text.

## Structure

```text
assessly/
├── backend/   # Java Spring Boot API
├── frontend/  # React + TypeScript UI
├── release/   # Reserved for future release artifacts
└── database-init.sql
```

## Local Database

PostgreSQL and pgvector are expected to be installed already.

Create the local database:

```powershell
psql -h localhost -p 5432 -U postgres -f database-init.sql
```

Runtime database credentials are injected through environment variables. Do not commit secrets.

```powershell
$env:ASSESSLY_DB_NAME="assessly"
$env:ASSESSLY_DB_HOST="localhost"
$env:ASSESSLY_DB_PORT="5432"
$env:ASSESSLY_DB_USER="postgres"
$env:ASSESSLY_DB_PASSWORD="your-local-password"
```

Flyway migrations enable and use `vector` in the Assessly database.

## Backend

```powershell
cd backend
.\gradlew.bat bootRun
```

Important API boundaries:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/deepseek`
- `GET /api/v1/auth/deepseek`
- `POST /api/v1/datasets`
- `POST /api/v1/datasets/{datasetId}/evidence`
- `POST /api/v1/datasets/{datasetId}/controls`
- `POST /api/v1/datasets/{datasetId}/rules/generate`
- `POST /api/v1/datasets/{datasetId}/rules/manual`
- `POST /api/v1/datasets/{datasetId}/assessments`

## Frontend

```powershell
cd frontend
npm install
npm run dev
```

The UI defaults to `http://localhost:8080/api/v1`.
Override with:

```powershell
$env:VITE_API_BASE_URL="http://localhost:8080/api/v1"
```

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
