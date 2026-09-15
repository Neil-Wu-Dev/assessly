# Assessly Test Files

Use these files to quickly test the PoC flow.

1. Create a Dataset in Assessly.
2. Upload `ASSESSLY_TEST_CONTROLS.txt` on the Controls page.
3. Upload `ASSESSLY_TEST_EVIDENCE.csv` on the Evidence Data page.
4. On Rule Builder, paste the contents of `ASSESSLY_TEST_RULESET.json` into the manual rule editor and save it.
5. Confirm the saved Rule Set.
6. Run Assessment using the uploaded evidence and confirmed Rule Set.

Expected violations:

- `Neil` violates `IAM-04` because he is an active admin and `mfa_enabled` is `false`.
- `Neil` violates `VM-01` because severity is `critical` and `remediation_days` is `45`.

Expected summary:

- Records evaluated: `4`
- Rules evaluated: `2`
- Violations detected: `2`
