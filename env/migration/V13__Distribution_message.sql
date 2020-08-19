CREATE TABLE IF NOT EXISTS distribution_message (
    id SERIAL PRIMARY KEY,
    message_ru TEXT,
    message_en TEXT
);

ALTER TABLE distribution DROP COLUMN message_ru;
ALTER TABLE distribution DROP COLUMN message_en;
ALTER TABLE distribution ADD COLUMN message_id INT NOT NULL REFERENCES distribution_message(id);