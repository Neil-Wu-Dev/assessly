# Assessly Functional Matrix

This document is the implementation checklist for the Assessly PoC. It maps the product requirements to backend APIs, frontend pages, required order, and trigger conditions.

## Core Rule

AI translates natural-language Security Controls into constrained machine-readable rule JSON. The Java Rule Engine performs all PASS/FAIL assessment deterministically. AI must never directly calculate final assessment results.

## Required User Flow

1. Register or log in.
2. Connect an AI provider key on the AI Provider page.
   - Provider name, base URL, and model name may be saved as user settings.
   - API key is stored only in the temporary server session.
3. Create or select a Dataset.
4. Upload Security Control documents.
5. Upload structured Evidence data.
6. Generate rules with AI, or build rules manually with the Visual Rule Builder.
7. Review generated rules, source controls, fields used, explanation, and status.
8. Confirm a READY Rule Set.
9. Run Assessment using selected Evidence and a confirmed Rule Set.
10. Review Assessment History and deterministic violation details.
11. Run AI Reliability Benchmark using built-in or custom cases.

## Backend API Map

| Capability | API | Preconditions | Result |
|---|---|---|---|
| Health check | `GET /api/v1/health` | None | Backend status for UI refresh check |
| Register | `POST /api/v1/auth/register` | Valid email, password length >= 8 | User session token |
| Login | `POST /api/v1/auth/login` | Existing email/password | User session token |
| Connect AI session | `POST /api/v1/auth/ai-session` | Logged in, provider config, API key | Validates provider, stores only temporary secret |
| AI session status | `GET /api/v1/auth/ai-session` | Logged in | Connected/expired/disconnected state |
| Create Dataset | `POST /api/v1/datasets` | Logged in | Dataset folder |
| List Datasets | `GET /api/v1/datasets` | Logged in | Dataset list |
| Dataset detail | `GET /api/v1/datasets/{datasetId}` | Logged in, owns dataset | Dataset, evidence, controls, rule sets, assessments |
| Delete Dataset | `DELETE /api/v1/datasets/{datasetId}` | Logged in, owns dataset | Cascades dataset content |
| Upload Evidence | `POST /api/v1/datasets/{datasetId}/evidence` | Dataset selected | Deterministically parses CSV/XLSX/JSON/JSONL/XML into rows/columns |
| Delete Evidence | `DELETE /api/v1/datasets/{datasetId}/evidence/{evidenceId}` | Dataset selected | Deletes one evidence file |
| Upload Controls | `POST /api/v1/datasets/{datasetId}/controls` | Dataset selected | Extracts full text, structure, chunks |
| AI Rule Generation | `POST /api/v1/datasets/{datasetId}/rules/generate` | Dataset, evidence, controls, connected AI session | Saves READY, CANNOT_AUTOMATE, or ERROR Rule Set |
| Manual Rule Save | `POST /api/v1/datasets/{datasetId}/rules/manual` | Dataset, evidence, valid rule AST | Saves READY Rule Set after AST and field validation |
| Confirm Rule Set | `POST /api/v1/datasets/{datasetId}/rules/{ruleSetId}/confirm` | READY Rule Set and compatible evidence schema | Marks Rule Set executable |
| Start Assessment | `POST /api/v1/datasets/{datasetId}/assessments` | Evidence and confirmed READY Rule Set | Runs Java Rule Engine, saves result |
| Built-in Benchmark Cases | `GET /api/v1/benchmarks/cases` | Logged in | Returns built-in benchmark cases |
| Run Benchmark | `POST /api/v1/benchmarks/run` | Logged in, custom/built-in benchmark payload | AI-generated or user-provided rule is compared against ground truth |

## Frontend Page Map

| Page | Purpose | Backend APIs Used | Disabled/Empty State Rules |
|---|---|---|---|
| Login/Register | Account access only | Register, Login, Health | Business pages hidden until login |
| AI Provider | BYOK provider session | Connect AI session, AI session status | Shows connected/expired/disconnected and expiry |
| Datasets | Dataset selection and deletion | Create/List/Detail/Delete Dataset | Empty state when no datasets exist |
| Controls | Upload and inspect Security Controls | Upload Controls, Dataset Detail | Requires selected Dataset |
| Evidence Data | Upload, list, delete, preview evidence rows | Upload Evidence, Delete Evidence, Dataset Detail | Requires selected Dataset; table preview empty state |
| Rule Builder | AI generation, visual manual builder, review, confirm | Generate Rules, Manual Rule Save, Confirm RuleSet | Requires Dataset; AI generation requires controls + evidence + connected AI; manual builder requires controls + evidence |
| Assessment | Deterministic execution | Start Assessment | Requires evidence and confirmed READY Rule Set |
| History | Assessment archive | Dataset Detail | Shows explicit no-history empty state |
| Benchmark | Built-in/custom reliability evaluation | Benchmark Cases, Benchmark Run | Can use connected AI when generated rule input is blank |
| API Coverage | Developer-facing endpoint coverage | None | Lists currently implemented backend endpoints |

## Operation Order Guards

- Controls and Evidence pages require a selected Dataset.
- AI rule generation requires: selected Dataset + at least one Control document + at least one Evidence file + connected AI session.
- Manual rule saving requires valid AST and fields compatible with the current Evidence schema.
- Rule confirmation requires READY status and compatible current Evidence schema.
- Assessment requires a selected Evidence file and confirmed READY Rule Set.
- Benchmark can compare pasted generated rule JSON, or call the connected AI provider when generated rule JSON is blank.

## Non-Negotiable Runtime Behaviors

- API keys are not stored in the database.
- Evidence parsing is deterministic and rejects unstructured data.
- Rule execution never calls AI.
- Assessment results are tied to the Rule Set version used at run time.
- Schema mismatch must fail clearly before execution.
- Rule generation must return READY, CANNOT_AUTOMATE, or ERROR, not a vague success/failure.
- Frontend buttons must either trigger a real API/action or be disabled with visible prerequisites.
