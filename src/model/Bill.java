package model;

import java.sql.Timestamp;

/**
 * Represents a row in the new "bills" table.
 * One common bill type covers both:
 *   - a prescription-based sale (prescriptionId is set)
 *   - a walk-in / counter sale with no prescription (prescriptionId is null)
 */
public class Bill {
    private int billId;
    private Integer prescriptionId; // null when there is no prescription
    private int pharmacistId;
    private double billAmount;
    private Timestamp billDate;

    public Bill() {
    }

    public Bill(int billId, Integer prescriptionId, int pharmacistId,
                double billAmount, Timestamp billDate) {
        this.billId = billId;
        this.prescriptionId = prescriptionId;
        this.pharmacistId = pharmacistId;
        this.billAmount = billAmount;
        this.billDate = billDate;
    }

    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public Integer getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Integer prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public int getPharmacistId() {
        return pharmacistId;
    }

    public void setPharmacistId(int pharmacistId) {
        this.pharmacistId = pharmacistId;
    }

    public double getBillAmount() {
        return billAmount;
    }

    public void setBillAmount(double billAmount) {
        this.billAmount = billAmount;
    }

    public Timestamp getBillDate() {
        return billDate;
    }

    public void setBillDate(Timestamp billDate) {
        this.billDate = billDate;
    }
}