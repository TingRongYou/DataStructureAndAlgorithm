package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Appointment;
import entity.MedicalTreatment;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TreatmentControl {
    private final ClinicADT<MedicalTreatment> allTreatments;
    private final String treatmentFilePath = "src/textFile/treatments.txt";

    // ===== Waiting Queue (FIFO) =====
    public static class WaitCase {
        public int appointmentId;
        public String patientId, patientName;
        public String doctorId, doctorName;
        public LocalDateTime scheduledTime;

        @Override
        public String toString() {
            return String.format("#%d %s(%s) with Dr.%s[%s] @ %s",
                    appointmentId, patientName, patientId, doctorName, doctorId,
                    scheduledTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
    }

    private final ClinicADT<WaitCase> waiting = new MyClinicADT<>();
    private final String queueFilePath = "src/textFile/treatment_queue.txt";

    public TreatmentControl(ClinicADT<MedicalTreatment> treatments) {
        this.allTreatments = treatments;
        loadTreatmentsFromFile();
        loadQueue(); // restore FIFO cases on startup
    }

    // === Enqueue from Consultation ===
    public void enqueueWaitingTreatment(int appointmentId,
                                        String patientId, String patientName,
                                        String doctorId, String doctorName,
                                        LocalDateTime scheduledTime) {
        WaitCase w = new WaitCase();
        w.appointmentId = appointmentId;
        w.patientId = patientId;
        w.patientName = patientName;
        w.doctorId = doctorId;
        w.doctorName = doctorName;
        w.scheduledTime = scheduledTime;
        waiting.enqueue(w);
        saveQueue();
    }

    public boolean hasWaiting() { return !waiting.isEmpty(); }

    /** Single-line preview (for banners) */
    public String peekFrontForDisplay() {
        if (waiting.isEmpty()) return "(empty)";
        WaitCase w = waiting.peek();
        return w == null ? "(empty)" : w.toString();
    }

    /** Remove front case (FIFO) for processing */
    public WaitCase dequeueFront() {
        if (waiting.isEmpty()) return null;
        WaitCase w = waiting.dequeue();
        saveQueue();
        return w;
    }

    /** Pretty table of the queue*/
    public void listWaiting() {
        if (waiting.isEmpty()) {
            System.out.println("Treatment waiting queue is empty.");
            return;
        }
        String line = "+----+------------+----------------------+------------+----------------------+---------------------+";
        String fmt  = "| %-2s | %-10s | %-20s | %-10s | %-20s | %-19s |%n";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        System.out.println("\n=== Treatment Waiting Queue ===");
        System.out.println(line);
        System.out.printf(fmt, "No", "PatientID", "Patient Name", "Doctor ID", "Doctor Name", "Appt Time");
        System.out.println(line);

        int i = 1;
        ClinicADT.MyIterator<WaitCase> it = waiting.iterator();
        while (it.hasNext()) {
            WaitCase w = it.next();
            System.out.printf(fmt, i++, w.patientId,
                    cut(w.patientName, 20), w.doctorId, cut(w.doctorName, 20),
                    w.scheduledTime.format(dtf));
        }
        System.out.println(line);
    }

    // === Immediate treatment (no booking) ===
    public MedicalTreatment recordImmediateTreatment(String patientId,
                                                     String patientName,
                                                     String doctorId,
                                                     String diagnosis,
                                                     String prescription,
                                                     LocalDateTime dateTimeSameAsConsult) {
        MedicalTreatment t = new MedicalTreatment(
                patientId, patientName, doctorId,
                safe(diagnosis, "N/A"),
                safe(prescription, "Given during consult"),
                dateTimeSameAsConsult,
                true // completed immediately
        );
        allTreatments.add(t);
        saveAllToFile();
        return t;
    }

    // === From Appointment FIFO===
    public boolean startNextTreatment(AppointmentControl apptCtrl, ClinicADT<MedicalTreatment> treatments) {
        Integer apptId = apptCtrl.dequeueNextTreatment();
        if (apptId == null) {
            System.out.println("No patients in the Treatment queue.");
            return false;
        }
        Appointment a = apptCtrl.getById(apptId);
        if (a == null) {
            System.out.println("Appointment not found for dequeued ID: " + apptId);
            return false;
        }
        if (a.getStatus() != Appointment.AppointmentStatus.TREATMENT) {
            System.out.println("Dequeued appointment not in TREATMENT status; skipping.");
            return false;
        }

        MedicalTreatment t = new MedicalTreatment(
                a.getPatientId(), a.getPatientName(),
                a.getDoctorId(),
                "Treatment performed", "As prescribed",
                LocalDateTime.now(),
                true
        );
        treatments.add(t);
        saveAllToFile();

        // Move forward to PENDING_PAYMENT
        a.setStatus(Appointment.AppointmentStatus.PENDING_PAYMENT);
        apptCtrl.persistForExternalMutation(); // small saver (see note below)
        System.out.println(">> Treatment completed for " + a.getPatientName() + " (Appt #" + a.getAppointmentId() + ").");
        return true;
    }

    // For reports / lists
    public ClinicADT<MedicalTreatment> getTreatmentsByPatient(String patientId, boolean includeFuture) {
        ClinicADT<MedicalTreatment> result = new MyClinicADT<>();
        LocalDateTime now = LocalDateTime.now();
        ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment mt = it.next();
            if (mt.getPatientId().equalsIgnoreCase(patientId)) {
                if (includeFuture || mt.getTreatmentDateTime().isBefore(now)) result.add(mt);
            }
        }
        return result;
    }

    public ClinicADT<MedicalTreatment> getAll() { return allTreatments; }

    // ===== Persistence: Treatments =====
    private void saveAllToFile() {
        try (FileWriter fw = new FileWriter(treatmentFilePath)) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            ClinicADT.MyIterator<MedicalTreatment> it = allTreatments.iterator();
            while (it.hasNext()) {
                MedicalTreatment t = it.next();
                fw.write(String.format("%d,%s,%s,%s,%s,%s,%s,%b%n",
                        t.getTreatmentId(),
                        t.getPatientId(),
                        t.getPatientName(),
                        t.getDoctorId(),
                        safe(t.getDiagnosis(), "N/A"),
                        safe(t.getPrescription(), "N/A"),
                        t.getTreatmentDateTime().format(fmt),
                        t.isCompleted()));
            }
        } catch (IOException e) {
            System.err.println("Error saving treatments: " + e.getMessage());
        }
    }

    public void loadTreatmentsFromFile() {
        allTreatments.clear();
        File f = new File(treatmentFilePath);
        if (!f.exists()) return;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length < 8) continue;
                try {
                    int id = Integer.parseInt(p[0].trim());
                    String pid = p[1].trim();
                    String pname = p[2].trim();
                    String did = p[3].trim();
                    String diag = p[4].trim();
                    String prescr = p[5].trim();
                    LocalDateTime dt = LocalDateTime.parse(p[6].trim(), fmt);
                    boolean done = Boolean.parseBoolean(p[7].trim());
                    allTreatments.add(new MedicalTreatment(id, pid, pname, did, diag, prescr, dt, done));
                } catch (Exception ignore) {}
            }
        } catch (IOException e) {
            System.err.println("Error loading treatments: " + e.getMessage());
        }
    }

    // ===== Persistence: Queue =====
    private void saveQueue() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(queueFilePath))) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            ClinicADT.MyIterator<WaitCase> it = waiting.iterator();
            while (it.hasNext()) {
                WaitCase w = it.next();
                bw.write(String.join(",",
                        String.valueOf(w.appointmentId),
                        w.patientId,
                        safeCsv(w.patientName),
                        w.doctorId,
                        safeCsv(w.doctorName),
                        w.scheduledTime.format(fmt)));
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving treatment queue: " + e.getMessage());
        }
    }

    private void loadQueue() {
        waiting.clear();
        File f = new File(queueFilePath);
        if (!f.exists()) return;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length < 6) continue;
                try {
                    WaitCase w = new WaitCase();
                    w.appointmentId = Integer.parseInt(p[0].trim());
                    w.patientId = p[1].trim();
                    w.patientName = p[2].trim();
                    w.doctorId = p[3].trim();
                    w.doctorName = p[4].trim();
                    w.scheduledTime = LocalDateTime.parse(p[5].trim(), fmt);
                    waiting.add(w);
                } catch (Exception ignore) {}
            }
        } catch (IOException e) {
            System.err.println("Error loading treatment queue: " + e.getMessage());
        }
    }

    // ===== utils =====
    private static String safe(String s, String def) { return (s == null || s.isBlank()) ? def : s; }
    private static String cut(String s,int n){ if (s==null) return ""; return s.length()<=n?s:s.substring(0,Math.max(0,n-1))+"…"; }
    private static String safeCsv(String s){ return (s==null)?"":s.replace(",", " "); }

 
    public interface PersistHook { void run(); }
}
