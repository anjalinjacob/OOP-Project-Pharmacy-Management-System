package model;

/**
 * A single medicine line (medicine + quantity + price) inside a
 * prescription or a bill.
 *
 * This one class is reused in three places to keep the project simple:
 *  1. As a temporary "cart line" while a prescription or a walk-in sale
 *     is being built in memory (before anything is saved).
 *  2. As a row loaded from / saved to "prescription_medicines".
 *  3. As a row loaded from / saved to "bill_items".
 * All three share the same columns (medicine_id, quantity, unit_price,
 * total_price), so one POJO is enough - no separate BillItem class.
 */
public class PrescriptionMedicine {
    private int lineId; // prescription_medicine_id OR bill_item_id, depending on context
    private int medicineId;
    private String medicineName; // transient - not a DB column, used only for display
    private int quantity;
    private double unitPrice;
    private double totalPrice;

    public PrescriptionMedicine() {
    }

    public PrescriptionMedicine(int medicineId, String medicineName, int quantity, double unitPrice) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = quantity * unitPrice;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}