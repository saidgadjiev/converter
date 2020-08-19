ALTER TABLE file_queue RENAME TO conversion_queue;
ALTER TABLE file_report RENAME TO conversion_report;

ALTER TABLE conversion_queue RENAME COLUMN message_id TO reply_to_message_id;
