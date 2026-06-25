USE healthcare_db;

-- Alter users table to add patient_id and doctor_id relationships
ALTER TABLE users ADD COLUMN patient_id BIGINT UNIQUE NULL;
ALTER TABLE users ADD COLUMN doctor_id BIGINT UNIQUE NULL;

-- Add foreign key constraints to users table
ALTER TABLE users ADD CONSTRAINT fk_user_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE SET NULL;
ALTER TABLE users ADD CONSTRAINT fk_user_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE SET NULL;

-- Create consultations table
CREATE TABLE IF NOT EXISTS consultations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_id BIGINT NULL,
    diagnosis TEXT NULL,
    prescription TEXT NULL,
    notes TEXT NULL,
    created_date DATETIME NOT NULL,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
);

-- Create payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT NULL,
    amount DOUBLE NOT NULL,
    payment_date DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(30) NULL,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
);
