# Pharmacy Management System

## Project Overview

The **Pharmacy Management System** is a group project developed to
simplify common pharmacy operations through a desktop application. It
provides separate access for **Admin** and **Pharmacist** users and
supports medicine inventory management, customer records, prescriptions,
billing, and reports.

The application uses **Java Swing** for its graphical user interface and
**MySQL** for persistent data storage.

## Group Members

Anjalin Jacob

Ann Mathew

Fathima Rana P K

Sohini Bawali

Sumedha N

Sreelakshmi J M

## Key Features

### 1. Login and Role-Based Access

-   Login using a username and password.
-   Supports two roles: **Admin** and **Pharmacist**.
-   Displays the features available to the logged-in role.

### 2. Admin Features

#### User Management

-   Add new Admin and Pharmacist accounts.
-   Manage user login details.

#### Medicine Management

-   Add, update, delete, view, and search medicines.
-   View stock and expiry information.
-   Arrange or review medicines according to stock condition.

#### Billing Reports

-   View a summary of pharmacist count, total medicines, bill count, and
    total income.
-   View bills and filter them by pharmacist.
-   Review medicines sold and quantities sold.
-   View the number of bills handled by each pharmacist.

### 3. Pharmacist Features

#### Customer Management

-   Add customer details.
-   Search customers by ID, name, or phone number.
-   Update customer details.
-   Delete customers only when they have no prescription history.

#### Prescriptions and Billing

-   Create a prescription for a customer.
-   Search for medicines and enter quantities.
-   Finalise a bill for a prescription.
-   View a customer's previous prescriptions and create a new
    prescription.

#### Issue Medicine Without a Prescription

-   Bill medicines that do not require a prescription.
-   Record the sale without creating a prescription.

#### Stock Refill Reminder

-   Identify medicines with low stock.
-   Identify medicines that are approaching or have passed their expiry
    date.

### 4. Session Management

-   Keep track of the currently logged-in user for role-based
    operations.
-   Allow users to log out of the application.

## Technology Stack

-   **Language:** Java
-   **GUI:** Java Swing
-   **Database:** MySQL
-   **Database Connectivity:** JDBC with MySQL Connector/J
-   **IDE:** Visual Studio Code or another Java-compatible IDE

## Project Structure

The source code is organised into packages according to their
responsibilities.

``` text
lib/
├── mysql-connector-j-26.7.0
src/
├── dao/
│   ├── BillDAO.java
│   ├── BillingReportsDAO.java
│   ├── CustomerDAO.java
│   ├── MedicineDAO.java
│   ├── PrescriptionDAO.java
│   └── UserDAO.java
├── database/
│   └── DatabaseConnection.java
├── gui/
│   ├── AddCustomerPanel.java
│   ├── AdminHomeFrame.java
│   ├── BillingPanel.java
│   ├── BillingReportsPanel.java
│   ├── DeleteCustomerPanel.java
│   ├── IssueMedicinePanel.java
│   ├── LoginFrame.java
│   ├── ManageUsersPanel.java
│   ├── MedicineManagementPanel.java
│   ├── PharmacistHomeFrame.java
│   ├── PrescriptionPanel.java
│   ├── SearchCustomerPanel.java
│   ├── StockReminderPanel.java
│   └── UpdateCustomerPanel.java
├── model/
│   ├── Bill.java
│   ├── Customer.java
│   ├── Medicine.java
│   ├── Prescription.java
│   ├── PrescriptionMedicine.java
│   └── User.java
├── session/
│   └── Session.java
└── Main.java
```

**Package responsibilities**

-   **`dao/`** --- Data Access Objects that handle database operations
    for users, customers, medicines, prescriptions, bills, and reports.
-   **`database/`** --- Database connection and related configuration.
-   **`gui/`** --- Swing frames and panels used to display the
    application's screens and workflows.
-   **`model/`** --- Java classes representing the application's main
    data entities.
-   **`session/`** --- Holds information about the current user session.
-   **`Main.java`** --- Application entry point that launches the login
    screen.

## Database Design

The database is named `pharmacy_management`. The provided schema defines
these tables:
```text
  -----------------------------------------------------------------------
  Table                               Purpose
  ----------------------------------- -----------------------------------
  `users`                             Login accounts and roles for Admins
                                      and Pharmacists

  `customers`                         Customer contact and registration
                                      details

  `medicines`                         Medicine category, price, stock,
                                      expiry date, and prescription
                                      requirement

  `prescriptions`                     Prescription records associated
                                      with customers

  `prescription_medicines`            Medicines, quantities, and prices
                                      associated with prescriptions

  `bills`                             Bill amount, date, pharmacist, and
                                      optional prescription reference

  `bill_items`                        Medicines and quantities included
                                      in each bill

  -----------------------------------------------------------------------
```
The SQL schema and sample records are provided in `sql/schema.sql`
## Setup and Running

### Prerequisites

-   Java Development Kit (JDK) installed.
-   MySQL Server installed and running.
-   MySQL Connector/J added to the project's classpath.
-   A Java IDE or terminal for compiling and running the application.

### 1. Create the database

1.  Open MySQL Workbench or another MySQL client.
2.  Review the SQL schema file.
3.  Execute the schema to create the `pharmacy_management` database and
    its tables.

### 2. Configure the database connection

Open the database connection configuration in the `database/` folder and
set the JDBC URL, MySQL username, and password for your local
environment.

### 3. Compile and run

Compile the Java source files with the MySQL Connector/J JAR available
on the classpath, then run the `Main` class. The exact command depends
on the folder structure and location of the connector JAR in your local
project.

If you use Visual Studio Code, ensure that the JDK and Java extensions
are configured correctly and that the database connector is available to
the project.

## Sample Login Data

The schema contains these sample accounts:
```text
  Role         Username    Password
  ------------ ----------- -------------
  Admin        `admin1`    `admin123`

  Pharmacist   `pharma1`   `pharma123`

  Pharmacist   `pharma2`   `pharma456`
  ```

These are development/demo credentials only.

## Group Project

This application is developed as a group project for academic purposes.
It demonstrates the integration of Java GUI programming, database
connectivity, object-oriented design, and SQL-based data management in a
practical pharmacy workflow.

## Future Improvements

-   Store passwords using a secure password-hashing method.
-   Add stronger validation and clearer error messages.
-   Improve audit logging for inventory changes and billing operations.
-   Add automated tests for database and business-logic operations.
-   Add database backup and recovery procedures.
