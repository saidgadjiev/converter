CREATE TYPE tg_file AS (
    file_id VARCHAR(512),
    mime_type VARCHAR(512),
    file_name text,
    size INT
    );

CREATE TABLE IF NOT EXISTS rename_queue
(
    id                  SERIAL PRIMARY KEY,
    created_at          TIMESTAMP             DEFAULT NOW() NOT NULL,
    user_id             INT          NOT NULL,
    file                tg_file      NOT NULL,
    new_file_name       VARCHAR(256) NOT NULL,
    reply_to_message_id INT,
    status              INT          NOT NULL DEFAULT 0
);