package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Appointment;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppointmentControl {

    // ===== Constructors =====
    public AppointmentControl(PatientControl patientControl, DoctorControl doctorControl) {
        loadAppointmentsFromFile();
        initTreatmentQueue();      // load queue file (respect empty), or build on first run
        rebuildCheckedInQueueOnly();
    }
    public AppointmentControl() {
        loadAppointmentsFromFile();
        initTreatmentQueue();
        rebuildCheckedInQueueOnly();
    }

    // ===== Storage =====
    private final ClinicADT<Appointment> all = new MyClinicADT<>();
    private final MyClinicADT<Integer>   queue = new MyClinicADT<>();          // CHECKED_IN order
    private final MyClinicADT<Integer>   treatmentQueue = new MyClinicADT<>(); // waiting for treatment (FIFO)
    private Integer calledId = null; // appointment currently CONSULTING

    // ===== Files =====
    private static final String FILE_PATH        = "src/textFile/appointments.txt";
    private static final String TREATMENT_Q_FILE = "src/textFile/treatment_queue.txt";
    private static final DateTimeFormatter FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ===== External accessors =====
    public ClinicADT<Appointment> getAll() { return all; }
    public Appointment getCalled() { return (calledId == null) ? null : getById(calledId); }

    public ClinicADT<Integer> getTreatmentQueueSnapshot() {
        MyClinicADT<Integer> snap = new MyClinicADT<>();
        MyClinicADT.MyIterator<Integer> it = treatmentQueue.iterator();
        while (it.hasNext()) snap.enqueue(it.next());
        return snap;
    }
    public int treatmentQueueSize() { try { return treatmentQueue.size(); } catch (Exception e) { return 0; } }
    public Integer peekNextTreatment() { try { return treatmentQueue.peek(); } catch (Exception e) { return null; } }
    public Integer dequeueNextTreatment() {
        try {
            Integer id = treatmentQueue.dequeue();
            persistQueues(); // reflect removal
            return id;
        } catch (Exception e) { return null; }
    }

    // ==================== Creation / Check-in ====================
    public Appointment addAppointment(
            String patientId, String patientName,
            String doctorId,  String doctorName,
            LocalDateTime when, Appointment.AppointmentType type
    ) {
        if (isDoctorBooked(doctorId, when)) {
            System.out.println("Selected slot already booked for Dr. " + doctorName + ".");
            return null;
        }

        removeConflictingAppointments(patientId, when);

        Appointment a = new Appointment(patientId, patientName, doctorId, doctorName, when, type);
        all.add(a);
        rebuildCheckedInQueueOnly();
        saveAppointmentsToFile();
        return a;
    }

    private void removeConflictingAppointments(String patientId, LocalDateTime when) {
        boolean removed = false;

        // collect matches first (don’t mutate while iterating)
        MyClinicADT<Appointment> toRemove = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) {
            Appointment existing = it.next();
            if (existing.getPatientId().equalsIgnoreCase(patientId)
                    && existing.getScheduledDateTime().equals(when)) {
                toRemove.add(existing);
            }
        }

        // remove them by index (object identity preserved → indexOf works)
        ClinicADT.MyIterator<Appointment> remIt = toRemove.iterator();
        while (remIt.hasNext()) {
            Appointment ex = remIt.next();
            int idx = all.indexOf(ex);
            if (idx >= 0) {
                int id = ex.getAppointmentId();
                all.remove(idx);
                try { queue.remove(id); } catch (Exception ignored) {}
                try { treatmentQueue.remove(id); } catch (Exception ignored) {}
                if (calledId != null && calledId == id) calledId = null;
                removed = true;
            }
        }

        if (removed) {
            rebuildCheckedInQueueOnly();
            persistQueues();
            saveAppointmentsToFile();
        }
    }

    public boolean checkIn(int appointmentId) {
        Appointment a = getById(appointmentId);
        if (a == null) return false;
        boolean ok = a.checkIn();
        if (ok) {
            rebuildCheckedInQueueOnly();
            saveAppointmentsToFile();
        }
        return ok;
    }

    public boolean callNext() {
        if (calledId != null) {
            System.out.println("A patient is already called (Appointment ID " + calledId + ").");
            return false;
        }
        if (queue.isEmpty()) {
            System.out.println("No checked-in patients in queue.");
            return false;
        }
        int id = queue.dequeue();
        Appointment a = getById(id);
        if (a == null) return false;

        if (!a.startConsultation()) return false; // sets CONSULTING
        calledId = id;

        System.out.println(">> CALLED: " + a.getPatientName() + " (" + a.getPatientId() + ")  "
                + a.getScheduledDateTime().format(FMT)
                + " with Dr. " + a.getDoctorName());
        System.out.println(">> STATUS: Changed to CONSULTING");

        saveAppointmentsToFile();
        return true;
    }

    // ================= Consultation completion (simplified) =================
    public boolean completeConsultation(String symptoms, boolean treatmentNeeded) {
        Appointment called = getCalled();
        if (called == null || called.getStatus() != Appointment.AppointmentStatus.CONSULTING) {
            return false;
        }

        // Use the entity’s own method so flags are set correctly
        called.completeConsultation(symptoms, "-", treatmentNeeded, false);

        if (treatmentNeeded) {
            enqueueTreatmentIfAbsent(called.getAppointmentId()); // prevent duplicates
            System.out.println("Status changed to TREATMENT. Patient sent to Treatment Waiting Queue.");
            persistQueues(); // write treatment_queue.txt now
        } else {
            System.out.println("No treatment required. Status changed to PENDING_PAYMENT.");
        }

        clearCalled();
        saveAppointmentsToFile();
        return true;
    }

    public boolean completeConsultation(String symptoms,
                                        String /*unused*/ diagnosis,
                                        boolean /*unused*/ treatmentPerformed,
                                        boolean treatmentNeeded) {
        return completeConsultation(symptoms, treatmentNeeded);
    }

    /** Called by Treatment module when a treatment session is finished. */
    public void finishTreatment(int apptId, boolean needsPharmacy) {
        Appointment a = getById(apptId);
        if (a == null) return;

        // advance status
        a.setStatus(Appointment.AppointmentStatus.PENDING_PAYMENT);

        // remove from FIFO and purge any other stale IDs
        try { treatmentQueue.remove(apptId); } catch (Exception ignored) {}
        purgeTreatmentQueueAgainstAppointments();

        // persist
        saveTreatmentQueueToFile();
        saveAppointmentsToFile();
    }

    /** Optional helper for Billing to finish the flow. */
    public boolean markCompleted(int appointmentId) {
        Appointment a = getById(appointmentId);
        if (a == null) return false;
        if (a.getStatus() != Appointment.AppointmentStatus.PENDING_PAYMENT) return false;
        a.completePayment();
        saveAppointmentsToFile();
        return true;
    }

    // =========================== Search / printing ===========================
    public Appointment peekNext() {
        if (queue.isEmpty()) return null;
        Integer id = queue.peek();
        return (id == null) ? null : getById(id);
    }

    public ClinicADT<Appointment> getOnlineAppointmentsPendingCheckIn() {
        ClinicADT<Appointment> out = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            if (a.getType() == Appointment.AppointmentType.ONLINE
                    && a.getStatus() == Appointment.AppointmentStatus.BOOKED) out.add(a);
        }
        return out;
    }

    public ClinicADT<Appointment> getConsultingAppointments() {
        ClinicADT<Appointment> out = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            if (a.getStatus() == Appointment.AppointmentStatus.CONSULTING) out.add(a);
        }
        return out;
    }

   // In AppointmentControl

    public ClinicADT<Appointment> searchAppointmentsByDoctor(String doctorId) {
        // 1) Copy into a working list (ADT only)
        ClinicADT<Appointment> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) sorted.add(it.next());

        // 2) Sort by doctorId (case-insensitive) using your ADT's sort
        ClinicADT.MyComparator<Appointment> comparator = new ClinicADT.MyComparator<Appointment>() {
            @Override 
            public int compare(Appointment a, Appointment b) {
                return a.getDoctorId().compareToIgnoreCase(b.getDoctorId());
            }
        };
        sorted.sort(comparator);

        // 3) Use ADT's search method to find any matching appointment
        // Create a dummy appointment for searching
        Appointment searchKey = new Appointment(null, null, doctorId, null, null, null);
        int foundIndex = ((MyClinicADT<Appointment>) sorted).search(searchKey, comparator);

        // If not found, return empty result
        if (foundIndex < 0) {
            return new MyClinicADT<>();
        }

        // 4) Expand left to find the first matching appointment
        int first = foundIndex;
        while (first > 0 && sorted.get(first - 1).getDoctorId().equalsIgnoreCase(doctorId)) {
            first--;
        }

        // 5) Expand right to find the last matching appointment
        int last = foundIndex;
        while (last < sorted.size() - 1 && sorted.get(last + 1).getDoctorId().equalsIgnoreCase(doctorId)) {
            last++;
        }

        // 6) Collect matches into a result ADT
        ClinicADT<Appointment> result = new MyClinicADT<>();
        for (int i = first; i <= last; i++) {
            result.add(sorted.get(i));
        }
        return result;
    }

    public ClinicADT<Appointment> searchAppointmentsByTimeSlot(LocalDateTime start, LocalDateTime end) {
        // 1) Copy into a working list (ADT only)
        ClinicADT<Appointment> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) sorted.add(it.next());

        // 2) Sort by scheduledDateTime
        ClinicADT.MyComparator<Appointment> comparator = new ClinicADT.MyComparator<Appointment>() {
            @Override 
            public int compare(Appointment a, Appointment b) {
                return a.getScheduledDateTime().compareTo(b.getScheduledDateTime());
            }
        };
        sorted.sort(comparator);

        // Create a dummy appointment for the start time
        Appointment startKey = new Appointment(null, null, null, null, start, null);
        int startIndex = ((MyClinicADT<Appointment>) sorted).search(startKey, comparator);

        // If exact start not found, convert negative index to insertion point
        if (startIndex < 0) {
            startIndex = -(startIndex + 1);
        }

        // Create a dummy appointment for the end time
        Appointment endKey = new Appointment(null, null, null, null, end, null);
        int endIndex = ((MyClinicADT<Appointment>) sorted).search(endKey, comparator);

        // Handle end boundary
        if (endIndex < 0) {
            // End time not found exactly, get insertion point
            endIndex = -(endIndex + 1);
        } else {
            // End time found exactly, include it and expand right while equal
            while (endIndex < sorted.size() - 1 && 
                   sorted.get(endIndex + 1).getScheduledDateTime().equals(end)) {
                endIndex++;
            }
            endIndex++; // Make it exclusive for the loop
        }

        // 5) Ensure we don't include appointments after the end time
        while (startIndex < sorted.size() && 
               sorted.get(startIndex).getScheduledDateTime().isBefore(start)) {
            startIndex++;
        }

        // 6) Collect appointments within the time range
        ClinicADT<Appointment> result = new MyClinicADT<>();
        for (int i = startIndex; i < endIndex && i < sorted.size(); i++) {
            LocalDateTime apptTime = sorted.get(i).getScheduledDateTime();
            if (!apptTime.isBefore(start) && !apptTime.isAfter(end)) {
                result.add(sorted.get(i));
            }
        }
        return result;
    }

    // Alternative version that's more efficient for time slot search
    public ClinicADT<Appointment> searchAppointmentsByTimeSlotOptimized(LocalDateTime start, LocalDateTime end) {
        // 1) Copy and sort (same as before)
        ClinicADT<Appointment> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) sorted.add(it.next());

        ClinicADT.MyComparator<Appointment> comparator = new ClinicADT.MyComparator<Appointment>() {
            @Override 
            public int compare(Appointment a, Appointment b) {
                return a.getScheduledDateTime().compareTo(b.getScheduledDateTime());
            }
        };
        sorted.sort(comparator);

        // 2) Manual lower bound for start (more efficient than search + expand)
        int n = sorted.size();
        int lo = 0, hi = n;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            LocalDateTime t = sorted.get(mid).getScheduledDateTime();
            if (t.isBefore(start)) lo = mid + 1; 
            else hi = mid;
        }
        int first = lo;

        // 3) Manual upper bound for end
        lo = first; 
        hi = n;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            LocalDateTime t = sorted.get(mid).getScheduledDateTime();
            if (t.isAfter(end)) hi = mid; 
            else lo = mid + 1;
        }
        int lastExcl = lo;

        // 4) Collect matches into a result ADT
        ClinicADT<Appointment> result = new MyClinicADT<>();
        for (int i = first; i < lastExcl; i++) {
            result.add(sorted.get(i));
        }
        return result;
    }


    public void printQueue() {
        System.out.println("\n=== Checked-in Queue (nearest scheduled time first) ===");
        if (queue.isEmpty()) { System.out.println("(empty)"); return; }

        String line = "+----+--------+---------------------+----------------------+----------------------+";
        String head = "| No |   ID   | Scheduled Time      | Patient              | Doctor               |%n";
        String row  = "| %-2d | %-6d | %-19s | %-20s | %-20s |%n";

        System.out.println(line);
        System.out.printf(head);
        System.out.println(line);

        int pos = 1;
        MyClinicADT.MyIterator<Integer> it = queue.iterator();
        while (it.hasNext()) {
            Appointment a = getById(it.next());
            if (a == null) continue;
            System.out.printf(row, pos++,
                    a.getAppointmentId(),
                    a.getScheduledDateTime().format(FMT),
                    fit(a.getPatientName(),20),
                    fit(a.getDoctorName(),20));
        }
        System.out.println(line);
    }

    // ================================ Internals ================================
    public Appointment getById(int apptId) {
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            if (a.getAppointmentId() == apptId) return a;
        }
        return null;
    }

    public boolean isDoctorBooked(String doctorId, LocalDateTime start) {
        LocalDateTime newEnd = start.plusHours(1);
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            if (!a.getDoctorId().equalsIgnoreCase(doctorId)) continue;

            LocalDateTime s = a.getScheduledDateTime();
            LocalDateTime e = s.plusHours(1);

            if (start.isBefore(e) && newEnd.isAfter(s)) return true; // overlap
        }
        return false;
    }

    private void clearCalled() { calledId = null; }

    private String fit(String s, int w) {
        if (s == null) s = "";
        if (s.length() <= w) return s;
        return s.substring(0, Math.max(0, w - 1)) + "…";
    }
    private String safe(String s) {
        if (s == null) return "";
        return s.replace('\n',' ').replace('\r',' ').replace(',', ' ');
    }
    private int parseIntSafe(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception ignored) { return -1; } }

    // ================================ Persistence ================================
    private void saveAppointmentsToFile() {
        try {
            File file = new File(FILE_PATH);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

            try (FileWriter w = new FileWriter(file)) {
                ClinicADT.MyIterator<Appointment> it = all.iterator();
                while (it.hasNext()) {
                    Appointment a = it.next();
                    w.write(formatAppointment(a));
                    w.write("\n");
                }
            }
        } catch (IOException e) {
            System.out.println("Error writing to file: " + FILE_PATH + " - " + e.getMessage());
        }
    }

    private void loadAppointmentsFromFile() {
        all.clear();
        File file = new File(FILE_PATH);
        if (!file.exists() || !file.isFile()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String raw = line.trim();
                if (raw.isEmpty() || raw.startsWith("#")) continue;

                String[] parts = raw.split(",", -1);
                if (parts.length < 8) continue;

                int idx = 0;
                /* int id = */ parseIntSafe(parts[idx++]); // entity doesn't restore explicit id
                String patientId   = parts[idx++].trim();
                String patientName = parts[idx++].trim();
                String doctorId    = parts[idx++].trim();
                String doctorName  = parts[idx++].trim();
                LocalDateTime scheduled = LocalDateTime.parse(parts[idx++].trim(), FMT);
                Appointment.AppointmentType type =
                        Appointment.AppointmentType.valueOf(parts[idx++].trim());
                Appointment.AppointmentStatus status =
                        Appointment.AppointmentStatus.valueOf(parts[idx++].trim());

                Appointment a = new Appointment(patientId, patientName, doctorId, doctorName, scheduled, type);

                // Recreate status chain (do NOT touch treatmentQueue here)
                switch (status) {
                    case BOOKED -> { /* ok */ }
                    case CHECKED_IN -> {
                        if (a.getStatus() == Appointment.AppointmentStatus.BOOKED) a.checkIn();
                    }
                    case CONSULTING -> {
                        if (a.getStatus() == Appointment.AppointmentStatus.BOOKED) a.checkIn();
                        a.startConsultation();
                    }
                    case TREATMENT -> {
                        if (a.getStatus() == Appointment.AppointmentStatus.BOOKED) a.checkIn();
                        a.startConsultation();
                        a.completeConsultation("-", "-", true, false); // set to TREATMENT
                    }
                    case PENDING_PAYMENT -> {
                        if (a.getStatus() == Appointment.AppointmentStatus.BOOKED) a.checkIn();
                        a.startConsultation();
                        a.completeConsultation("-", "-", false, false);
                    }
                    case COMPLETED -> {
                        if (a.getStatus() == Appointment.AppointmentStatus.BOOKED) a.checkIn();
                        a.startConsultation();
                        a.completeConsultation("-", "-", false, false);
                        a.completePayment();
                    }
                    default -> { /* no-op */ }
                }
                all.add(a);
            }
        } catch (Exception e) {
            System.out.println("Error loading appointments: " + e.getMessage());
        }
    }

    private String formatAppointment(Appointment a) {
        return a.getAppointmentId() + "," +
               safe(a.getPatientId()) + "," + safe(a.getPatientName()) + "," +
               safe(a.getDoctorId()) + "," + safe(a.getDoctorName()) + "," +
               a.getScheduledDateTime().format(FMT) + "," +
               a.getType() + "," + a.getStatus();
    }

    // ===================== Treatment FIFO Persistence =====================

    /** Initialize treatment queue: if the queue file exists, respect it even if empty.
     *  Only rebuild from appointments when the file does not exist (first run). */
    private void initTreatmentQueue() {
        treatmentQueue.clear();

        File f = new File(TREATMENT_Q_FILE);
        if (f.exists() && f.isFile()) {
            // Respect existing file contents (even if it loads zero items)
            loadTreatmentQueueFromFile();   // ignore boolean return
            saveTreatmentQueueToFile();     // normalize formatting & purge
        } else {
            // First run: derive from appointments and create the file
            rebuildTreatmentQueueFromAppointments();
            saveTreatmentQueueToFile();
        }
    }

    /** Rebuild FIFO from appointments currently in TREATMENT (stable by time then id). */
    private void rebuildTreatmentQueueFromAppointments() {
        // Collect candidates using iterator
        MyClinicADT<Appointment> candidates = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            if (a.getStatus() == Appointment.AppointmentStatus.TREATMENT) {
                candidates.add(a);
            }
        }

        // Sort by scheduled time, then by id (same as original selection logic)
        candidates.sort((x, y) -> {
            int c = x.getScheduledDateTime().compareTo(y.getScheduledDateTime());
            if (c != 0) return c;
            return Integer.compare(x.getAppointmentId(), y.getAppointmentId());
        });

        // Rebuild FIFO in that order
        treatmentQueue.clear();
        ClinicADT.MyIterator<Appointment> it2 = candidates.iterator();
        while (it2.hasNext()) {
            treatmentQueue.enqueue(it2.next().getAppointmentId());
        }
    }

    /** Load queue from file; return true if the file existed and was parsed (even if 0 items). */
    private boolean loadTreatmentQueueFromFile() {
        File f = new File(TREATMENT_Q_FILE);
        if (!f.exists() || !f.isFile()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String raw = line.trim();
                if (raw.isEmpty() || raw.startsWith("#")) continue;

                // Format: id,patientId,patientName,doctorId,doctorName,yyyy-MM-dd HH:mm
                String[] a = raw.split(",", -1);
                if (a.length < 6) continue;
                int id = parseIntSafe(a[0]);
                if (id <= 0) continue;

                Appointment ap = getById(id);
                if (ap != null && ap.getStatus() == Appointment.AppointmentStatus.TREATMENT) {
                    enqueueTreatmentIfAbsent(id); // avoid duplicates
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading " + TREATMENT_Q_FILE + ": " + e.getMessage());
            return false;
        }
        return true; // File existed and was parsed, even if 0 items
    }

    /** Prevent duplicates in FIFO. */
    private void enqueueTreatmentIfAbsent(int apptId) {
        MyClinicADT.MyIterator<Integer> it = treatmentQueue.iterator();
        while (it.hasNext()) if (it.next() == apptId) return;
        treatmentQueue.enqueue(apptId);
    }

    /** Remove any IDs that are no longer in TREATMENT. */
    private void purgeTreatmentQueueAgainstAppointments() {
        MyClinicADT<Integer> cleaned = new MyClinicADT<>();
        MyClinicADT.MyIterator<Integer> it = treatmentQueue.iterator();
        while (it.hasNext()) {
            int id = it.next();
            Appointment a = getById(id);
            if (a != null && a.getStatus() == Appointment.AppointmentStatus.TREATMENT) {
                cleaned.enqueue(id);
            }
        }
        treatmentQueue.clear();
        MyClinicADT.MyIterator<Integer> it2 = cleaned.iterator();
        while (it2.hasNext()) treatmentQueue.enqueue(it2.next());
    }

    /** Overwrite queue file to reflect current FIFO exactly. */
    private void saveTreatmentQueueToFile() {
        // ensure the in-memory FIFO has no stale IDs
        purgeTreatmentQueueAgainstAppointments();

        try {
            File f = new File(TREATMENT_Q_FILE);
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();

            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                MyClinicADT.MyIterator<Integer> it = treatmentQueue.iterator();
                while (it.hasNext()) {
                    int id = it.next();
                    Appointment a = getById(id);
                    if (a == null) continue;
                    // only write those still in TREATMENT
                    if (a.getStatus() != Appointment.AppointmentStatus.TREATMENT) continue;

                    pw.printf("%d,%s,%s,%s,%s,%s%n",
                            a.getAppointmentId(),
                            safe(a.getPatientId()),
                            safe(a.getPatientName()),
                            safe(a.getDoctorId()),
                            safe(a.getDoctorName()),
                            a.getScheduledDateTime().format(FMT));
                }
            }
        } catch (IOException e) {
            System.out.println("Error writing to file: " + TREATMENT_Q_FILE + " - " + e.getMessage());
        }
    }

    /** Call whenever the treatmentQueue content changes. */
    private void persistQueues() { saveTreatmentQueueToFile(); }

    // ==================== Checked-in queue ====================
    private void rebuildCheckedInQueueOnly() {
        // Collect candidates (CHECKED_IN)
        MyClinicADT<Appointment> candidates = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            if (a.getStatus() == Appointment.AppointmentStatus.CHECKED_IN) {
                candidates.add(a);
            }
        }

        if (candidates.isEmpty()) {
            queue.clear();
            return;
        }

        // Sort by:
        // 1) scheduled time asc
        // 2) check-in time asc (nulls last)
        // 3) appointment id asc
        candidates.sort((x, y) -> {
            int c = x.getScheduledDateTime().compareTo(y.getScheduledDateTime());
            if (c != 0) return c;

            // nulls last for check-in
            if (x.getCheckInTime() == null && y.getCheckInTime() != null) return 1;
            if (x.getCheckInTime() != null && y.getCheckInTime() == null) return -1;
            if (x.getCheckInTime() != null && y.getCheckInTime() != null) {
                int cc = x.getCheckInTime().compareTo(y.getCheckInTime());
                if (cc != 0) return cc;
            }
            return Integer.compare(x.getAppointmentId(), y.getAppointmentId());
        });

        // Rebuild queue in that order
        queue.clear();
        ClinicADT.MyIterator<Appointment> it2 = candidates.iterator();
        while (it2.hasNext()) {
            queue.enqueue(it2.next().getAppointmentId());
        }
    }

    public void persistForExternalMutation() {
        saveAppointmentsToFile();
    }

    // === Begin a consultation for a specific appointment (used by UI handover) ===
    public boolean beginConsultation(int apptId) {
        Appointment a = getById(apptId);
        if (a == null) return false;

        if (calledId != null && !calledId.equals(apptId)) return false;

        if (a.getStatus() == Appointment.AppointmentStatus.BOOKED) a.checkIn();

        try { queue.remove(apptId); } catch (Exception ignored) {}

        if (a.getStatus() != Appointment.AppointmentStatus.CONSULTING) {
            if (!a.startConsultation()) return false;
        }

        calledId = apptId;
        saveAppointmentsToFile();
        return true;
    }
}
