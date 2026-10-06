ALTER TABLE class_schedule DROP CHECK chk_schedule_status;
ALTER TABLE class_schedule
    ADD CONSTRAINT chk_schedule_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED'));
