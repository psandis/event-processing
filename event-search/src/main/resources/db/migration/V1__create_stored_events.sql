CREATE TABLE stored_events (
    id               VARCHAR(255) PRIMARY KEY,
    type             VARCHAR(255) NOT NULL,
    source           VARCHAR(255) NOT NULL,
    payload          TEXT NOT NULL,
    metadata         TEXT,
    status           VARCHAR(20) NOT NULL,
    error_message    TEXT,
    received_at      TIMESTAMPTZ NOT NULL,
    processed_at     TIMESTAMPTZ,
    stored_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stored_events_type ON stored_events(type);
CREATE INDEX idx_stored_events_source ON stored_events(source);
CREATE INDEX idx_stored_events_status ON stored_events(status);
CREATE INDEX idx_stored_events_received_at ON stored_events(received_at);
CREATE INDEX idx_stored_events_stored_at ON stored_events(stored_at);
