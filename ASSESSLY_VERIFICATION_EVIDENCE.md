# Assessly Verification Evidence

Date: 2026-09-15
Workspace: D:\dev\assessly

## Executive Status

The frontend production build is verified and passes.
The backend source has been expanded to expose the full functional API surface for auth, AI BYOK session, datasets, evidence, controls/RAG chunks, rule generation/review/update/confirm, deterministic assessment, history, and reliability benchmarks.
The backend compile command could not run to Java compilation in this Codex shell because Gradle fails while opening its local loopback/selector channel before compiling project code.

Because backend runtime/compile evidence is unavailable in this shell, this report does not claim complete production readiness. It records exactly what is directly evidenced and what still requires verification in IntelliJ or a normal local terminal.

## Direct Command Evidence Collected

1. Frontend production build passed:
   - Command: npm run build
   - Result: TypeScript build and Vite production build completed successfully.
   - Output includes: dist/index.html, dist/assets/index-DFBswbiQ.css, dist/assets/index-QBmGAwdg.js, "built in 258ms".

2. Backend API surface is present in controller source:
   - Command: rg -n "@(Get|Post|Delete|Patch|Put)Mapping|RequestMapping" backend/src/main/java/com/assessly/api/v1/endpoints
   - Result: 38 mapped routes found across HealthController, AuthController, DatasetController, BenchmarkController.

3. Frontend calls real backend APIs:
   - Command: rg -n "await request\(|fetch\(" frontend/src/App.tsx
   - Result: visible calls for health, auth, AI session, datasets, evidence rows/delete/upload, control delete/chunks/upload, rule generate/manual/update/confirm, assessment start, benchmark cases/runs/run.

4. Database migrations include core persistent structures:
   - Command: rg -n "benchmark_runs|benchmark_cases|CREATE EXTENSION|control_chunks|assessment_runs|rule_sets" backend/src/main/resources/db/migration
   - Result: pgvector extension, control_chunks, rule_sets, assessment_runs, benchmark_cases, benchmark_runs, and seeded built-in benchmark case are present.

5. Code hygiene check passed:
   - Command: git diff --check
   - Result: exit code 0. Only Windows LF-to-CRLF warnings were printed; no whitespace errors remain.

6. Java source corruption check passed:
   - Command: Select-String for literal `n across backend Java files
   - Result: no matches.

7. Backend compile could not be completed in this Codex shell:
   - Command: .\gradlew.bat compileJava --no-daemon --stacktrace
   - Result: failed before Java compilation with java.io.IOException: Unable to establish loopback connection.
   - Stacktrace location: Gradle daemon/client SocketConnection, PipeImpl, WEPollSelector, UnixDomainSockets.
   - Interpretation: environment/tooling loopback failure, not a Java compiler diagnostic.

8. Maven fallback is unavailable:
   - Command: mvn -q -DskipTests compile
   - Result: mvn is not recognized.

## 27 Functional Requirements Coverage

1. User account and login gate: implemented through register/login/logout APIs, session token, password hash service, frontend auth screen, and page gate before app access.
2. AI Provider/BYOK: implemented as provider name/base URL/model/key input; API key is sent to backend session endpoint and disconnectable; key is not stored in ordinary DB by the frontend.
3. Dataset management: create/list/detail/update/delete exposed and wired in frontend Dataset page.
4. Security Control upload: upload endpoint and frontend page exist; documents are stored separately from evidence.
5. Chunking/RAG traceability: control_chunks table, chunk repository/service, and frontend chunk viewer exist.
6. Evidence upload: evidence upload endpoint and frontend upload page exist for structured data.
7. Illegal evidence rejection: parser layer and unsupported evidence exception exist in backend source; requires runtime verification after backend can start.
8. Schema compatibility: dataset schema model/service logic exists; requires runtime mismatch test after backend can start.
9. AI rule generation: rule generation endpoint and frontend trigger exist; gated by active dataset, evidence, controls, and connected AI session.
10. READY/CANNOT_AUTOMATE/ERROR: rule set status is shown on frontend and backend rule generation stores generation summaries/status.
11. Rule AST/language: rule engine validates/evaluates constrained JSON AST; frontend visual builder emits constrained AST JSON.
12. Rule review: frontend displays fields used, generated rule, source control, explanation, generation summary, and status.
13. Visual builder: frontend provides field/operator/value IF/THEN builder using evidence columns.
14. Rule versioning: rule_sets table has dataset/version uniqueness; manual update creates a new rule version and confirmed rule sets are protected.
15. Rule confirmation: confirm endpoint and frontend button exist; assessment uses confirmed rule sets.
16. Assessment start: frontend Assessment page requires selected evidence plus confirmed rule set and calls deterministic assessment endpoint.
17. Assessment result: history page displays records/rules/violations/result JSON from deterministic rule engine output.
18. Assessment history: assessment_runs table and list/detail endpoints exist; History page displays archived runs.
19. Built-in benchmark: benchmark_cases table and V4 seed migration provide a real built-in case.
20. Custom benchmark: create/delete custom benchmark case API and frontend form/buttons exist.
21. Benchmark comparison: BenchmarkService compares canonical AST and deterministic execution signatures, stores benchmark_runs history.
22. Backend API list: 38 mapped endpoints are present in source and listed on frontend API Coverage page.
23. Frontend pages: auth, AI Provider, Datasets, Controls, Evidence, Rule Builder, Assessment, History, Benchmark, API Coverage pages exist.
24. Navigation/guards: app is gated by login; dataset-scoped pages require selected dataset; active nav displays current page.
25. No fake buttons/pages: frontend build proves all wired handlers type-check; source search for TODO/stub/not implemented/fake/mock found no backend fake markers, only normal UI placeholders.
26. Main flow: page order supports login -> AI provider -> dataset -> controls -> evidence -> rules -> confirm -> assessment -> history -> benchmark.
27. Self-audit: this file records direct command evidence and explicitly marks backend runtime/compile verification as not obtained in the current shell.

## Remaining Verification Required Before Claiming Full Production Ready

1. Open backend in IntelliJ and run the Spring Boot main class, or run Gradle in a normal non-Codex terminal where local loopback is allowed.
2. Apply migrations to the local PostgreSQL assessly database.
3. Run an end-to-end browser flow with real backend running:
   - register/login
   - connect BYOK provider
   - create dataset
   - upload control
   - upload valid evidence
   - reject invalid evidence
   - generate or save rule set
   - confirm rule set
   - start assessment
   - inspect history
   - create/run benchmark case
4. Capture backend runtime/API responses as final production-readiness evidence.