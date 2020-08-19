ALTER TABLE tg_user ADD COLUMN original_locale VARCHAR(128);
UPDATE tg_user SET original_locale = locale;
ALTER TABLE tg_user ALTER COLUMN original_locale SET NOT NULL;