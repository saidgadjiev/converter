ALTER TABLE archive_queue
    ALTER COLUMN total_file_size TYPE BIGINT;

ALTER TABLE conversion_queue
    ALTER COLUMN size TYPE BIGINT;

ALTER TABLE unzip_queue
    ALTER COLUMN extract_file_size TYPE BIGINT;