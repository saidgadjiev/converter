CREATE TABLE IF NOT EXISTS unzip_queue (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    user_id INT NOT NULL,
    file tg_file,
    type VARCHAR(32),
    item_type INT NOT NULL,
    extract_file_id INT,
    extract_file_size INT,
    message_id INT,
    status INT DEFAULT 0 NOT NULL
);