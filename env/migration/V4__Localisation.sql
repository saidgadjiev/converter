ALTER TABLE tg_user ADD COLUMN locale VARCHAR(16);
UPDATE tg_user SET locale = 'ru';
ALTER TABLE tg_user ALTER COLUMN locale SET NOT NULL;