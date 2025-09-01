package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Payment;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Payment control – builds payments, renders/prints receipts,
 * appends full receipt blocks (with a META line) to a single receipts.txt file
 */
public class PaymentControl {

    // ======================= File Path =======================
    private final String receiptsFilePath = "src/textFile/receipts.txt";

    // ======================= DTO for reports/search =======================
    public static class PaymentRecord {
        public String receiptId;
        public LocalDateTime dateTime;
        public int apptId;                 // -1 if unknown
        public String patientId;           // may be empty
        public String patientName;         // may be empty
        public Payment.PaymentMethod method;
        public double consultation;
        public double treatment;
        public double medicine;
        public double total;
    }

    // ======================= Public API =======================

    /** Build a payment and immediately append receipt block + META (with appt/patient info). */
    public Payment buildPayment(boolean hasTreatment, double medicineFee,
                                Payment.PaymentMethod paymentMethod, double amountPaid,
                                int apptId, String patientId, String patientName) {
        Payment payment = new Payment(hasTreatment, medicineFee, paymentMethod, amountPaid,
                                      apptId, patientId, patientName, "", "");
        appendReceiptBlock(payment, "TARUMTClinic", apptId, safeMeta(patientId), safeMeta(patientName));
        return payment;
    }

    /** Backward-compat: build and append without appt/patient meta (old callers). */
    public Payment buildPayment(boolean hasTreatment, double medicineFee,
                                Payment.PaymentMethod paymentMethod, double amountPaid) {
        Payment payment = new Payment(hasTreatment, medicineFee, paymentMethod, amountPaid);
        appendReceiptBlock(payment, "TARUMTClinic"); // no META line – supported by fallback parser
        return payment;
    }

    /** Backward-compat (not persisted until appendReceiptBlock is called). */
    public Payment buildPayment(boolean hasTreatment, double medicineFee) {
        return new Payment(hasTreatment, medicineFee, Payment.PaymentMethod.CASH, 0.0);
    }

    /** Simple helper to compute a medicine line cost. */
    public double lineCost(entity.Medicine med, int qty) {
        if (med == null || qty <= 0) return 0.0;
        return med.getPricePerUnit() * qty;
    }

    public Payment.PaymentMethod[] getAvailablePaymentMethods() {
        return Payment.PaymentMethod.values();
    }

    public Payment.PaymentMethod getPaymentMethodByChoice(int choice) {
        Payment.PaymentMethod[] m = getAvailablePaymentMethods();
        return (choice >= 1 && choice <= m.length) ? m[choice - 1] : null;
    }

    // ======================= Persistence (Append) =======================

    /**
     * Append a complete receipt block with META.
     * META CSV format (kept the same for compatibility):
     * META,<receiptId>,<isoDateTime>,<apptId>,<patientId>,<patientName>,<method>,<consultation>,<treatment>,<medicine>,<total>
     */
    public void appendReceiptBlock(Payment p, String clinicName, int apptId, String patientId, String patientName) {
        ensureParentDir(receiptsFilePath);
        try (PrintWriter out = new PrintWriter(new FileWriter(receiptsFilePath, true))) {
            // BEGIN marker
            out.printf("=== RECEIPT BEGIN [%s] ===%n", p.getReceiptId());
            // META line (machine-friendly)
            out.printf("META,%s,%s,%d,%s,%s,%s,%.2f,%.2f,%.2f,%.2f%n",
                    p.getReceiptId(),
                    p.getDateTime().replace(' ', 'T'), // getDateTime() is a String
                    apptId,
                    stripCsv(patientId),
                    stripCsv(patientName),
                    p.getPaymentMethod().name(),
                    p.getConsultationFee(),
                    p.getTreatmentFee(),
                    p.getMedicineFee(),
                    p.getTotal());

            // Pretty layout (human-friendly)
            out.print(renderReceiptLayout(p, clinicName));

            // END marker
            out.printf("=== RECEIPT END   [%s] ===%n%n", p.getReceiptId());
        } catch (IOException e) {
            System.out.println("Error appending receipt: " + e.getMessage());
        }
    }

    /** Old overload: append without META. Fallback parser still supports it. */
    public void appendReceiptBlock(Payment p, String clinicName) {
        ensureParentDir(receiptsFilePath);
        try (PrintWriter out = new PrintWriter(new FileWriter(receiptsFilePath, true))) {
            out.printf("=== RECEIPT BEGIN [%s] ===%n", p.getReceiptId());
            out.print(renderReceiptLayout(p, clinicName));
            out.printf("=== RECEIPT END   [%s] ===%n%n", p.getReceiptId());
        } catch (IOException e) {
            System.out.println("Error appending receipt: " + e.getMessage());
        }
    }

    // ======================= Renderer =======================

    /** Render the pretty, boxed receipt as a String (no BEGIN/META/END markers here). */
    public String renderReceiptLayout(Payment p, String clinicName) {
        final int inner = 46; // content width (between |                  |)
        final String top = "+" + "=".repeat(inner) + "+";
        final String hr  = "+" + "-".repeat(inner) + "+";

        StringBuilder sb = new StringBuilder();

        sb.append(top).append("\n");
        sb.append(boxCenter(clinicName, inner)).append("\n");
        sb.append(boxCenter("CLINIC RECEIPT", inner)).append("\n");
        sb.append(boxLeft("Receipt ID: " + p.getReceiptId(), inner)).append("\n");
        sb.append(boxLeft("Date/Time: " + p.getDateTime(), inner)).append("\n");

        // Show appointment/patient/doctor meta when available (falls back to "-")
        String apptStr   = (p.getApptId() > 0) ? String.valueOf(p.getApptId()) : "-";
        String patIdStr  = (p.getPatientId()  == null || p.getPatientId().isBlank())   ? "-" : p.getPatientId();
        String patNmStr  = (p.getPatientName()== null || p.getPatientName().isBlank()) ? "-" : p.getPatientName();
        String docIdStr  = (p.getDoctorId()   == null || p.getDoctorId().isBlank())    ? "-" : p.getDoctorId();
        String docNmStr  = (p.getDoctorName() == null || p.getDoctorName().isBlank())  ? "-" : p.getDoctorName();

        sb.append(hr).append("\n");
        sb.append(boxLeft("Appointment ID: " + apptStr, inner)).append("\n");
        sb.append(boxLeft("Patient ID    : " + patIdStr, inner)).append("\n");
        sb.append(boxLeft("Patient Name  : " + patNmStr, inner)).append("\n");
        sb.append(boxLeft("Doctor ID     : " + docIdStr, inner)).append("\n");
        sb.append(boxLeft("Doctor Name   : " + docNmStr, inner)).append("\n");
        sb.append(hr).append("\n");

        sb.append(moneyRow("Consultation", p.getConsultationFee(), inner)).append("\n");
        if (p.hasTreatment()) sb.append(moneyRow("Treatment", p.getTreatmentFee(), inner)).append("\n");
        if (p.getMedicineFee() > 0.0) sb.append(moneyRow("Medicine", p.getMedicineFee(), inner)).append("\n");

        sb.append(hr).append("\n");
        sb.append(moneyRow("TOTAL", p.getTotal(), inner)).append("\n");
        sb.append(hr).append("\n");
        sb.append(boxLeft("Payment Method: " + p.getPaymentMethod().getDisplayName(), inner)).append("\n");
        sb.append(boxLeft("Amount Paid: RM " + String.format("%.2f", p.getAmountPaid()), inner)).append("\n");
        if (p.getPaymentMethod() == Payment.PaymentMethod.CASH && p.getChange() > 0) {
            sb.append(boxLeft("Change: RM " + String.format("%.2f", p.getChange()), inner)).append("\n");
        }
        sb.append(top).append("\n");
        sb.append(boxCenter("THANK YOU & GET WELL SOON!", inner)).append("\n");
        sb.append(top).append("\n");
        return sb.toString();
    }

    /** Print a saved receipt block (pretty layout) by ID from the single file. */
    public boolean printSavedReceipt(String receiptId) {
        String block = loadReceiptBlock(receiptId);
        if (block == null) return false;
        System.out.println(block);
        return true;
    }

    /** Load the full receipt block (layout included) by ID from receipts.txt. */
    public String loadReceiptBlock(String receiptId) {
        File f = new File(receiptsFilePath);
        if (!f.exists()) return null;

        String begin = "=== RECEIPT BEGIN [" + receiptId + "] ===";
        String end   = "=== RECEIPT END   [" + receiptId + "] ===";

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = null;
            String ln;
            while ((ln = br.readLine()) != null) {
                if (ln.equals(begin)) {
                    sb = new StringBuilder();
                    sb.append(ln).append("\n");
                    continue;
                }
                if (sb != null) {
                    sb.append(ln).append("\n");
                    if (ln.equals(end)) return sb.toString();
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading receipt: " + e.getMessage());
        }
        return null;
    }

    // ======================= Loader for reports/search =======================

    /**
     * Read all receipts in receipts.txt and return summarized PaymentRecord(s).
     * Prefers META lines; falls back to parsing the pretty box when META is absent.
     */
    public ClinicADT<PaymentRecord> loadAllRecords() {
        MyClinicADT<PaymentRecord> out = new MyClinicADT<>();
        File f = new File(receiptsFilePath);
        if (!f.exists()) return out;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String ln;
            PaymentRecord current = null;
            boolean inBlock = false;
            boolean skipFallback = false; // if META was seen, skip the fallback block parse

            while ((ln = br.readLine()) != null) {
                String raw = ln;              // keep raw for header parsing
                ln = ln.trim();

                // META (fast path)
                if (ln.startsWith("META,")) {
                    PaymentRecord rec = parseMetaLine(ln);
                    if (rec != null) out.add(rec);
                    if (inBlock) skipFallback = true;
                    continue;
                }

                // Block begin
                if (ln.startsWith("=== RECEIPT BEGIN [")) {
                    inBlock = true;
                    skipFallback = false;
                    current = new PaymentRecord();
                    current.apptId = -1;

                    // extract receipt id from header between [ and ]
                    int l = raw.indexOf('[');
                    int r = raw.indexOf(']');
                    if (l >= 0 && r > l) {
                        current.receiptId = raw.substring(l + 1, r).trim();
                    }
                    continue;
                }

                // Block end
                if (ln.startsWith("=== RECEIPT END")) {
                    if (inBlock) {
                        if (!skipFallback && current != null) {
                            if (current.dateTime == null) current.dateTime = LocalDateTime.now();
                            if (current.method == null) current.method = Payment.PaymentMethod.CASH;
                            out.add(current);
                        }
                        current = null;
                        inBlock = false;
                        skipFallback = false;
                    }
                    continue;
                }

                // Fallback parse (pretty box) only if META not seen in this block
                if (inBlock && !skipFallback && current != null) {
                    if (ln.startsWith("| Receipt ID:")) {
                        current.receiptId = extractRightOfColon(ln);
                    } else if (ln.startsWith("| Date/Time:")) {
                        current.dateTime = parseIsoOrHuman(extractRightOfColon(ln));
                    } else if (ln.contains("Payment Method:")) {
                        String disp = extractRightOfColon(ln);
                        current.method = mapDisplayToMethod(disp);
                    } else if (ln.contains("| Consultation")) {
                        current.consultation = extractAmount(ln);
                    } else if (ln.contains("| Treatment")) {
                        current.treatment = extractAmount(ln);
                    } else if (ln.contains("| Medicine")) {
                        current.medicine = extractAmount(ln);
                    } else if (ln.contains("| TOTAL")) {
                        current.total = extractAmount(ln);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading receipts: " + e.getMessage());
        }
        return out;
    }

    // ======================= Parsing helpers =======================

    private static PaymentRecord parseMetaLine(String ln) {
        // META,<rid>,<isoDateTime>,<apptId>,<patientId>,<patientName>,<method>,<consult>,<treat>,<med>,<total>
        String[] p = ln.split(",", -1);
        if (p.length < 11) return null;
        try {
            PaymentRecord r = new PaymentRecord();
            int i = 1;
            r.receiptId    = p[i++].trim();
            r.dateTime     = parseIsoOrHuman(p[i++].trim());
            r.apptId       = parseIntSafe(p[i++].trim());
            r.patientId    = p[i++].trim();
            r.patientName  = p[i++].trim();
            r.method       = Payment.PaymentMethod.valueOf(p[i++].trim());
            r.consultation = parseDoubleSafe(p[i++].trim());
            r.treatment    = parseDoubleSafe(p[i++].trim());
            r.medicine     = parseDoubleSafe(p[i++].trim());
            r.total        = parseDoubleSafe(p[i++].trim());
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseIsoOrHuman(String s) {
        try { return LocalDateTime.parse(s); } catch (Exception ignored) {}
        try { return LocalDateTime.parse(s.replace(' ', 'T')); } catch (Exception ignored) {}
        try { return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); } catch (Exception ignored) {}
        try { return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); } catch (Exception ignored) {}
        return LocalDateTime.now();
    }

    private static String extractRightOfColon(String boxedLine) {
        // assumes "| Label: value ....|"
        int idx = boxedLine.indexOf(':');
        String s = (idx >= 0) ? boxedLine.substring(idx + 1) : boxedLine;
        s = s.replace('|',' ').trim();
        return s;
    }

    private static double extractAmount(String boxedMoneyRow) {
        // assumes "... RM  123.45 |"
        int idx = boxedMoneyRow.lastIndexOf("RM");
        if (idx < 0) return 0.0;
        String s = boxedMoneyRow.substring(idx + 2).replace("|","").trim();
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private static Payment.PaymentMethod mapDisplayToMethod(String display) {
        // Try exact enum name first, then by display name
        for (Payment.PaymentMethod m : Payment.PaymentMethod.values()) {
            if (m.name().equalsIgnoreCase(display)) return m;
            if (m.getDisplayName().equalsIgnoreCase(display)) return m;
        }
        return Payment.PaymentMethod.CASH;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; }
    }

    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    private static void ensureParentDir(String path) {
        try {
            File f = new File(path);
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
        } catch (Exception ignored) {}
    }

    private static String safeMeta(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static String stripCsv(String s) {
        if (s == null) return "";
        // strip commas/newlines from meta fields
        return s.replace(',', ' ').replace('\n',' ').replace('\r',' ').trim();
    }

    // ======================= Renderer helpers =======================

    private static String boxCenter(String text, int inner) {
        int pad = Math.max(0, (inner - text.length()) / 2);
        String line = " ".repeat(pad) + text;
        if (line.length() < inner) line = line + " ".repeat(inner - line.length());
        return "|" + line + "|";
    }

    private static String boxLeft(String text, int inner) {
        String t = text.length() > inner ? text.substring(0, inner) : text;
        t = t + " ".repeat(inner - t.length());
        return "|" + t + "|";
    }

    private static String moneyRow(String label, double amount, int inner) {
        String left = String.format("%-20s", label);
        String amt  = String.format("%10.2f", amount);
        String line = String.format("| %s RM %s |", left, amt);
        int padLen = inner + 2 - line.length();
        if (padLen > 0) line = line.substring(0, line.length() - 1) + " ".repeat(padLen) + "|";
        return line;
    }

    // ======================= Optional: doctor meta build =======================

    // set doctor meta before we append the receipt
    public Payment buildPaymentWithDoctor(boolean hasTreatment, double medicineFee,
                                          Payment.PaymentMethod method, double amountPaid,
                                          int apptId, String patientId, String patientName,
                                          String doctorId, String doctorName) {
        Payment p = new Payment(hasTreatment, medicineFee, method, amountPaid,
                                apptId, patientId, patientName, doctorId, doctorName);
        appendReceiptBlock(p, "TARUMTClinic", apptId, safeMeta(patientId), safeMeta(patientName));
        return p;
    }
}
