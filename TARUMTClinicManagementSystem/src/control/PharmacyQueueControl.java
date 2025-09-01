package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Medicine;
import entity.MedicinePrescription;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PharmacyQueueControl
 * - READY and DISPENSED queues persisted under src/textFile/
 * - 11-field CSV schema:
 *   prescriptionId,patientId,patientName,appointmentId,medicineId,medicineName,quantity,dosage,instructions,unitPrice,status
 * - Accurate money math with BigDecimal (2dp).
 */
public class PharmacyQueueControl {
    private final ClinicADT<MedicinePrescription> readyQueue     = new MyClinicADT<>();
    private final ClinicADT<MedicinePrescription> dispensedList  = new MyClinicADT<>();

    private final String readyFile     = "src/textFile/ready_queue.txt";
    private final String dispensedFile = "src/textFile/dispensed_queue.txt";

    private static final RoundingMode RM = RoundingMode.HALF_UP;

    public PharmacyQueueControl() {
        initializeDataDirectory();
        loadQueues();
    }

    // ---------- Initialization ----------
    private void initializeDataDirectory() {
        ensureParentDir(readyFile);
        ensureParentDir(dispensedFile);
        try {
            Path dataDir = Paths.get("src/textFile");
            if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
        } catch (Exception e) {
            System.err.println("Error creating src/textFile directory: " + e.getMessage());
        }
    }

    // ---------- Queue operations ----------
    public void enqueuePrescription(MedicinePrescription p) {
        if (p == null) return;
        try {
            if (p.getStatus() != MedicinePrescription.PrescriptionStatus.READY) {
                p.setStatus(MedicinePrescription.PrescriptionStatus.READY);
            }
        } catch (Throwable ignored) {}
        readyQueue.enqueue(p);
        saveQueues();
    }

    public MedicinePrescription enqueue(String patientId, String patientName, int appointmentId,
                                        String medicineId, String medicineName, int quantity,
                                        String dosage, String instructions, double unitPriceAtPrepare) {
        MedicinePrescription p = new MedicinePrescription(
                patientId, patientName, appointmentId,
                medicineId, medicineName, quantity,
                dosage, instructions, unitPriceAtPrepare
        );
        try { p.setStatus(MedicinePrescription.PrescriptionStatus.READY); } catch (Throwable ignored) {}
        enqueuePrescription(p);
        return p;
    }

    public MedicinePrescription peekFront() {
        try { return readyQueue.peek(); } catch (Exception e) { return null; }
    }

    /** Move the front READY item to DISPENSED (stock is decreased). */
    public boolean dispenseFront(PharmacyControl stockControl) {
        if (readyQueue.isEmpty()) {
            System.out.println("Ready queue is empty - nothing to dispense");
            return false;
        }

        MedicinePrescription front = readyQueue.dequeue();
        if (front == null) return false;

        Medicine med = stockControl.getMedicineById(front.getMedicineId());
        if (med == null) {
            readyQueue.enqueue(front);
            saveQueues();
            System.out.println("Medicine not found in stock: " + front.getMedicineId());
            return false;
        }

        boolean ok = stockControl.dispenseMedicineById(front.getMedicineId(), front.getQuantity());
        if (!ok) {
            // Put back, persist, and fail
            readyQueue.enqueue(front);
            saveQueues();
            System.out.println("Unable to dispense (insufficient stock or validation failed) for: " + front.getMedicineId());
            return false;
        }

        try { front.dispense(); } catch (Throwable ignored) {}
        try { front.setStatus(MedicinePrescription.PrescriptionStatus.DISPENSED); } catch (Throwable ignored) {}
        dispensedList.enqueue(front);
        saveQueues();

        System.out.printf("%d units of %s dispensed to %s (%s).%n",
                front.getQuantity(), front.getMedicineName(),
                front.getPatientName(), front.getPatientId());
        return true;
    }

    // ---------- Helpers to avoid “stale” duplicates ----------
    /** Remove ALL READY & DISPENSED lines for an appointment (use with care). */
    public void clearAllMedicineForAppointment(int apptId) {
        for (int i = readyQueue.size() - 1; i >= 0; i--) {
            if (readyQueue.get(i).getAppointmentId() == apptId) readyQueue.remove(i);
        }
        for (int i = dispensedList.size() - 1; i >= 0; i--) {
            if (dispensedList.get(i).getAppointmentId() == apptId) dispensedList.remove(i);
        }
        saveQueues();
    }

    public void replaceAllMedicineForAppointment(int apptId, ClinicADT<MedicinePrescription> newLines) {
        clearAllMedicineForAppointment(apptId);
        ClinicADT.MyIterator<MedicinePrescription> it = newLines.iterator();
        while (it.hasNext()) {
            MedicinePrescription p = it.next();
            try { p.setStatus(MedicinePrescription.PrescriptionStatus.READY); } catch (Throwable ignored) {}
            readyQueue.enqueue(p);
        }
        saveQueues();
    }

    // ---------- Reporting / printing ----------
    public void printReadyQueue() {
        System.out.println("\n=== Pharmacy READY Queue ===");
        if (readyQueue.isEmpty()) { System.out.println("(empty)"); return; }

        String line = "+----+----------------------+-----------+--------+------------------------------+-----+";
        String hdr  = "| No | Patient             | PatientID | Med.ID | Medicine                      | Qty |";
        String fmt  = "| %-2d | %-20s | %-9s | %-6s | %-28s | %-3d |";

        System.out.println(line);
        System.out.println(hdr);
        System.out.println(line);

        int i = 1;
        ClinicADT.MyIterator<MedicinePrescription> it = readyQueue.iterator();
        while (it.hasNext()) {
            MedicinePrescription p = it.next();
            System.out.println(String.format(fmt, i++,
                    cutAscii(p.getPatientName(), 20),
                    p.getPatientId(),
                    p.getMedicineId(),
                    cutAscii(p.getMedicineName(), 28),
                    p.getQuantity()));
        }
        System.out.println(line);
    }

    public void printDispensedHistory() {
        System.out.println("\n=== Pharmacy Dispensed History ===");
        if (dispensedList.isEmpty()) { System.out.println("(none)"); return; }

        String line = "+------+--------------+------------+----------------------+----------+-------------------------------+-----+";
        String hdr  = "| %-4s | %-12s | %-10s | %-20s | %-8s | %-29s | %-3s |%n";
        String row  = "| %-4d | %-12d | %-10s | %-20s | %-8s | %-29s | %-3d |%n";

        System.out.println(line);
        System.out.printf(hdr, "No", "Prescription", "PatientID", "Patient Name", "Med ID", "Medicine Name", "Qty");
        System.out.println(line);

        int i = 1;
        ClinicADT.MyIterator<MedicinePrescription> it = dispensedList.iterator();
        while (it.hasNext()) {
            MedicinePrescription p = it.next();
            int pid;
            try { pid = p.getPrescriptionId(); } catch (Throwable t) { pid = i; }
            System.out.printf(row, i++,
                    pid,
                    p.getPatientId(),
                    cutAscii(p.getPatientName(), 20),
                    p.getMedicineId(),
                    cutAscii(p.getMedicineName(), 29),
                    p.getQuantity());
        }
        System.out.println(line);
    }

    /**
     * Medicine total for an appointment, counting ONLY billable statuses:
     * READY (prepared/ready to pay) and DISPENSED.
     * Uses BigDecimal to avoid floating-point drift and rounds to 2 dp.
     */
    public double getTotalMedicineFeeForAppointment(int appointmentId, PharmacyControl pharmacy) {
        BigDecimal total = BigDecimal.ZERO;

        // READY
        ClinicADT.MyIterator<MedicinePrescription> rit = readyQueue.iterator();
        while (rit.hasNext()) {
            MedicinePrescription p = rit.next();
            if (p.getAppointmentId() != appointmentId) continue;
            try {
                if (p.getStatus() != MedicinePrescription.PrescriptionStatus.READY) continue;
            } catch (Throwable ignored) { /* older lines: READY file implies billable */ }
            total = total.add(priceForLine(p, pharmacy));
        }

        // DISPENSED
        ClinicADT.MyIterator<MedicinePrescription> dit = dispensedList.iterator();
        while (dit.hasNext()) {
            MedicinePrescription p = dit.next();
            if (p.getAppointmentId() != appointmentId) continue;
            try {
                if (p.getStatus() != MedicinePrescription.PrescriptionStatus.DISPENSED) continue;
            } catch (Throwable ignored) { /* dispensed file implies billable */ }
            total = total.add(priceForLine(p, pharmacy));
        }

        return total.setScale(2, RM).doubleValue();
    }

    /** Text breakdown for the cashier/receipt UI (READY + DISPENSED only). */
    public String getMedicineBreakdownForAppointment(int appointmentId) {
        StringBuilder sb = new StringBuilder();

        ClinicADT.MyIterator<MedicinePrescription> rit = readyQueue.iterator();
        while (rit.hasNext()) {
            MedicinePrescription p = rit.next();
            if (p.getAppointmentId() == appointmentId) {
                sb.append(String.format("  - %s x%d (READY)%n", p.getMedicineName(), p.getQuantity()));
            }
        }

        ClinicADT.MyIterator<MedicinePrescription> dit = dispensedList.iterator();
        while (dit.hasNext()) {
            MedicinePrescription p = dit.next();
            if (p.getAppointmentId() == appointmentId) {
                sb.append(String.format("  - %s x%d (DISPENSED)%n", p.getMedicineName(), p.getQuantity()));
            }
        }

        return sb.length() == 0 ? "  (no finalized medicine lines)\n" : sb.toString();
    }

    // ---------- Persistence ----------
    private static void ensureParentDir(String path) {
        try {
            File f = new File(path);
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
        } catch (Exception ignored) {}
    }

    private void saveQueues() {
        ensureParentDir(readyFile);
        ensureParentDir(dispensedFile);
        saveQueueToFile(readyQueue, readyFile);
        saveQueueToFile(dispensedList, dispensedFile);
    }

    private static String esc(String s) {
        if (s == null) return "";
        // Strip commas and newlines to keep CSV single-line & parseable
        return s.replace(',', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    /** 11-field schema writer (UTF-8). Unit price forced to 2 dp with dot decimal. */
    private void saveQueueToFile(ClinicADT<MedicinePrescription> queue, String filePath) {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            ClinicADT.MyIterator<MedicinePrescription> it = queue.iterator();
            while (it.hasNext()) {
                MedicinePrescription p = it.next();

                int pid = 0;
                try { pid = p.getPrescriptionId(); } catch (Throwable ignored) {}

                String status = "READY";
                try { status = p.getStatus().name(); } catch (Throwable ignored) {}

                BigDecimal unit = bd(p.getUnitPriceAtPrepare());

                writer.printf("%d,%s,%s,%d,%s,%s,%d,%s,%s,%.2f,%s%n",
                        pid,
                        esc(p.getPatientId()),
                        esc(p.getPatientName()),
                        p.getAppointmentId(),
                        esc(p.getMedicineId()),
                        esc(p.getMedicineName()),
                        p.getQuantity(),
                        esc(p.getDosage()),
                        esc(p.getInstructions()),
                        unit.doubleValue(),
                        status
                );
            }
        } catch (IOException e) {
            System.err.println("Error saving queue to " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadQueues() {
        readyQueue.clear();
        dispensedList.clear();
        loadQueueFromFile(readyQueue, readyFile, MedicinePrescription.PrescriptionStatus.READY);
        loadQueueFromFile(dispensedList, dispensedFile, MedicinePrescription.PrescriptionStatus.DISPENSED);
    }

    private void loadQueueFromFile(ClinicADT<MedicinePrescription> queue, String filename,
                                   MedicinePrescription.PrescriptionStatus expectedStatus) {
        File file = new File(filename);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                MedicinePrescription p = tryFromCsvFirst(trimmed);
                if (p == null) p = manualParseLine(trimmed);
                if (p == null) continue;

                // Coerce/verify status
                try {
                    if (p.getStatus() != expectedStatus) {
                        p.setStatus(expectedStatus);
                    }
                } catch (Throwable ignored) {}

                queue.enqueue(p);
            }
        } catch (IOException e) {
            System.err.println("Error loading queue from " + filename + ": " + e.getMessage());
        }
    }

    private MedicinePrescription tryFromCsvFirst(String line) {
        try {
            return MedicinePrescription.fromCsv(line);
        } catch (Throwable t) {
            return null;
        }
    }

    private MedicinePrescription manualParseLine(String line) {
        String[] a = line.split(",", -1);
        try {
            if (a.length >= 11) {
                int prescriptionId = safeInt(a[0]);
                String patientId   = a[1].trim();
                String patientName = a[2].trim();
                int appointmentId  = safeInt(a[3]);
                String medId       = a[4].trim();
                String medName     = a[5].trim();
                int qty            = safeInt(a[6]);
                String dosage      = a[7].trim();
                String instr       = a[8].trim();
                double unitPrice   = safeMoney(a[9]);  // strict money parse
                String statusStr   = a[10].trim();

                MedicinePrescription p = new MedicinePrescription(
                        patientId, patientName, appointmentId,
                        medId, medName, qty, dosage, instr, unitPrice
                );
                try { p.setPrescriptionId(prescriptionId); } catch (Throwable ignored) {}
                try {
                    p.setStatus(MedicinePrescription.PrescriptionStatus.valueOf(statusStr));
                } catch (Throwable ignored) {}
                return p;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // ---------- Misc ----------
    private static String cutAscii(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        // ensure total length <= max using ASCII "..."
        if (max <= 3) return ".".repeat(Math.max(0, max));
        return s.substring(0, max - 3) + "...";
    }

    public int getReservedQtyForMedicine(String medId) {
        if (medId == null || medId.trim().isEmpty()) return 0;
        int sum = 0;
        ClinicADT.MyIterator<MedicinePrescription> it = getReadyQueue().iterator();
        while (it.hasNext()) {
            MedicinePrescription p = it.next();
            if (medId.equalsIgnoreCase(p.getMedicineId())) sum += p.getQuantity();
        }
        return sum;
    }

    public void reload() { loadQueues(); }

    public void printQueueStatus() {
        System.out.println("\n=== Queue Status ===");
        System.out.println("Ready queue size: " + readyQueue.size());
        System.out.println("Dispensed list size: " + dispensedList.size());
    }

    // ---------- Accessors ----------
    public ClinicADT<MedicinePrescription> getReadyQueue()    { return readyQueue; }
    public ClinicADT<MedicinePrescription> getDispensedList() { return dispensedList; }

    // ---------- Safe parse / money helpers ----------
    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    /** Accepts only digits and one dot; ignores stray chars (e.g., "RM 50.00", "50.00\r"). */
    private static double safeMoney(String s) {
        if (s == null) return 0.0;
        String cleaned = s.trim().replaceAll("[^0-9.]", "");
        int firstDot = cleaned.indexOf('.');
        if (firstDot >= 0) {
            // remove any extra dots after the first
            cleaned = cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "");
        }
        try {
            return new BigDecimal(cleaned.isEmpty() ? "0" : cleaned).setScale(2, RM).doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static BigDecimal bd(double v) {
        // Normalize to 2dp to avoid carrying inexact binary tails
        return BigDecimal.valueOf(v).setScale(2, RM);
    }

    // ---------- Price helper ----------
    private static BigDecimal priceForLine(MedicinePrescription p, PharmacyControl pharmacy) {
        BigDecimal captured = bd(p.getUnitPriceAtPrepare());
        if (captured.compareTo(BigDecimal.ZERO) > 0) {
            return captured.multiply(BigDecimal.valueOf(p.getQuantity()));
        }
        Medicine m = pharmacy.getMedicineById(p.getMedicineId());
        BigDecimal current = (m != null) ? bd(m.getPricePerUnit()) : BigDecimal.ZERO;
        return current.multiply(BigDecimal.valueOf(p.getQuantity()));
    }
}
