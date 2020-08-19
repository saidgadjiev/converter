CREATE TABLE IF NOT EXISTS distribution (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES tg_user(user_id),
    message_ru TEXT,
    message_en TEXT
)