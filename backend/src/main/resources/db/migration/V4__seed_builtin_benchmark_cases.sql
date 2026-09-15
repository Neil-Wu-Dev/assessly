INSERT INTO benchmark_cases (
    id,
    owner_id,
    name,
    control_text,
    test_data_json,
    ground_truth_rule_json,
    expected_result_json,
    builtin,
    created_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    NULL,
    'Administrative MFA',
    'Administrative accounts must use MFA.',
    '[{"role":"admin","mfa_enabled":false},{"role":"user","mfa_enabled":true}]',
    '{"rules":[{"id":"gt-iam-mfa","fieldsUsed":["role","mfa_enabled"],"sourceControl":{"controlId":"IAM-04","text":"Administrative accounts must use MFA."},"explanation":"Admin accounts must have MFA enabled.","ast":{"type":"if","if":{"type":"condition","field":"role","operator":"=","value":"admin"},"then":{"type":"condition","field":"mfa_enabled","operator":"=","value":true}}}]}',
    '',
    TRUE,
    NOW()
) ON CONFLICT (id) DO NOTHING;
