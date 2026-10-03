CREATE TABLE class_student (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ENROLLED',
    enrolled_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_student (class_id, student_id),
    KEY idx_class_student_student (student_id, status, enrolled_at),
    CONSTRAINT fk_class_student_class FOREIGN KEY (class_id) REFERENCES edu_class(id),
    CONSTRAINT fk_class_student_user FOREIGN KEY (student_id) REFERENCES sys_user(id),
    CONSTRAINT chk_class_student_status CHECK (status IN ('ENROLLED', 'WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE enrollment_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    expire_at DATETIME(3) NOT NULL,
    paid_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollment_order_no (order_no),
    KEY idx_order_student_status (student_id, status, created_at, id),
    KEY idx_order_class_status (class_id, status, id),
    KEY idx_order_status_expire (status, expire_at, id),
    CONSTRAINT fk_enrollment_order_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
    CONSTRAINT fk_enrollment_order_class FOREIGN KEY (class_id) REFERENCES edu_class(id),
    CONSTRAINT chk_enrollment_order_amount CHECK (amount >= 0),
    CONSTRAINT chk_enrollment_order_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payment_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_no VARCHAR(48) NOT NULL,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
    paid_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_payment_order (order_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES enrollment_order(id),
    CONSTRAINT chk_payment_amount CHECK (amount >= 0),
    CONSTRAINT chk_payment_status CHECK (status = 'SUCCESS')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE message_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    dedupe_key VARCHAR(120) NOT NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    available_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_until DATETIME(3) NULL,
    published_at DATETIME(3) NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_dedupe_key (dedupe_key),
    KEY idx_outbox_claim (status, available_at, locked_until, id),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY', 'SENT')),
    CONSTRAINT chk_outbox_attempts CHECK (attempts >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE message_consume_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    consumer_name VARCHAR(80) NOT NULL,
    event_id BIGINT NOT NULL,
    consumed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_consume_consumer_event (consumer_name, event_id),
    CONSTRAINT fk_consume_outbox_event FOREIGN KEY (event_id) REFERENCES message_outbox(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
