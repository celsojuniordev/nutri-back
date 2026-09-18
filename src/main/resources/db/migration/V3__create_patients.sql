CREATE TABLE patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    sex VARCHAR(20) NOT NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(30) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    nutritionist_id BIGINT NOT NULL,
    CONSTRAINT fk_patients_nutritionist FOREIGN KEY (nutritionist_id) REFERENCES nutritionists (id)
);
