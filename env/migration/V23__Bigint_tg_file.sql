ALTER TABLE unzip_queue ALTER COLUMN file SET DATA TYPE TEXT;
ALTER TABLE rename_queue ALTER COLUMN file SET DATA TYPE TEXT;
ALTER TABLE rename_queue ALTER COLUMN thumb SET DATA TYPE text;
ALTER TABLE archive_queue ALTER COLUMN files SET DATA TYPE TEXT[];

ALTER TYPE tg_file ALTER ATTRIBUTE size SET DATA TYPE BIGINT;

ALTER TABLE unzip_queue ALTER COLUMN file SET DATA TYPE tg_file USING file::tg_file;
ALTER TABLE rename_queue ALTER COLUMN file SET DATA TYPE tg_file USING file::tg_file;
ALTER TABLE rename_queue ALTER COLUMN thumb SET DATA TYPE tg_file USING file::tg_file;
ALTER TABLE archive_queue ALTER COLUMN files SET DATA TYPE tg_file[] USING files::tg_file[];