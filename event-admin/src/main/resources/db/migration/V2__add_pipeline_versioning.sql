ALTER TABLE pipeline_definitions ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE pipeline_definitions ADD COLUMN state VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE pipeline_definitions DROP COLUMN IF EXISTS enabled;

CREATE INDEX idx_pipeline_state ON pipeline_definitions(state);
CREATE INDEX idx_pipeline_name_version ON pipeline_definitions(name, version);
