CREATE TABLE attendance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    checked_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_schedule_student (schedule_id, student_id),
    KEY idx_attendance_student_checked (student_id, checked_at),
    CONSTRAINT fk_attendance_schedule FOREIGN KEY (schedule_id) REFERENCES class_schedule(id),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
    CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENT','LATE','ABSENT','LEAVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE assignment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    deadline DATETIME(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_assignment_class_status_deadline (class_id, status, deadline),
    KEY idx_assignment_teacher_created (teacher_id, created_at),
    CONSTRAINT fk_assignment_class FOREIGN KEY (class_id) REFERENCES edu_class(id),
    CONSTRAINT fk_assignment_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id),
    CONSTRAINT chk_assignment_status CHECK (status IN ('DRAFT','PUBLISHED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE assignment_submission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED',
    score DECIMAL(6,2) NULL,
    feedback TEXT NULL,
    submitted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    graded_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_submission_assignment_student (assignment_id, student_id),
    KEY idx_submission_student_status (student_id, status),
    CONSTRAINT fk_submission_assignment FOREIGN KEY (assignment_id) REFERENCES assignment(id),
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
    CONSTRAINT chk_submission_status CHECK (status IN ('SUBMITTED','GRADED')),
    CONSTRAINT chk_submission_score CHECK (score IS NULL OR (score >= 0 AND score <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    content VARCHAR(500) NOT NULL,
    read_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_source_user (user_id, source_type, source_id),
    KEY idx_notification_user_created (user_id, created_at),
    KEY idx_notification_user_unread (user_id, read_at, created_at),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
