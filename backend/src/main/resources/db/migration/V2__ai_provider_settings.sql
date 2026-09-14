CREATE TABLE ai_provider_settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
    provider_name VARCHAR(80) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    model_name VARCHAR(160) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
