CREATE TABLE IF NOT EXISTS tg_user (
    user_id INT NOT NULL UNIQUE PRIMARY KEY,
    username VARCHAR(128) UNIQUE,
    created_at TIMESTAMP(0) DEFAULT now(),
    last_logged_in_at TIMESTAMP(0) DEFAULT now()
);
