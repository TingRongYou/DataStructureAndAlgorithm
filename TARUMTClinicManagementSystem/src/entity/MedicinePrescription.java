package entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Medicine prescription entity for pharmacy queue
public class MedicinePrescription {
    private static int nextId = 1;

    private int prescriptionId;
    private String patientId;
    private String patientName;
    private int appointmentId;
    private String medicineId;
    private String medicineName;
    private int quantity;
    private String dosage;
    private String instructions;
    private PrescriptionStatus status;
    private LocalDateTime prescribedDateTime;
    private LocalDateTime dispensedDateTime;
    private double unitPriceAtPrepare; // captured unit price at prepare time

    public enum PrescriptionStatus {
        READY,      // In ready queue
        DISPENSED   // In dispensed queue
    }

    public MedicinePrescription(String patientId, String patientName, int appointmentId,
                                String medicineId, String medicineName, int quantity,
                                String dosage, String instructions, double unitPriceAtPrepare) {
        this.prescriptionId = nextId++;
        this.patientId = patientId;
        this.patientName = patientName;
        this.appointmentId = appointmentId;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.quantity = quantity;
        this.dosage = dosage;
        this.instructions = instructions;
        this.status = PrescriptionStatus.READY;
        this.prescribedDateTime = LocalDateTime.now();
        this.unitPriceAtPrepare = Math.max(0.0, unitPriceAtPrepare);
    }

    // Constructor used when loading from storage
    public MedicinePrescription(int prescriptionId, String patientId, String patientName, int appointmentId,
                                String medicineId, String medicineName, int quantity,
                                String dosage, String instructions,
                                PrescriptionStatus status,
                                LocalDateTime prescribedDateTime,
                                LocalDateTime dispensedDateTime,
                                double unitPriceAtPrepare) {
        this.prescriptionId = prescriptionId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.appointmentId = appointmentId;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.quantity = quantity;
        this.dosage = dosage;
        this.instructions = instructions;
        this.status = status;
        this.prescribedDateTime = prescribedDateTime;
        this.dispensedDateTime = dispensedDateTime;
        this.unitPriceAtPrepare = Math.max(0.0, unitPriceAtPrepare);

        if (prescriptionId >= nextId) nextId = prescriptionId + 1; // avoid duplicate IDs
    }

    public boolean dispense() {
        if (status == PrescriptionStatus.READY) {
            this.status = PrescriptionStatus.DISPENSED;
            this.dispensedDateTime = LocalDateTime.now();
            return true;
        }
        return false;
    }

    // --- Persistence (to CSV string) ---
    // Columns:
    // 0:id,1:patientId,2:patientName,3:appointmentId,4:medicineId,5:medicineName,6:qty,
    // 7:dosage,8:instructions,9:status,10:prescribedDT,11:dispensedDT,12:unitPriceAtPrepare
   public String toCsv() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.join(",",
                String.valueOf(prescriptionId),
                patientId,
                patientName,
                String.valueOf(appointmentId),
                medicineId,
                medicineName,
                String.valueOf(quantity),
                (dosage == null ? "" : dosage),
                (instructions == null ? "" : instructions),
                status.name(),
                prescribedDateTime.format(fmt),
                (dispensedDateTime == null ? "" : dispensedDateTime.format(fmt)),
                String.format(java.util.Locale.ROOT, "%.4f", unitPriceAtPrepare) // <— ADDED
        );
    }


   public static MedicinePrescription fromCsv(String line) {
    try {
        String[] p = line.split(",", -1);
        if (p.length < 11) return null;

        int id = Integer.parseInt(p[0].trim());
        String patientId = p[1].trim();
        String patientName = p[2].trim();
        int appointmentId = Integer.parseInt(p[3].trim());
        String medId = p[4].trim();
        String medName = p[5].trim();
        int qty = Integer.parseInt(p[6].trim());
        String dosage = p[7].trim();
        String instr = p[8].trim();
        PrescriptionStatus st = PrescriptionStatus.valueOf(p[9].trim());
        LocalDateTime presDT = LocalDateTime.parse(p[10].trim(),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime dispDT = (p.length > 11 && !p[11].trim().isEmpty())
                ? LocalDateTime.parse(p[11].trim(),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : null;

        double unitPrice = 0.0;
        if (p.length > 12 && !p[12].trim().isEmpty()) {
            try { unitPrice = Double.parseDouble(p[12].trim()); } catch (Exception ignored) {}
        }

        return new MedicinePrescription(id, patientId, patientName, appointmentId,
                medId, medName, qty, dosage, instr, st, presDT, dispDT, unitPrice);
    } catch (Exception e) {
        return null;
    }
}


    // Getters
    public int getPrescriptionId() { return prescriptionId; }
    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public int getAppointmentId() { return appointmentId; }
    public String getMedicineId() { return medicineId; }
    public String getMedicineName() { return medicineName; }
    public int getQuantity() { return quantity; }
    public String getDosage() { return dosage; }
    public String getInstructions() { return instructions; }
    public PrescriptionStatus getStatus() { return status; }
    public LocalDateTime getPrescribedDateTime() { return prescribedDateTime; }
    public LocalDateTime getDispensedDateTime() { return dispensedDateTime; }
    public double getUnitPriceAtPrepare() { return unitPriceAtPrepare; }
    
    // Setters
    public void setUnitPriceAtPrepare(double v) { this.unitPriceAtPrepare = Math.max(0.0, v); }
    public void setPrescriptionId(int id) {this.prescriptionId = id;}
    public void setStatus(PrescriptionStatus status) {this.status = status;}

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return String.format(
                "Prescription ID: %d | Patient: %s (%s) | Medicine: %s | Qty: %d | Status: %s | Prescribed: %s",
                prescriptionId, patientName, patientId, medicineName, quantity, status,
                prescribedDateTime.format(formatter));
    }
}
