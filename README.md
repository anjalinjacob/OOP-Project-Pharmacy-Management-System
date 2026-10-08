# Pharmacy Management System (Java Swing + JDBC + MySQL) - v2

A simple desktop Pharmacy Management System built for a 2nd-year BTech
project, focused on the **Pharmacist** role. Built with plain Java, Swing,
JDBC and MySQL — no Spring, Hibernate, JavaFX, or cloud services.

This version uses **one single window** (`PharmacistHomeFrame`) with a
header, a navigation bar, and a `CardLayout` content area, instead of
opening a new `JFrame` for every function.

---

## 1. Requirements

- JDK 11 or later
- MySQL Server 8.x (or compatible)
- MySQL Connector/J (JDBC driver) — download `mysql-connector-j-8.x.x.jar`
  from https://dev.mysql.com/downloads/connector/j/ and place it in `lib/`.

---

## 2. Database Setup

```
mysql -u root -p < sql/schema.sql
```

This creates the `pharmacy_management` database with 7 tables (`users`,
`customers`, `medicines`, `prescriptions`, `prescription_medicines`,
`bills`, `bill_items`) and sample data (1 admin, 2 pharmacists, 3
customers, 10 medicines with a mix of prescription-required / not,
low-stock, expired, and expiring-soon items).

Sample logins:
- Pharmacist: `pharma1` / `pharma123`
- Pharmacist: `pharma2` / `pharma456`
- Admin: `admin1` / `admin123` (admin module not implemented)

---

## 3. Configure & Run

1. Edit `src/database/DatabaseConnection.java` and set your MySQL password.
2. Compile and run:

```
javac -cp "lib/mysql-connector-j-8.x.x.jar" -d out $(find src -name "*.java")
java -cp "out:lib/mysql-connector-j-8.x.x.jar" Main
```

(Windows: use `;` instead of `:` in the classpath.)

---

## 4. Project Structure

```
PharmacyManagementSystem/
├── sql/schema.sql
├── lib/                              -- put mysql-connector-j jar here
├── src/
│   ├── model/
│   │   ├── User.java
│   │   ├── Customer.java             -- now has dateOfBirth, gender
│   │   ├── Medicine.java             -- manufacturer removed, prescriptionRequired added
│   │   ├── Prescription.java         -- pharmacistId & totalAmount removed
│   │   ├── PrescriptionMedicine.java -- reused for prescription_medicines AND bill_items
│   │   └── Bill.java                 -- NEW
│   ├── dao/
│   │   ├── UserDAO.java
│   │   ├── CustomerDAO.java
│   │   ├── MedicineDAO.java
│   │   ├── PrescriptionDAO.java      -- no longer does billing/stock
│   │   └── BillDAO.java              -- NEW: billing + stock update, both sale types
│   ├── gui/
│   │   ├── LoginFrame.java
│   │   ├── PharmacistHomeFrame.java  -- the ONE window: header + nav + CardLayout
│   │   ├── AddCustomerPanel.java     -- NEW (content panel, not a JFrame)
│   │   ├── UpdateCustomerPanel.java  -- NEW (search/select, then edit)
│   │   ├── DeleteCustomerPanel.java  -- NEW (search/select, then delete)
│   │   ├── SearchCustomerPanel.java  -- NEW
│   │   ├── IssueMedicinePanel.java   -- NEW: walk-in / no-prescription sale
│   │   ├── StockReminderPanel.java   -- NEW: low stock + expired + expiring soon
│   │   ├── PrescriptionPanel.java    -- full-screen workflow panel, nav bar hidden
│   │   └── BillingPanel.java         -- full-screen workflow panel, nav bar hidden
│   ├── database/DatabaseConnection.java
│   ├── session/Session.java
│   └── Main.java
└── README.md
```

---

## 5. How the Single-Window Navigation Works

`PharmacistHomeFrame` holds a top-level `CardLayout` with two cards:

- **HOME** — header + navigation bar + a content panel. Clicking a nav
  button (Add/Update/Delete/Search Customer, Issue Medicine, Stock
  Reminder) just swaps the panel shown inside the content area. The
  window itself, the header, and the nav bar never change.
- **WORKFLOW** — a full-width panel with no header/nav bar at all. The
  app switches to this card only when the pharmacist starts a
  prescription (`PrescriptionPanel`) or reaches billing (`BillingPanel`).
  Finishing or cancelling the workflow calls `returnToHome()`, which
  switches back to the HOME card.

This satisfies the requirement that only ONE JFrame exists for the whole
pharmacist session, with the nav bar hidden specifically during the
prescription → billing flow.

---

## 6. Database Schema Changes

| Table | Change |
|---|---|
| `customers` | `date_registered` replaced with `date_of_birth` and `gender` |
| `medicines` | `manufacturer` column removed; `prescription_required` (boolean) added |
| `prescriptions` | `pharmacist_id` and `total_amount` **removed** — a prescription now just records `customer_id` + `prescription_date` |
| `bills` | **New table.** One bill per sale, prescription-based or walk-in. `prescription_id` is `NULL` for a walk-in sale. `pharmacist_id` always comes from `Session.userId`. |
| `bill_items` | **New table.** Line items for every bill (replaces the old idea of billing straight off `prescription_medicines`). |

`prescription_medicines` is unchanged in structure — it still records what
was noted on a prescription — but it's now a pre-billing record, not the
source of the final invoice. `bill_items` is what the money is actually
charged against.

---

## 7. New / Changed Workflows

### Prescription sale (customer-linked)
```
Pharmacist Home (nav visible)
  → Add/Search Customer → PrescriptionPanel (nav hidden)
      → Save Prescription (writes prescriptions + prescription_medicines, no stock change)
      → BillingPanel (nav hidden) → Finalize Bill
            (writes bills + bill_items, reduces stock, all in one transaction)
      → back to Pharmacist Home (nav visible)
```

### Walk-in sale (no customer, no prescription)
```
Pharmacist Home → "Issue Medicine Without Prescription" (nav stays visible)
  → search medicines with prescription_required = false
  → build cart → Finalize Bill directly
        (writes bills with prescription_id = NULL, writes bill_items, reduces stock)
  → stays on the same screen, ready for the next walk-in customer
```

### Stock Refill Reminder
Now flags THREE conditions, not just low stock:
- `stock_quantity <= 10` → **LOW STOCK**
- `expiry_date < CURDATE()` → **EXPIRED**
- expiring within 30 days → **EXPIRING SOON**
- both low stock and expiring soon → **LOW STOCK + EXPIRING SOON**

---

## 8. A Note on One Inconsistency in the Spec

The change request's "Update Customer" section (§7) shows a form with
only Name/Phone/Email/Address editable, but its SQL example also sets
`date_of_birth` and `gender`. The authoritative table list (§22) confirms
`customers` has `date_of_birth`/`gender` and **no** `date_registered` at
all. I went with the §22 schema everywhere (Add Customer collects DOB +
gender instead of a registration date), and made DOB/gender editable in
Update Customer too, to match the UPDATE query you provided. If you'd
rather keep a `date_registered` column alongside DOB/gender, that's an
easy follow-up change.

---

## 9. JDBC & Validation Practices Kept From v1

- Every query uses `PreparedStatement`.
- `try-with-resources` for `Connection`/`PreparedStatement`/`ResultSet`.
- Stock is only ever reduced inside `BillDAO`, during Finalize Bill,
  using `UPDATE ... WHERE stock_quantity >= ?` so a race condition fails
  safely instead of going negative.
- Saving a prescription and finalizing a bill both run inside explicit
  JDBC transactions (`setAutoCommit(false)` + `commit()` / `rollback()`).
- All user-facing errors go through `JOptionPane` instead of raw stack traces.
