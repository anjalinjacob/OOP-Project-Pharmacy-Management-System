package model;

import java.sql.Timestamp;

/**
 * Represents a row in the "prescriptions" table.
 *
 * NOTE: pharmacist_id and total_amount used to live here. They have
 * moved to the "bills" table instead: a prescription is just a record
 * of what was noted down for a customer, and the Bill is what records
 * who handled it and how much it came to.
 */
public class Prescription {
    private int prescriptionId;
    private int customerId;
    private Timestamp prescriptionDate;

    public Prescription() {
    }

    public Prescription(int prescriptionId, int customerId, Timestamp prescriptionDate) {
        this.prescriptionId = prescriptionId;
        this.customerId = customerId;
        this.prescriptionDate = prescriptionDate;
    }

    public int getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(int prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public Timestamp getPrescriptionDate() {
        return prescriptionDate;
    }

    public void setPrescriptionDate(Timestamp prescriptionDate) {
        this.prescriptionDate = prescriptionDate;
    }
}