CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE datasets (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    schema_json TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE evidence_files (
    id UUID PRIMARY KEY,
    dataset_id UUID NOT NULL REFERENCES datasets(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    format VARCHAR(24) NOT NULL,
    columns_json TEXT NOT NULL,
    rows_json TEXT NOT NULL,
    row_count INTEGER NOT NULL,
    metadata_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_documents (
    id UUID PRIMARY KEY,
    dataset_id UUID NOT NULL REFERENCES datasets(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    format VARCHAR(24) NOT NULL,
    title VARCHAR(255),
    full_text TEXT NOT NULL,
    structure_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE control_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES control_documents(id) ON DELETE CASCADE,
    control_id VARCHAR(120),
    section VARCHAR(255),
    page INTEGER,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    parent_reference VARCHAR(255) NOT NULL,
    embedding vector(1536),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE rule_sets (
    id UUID PRIMARY KEY,
    dataset_id UUID NOT NULL REFERENCES datasets(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    rules_json TEXT NOT NULL,
    generation_summary_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    UNIQUE(dataset_id, version)
);

CREATE TABLE assessment_runs (
    id UUID PRIMARY KEY,
    dataset_id UUID NOT NULL REFERENCES datasets(id) ON DELETE CASCADE,
    rule_set_id UUID NOT NULL REFERENCES rule_sets(id),
    evidence_file_id UUID NOT NULL REFERENCES evidence_files(id),
    status VARCHAR(32) NOT NULL,
    records_evaluated INTEGER NOT NULL,
    rules_evaluated INTEGER NOT NULL,
    violations_detected INTEGER NOT NULL,
    result_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE benchmark_cases (
    id UUID PRIMARY KEY,
    owner_id UUID REFERENCES app_users(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    control_text TEXT NOT NULL,
    test_data_json TEXT NOT NULL,
    ground_truth_rule_json TEXT NOT NULL,
    expected_result_json TEXT NOT NULL,
    builtin BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_datasets_owner ON datasets(owner_id);
CREATE INDEX idx_evidence_dataset ON evidence_files(dataset_id);
CREATE INDEX idx_controls_dataset ON control_documents(dataset_id);
CREATE INDEX idx_chunks_document ON control_chunks(document_id);
CREATE INDEX idx_rule_sets_dataset_version ON rule_sets(dataset_id, version DESC);
CREATE INDEX idx_assessment_dataset ON assessment_runs(dataset_id, created_at DESC);
