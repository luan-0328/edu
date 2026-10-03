CREATE TABLE course (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_course_status_created (status, created_at, id),
    CONSTRAINT chk_course_price CHECK (price >= 0),
    CONSTRAINT chk_course_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE edu_class (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    capacity INT NOT NULL,
    reserved_count INT NOT NULL DEFAULT 0,
    enrolled_count INT NOT NULL DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_edu_class_course_status (course_id, status),
    KEY idx_edu_class_teacher_status (teacher_id, status),
    CONSTRAINT fk_edu_class_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_edu_class_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id),
    CONSTRAINT chk_edu_class_capacity CHECK (capacity > 0 AND reserved_count >= 0 AND enrolled_count >= 0 AND capacity >= reserved_count + enrolled_count),
    CONSTRAINT chk_edu_class_dates CHECK (start_date <= end_date),
    CONSTRAINT chk_edu_class_status CHECK (status IN ('DRAFT', 'ENROLLING', 'IN_PROGRESS', 'FINISHED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE classroom (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_classroom_name (name),
    KEY idx_classroom_status (status),
    CONSTRAINT chk_classroom_capacity CHECK (capacity > 0),
    CONSTRAINT chk_classroom_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE class_schedule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_schedule_teacher_time (teacher_id, status, start_time, end_time),
    KEY idx_schedule_room_time (classroom_id, status, start_time, end_time),
    KEY idx_schedule_class_time (class_id, status, start_time, end_time),
    CONSTRAINT fk_schedule_class FOREIGN KEY (class_id) REFERENCES edu_class(id),
    CONSTRAINT fk_schedule_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id),
    CONSTRAINT fk_schedule_classroom FOREIGN KEY (classroom_id) REFERENCES classroom(id),
    CONSTRAINT chk_schedule_time CHECK (start_time < end_time),
    CONSTRAINT chk_schedule_status CHECK (status IN ('SCHEDULED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
