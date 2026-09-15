CREATE TABLE benchmark_runs (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    benchmark_case_id UUID REFERENCES benchmark_cases(id) ON DELETE SET NULL,
    control_text TEXT NOT NULL,
    test_data_json TEXT NOT NULL,
    ground_truth_rule_json TEXT NOT NULL,
    expected_result_json TEXT,
    ai_generated_rule_json TEXT NOT NULL,
    generated_execution_json TEXT NOT NULL,
    ground_truth_execution_json TEXT NOT NULL,
    differences_json TEXT NOT NULL,
    structurally_equivalent BOOLEAN NOT NULL,
    execution_equivalent BOOLEAN NOT NULL,
    accuracy DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_benchmark_runs_owner ON benchmark_runs(owner_id, created_at DESC);
