package boundary;

import adt.ClinicADT;
import adt.MyClinicADT;
import control.*;
import entity.Appointment;
import entity.MedicalTreatment;
import entity.Patient;
import entity.Doctor;
import entity.DiagnosisCatalog;
import utility.Report;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class TreatmentUI {
    private final TreatmentControl control;
    private final DoctorControl doctorControl;
    private final PatientControl patientControl;

    // single sources of truth for queues and pharmacy
    private final AppointmentControl apptCtrl;
    private final PharmacyControl pharmacy;
    private final PharmacyQueueControl pharmQueue;

    private final ClinicADT<MedicalTreatment> treatments;
    private final Scanner scanner;

    private static final int WIDTH = 120;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TreatmentUI(PatientControl patientControl,
                       DoctorControl doctorControl,
                       ClinicADT<MedicalTreatment> treatments,
                       AppointmentControl apptCtrl,
                       PharmacyControl pharmacy,
                       PharmacyQueueControl pharmQueue) {
        this.patientControl = patientControl;
        this.doctorControl  = doctorControl;
        this.treatments     = treatments;

        this.control  = new TreatmentControl(treatments);
        this.apptCtrl = apptCtrl;
        this.pharmacy = pharmacy;
        this.pharmQueue = pharmQueue;

        this.scanner = new Scanner(System.in);
    }
    
    public void patientTreatmentMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("            PATIENT - TREATMENTS");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Add Immediate Treatment");
            System.out.println(" 2) View Treatment Waiting Queue");
            System.out.println(" 0) Back");
            int choice = getInt("Choice: ");
            switch (choice) {
                case 1 -> addImmediateTreatment();
                case 2 -> printApptTreatmentQueue();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    public void doctorTreatmentMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("             DOCTOR - TREATMENTS");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Process Treatment (front of FIFO)");
            System.out.println(" 2) View Treatment Waiting Queue");
            System.out.println(" 3) List All Treatments (Sorted)");
            System.out.println(" 4) View Patient Treatment History");
            System.out.println(" 5) Search Treatments");
            System.out.println(" 0) Back");
            int choice = getInt("Choice: ");
            switch (choice) {
                case 1 -> processFront();
                case 2 -> printApptTreatmentQueue();
                case 3 -> printAllTreatmentsSorted();
                case 4 -> viewPatientHistory();
                case 5 -> searchMenu();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    
    public void adminTreatmentMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("             ADMIN - TREATMENTS");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Generate Report");
            System.out.println(" 0) Back");
            int choice = getInt("Choice: ");
            switch (choice) {
                case 1 -> generateReport();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    
    public void run() {
        int choice;
        do {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("             TARUMT CLINIC MEDICAL TREATMENT MANAGEMENT");
            System.out.println("=".repeat(60));
            System.out.println("1. Add Immediate Treatment");
            System.out.println("2. Process Treatment (front of FIFO)");
            System.out.println("3. View Treatment Waiting Queue");
            System.out.println("4. List All Treatments (Sorted)");
            System.out.println("5. View Patient Treatment History");
            System.out.println("6. Generate Report");
            System.out.println("7. Search Treatments");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (Exception e) {
                choice = -1;
            }

            switch (choice) {
                case 1 -> addImmediateTreatment();
                case 2 -> processFront();
                case 3 -> printApptTreatmentQueue();
                case 4 -> printAllTreatmentsSorted();
                case 5 -> viewPatientHistory();
                case 6 -> generateReport();
                case 7 -> searchMenu();
                case 0 -> System.out.println("Returning to main menu...");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 0);
    }

    // =========================
    // Reports menu (5 items)
    // =========================
    private void generateReport() {
        int choice;
        do {
            System.out.println("\n==========================================");
            System.out.println("             Generate Report              ");
            System.out.println("==========================================");
            System.out.println("1. Treatment Analysis Report");
            System.out.println("2. Treatment Frequency Distribution Report");
            System.out.println("3. Treatments by Doctor");
            System.out.println("4. Treatments by Day of Week");
            System.out.println("5. Monthly Treatment Trend (This Year)");
            System.out.println("0. Back");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number.");
                choice = -1;
            }

            switch (choice) {
                case 1 -> analysisReport();
                case 2 -> frequencyDistributionReport();
                case 3 -> treatmentsByDoctorReport();
                case 4 -> treatmentsByDayOfWeekReport();
                case 5 -> monthlyTreatmentTrendReport();
                case 0 -> System.out.println("Returning...");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 0);
    }

    // ===== FIFO treatment flow with diagnosis menu + optional pharmacy =====
    private void processFront() {
        System.out.println("\n=== Process Treatment (Front of FIFO) ===");

        Integer nextId = apptCtrl.peekNextTreatment();
        if (nextId == null) {
            System.out.println("Queue is empty.");
            return;
        }

        Appointment a = apptCtrl.getById(nextId);
        if (a == null) {
            System.out.println("Invalid front item.");
            return;
        }

        String line = "+----------+------------+----------------------+----------------------+---------------------+";
        String hdr  = "| Appt ID  | Patient ID | Patient Name         | Doctor               | Scheduled Time      |";
        String row  = "| %-8d | %-10s | %-20s | %-20s | %-19s |";
        System.out.println(line);
        System.out.println(hdr);
        System.out.println(line);
        System.out.println(String.format(row,
                a.getAppointmentId(), a.getPatientId(),
                cut(a.getPatientName(),20), cut(a.getDoctorName(),20),
                a.getScheduledDateTime().format(DTF)));
        System.out.println(line);

        System.out.print("Process this case now? (y/n): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("y")) return;

        final String[] dxMenu = {
            "Common Cold", "Influenza", "Hypertension", "Diabetes",
            "Allergies", "Asthma", "Gastroenteritis", "Migraine", "COVID-19 suspected", "Other"
        };
        System.out.println("\nSelect Diagnosis:");
        for (int i=0;i<dxMenu.length;i++) System.out.printf("%2d. %s%n", i+1, dxMenu[i]);

        int sel;
        while (true) {
            sel = getInt("Choice (1-"+dxMenu.length+"): ");
            if (sel>=1 && sel<=dxMenu.length) break;
            System.out.println("Invalid choice. Please select 1-" + dxMenu.length);
        }

        String diagnosis = (sel == dxMenu.length) ? prompt("Enter custom diagnosis: ") : dxMenu[sel-1];

        boolean needMed;
        while (true) {
            System.out.print("Need medicine? (y/n): ");
            String s = scanner.nextLine().trim().toLowerCase();
            if (s.equals("y")) { needMed = true; break; }
            if (s.equals("n")) { needMed = false; break; }
            System.out.println("Please enter 'y' or 'n'");
        }

        MedicalTreatment t = control.recordImmediateTreatment(
                a.getPatientId(), a.getPatientName(), a.getDoctorId(),
                diagnosis, (needMed? "With prescription":"No medicine"), LocalDateTime.now()
        );

        if (!needMed) {
            apptCtrl.finishTreatment(a.getAppointmentId(), false);
            System.out.println("No medicine required. Routed to PENDING PAYMENT.");
            printTreatmentConfirmRow(t);
            return;
        }

        // ---- Auto-pick medicine based on diagnosis (no manual type selection) ----
        DiagnosisCatalog.Suggestion sug = DiagnosisCatalog.suggest(diagnosis, pharmacy);
        if (sug != null) {
            handleSuggestedMedicine(a, sug);
        } else {
            System.out.println("No suitable medicine found in stock for this diagnosis. Switching to manual selection.");
            manualPickAndEnqueue(a, diagnosis);
        }

        apptCtrl.finishTreatment(a.getAppointmentId(), true);
        printTreatmentConfirmRow(t);
        System.out.println("Routed to PENDING PAYMENT (medicine cost included in billing).");
    }

    // Updated to take the concrete Suggestion type
    private void handleSuggestedMedicine(Appointment a, DiagnosisCatalog.Suggestion suggestion) {
        try {
            String medId  = suggestion.medId;
            String dosage = suggestion.dosage;
            String instr  = suggestion.instr;

            entity.Medicine med = pharmacy.getMedicineById(medId);

            String mLine = "+-------+----------------------+----------+----------+------------+--------------------------------------+";
            String mHdr  = "| MedID | Name                 | Stock    | Unit/Qty | Unit Price | Default Dosage/Instruction            |";
            String mRow  = "| %-5s | %-20s | %8d | %-8s | %10.2f | %-35s |";

            System.out.println("\nAuto-selected medicine (based on diagnosis):");
            System.out.println(mLine);
            System.out.println(mHdr);
            System.out.println(mLine);

            if (med == null || med.getQuantity() <= 0) {
                System.out.println("|  N/A  | (Suggested item unavailable in stock)                                            |");
                System.out.println(mLine);
                System.out.println("Suggested medicine not available. Switching to manual selection.\n");
                manualPickAndEnqueue(a, "");
                return;
            }

            System.out.println(String.format(mRow,
                    med.getId(), cut(med.getName(),20), med.getQuantity(), med.getUnit(),
                    med.getPricePerUnit(),
                    cut(dosage + " | " + instr,24)));
            System.out.println(mLine);

            // Ask only for quantity now
            int qty;
            while (true) {
                qty = getInt("Enter quantity to prepare: ");
                if (isValidQtyConsideringReserved(med, qty)) break;
            }

            double unitPriceAtPrepare = med.getPricePerUnit();

            var pres = pharmQueue.enqueue(
                    a.getPatientId(), a.getPatientName(), a.getAppointmentId(),
                    med.getId(), med.getName(), qty, dosage, instr, unitPriceAtPrepare
            );
            System.out.println("\nPrescription queued for preparation: " + pres);

        } catch (Exception e) {
            System.out.println("Error processing suggested medicine: " + e.getMessage());
            manualPickAndEnqueue(a, "");
        }
    }

    private void manualPickAndEnqueue(Appointment a, String diagnosis) {
        String sLine = "+-------+----------------------+----------+----------+------------+";
        String sHdr  = "| ID    | Name                 |   Stock  |  Unit    | Price(MYR) |";
        String sRow  = "| %-5s | %-20s | %8d | %-8s | %10.2f |";

        System.out.println("\nStock (pick an ID):");
        System.out.println(sLine);
        System.out.println(sHdr);
        System.out.println(sLine);

        ClinicADT.MyIterator<entity.Medicine> it = pharmacy.getAllMedicines().iterator();
        while (it.hasNext()) {
            var m = it.next();
            System.out.println(String.format(sRow, m.getId(), cut(m.getName(),20),
                    m.getQuantity(), m.getUnit(), m.getPricePerUnit()));
        }
        System.out.println(sLine);

        entity.Medicine med;
        while (true) {
            String medId = prompt("Medicine ID: ").toUpperCase();
            med = pharmacy.getMedicineById(medId);
            if (med != null) break;
            System.out.println("Invalid Medicine ID. Try again.");
        }

        int qty;
        while (true) {
            qty = getInt("Enter quantity to prepare: ");
            if (isValidQtyConsideringReserved(med, qty)) break;
        }

        String dosage = prompt("Dosage (free text): ");
        String instr  = prompt("Instruction: ");
        double unitPriceAtPrepare = med.getPricePerUnit();

        var pres = pharmQueue.enqueue(
                a.getPatientId(), a.getPatientName(), a.getAppointmentId(),
                med.getId(), med.getName(), qty, dosage, instr, unitPriceAtPrepare
        );
        System.out.println("\nPrescription queued for preparation: " + pres);
    }

    private boolean isValidQtyConsideringReserved(entity.Medicine med, int qty) {
        if (med == null) { System.out.println("Invalid medicine."); return false; }
        if (qty <= 0) { System.out.println("Quantity must be > 0."); return false; }

        int reserved = pharmQueue.getReservedQtyForMedicine(med.getId());
        int available = med.getQuantity() - reserved;
        if (available < 0) available = 0;

        if (qty > available) {
            System.out.printf("Cannot prepare %d unit(s). Available after reservations: %d (stock %d, reserved %d)%n",
                    qty, available, med.getQuantity(), reserved);
            return false;
        }
        return true;
    }

    private void printTreatmentConfirmRow(MedicalTreatment t) {
        String line   = "+--------------+------------+----------------------+------------+---------------------+-----------+";
        String hdrFmt = "| %-12s | %-10s | %-20s | %-10s | %-19s | %-9s |%n";
        String rowFmt = "| %-12s | %-10s | %-20s | %-10s | %-19s | %-9s |%n";

        System.out.println("\nTreatment Completed:");
        System.out.println(line);
        System.out.printf(hdrFmt, "Treatment ID", "PatientID", "Patient Name", "Doctor ID", "Date/Time", "Status");
        System.out.println(line);
        System.out.printf(rowFmt,
                String.valueOf(t.getTreatmentId()),
                t.getPatientId(),
                cut(t.getPatientName(), 20),
                t.getDoctorId(),
                t.getTreatmentDateTime().format(DTF),
                "Completed");
        System.out.println(line);
    }

    private void printApptTreatmentQueue() {
        System.out.println("\n=== Treatment Waiting Queue ===");
        if (apptCtrl.treatmentQueueSize() == 0) {
            System.out.println("(empty)");
            return;
        }

        String line = "+----+----------+----------------------+----------------------+---------------------+";
        String hdr  = "| No | Appt ID  | Patient              | Doctor               | Scheduled Time      |";
        String row  = "| %-2d | %-8d | %-20s | %-20s | %-19s |";

        System.out.println(line);
        System.out.println(hdr);
        System.out.println(line);

        int pos = 1;
        ClinicADT.MyIterator<Integer> it = apptCtrl.getTreatmentQueueSnapshot().iterator();
        while (it.hasNext()) {
            Integer id = it.next();
            Appointment a = apptCtrl.getById(id);
            if (a == null) continue;

            System.out.println(String.format(
                    row, pos++,
                    a.getAppointmentId(),
                    cut(a.getPatientName(), 20),
                    cut(a.getDoctorName(), 20),
                    a.getScheduledDateTime().format(DTF)
            ));
        }
        System.out.println(line);
    }

    private void addImmediateTreatment() {
        System.out.println("\n=== Add Immediate Treatment ===");
        Patient p = pickPatient();
        if (p == null) return;

        Doctor d = pickDoctor();
        if (d == null) return;

        String diagnosis   = prompt("Diagnosis: ");
        String prescription = prompt("Prescription: ");

        LocalDateTime now = LocalDateTime.now();
        MedicalTreatment t = control.recordImmediateTreatment(
                p.getId(), p.getName(), d.getId(), diagnosis, prescription, now
        );

        String line = "+--------------+------------+----------------------+------------+---------------------+-----------+";
        String fmt  = "| %-12s | %-10s | %-20s | %-10s | %-19s | %-9s |%n";
        System.out.println("\nTreatment Created:");
        System.out.println(line);
        System.out.printf(fmt, "Treatment ID", "PatientID", "Patient Name", "Doctor ID",
                "Date/Time", "Status");
        System.out.println(line);
        System.out.printf(fmt, String.valueOf(t.getTreatmentId()), t.getPatientId(),
                cut(t.getPatientName(), 20), t.getDoctorId(),
                now.format(DTF), "Completed");
        System.out.println(line);
        System.out.println("Fees: Consultation RM100 (if applicable) + Treatment RM200 (Billing).");
    }

    private void viewPatientHistory() {
        Patient p = pickPatient();
        if (p == null) return;

        ClinicADT<MedicalTreatment> hist = control.getTreatmentsByPatient(p.getId(), false);
        if (hist.isEmpty()) {
            System.out.println("No past treatments for " + p.getId());
            return;
        }

        final String line = "+--------------+------------+-----------------+------------+---------------------+-----------+";
        final String fmtH = "| %-12s | %-10s | %-15s | %-10s | %-19s | %-9s |%n";
        final String fmtR = "| %-12s | %-10s | %-15s | %-10s | %-19s | %-9s |%n";

        final int PAGE_SIZE = 10;
        int total = hist.size();
        int totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;

        Report.cprintln("====== Treatment History ======");

        Runnable printHeader = () -> {
            System.out.println(line);
            System.out.printf(fmtH, "Treatment ID", "Patient ID", "Patient Name", "Doctor ID", "Date", "Status");
            System.out.println(line);
        };

        int printed = 0;
        int page = 1;
        printHeader.run();

        ClinicADT.MyIterator<MedicalTreatment> it = hist.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            System.out.printf(fmtR,
                    String.valueOf(t.getTreatmentId()),
                    t.getPatientId(),
                    cut(t.getPatientName(), 15),
                    t.getDoctorId(),
                    t.getTreatmentDateTime().format(DTF),
                    t.isCompleted() ? "Completed" : "Pending");

            printed++;

            boolean endOfPage = (printed % PAGE_SIZE == 0);
            boolean endOfData = !it.hasNext();

            if (endOfPage && !endOfData) {
                System.out.println(line);
                System.out.printf("-- Page %d/%d -- Press Enter to continue --", page, totalPages);
                scanner.nextLine();
                page++;
                printHeader.run();
            }
        }

        System.out.println(line);
        System.out.printf("Total records: %d (Pages: %d, Page size: %d)%n", total, totalPages, PAGE_SIZE);
    }

    private void printAllTreatmentsSorted() {
        // make a sorted copy by date/time
        MyClinicADT<MedicalTreatment> copy = new MyClinicADT<>();
        ClinicADT.MyIterator<MedicalTreatment> it = treatments.iterator();
        while (it.hasNext()) copy.add(it.next());

        copy.sort(new ClinicADT.MyComparator<MedicalTreatment>() {
            @Override public int compare(MedicalTreatment t1, MedicalTreatment t2) {
                return t1.getTreatmentDateTime().compareTo(t2.getTreatmentDateTime());
            }
        });

        final int PAGE_SIZE = 10;
        final int total = copy.size();
        final int totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;

        final String line = "+--------------+------------+-----------------+------------+---------------------+-----------+";
        final String fmtH = "| %-12s | %-10s | %-15s | %-10s | %-19s | %-9s |%n";
        final String fmtR = "| %-12s | %-10s | %-15s | %-10s | %-19s | %-9s |%n";

        if (total == 0) {
            System.out.println("====== All Treatments (Sorted by Date) ======");
            System.out.println("(no records)");
            return;
        }

        Report.cprintln("====== All Treatments (Sorted by Date) ======");

        Runnable printHeader = () -> {
            System.out.println(line);
            System.out.printf(fmtH, "Treatment ID", "Patient ID", "Patient Name", "Doctor ID", "Date", "Status");
            System.out.println(line);
        };

        int printed = 0;
        int page = 1;

        printHeader.run();

        for (int i = 0; i < total; i++) {
            MedicalTreatment t = copy.get(i);

            System.out.printf(fmtR,
                    String.valueOf(t.getTreatmentId()),
                    t.getPatientId(),
                    cut(t.getPatientName(), 15),
                    t.getDoctorId(),
                    t.getTreatmentDateTime().format(DTF),
                    t.isCompleted() ? "Completed" : "Pending");

            printed++;

            boolean endOfPage = (printed % PAGE_SIZE == 0);
            boolean endOfData = (i == total - 1);

            if (endOfPage && !endOfData) {
                System.out.println(line);
                System.out.printf("-- Page %d/%d -- Press Enter to continue --", page, totalPages);
                scanner.nextLine();
                page++;
                printHeader.run();
            }
        }

        System.out.println(line);
        System.out.printf("Total rows: %d  (Pages: %d, Page size: %d)%n", total, totalPages, PAGE_SIZE);
    }

    // ======= Search submenu (ADT-only) =======
    private void searchMenu() {
        int c;
        do {
            System.out.println("\n=== Search Treatments ===");
            System.out.println("1. By Patient Name (contains)");
            System.out.println("2. By Doctor Name");
            System.out.println("3. By Date Range (YYYY-MM-DD .. YYYY-MM-DD)");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            try { c = Integer.parseInt(scanner.nextLine().trim()); } catch (Exception e) { c = -1; }
            switch (c) {
                case 1 -> searchByPatientName();
                case 2 -> searchByDoctorName();
                case 3 -> searchByDateRange();
                case 0 -> {}
                default -> System.out.println("Invalid choice.");
            }
        } while (c != 0);
    }

   private void searchByPatientName() {
        System.out.print("Enter part of patient name: ");
        String q = scanner.nextLine().trim().toLowerCase();
        if (q.isEmpty()) { 
            System.out.println("Empty search."); 
            return; 
        }

        MyClinicADT<MedicalTreatment> out = new MyClinicADT<>();

        // Use the ADT's iterator
        ClinicADT.MyIterator<MedicalTreatment> it = treatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            String name = (t.getPatientName() == null ? "" : t.getPatientName());
            if (name.toLowerCase().contains(q)) {
                out.add(t);
            }
        }

        displayTreatmentTable(out, "Results: Patient name contains \"" + q + "\"");
    }

   private void searchByDoctorName() {
    System.out.print("Enter doctor name (exact): ");
    String query = scanner.nextLine().trim();
    if (query.isEmpty()) { 
        System.out.println("Empty search."); 
        return; 
    }
    
    // Create comparator for doctor name using MyComparator
    ClinicADT.MyComparator<MedicalTreatment> doctorNameComparator = new ClinicADT.MyComparator<MedicalTreatment>() {
        @Override
        public int compare(MedicalTreatment a, MedicalTreatment b) {
            String aName = getDoctorNameById(a.getDoctorId());
            String bName = getDoctorNameById(b.getDoctorId());
            return aName.compareToIgnoreCase(bName);
        }
        
        private String getDoctorNameById(String doctorId) {
            if (doctorId == null) return "";
            Doctor doctor = doctorControl.getDoctorById(doctorId);
            return (doctor != null && doctor.getName() != null) ? doctor.getName() : "";
        }
    };
    
    // Sort treatments by doctor name using ADT's sort method
    treatments.sort(doctorNameComparator);
    
    // Create a dummy treatment for binary search
    // We need to find a doctor ID that corresponds to the query name
    String targetDoctorId = "";
    ClinicADT<Doctor> allDoctors = doctorControl.getAllDoctors();
    ClinicADT.MyIterator<Doctor> docIt = allDoctors.iterator();
    while (docIt.hasNext()) {
            Doctor doc = docIt.next();
            if (doc.getName() != null && doc.getName().equalsIgnoreCase(query)) {
                targetDoctorId = doc.getId();
                break;
            }
        }

        MedicalTreatment searchKey = new MedicalTreatment(
            "",              // patientId (dummy)
            "",              // patientName (dummy) 
            targetDoctorId,  // doctorId that maps to the target name
            "",              // diagnosis (dummy)
            "",              // prescription (dummy)
            LocalDateTime.now(), // treatmentDateTime (dummy)
            false            // completed (dummy)
        );

        // Use ADT's binary search method
        int index = ((MyClinicADT<MedicalTreatment>) treatments).search(searchKey, doctorNameComparator);
        MyClinicADT<MedicalTreatment> out = new MyClinicADT<>();

        if (index >= 0) {
            // Found exact match, now find all matches (since there might be duplicates)
            int start = index;
            while (start > 0 && doctorNameComparator.compare(treatments.get(start - 1), searchKey) == 0) {
                start--;
            }

            int end = index;
            while (end < treatments.size() - 1 && doctorNameComparator.compare(treatments.get(end + 1), searchKey) == 0) {
                end++;
            }

            // Add all matching treatments using ADT
            for (int i = start; i <= end; i++) {
                out.add(treatments.get(i));
            }
        }

        displayTreatmentTable(out, "Results: Doctor name = " + query);
    }
    private void searchByDateRange() {
        System.out.print("Start date (YYYY-MM-DD): ");
        LocalDate startDate;
        try { 
            startDate = LocalDate.parse(scanner.nextLine().trim()); 
        } catch (Exception e) { 
            System.out.println("Bad date."); 
            return; 
        }

        System.out.print("End date   (YYYY-MM-DD): ");
        LocalDate endDate;
        try { 
            endDate = LocalDate.parse(scanner.nextLine().trim()); 
        } catch (Exception ex) { 
            System.out.println("Bad date."); 
            return; 
        }

        if (endDate.isBefore(startDate)) { 
            System.out.println("End before start."); 
            return; 
        }

        // Create comparator for date using MyComparator
        ClinicADT.MyComparator<MedicalTreatment> dateComparator = new ClinicADT.MyComparator<MedicalTreatment>() {
            @Override
            public int compare(MedicalTreatment a, MedicalTreatment b) {
                return a.getTreatmentDateTime().toLocalDate().compareTo(b.getTreatmentDateTime().toLocalDate());
            }
        };

        // Sort treatments by date using ADT's sort method
        treatments.sort(dateComparator);

        MyClinicADT<MedicalTreatment> out = new MyClinicADT<>();

        // Use ADT's iterator to find treatments in date range
        ClinicADT.MyIterator<MedicalTreatment> it = treatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            LocalDate d = t.getTreatmentDateTime().toLocalDate();
            if ((d.isAfter(startDate) || d.equals(startDate)) && (d.isBefore(endDate) || d.equals(endDate))) {
                out.add(t);
            }
        }

        displayTreatmentTable(out, "Results: " + startDate + " to " + endDate);
    }
    // Compact table printer reused by the 3 searches
    private void displayTreatmentTable(ClinicADT<MedicalTreatment> list, String title) {
        System.out.println("\n=== " + title + " ===");
        String line = "+------------+-----------------+------------+---------------------+-----------+";
        String hdr  = "| Patient ID | Patient Name    | Doctor ID  | Date/Time           | Status    |";
        String row  = "| %-10s | %-15s | %-10s | %-19s | %-9s |";

        System.out.println(line);
        System.out.println(hdr);
        System.out.println(line);

        if (list == null || list.size() == 0) {
            System.out.println("| (no records found)                                                             |");
            System.out.println(line);
            return;
        }

        ClinicADT.MyIterator<MedicalTreatment> it = list.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            System.out.println(String.format(row,
                    t.getPatientId(),
                    cut(t.getPatientName(), 15),
                    (t.getDoctorId()==null ? "-" : t.getDoctorId()),
                    t.getTreatmentDateTime().format(DTF),
                    t.isCompleted() ? "Completed" : "Pending"
            ));
        }
        System.out.println(line);
    }

    // ===== console centering utils =====
    private static String center(String s) { if (s==null) s=""; int pad=Math.max(0,(WIDTH-s.length())/2); return " ".repeat(pad)+s; }
    private static String repeat(String s, int n){ return (n<=0) ? "" : s.repeat(n); }
    private static String padCenter(String s, int w){
        if (s == null) s = "";
        if (s.length() >= w) return s.substring(0, w);
        int left = (w - s.length()) / 2;
        int right = w - s.length() - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }

    private void renderVerticalBarChart(String title, String[] labels, int[] counts, int maxHeight) {
        if (labels == null || counts == null || labels.length != counts.length || labels.length == 0) return;

        int max = 0;
        for (int c : counts) if (c > max) max = c;
        if (max == 0) max = 1;
        int unit = (max + maxHeight - 1) / maxHeight;

        final int colW = 10;
        final String bar = "| |";
        final String cap = "+-";
        final String sp  = "  ";

        System.out.println();
        System.out.println(center(title));
        System.out.println(center(repeat("-", title.length())));

        String top = "";
        for (int c : counts) top += padCenter(String.valueOf(c), colW);
        System.out.println(center(top));

        for (int h = maxHeight; h >= 1; h--) {
            String row = "";
            for (int c : counts) {
                int barH = (c + unit - 1) / unit;
                row += padCenter(barH >= h ? (barH == h ? cap : bar) : sp, colW);
            }
            System.out.println(center(row));
        }

        System.out.println(center(repeat("-", labels.length * colW)));

        String labLine = "";
        for (String lab : labels) labLine += padCenter(lab, colW);
        System.out.println(center(labLine));
    }

    // ===== Report #1: Analysis =====
    private void analysisReport() {
        Report.printHeader("TREATMENT ANALYSIS REPORT");

        int total = 0, completed = 0, pending = 0, overdue = 0;

        ClinicADT.MyIterator<MedicalTreatment> it = treatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            total++;
            if (t.isCompleted()) completed++;
            else { pending++; if (t.isOverdue()) overdue++; }
        }

        String line = "*".repeat(WIDTH);
        System.out.println(center(line));
        final int labelW = 24;
        System.out.println(center(String.format("%-" + labelW + "s : %d", "Total Treatments", total)));
        System.out.println(center(String.format("%-" + labelW + "s : %d", "Completed Treatments", completed)));
        System.out.println(center(String.format("%-" + labelW + "s : %d", "Pending Treatments", pending)));
        System.out.println(center(String.format("%-" + labelW + "s : %d", "Overdue Treatments", overdue)));
        System.out.println(center(line));

        String[] labs = { "Completed", "Pending", "Overdue" };
        int[] vals    = { completed,   pending,   overdue   };
        renderVerticalBarChart("Bar Chart (each block ~= scaled count)", labs, vals, 10);

        System.out.println();
        System.out.println(center("Dot Plot (1 dot = 1 treatment)"));
        System.out.println(center("---------------------------------"));
        String fmt = "%-10s (%3d): %s";
        System.out.println(center(String.format(fmt, "Completed", completed, ".".repeat(Math.max(0, completed)))));
        System.out.println(center(String.format(fmt, "Pending",   pending,   ".".repeat(Math.max(0, pending)))));
        System.out.println(center(String.format(fmt, "Overdue",   overdue,   ".".repeat(Math.max(0, overdue)))));

        Report.printFooter();
    }

    // ===== Report #2: Diagnosis frequency =====
    private void frequencyDistributionReport() {
        Report.printHeader("TREATMENT FREQUENCY DISTRIBUTION REPORT");

        MyClinicADT<String> dx = new MyClinicADT<>();
        MyClinicADT<Integer> ct = new MyClinicADT<>();

        int total = 0;
        ClinicADT.MyIterator<MedicalTreatment> it = treatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            String name = t.getDiagnosis();
            if (name == null || name.isBlank() || "0".equals(name)) name = "Unknown";

            int idx = -1;
            for (int i = 0; i < dx.size(); i++) {
                if (dx.get(i).equalsIgnoreCase(name)) { idx = i; break; }
            }
            if (idx == -1) { dx.add(name); ct.add(1); }
            else { ct.set(idx, ct.get(idx) + 1); }
            total++;
        }

        if (total == 0) {
            System.out.println(center("No treatment records found."));
            Report.printFooter();
            return;
        }

        MyClinicADT<DiagnosisCount> diagnosisCounts = new MyClinicADT<>();
        for (int i = 0; i < dx.size(); i++) diagnosisCounts.add(new DiagnosisCount(dx.get(i), ct.get(i)));

        diagnosisCounts.sort(new ClinicADT.MyComparator<DiagnosisCount>() {
            @Override public int compare(DiagnosisCount d1, DiagnosisCount d2) {
                return Integer.compare(d2.count, d1.count);
            }
        });

        String line = "+------------------------------+-------+---------+";
        System.out.println(center(line));
        System.out.println(center("| Diagnosis                    | Count | %       |"));
        System.out.println(center(line));

        ClinicADT.MyIterator<DiagnosisCount> dcIt = diagnosisCounts.iterator();
        while (dcIt.hasNext()) {
            DiagnosisCount dc = dcIt.next();
            double pct = (dc.count * 100.0) / total;
            String row = String.format("| %-28s | %5d | %6.1f%% |", dc.name, dc.count, pct);
            System.out.println(center(row));
        }
        System.out.println(center(line));
        System.out.println(center("Grand Total: " + total));

        System.out.println();
        System.out.println(center("Dot Plot (1 dot = 2%)"));
        System.out.println(center("----------------------"));

        ClinicADT.MyIterator<DiagnosisCount> dcIt2 = diagnosisCounts.iterator();
        while (dcIt2.hasNext()) {
            DiagnosisCount dc = dcIt2.next();
            double pct = (dc.count * 100.0) / total;
            int dots = (int)Math.round(pct / 2.0);

            String label = String.format("%-20s (%2d, %5.1f%%)", dc.name, dc.count, pct);
            System.out.println(center(label));
            System.out.println(center(".".repeat(Math.max(1, dots))));
        }

        Report.printFooter();
    }

    private static class DiagnosisCount {
        final String name; final int count;
        DiagnosisCount(String name, int count) { this.name = name; this.count = count; }
    }

    // ===== Report #3: Treatments by Doctor =====
    private void treatmentsByDoctorReport() {
        Report.printHeader("TREATMENTS BY DOCTOR");

        MyClinicADT<String> docIds = new MyClinicADT<>();
        MyClinicADT<Integer> total = new MyClinicADT<>();
        MyClinicADT<Integer> done  = new MyClinicADT<>();

        ClinicADT.MyIterator<MedicalTreatment> it = treatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            String did = (t.getDoctorId() == null || t.getDoctorId().isBlank()) ? "(N/A)" : t.getDoctorId();

            int idx = -1;
            for (int i=0;i<docIds.size();i++) if (docIds.get(i).equalsIgnoreCase(did)) { idx=i; break; }
            if (idx==-1) { docIds.add(did); total.add(0); done.add(0); idx = docIds.size()-1; }

            total.set(idx, total.get(idx)+1);
            if (t.isCompleted()) done.set(idx, done.get(idx)+1);
        }

        if (docIds.size()==0) {
            System.out.println(center("No treatment records found."));
            Report.printFooter();
            return;
        }

        String header = "+----------------------------+-------+----------+";
        System.out.println(center(header));
        System.out.println(center("| Doctor                     | Total | Complete |"));
        System.out.println(center(header));

        int grand = 0;
        String[] labels = new String[docIds.size()];
        int[] counts = new int[docIds.size()];

        for (int i=0;i<docIds.size();i++) {
            String did = docIds.get(i);
            Doctor d = doctorControl.getDoctorById(did);
            String nameCell = (d!=null? d.getName() + " ("+did+")" : did);
            int tCount = total.get(i);
            int cCount = done.get(i);
            String row = String.format("| %-26s | %5d | %8d |", cut(nameCell,26), tCount, cCount);
            System.out.println(center(row));
            labels[i] = cut(d!=null?d.getName():did, 12);
            counts[i] = tCount;
            grand += tCount;
        }

        System.out.println(center(header));
        System.out.println(center("Grand Total: " + grand));

        renderVerticalBarChart("Treatments per Doctor", labels, counts, 10);
        Report.printFooter();
    }

    // ===== Report #4: Treatments by Day of Week =====
    private void treatmentsByDayOfWeekReport() {
        Report.printHeader("TREATMENTS BY DAY OF WEEK");

        int[] day = new int[7]; // 0=Mon..6=Sun
        ClinicADT.MyIterator<MedicalTreatment> it = treatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            DayOfWeek d = t.getTreatmentDateTime().getDayOfWeek();
            int idx = (d.getValue()+6)%7; // Monday=1 → 0
            day[idx]++;
        }

        String[] labels = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        String line = "+-----+-------+";
        System.out.println(center(line));
        System.out.println(center("| Day | Count |"));
        System.out.println(center(line));
        int total = 0;
        for (int i=0;i<7;i++) {
            String row = String.format("| %-3s | %5d |", labels[i], day[i]);
            System.out.println(center(row));
            total += day[i];
        }
        System.out.println(center(line));
        System.out.println(center("Total: " + total));

        renderVerticalBarChart("Weekly Distribution", labels, day, 10);
        Report.printFooter();
    }

    // ===== Report #5: Monthly Trend (current year) =====
    private void monthlyTreatmentTrendReport() {
        Report.printHeader("MONTHLY TREATMENT TREND (THIS YEAR)");

        int year = LocalDate.now().getYear();
        int[] m = new int[12]; // Jan..Dec
        ClinicADT.MyIterator<MedicalTreatment> it = treatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            LocalDateTime dt = t.getTreatmentDateTime();
            if (dt.getYear()==year) m[dt.getMonthValue()-1]++;
        }

        String[] labels = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        String line = "+-----+-------+";
        System.out.println(center(line));
        System.out.println(center("| Mon | Count |"));
        System.out.println(center(line));
        int total = 0;
        for (int i=0;i<12;i++) {
            String row = String.format("| %-3s | %5d |", labels[i], m[i]);
            System.out.println(center(row));
            total += m[i];
        }
        System.out.println(center(line));
        System.out.println(center("Total: " + total));

        renderVerticalBarChart("Monthly Distribution ("+year+")", labels, m, 10);
        Report.printFooter();
    }

    // ===== pickers =====
    private Patient pickPatient() {
        var all = patientControl.getAllPatients();
        if (all==null || all.size()==0){
            System.out.println("No patients.");
            return null;
        }
        String line = "+----+------------+----------------------+";
        String fmt  = "| %-2s | %-10s | %-20s |%n";
        System.out.println(line);
        System.out.printf(fmt,"No","ID","Name");
        System.out.println(line);
        for (int i=0;i<all.size();i++){
            var p=all.get(i);
            System.out.printf(fmt,i+1,p.getId(),cut(p.getName(),20));
        }
        System.out.println(line);
        int sel = getInt("Select No. (0=cancel): ");
        if (sel<=0 || sel>all.size()) return null;
        return all.get(sel-1);
    }

    private Doctor pickDoctor() {
        var all = doctorControl.getAllDoctors();
        if (all==null || all.size()==0){
            System.out.println("No doctors.");
            return null;
        }
        String line = "+----+------------+----------------------+------+"; 
        String fmt="| %-2s | %-10s | %-20s | %-4s |%n";
        System.out.println(line);
        System.out.printf(fmt,"No","ID","Name","Room");
        System.out.println(line);
        for (int i=0;i<all.size();i++){
            var d=all.get(i);
            System.out.printf(fmt,i+1,d.getId(),cut(d.getName(),20),d.getRoomNumber());
        }
        System.out.println(line);
        int sel = getInt("Select No. (0=cancel): ");
        if (sel<=0 || sel>all.size()) return null;
        return all.get(sel-1);
    }

    // ===== helpers =====
    private String prompt(String m){
        System.out.print(m);
        return scanner.nextLine().trim();
    }
    private int getInt(String m){
        while(true){
            try{
                System.out.print(m);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch(Exception e){
                System.out.println("Enter a number.");
            }
        }
    }

    // ASCII-safe cutter to avoid replacement glyphs in terminals
    private static String cut(String s,int n){
        if (s==null) return "";
        if (s.length() <= n) return s;
        if (n <= 3) return s.substring(0, n);
        return s.substring(0, n - 3) + "...";
    }
}
