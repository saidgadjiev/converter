CREATE TABLE IF NOT EXISTS file_queue (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES tg_user(user_id) ON DELETE RESTRICT,
    file_id TEXT NOT NULL,
    file_name VARCHAR(256),
    size INT NOT NULL,
    mime_type VARCHAR(256),
    message_id INT NOT NULL,
    created_at TIMESTAMP(0) DEFAULT NOW(),
    started_at TIMESTAMP(0),
    last_run_at TIMESTAMP(0),
    completed_at TIMESTAMP(0),
    format VARCHAR(32) NOT NULL,
    target_format VARCHAR(32) NOT NULL,
    status INT NOT NULL DEFAULT 0,
    exception TEXT
);