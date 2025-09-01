package entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Payment {
    // ---- Fixed fees ----
    public static final double CONSULTATION_FEE = 100.0; // always applied
    public static final double TREATMENT_FEE    = 200.0; // optional

    // ---- Payment method ----
    public enum PaymentMethod {
        TNG("Touch 'n Go eWallet"),
        ONLINE_BANKING("Online Banking"),
        CASH("Cash"),
        CREDIT_CARD("Credit/Debit Card");

        private final String displayName;
        PaymentMethod(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // ---- Core fields ----
    private final String receiptId;
    /** Stored in "yyyy-MM-dd HH:mm:ss" for compatibility with file parsing */
    private final String dateTime;

    private final boolean hasTreatment;
    private final double medicineFee;

    private final PaymentMethod paymentMethod;
    private final double amountPaid;
    private final double change;

    // ---- Receipt META (new) ----
    private int    apptId = -1;      
    private String patientId = "";
    private String patientName = "";
    private String doctorId = "";
    private String doctorName = "";

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    /** Original constructor (no META). */
    public Payment(boolean hasTreatment, double medicineFee,
                   PaymentMethod paymentMethod, double amountPaid) {
        this.receiptId = generateId();
        this.dateTime  = nowHuman();
        this.hasTreatment   = hasTreatment;
        this.medicineFee    = Math.max(0.0, medicineFee);
        this.paymentMethod  = (paymentMethod != null) ? paymentMethod : PaymentMethod.CASH;
        this.amountPaid     = amountPaid;
        this.change         = Math.max(0.0, amountPaid - getTotal());
    }

    /** New: constructor including META fields. */
    public Payment(boolean hasTreatment, double medicineFee,
                   PaymentMethod paymentMethod, double amountPaid,
                   int apptId, String patientId, String patientName,
                   String doctorId, String doctorName) {
        this(hasTreatment, medicineFee, paymentMethod, amountPaid);
        this.apptId      = apptId;
        this.patientId   = safe(patientId);
        this.patientName = safe(patientName);
        this.doctorId    = safe(doctorId);
        this.doctorName  = safe(doctorName);
    }

    /** Backfill constructor (legacy support), no META. */
    public Payment(String receiptId, String dateTime,
                   boolean hasTreatment, double medicineFee,
                   PaymentMethod paymentMethod, double amountPaid) {
        this.receiptId = (receiptId != null) ? receiptId : generateId();
        this.dateTime  = (dateTime  != null) ? dateTime  : nowHuman();
        this.hasTreatment   = hasTreatment;
        this.medicineFee    = Math.max(0.0, medicineFee);
        this.paymentMethod  = (paymentMethod != null) ? paymentMethod : PaymentMethod.CASH;
        this.amountPaid     = amountPaid;
        this.change         = Math.max(0.0, amountPaid - getTotal());
    }

    /** Backfill constructor with META. */
    public Payment(String receiptId, String dateTime,
                   boolean hasTreatment, double medicineFee,
                   PaymentMethod paymentMethod, double amountPaid,
                   int apptId, String patientId, String patientName,
                   String doctorId, String doctorName) {
        this(receiptId, dateTime, hasTreatment, medicineFee, paymentMethod, amountPaid);
        this.apptId      = apptId;
        this.patientId   = safe(patientId);
        this.patientName = safe(patientName);
        this.doctorId    = safe(doctorId);
        this.doctorName  = safe(doctorName);
    }

    // ----------------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------------

    public String getReceiptId() { return receiptId; }
    public String getDateTime()  { return dateTime; } // "yyyy-MM-dd HH:mm:ss"
    public boolean hasTreatment() { return hasTreatment; }

    public double getConsultationFee() { return CONSULTATION_FEE; }
    public double getTreatmentFee()    { return hasTreatment ? TREATMENT_FEE : 0.0; }
    public double getMedicineFee()     { return medicineFee; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public double getAmountPaid() { return amountPaid; }
    public double getChange()     { return change; }

    public int getApptId()            { return apptId; }
    public String getPatientId()      { return patientId; }
    public String getPatientName()    { return patientName; }
    public String getDoctorId()       { return doctorId; }
    public String getDoctorName()     { return doctorName; }

    // Optional: lightweight setters if you want to inject META after creation
    public void setApptMeta(int apptId, String patientId, String patientName) {
        this.apptId = apptId;
        this.patientId = safe(patientId);
        this.patientName = safe(patientName);
    }
    public void setDoctorMeta(String doctorId, String doctorName) {
        this.doctorId = safe(doctorId);
        this.doctorName = safe(doctorName);
    }

    // ----------------------------------------------------------------------
    // Calculated amounts
    // ----------------------------------------------------------------------

    public double getTotal() {
        return getConsultationFee() + getTreatmentFee() + getMedicineFee();
    }

    // ----------------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------------

    private static String generateId() {
        return "R" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                                      .format(LocalDateTime.now());
    }

    private static String nowHuman() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                .format(LocalDateTime.now());
    }

    private static String safe(String s) { return (s == null) ? "" : s.trim(); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Consultation  : RM %.2f%n", getConsultationFee()));
        sb.append(String.format("Treatment     : %sRM %.2f%n", hasTreatment ? "" : "(skip) ", getTreatmentFee()));
        sb.append(String.format("Medicine      : RM %.2f%n", getMedicineFee()));
        sb.append("----------------------------------------\n");
        sb.append(String.format("TOTAL         : RM %.2f%n", getTotal()));
        sb.append(String.format("Payment Method: %s%n", paymentMethod.getDisplayName()));
        // Meta preview (optional)
        if (apptId > 0 || !patientId.isEmpty() || !patientName.isEmpty()
                || !doctorId.isEmpty() || !doctorName.isEmpty()) {
            sb.append(String.format("Appt/Patient : #%s / %s (%s)%n",
                    (apptId > 0 ? apptId : "-"),
                    patientName.isEmpty() ? "-" : patientName,
                    patientId.isEmpty() ? "-" : patientId));
            sb.append(String.format("Doctor       : %s (%s)%n",
                    doctorName.isEmpty() ? "-" : doctorName,
                    doctorId.isEmpty() ? "-" : doctorId));
        }
        return sb.toString();
    }
}
