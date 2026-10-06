CREATE TABLE operation_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    actor_role VARCHAR(16) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    response_status SMALLINT NOT NULL,
    outcome VARCHAR(8) NOT NULL,
    request_id VARCHAR(64),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_audit_actor_time (actor_id, created_at),
    INDEX idx_audit_created_at (created_at)
);

CREATE TABLE course_cache_invalidation (
    course_id BIGINT NOT NULL PRIMARY KEY,
    attempts INT NOT NULL DEFAULT 0,
    available_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_error VARCHAR(500),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_cache_invalidation_course FOREIGN KEY (course_id) REFERENCES course(id)
);
