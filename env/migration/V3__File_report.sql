CREATE TABLE IF NOT EXISTS file_report (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES tg_user(user_id) ON DELETE CASCADE,
    queue_item_id INT NOT NULL REFERENCES file_queue(id) ON DELETE CASCADE,
    UNIQUE (user_id, queue_item_id)
)