CREATE TABLE nutritionists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    company VARCHAR(255) NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL,
    google_subject VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_nutritionists_email UNIQUE (email),
    CONSTRAINT uk_nutritionists_google_subject UNIQUE (google_subject)
);
