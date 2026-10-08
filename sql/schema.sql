-- ============================================================
-- Pharmacy Management System - Database Schema (v2)
-- ============================================================
DROP DATABASE IF EXISTS pharmacy_management;
CREATE DATABASE pharmacy_management;
USE pharmacy_management;
CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(50) NOT NULL,
    role        VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'PHARMACIST'))
);
CREATE TABLE customers (
    customer_id     INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    phone           VARCHAR(15) NOT NULL,
    email           VARCHAR(100),
    address         VARCHAR(255),
    date_registered   DATE DEFAULT(CURRENT_DATE)
);
CREATE TABLE medicines (
    medicine_id             INT PRIMARY KEY,
    medicine_name           VARCHAR(100) NOT NULL,
    category                VARCHAR(50),
    price                   DOUBLE NOT NULL,
    stock_quantity          INT NOT NULL,
    expiry_date             DATE NOT NULL,
    prescription_required   BOOLEAN NOT NULL
);
CREATE TABLE prescriptions (
    prescription_id   INT AUTO_INCREMENT PRIMARY KEY,
    customer_id       INT NOT NULL,
    prescription_date DATETIME NOT NULL,
    CONSTRAINT fk_prescription_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);
CREATE TABLE prescription_medicines (
    prescription_medicine_id INT AUTO_INCREMENT PRIMARY KEY,
    prescription_id          INT NOT NULL,
    medicine_id              INT NOT NULL,
    quantity                 INT NOT NULL,
    unit_price                DECIMAL(10,2) NOT NULL,
    total_price               DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_pm_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id),
    CONSTRAINT fk_pm_medicine
        FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
);
CREATE TABLE bills (
    bill_id          INT AUTO_INCREMENT PRIMARY KEY,
    prescription_id  INT NULL,
    pharmacist_id    INT NOT NULL,
    bill_date        DATETIME DEFAULT CURRENT_TIMESTAMP,
    bill_amount      DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_bill_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id),
    CONSTRAINT fk_bill_pharmacist
        FOREIGN KEY (pharmacist_id) REFERENCES users(user_id)
);
CREATE TABLE bill_items (
    bill_item_id  INT AUTO_INCREMENT PRIMARY KEY,
    bill_id       INT NOT NULL,
    medicine_id   INT NOT NULL,
    quantity      INT NOT NULL,
    unit_price    DECIMAL(10,2) NOT NULL,
    total_price   DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_bi_bill
        FOREIGN KEY (bill_id) REFERENCES bills(bill_id),
    CONSTRAINT fk_bi_medicine
        FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
);
-- Users: 1 admin, 2 pharmacists
INSERT INTO users (username, password, role) VALUES
('admin1', 'admin123', 'ADMIN'),
('pharma1', 'pharma123', 'PHARMACIST'),
('pharma2', 'pharma456', 'PHARMACIST');

-- Customers: at least 3
INSERT INTO customers (name, phone, email, address, date_registered) VALUES
('Ravi Kumar', '9876543210', 'ravi.kumar@example.com', 'MG Road, Kochi', CURDATE()),
('Anjali Nair', '9123456780', 'anjali.nair@example.com', 'Panampilly Nagar, Kochi', CURDATE()),
('Suresh Menon', '9988776655', 'suresh.menon@example.com', 'Edappally, Kochi', CURDATE());

INSERT INTO medicines (medicine_id,medicine_name, category, price, stock_quantity, expiry_date, prescription_required) VALUES
(101,'Paracetamol 500mg', 'Analgesic', 20.00, 150, '2027-06-30', FALSE),
(102,'Amoxicillin 250mg', 'Antibiotic', 45.50, 8,   '2026-12-31', TRUE),
(103,'Cetirizine 10mg', 'Antihistamine', 15.00, 200, '2027-03-31', FALSE),
(104,'Ibuprofen 400mg', 'Analgesic', 25.00, 5,   '2026-10-15', FALSE),
(105,'Metformin 500mg', 'Antidiabetic', 30.00, 120, '2027-08-31', TRUE),
(106,'Omeprazole 20mg', 'Antacid', 40.00, 9,   '2026-10-20', FALSE),
(107,'Azithromycin 500mg', 'Antibiotic', 60.00, 60,  '2026-09-15', TRUE),
(108,'Vitamin C 500mg', 'Supplement', 18.00, 3,   '2027-01-31', FALSE),
(109,'Cough Syrup 100ml', 'Cough Relief', 55.00, 75,  '2026-12-15', FALSE),
(110,'Insulin Injection', 'Antidiabetic', 250.00, 40,  '2026-10-05', TRUE);


-- the expiry_date values if you run it much later.
