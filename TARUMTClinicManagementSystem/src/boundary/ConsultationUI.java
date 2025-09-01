package boundary;

import adt.ClinicADT;
import adt.MyClinicADT;
import control.AppointmentControl;
import control.ConsultationControl;
import control.DoctorControl;
import control.PatientControl;
import control.PharmacyQueueControl;
import control.TreatmentControl;
import entity.Appointment;
import entity.Consultation;
import entity.Doctor;
import entity.MedicalTreatment;
import entity.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
import utility.Report;

public class ConsultationUI {
    private static final int CONSULTATION_DURATION_MIN = 60; // used for printing

    private final ConsultationControl consultationControl;
    private final TreatmentControl treatmentControl;
    private final Scanner sc;
    private final PatientControl patientControl;
    private final DoctorControl doctorControl;
    private final ClinicADT<Consultation> consultations;
    private final ClinicADT<MedicalTreatment> treatments;
    private final AppointmentControl appointmentControl;

    public ConsultationUI(PatientControl patientControl, DoctorControl doctorControl,
                          ClinicADT<Consultation> consultations, ClinicADT<MedicalTreatment> treatments,
                          AppointmentControl appointmentControl) {
        this.patientControl = patientControl;
        this.doctorControl = doctorControl;
        this.consultations = consultations;
        this.treatments = treatments;
        this.consultationControl = new ConsultationControl(patientControl, doctorControl, consultations, treatments, appointmentControl);
        this.treatmentControl = new TreatmentControl(treatments);
        this.appointmentControl = appointmentControl;
        this.sc = new Scanner(System.in);
    }

    // =========================
    // ========  MENU  =========
    // =========================
    public void run() {
        while (true) {
            showConsultingBanner();
            displayMainMenu();
            int choice = getInt("Choice: ");
            switch (choice) {
                case 1 -> processConsultation();
                case 2 -> consultationControl.listConsultations();
                case 3 -> searchSubmenu();
                case 4 -> checkDoctorAvailability();
                case 5 -> showWorkingHours();
                case 6 -> reportsSubmenu();
                case 7 -> sortConsultationsByPatientId();
                case 8 -> sortConsultationsByDoctorId();
                case 0 -> { System.out.println("Exiting Consultation Module..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    
    public void patientConsultationMenu() {
        while (true) {
            showConsultingBanner();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("        PATIENT - CONSULTATION");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Check Doctor Availability (by Date)");
            System.out.println(" 2) View Working Hours");
            System.out.println(" 0) Back");
            int choice = getInt("Choice: ");
            switch (choice) {
                case 1 -> checkDoctorAvailability();
                case 2 -> showWorkingHours();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    
    public void doctorConsultationMenu() {
        while (true) {
            showConsultingBanner();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("          DOCTOR - CONSULTATION");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Process Consultation");
            System.out.println(" 2) List All Consultations");
            System.out.println(" 0) Back");
            int choice = getInt("Choice: ");
            switch (choice) {
                case 1 -> processConsultation();
                case 2 -> consultationControl.listConsultations();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    
    public void adminConsultationMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("           ADMIN - CONSULTATION");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Check Doctor Availability (by Date)");
            System.out.println(" 2) Search & Analytics");
            System.out.println(" 3) Generate Reports");
            System.out.println(" 0) Back");
            int choice = getInt("Choice: ");
            switch (choice) {
                case 1 -> checkDoctorAvailability();
                case 2 -> searchSubmenu();
                case 3 -> reportsSubmenu();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    
   
    private void displayMainMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("             TARUMT CLINIC CONSULTATION MANAGEMENT");
        System.out.println("=".repeat(60));
        System.out.println(" 1. Process Consultation (route to Treatment / Billing)");
        System.out.println(" 2. List All Consultations");
        System.out.println(" 3. Search & Analytics");
        System.out.println(" 4. Check Doctor Availability for Date");
        System.out.println(" 5. View Working Hours");
        System.out.println(" 6. Generate Reports");

        System.out.println(" 0. Exit");
    }

    // ============== ENHANCED SEARCH SUBMENU ==============
    private void searchSubmenu() {
        while (true) {
            System.out.println("\n=== Search & Analytics ===");
            System.out.println(" 1. Search by Patient (with Binary Search)");
            System.out.println(" 2. Search by Doctor (with Binary Search)");
            System.out.println(" 3. Search by Date Range (with Binary Search)");
            System.out.println(" 0. Back");
            int c = getInt("Choice: ");
            switch (c) {
                case 1 -> searchByPatientId();
                case 2 -> searchByDoctorNamePartial();
                case 3 -> searchByDateRangeBinary();
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ============== ENHANCED REPORTS SUBMENU ==============
    private void reportsSubmenu() {
        while (true) {
            System.out.println("\n=== Report Features ===");
            System.out.println(" 1. Consultation Analysis Report (Sorted by Date)");
            System.out.println(" 2. Doctor Performance & Workload Report");
            System.out.println(" 3. Patient Visit Frequency Analysis");
            System.out.println(" 4. Daily/Weekly Consultation Trends");
            System.out.println(" 5. Diagnosis Distribution Report");
            System.out.println(" 0. Back");
            int c = getInt("Choice: ");
            switch (c) {
                case 1 -> consultationsSortedByDateReport();
                case 2 -> doctorPerformanceReport();
                case 3 -> patientFrequencyAnalysis();
                case 4 -> consultationTrendsReport();
                case 5 -> diagnosisDistributionReport();
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // =========================
    // === BINARY SEARCH IMPLEMENTATIONS ===
    // =========================

    // Fixed search methods that use ADT's search, sort, and iterator
    private void searchByPatientId() {
        System.out.print("Enter Patient ID (exact): ");
        String patientId = sc.nextLine().trim();
        if (patientId.isEmpty()) {
            System.out.println("Empty search.");
            return;
        }

        // Create comparator using MyComparator
        ClinicADT.MyComparator<Consultation> comparator = new ClinicADT.MyComparator<Consultation>() {
            @Override 
            public int compare(Consultation a, Consultation b) {
                return a.getPatientId().compareTo(b.getPatientId());
            }
        };

        // Sort consultations using ADT's sort method
        consultations.sort(comparator);

        // Create dummy consultation for binary search
        Consultation searchKey = new Consultation(patientId, null, null, null, null, null);

        // Use ADT's binary search method
        int foundIndex = ((MyClinicADT<Consultation>) consultations).search(searchKey, comparator);
        MyClinicADT<Consultation> results = new MyClinicADT<>();

        if (foundIndex >= 0) {
            // Find all matches (handle duplicates)
            int first = foundIndex;
            while (first > 0 && consultations.get(first - 1).getPatientId().equals(patientId)) {
                first--;
            }

            int last = foundIndex;
            while (last < consultations.size() - 1 && consultations.get(last + 1).getPatientId().equals(patientId)) {
                last++;
            }

            // Collect all matches using ADT
            for (int i = first; i <= last; i++) {
                results.add(consultations.get(i));
            }
        }

        if (results.isEmpty()) {
            System.out.println("No consultations found for Patient ID: " + patientId);
        } else {
            Report.cprintln("====== Results: Patient ID = " + patientId + " ======");
            displayConsultationResults(results);
            displayPatientSummary(results);
        }
    }

   private void searchByDoctorNamePartial() {
        System.out.print("Enter Doctor Name (partial match): ");
        String doctorName = sc.nextLine().trim(); // Ensure that the user input is trimmed
        if (doctorName.isEmpty()) {
            System.out.println("Empty search.");
            return;
        }

        // Step 1: Sort consultations by Doctor Name using ADT's binary search and comparator
        ClinicADT.MyComparator<Consultation> comparator = new ClinicADT.MyComparator<Consultation>() {
            @Override
            public int compare(Consultation a, Consultation b) {
                String nameA = a.getDoctorName();
                String nameB = b.getDoctorName();

                // Handle null and empty string values in doctorName
                if (nameA == null && nameB == null) return 0;  // Both are null, return 0
                if (nameA == null || nameA.isEmpty()) return -1;  // a's doctorName is null or empty, so it's less
                if (nameB == null || nameB.isEmpty()) return 1;   // b's doctorName is null or empty, so it's greater

                // Trim spaces and perform the string comparison
                return nameA.trim().compareTo(nameB.trim());   // Neither are null/empty, perform comparison
            }
        };

        // Sort consultations using ADT's sort method
        consultations.sort(comparator);

        // Step 2: Perform the search (using linear scan) for partial matches in sorted list
        MyClinicADT<Consultation> results = new MyClinicADT<>();

        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            // Perform a partial match check using contains() for the Doctor's name
            if (c.getDoctorName() != null && c.getDoctorName().toLowerCase().contains(doctorName.toLowerCase())) {
                results.add(c);
            }
        }

        // Step 3: Display results
        if (results.isEmpty()) {
            System.out.println("No consultations found for Doctor Name: " + doctorName);
        } else {
            Report.cprintln("====== Results: Doctor Name contains = " + doctorName + " ======");
            displayConsultationResults(results);
            displayDoctorSummary(results);
        }
    }
   
    private void searchByDateRangeBinary() {
        System.out.print("Start date (YYYY-MM-DD): ");
        LocalDate startDate;
        try {
            startDate = LocalDate.parse(sc.nextLine().trim());
        } catch (Exception e) {
            System.out.println("Invalid start date.");
            return;
        }

        System.out.print("End date (YYYY-MM-DD): ");
        LocalDate endDate;
        try {
            endDate = LocalDate.parse(sc.nextLine().trim());
        } catch (Exception e) {
            System.out.println("Invalid end date.");
            return;
        }

        if (endDate.isBefore(startDate)) {
            System.out.println("End date cannot be before start date.");
            return;
        }

        // Create comparator for comparing consultation dates (using LocalDate only if stored without time)
        ClinicADT.MyComparator<Consultation> comparator = new ClinicADT.MyComparator<Consultation>() {
            @Override
            public int compare(Consultation a, Consultation b) {
                // Compare only the LocalDate part (ignore the time if stored only with date)
                return a.getConsultationDate().toLocalDate().compareTo(b.getConsultationDate().toLocalDate());
            }
        };

        // Sort consultations using ADT's sort method
        consultations.sort(comparator);

        // Create dummy consultation for binary search with LocalDate only (ignoring time)
        Consultation startKey = new Consultation(null, null, null, null, startDate.atStartOfDay(), null);
        int startIndex = ((MyClinicADT<Consultation>) consultations).search(startKey, comparator);

        // If no consultation matches or is found in range
        if (startIndex < 0) {
            System.out.println("No consultations found for the given start date.");
            return;
        }

        // Create dummy consultation for binary search with LocalDate only (ignoring time)
        Consultation endKey = new Consultation(null, null, null, null, endDate.atTime(23, 59, 59), null);
        int endIndex = ((MyClinicADT<Consultation>) consultations).search(endKey, comparator);

        // If no consultation matches or is found in range
        if (endIndex < 0) {
            System.out.println("No consultations found for the given end date.");
            return;
        }

        // Now we can gather all consultations from startIndex to endIndex (inclusive)
        MyClinicADT<Consultation> results = new MyClinicADT<>();
        for (int i = startIndex; i <= endIndex; i++) {
            results.add(consultations.get(i));
        }

        // If no results, print that no consultations were found
        if (results.isEmpty()) {
            System.out.println("No consultations found in date range: " + startDate + " to " + endDate);
        } else {
            Report.cprintln("====== Results: " + startDate + " to " + endDate + " ======");
            displayConsultationResults(results);
            displayDateRangeSummary(results, startDate, endDate);
        }
    }


    // Helper method to display results using ADT's iterator
    private void displayConsultationResults(ClinicADT<Consultation> results) {
        String format = "| %-4s | %-12s | %-20s | %-15s | %-16s | %-8s | %-10s |\n";
        String line = "+------+--------------+----------------------+-----------------+------------------+----------+------------+";

        System.out.println(line);
        System.out.printf(format, "ID", "Patient ID", "Patient Name", "Doctor", "Date", "Time", "Status");
        System.out.println(line);

        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Use ADT's iterator to display results
        ClinicADT.MyIterator<Consultation> it = results.iterator();
        while (it.hasNext()) {
            Consultation c = it.next();
            LocalDateTime consultDate = c.getConsultationDate();

            String date = (consultDate == null) ? "N/A" : consultDate.format(df);
            String time = (consultDate == null) ? "N/A" : consultDate.toLocalTime().format(tf);

            System.out.printf(format,
                    c.getId(),
                    c.getPatientId(),
                    cut(c.getPatientName(), 20),
                    cut(c.getDoctorName(), 15),
                    date,
                    time,
                    c.getStatus().toString());
        }
        System.out.println(line);
    }

    // =========================
    // === SORTING METHODS ===
    // =========================

    private ClinicADT<Consultation> sortConsultationsByPatientId() {
        // Copy consultations
        ClinicADT<Consultation> sorted = new MyClinicADT<>();
        for (int i = 0; i < consultations.size(); i++) {
            sorted.add(consultations.get(i));
        }

        // Use ADT's merge sort instead of bubble sort
        sorted.sort(new ClinicADT.MyComparator<Consultation>() {
            @Override 
            public int compare(Consultation a, Consultation b) {
                return a.getPatientId().compareTo(b.getPatientId());
            }
        });

        return sorted;
    }

    private ClinicADT<Consultation> sortConsultationsByDoctorId() {
        // Copy consultations
        ClinicADT<Consultation> sorted = new MyClinicADT<>();
        for (int i = 0; i < consultations.size(); i++) {
            sorted.add(consultations.get(i));
        }

        // Use ADT's merge sort instead of bubble sort
        sorted.sort(new ClinicADT.MyComparator<Consultation>() {
            @Override 
            public int compare(Consultation a, Consultation b) {
                return a.getDoctorId().compareTo(b.getDoctorId());
            }
        });

        return sorted;
    }

    private ClinicADT<Consultation> sortConsultationsByDate() {
        // Copy consultations
        ClinicADT<Consultation> sorted = new MyClinicADT<>();
        for (int i = 0; i < consultations.size(); i++) {
            sorted.add(consultations.get(i));
        }

        // Use ADT's merge sort instead of bubble sort
        sorted.sort(new ClinicADT.MyComparator<Consultation>() {
            @Override 
            public int compare(Consultation a, Consultation b) {
                if (a.getConsultationDate() == null && b.getConsultationDate() == null) return 0;
                if (a.getConsultationDate() == null) return 1;
                if (b.getConsultationDate() == null) return -1;
                return a.getConsultationDate().compareTo(b.getConsultationDate());
            }
        });

        return sorted;
    }
    // =========================
    // === REPORT METHODS ===
    // =========================
    private void consultationsSortedByDateReport(){
        Report.printHeader("Consulation Sort By Date Report");
         consultationsSortedByDate();
         Report.printFooter();   
    }
    private void doctorPerformanceReport() {
        Report.printHeader("Doctor Performance & Workload Report");
        if (consultations.isEmpty()) {
            Report.cprintln("No consultations available for report.");
            Report.printFooter();
            return;
        }

        Report.cprintln("======== Doctor Performance & Workload Report ========");

        ClinicADT<String> doctorIds = new MyClinicADT<>();
        ClinicADT<String> doctorNames = new MyClinicADT<>();
        ClinicADT<Integer> consultationCounts = new MyClinicADT<>();
        ClinicADT<Integer> processingTimes = new MyClinicADT<>();

        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            String doctorId = c.getDoctorId();

            int index = findDoctorIndex(doctorIds, doctorId);
            if (index == -1) {
                doctorIds.add(doctorId);
                doctorNames.add(c.getDoctorName());
                consultationCounts.add(1);
                processingTimes.add(CONSULTATION_DURATION_MIN);
            } else {
                consultationCounts.set(index, consultationCounts.get(index) + 1);
                processingTimes.set(index, processingTimes.get(index) + CONSULTATION_DURATION_MIN);
            }
        }

        String line = "+------------+----------------------+--------+----------+-----------+----------+";
        String fmt  = "| %-10s | %-20s | %6s | %8s | %9s | %8s |";

        Report.cprintln(line);
        Report.cprintln(String.format(fmt, "Doctor ID", "Doctor Name", "Count", "Avg/Day", "Total Hrs", "Rating"));
        Report.cprintln(line);

        for (int i = 0; i < doctorIds.size(); i++) {
            int count = consultationCounts.get(i);
            int totalMinutes = processingTimes.get(i);
            double avgPerDay = count / 30.0;
            double totalHours = totalMinutes / 60.0;
            String rating = getRatingForWorkload(count);

            Report.cprintln(String.format(fmt,
                    doctorIds.get(i),
                    cut(doctorNames.get(i), 20),
                    count,
                    String.format("%.1f", avgPerDay),
                    String.format("%.1f", totalHours),
                    rating));
        }
        Report.cprintln(line);
        Report.cprintln("");
        Report.cprintln("======== Summary ========");
        int totalConsultations = consultations.size();
        int totalDoctors = doctorIds.size();
        double avgConsultationsPerDoctor = (double) totalConsultations / Math.max(1, totalDoctors);

        Report.cprintln(String.format("Total Consultations: %d", totalConsultations));
        Report.cprintln(String.format("Active Doctors: %d", totalDoctors));
        Report.cprintln(String.format("Average per Doctor: %.1f", avgConsultationsPerDoctor));
        Report.printFooter();
    }


    private void patientFrequencyAnalysis() {
     Report.printHeader("Patient Visit Frequency Analysis");
     if (consultations.isEmpty()) {
         System.out.println("No consultations available for analysis.");
         Report.printFooter();
         return;
     }
     Report.cprintln("========= Patient Visit Frequency Analysis ==========");

     // Count visits per patient
     ClinicADT<String>     patientIds   = new MyClinicADT<>();
     ClinicADT<String>     patientNames = new MyClinicADT<>();
     ClinicADT<Integer>    visitCounts  = new MyClinicADT<>();
     ClinicADT<java.time.LocalDate> firstVisits = new MyClinicADT<>();
     ClinicADT<java.time.LocalDate> lastVisits  = new MyClinicADT<>();

     for (int i = 0; i < consultations.size(); i++) {
         Consultation c = consultations.get(i);
         String pid  = c.getPatientId();
         java.time.LocalDate visitDate = c.getConsultationDate().toLocalDate();

         int idx = findPatientIndex(patientIds, pid);
         if (idx == -1) {
             patientIds.add(pid);
             patientNames.add(c.getPatientName());
             visitCounts.add(1);
             firstVisits.add(visitDate);
             lastVisits.add(visitDate);
         } else {
             visitCounts.set(idx, visitCounts.get(idx) + 1);
             if (visitDate.isBefore(firstVisits.get(idx))) firstVisits.set(idx, visitDate);
             if (visitDate.isAfter(lastVisits.get(idx)))   lastVisits.set(idx, visitDate);
         }
     }

     // Display patient details table
     String line = "+------------+----------------------+--------+------------+------------+----------+";
    String fmt  = "| %-10s | %-20s | %6s | %10s | %10s | %8s |";
    Report.cprintln(line);
    Report.cprintln(String.format(fmt, "Patient ID", "Patient Name", "Visits", "First", "Latest", "Category"));
    Report.cprintln(line);
    // rows
    for (int i = 0; i < patientIds.size(); i++) {
        int v = visitCounts.get(i);
        Report.cprintln(String.format(fmt,
            patientIds.get(i),
            cut(patientNames.get(i), 20),
            v,
            firstVisits.get(i).toString(),
            lastVisits.get(i).toString(),
            getPatientCategory(v)));
    }
    Report.cprintln(line);
    Report.cprintln("");
     // --- Build visit-frequency distribution using ADT (bins: 1,2,3,4,5,6+) ---
     MyClinicADT<Integer> dist = new MyClinicADT<>();
     for (int i = 0; i < 6; i++) dist.add(0);

     ClinicADT.MyIterator<Integer> it = visitCounts.iterator();
     while (it.hasNext()) {
         int v = it.next();
         int bin = (v >= 6) ? 5 : (v - 1);
         dist.set(bin, dist.get(bin) + 1);
     }
     displayVerticalBarChart(dist, patientIds.size());

     Report.printFooter();
 }

    /** Vertical bar chart using only the '|' character; ADT iterator throughout. */
    /** Improved vertical bar chart: uses wide '||||||' bars that fill the column width. */
    /** Vertical bar chart with a “roof” (+-+) and side walls (|   |) per bar. */
    private void displayVerticalBarChart(ClinicADT<Integer> distribution, int totalPatients) {
        final int n = distribution.size();
        if (n == 0 || totalPatients <= 0) { Report.cprintln("No data to display."); return; }

        int[] counts = new int[n];
        int max = 0;
        for (int i = 0; i < n; i++) { counts[i] = Math.max(0, distribution.get(i)); if (counts[i] > max) max = counts[i]; }
        if (max == 0) { Report.cprintln("No data to display."); return; }

        final int colW = 12, innerW = colW - 2;
        final int maxHeight = Math.min(12, Math.max(1, max));
        final int unit = (int)Math.ceil(max / (double)maxHeight);

        Report.cprintln("Visit Frequency Distribution Chart");
        Report.cprintln("==================================");
        Report.cprintln(String.format("Scale: each row = %d patient%s", unit, (unit==1?"":"s")));
        Report.cprintln("");

        String nums = "";
        for (int i = 0; i < n; i++) nums += padCenter(String.valueOf(counts[i]), colW);
        Report.cprintln(nums);

        for (int h = maxHeight; h >= 1; h--) {
            String row = "";
            for (int i = 0; i < n; i++) {
                int barH = (int)Math.ceil(counts[i] / (double)unit);
                String cell;
                if (barH >= h) {
                    cell = (barH == h) ? "+" + repeat("-", innerW) + "+" : "|" + repeat(" ", innerW) + "|";
                } else cell = repeat(" ", colW);
                row += padCenter(cell, colW);
            }
            Report.cprintln(row);
        }

        Report.cprintln(repeat("-", n * colW));

        String[] labels = {"1 visit","2 visits","3 visits","4 visits","5 visits","6+ visits"};
        String labs="", cnts="", pcts="";
        for (int i = 0; i < n; i++) {
            labs += padCenter(labels[i], colW);
            cnts += padCenter(String.valueOf(counts[i]), colW);
            pcts += padCenter(String.format("%.1f%%", (counts[i] * 100.0) / totalPatients), colW);
        }
        Report.cprintln(labs);
        Report.cprintln(cnts);
        Report.cprintln(pcts);

        Report.cprintln("");
        Report.cprintln("======== Summary Statistics ========");
        Report.cprintln(String.format("Total Patients: %d", totalPatients));
        double sumVisits = 0.0;
        for (int i = 0; i < n; i++) sumVisits += counts[i] * ((i < 5) ? (i + 1) : 6);
        Report.cprintln(String.format("Average Visits per Patient: %.2f", sumVisits / totalPatients));
        int best = 0; for (int i = 1; i < n; i++) if (counts[i] > counts[best]) best = i;
        Report.cprintln(String.format("Most Common Visit Frequency: %s (%d patients, %.1f%%)",
                labels[best], counts[best], (counts[best]*100.0)/totalPatients));
    }


    private static String padCenter(String s, int w) {
        if (s == null) s = "";
        int pad = Math.max(0, (w - s.length()) / 2);
        String out = " ".repeat(pad) + s;
        if (out.length() < w) out += " ".repeat(w - out.length());
        return out;
    }

    private static String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(s.length()*n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    // Helper method to determine patient category based on visit count
    private String getPatientCategory(int visits) {
        if (visits == 1) return "New";
        else if (visits <= 3) return "Regular";
        else if (visits <= 5) return "Frequent";
        else return "VIP";
    }

    private void consultationTrendsReport() {
        Report.printHeader("Daily/Weekly Consultation Trends");
        if (consultations.isEmpty()) {
            System.out.println("No consultations available for trends analysis.");
            return;
        }

        Report.cprintln("========== Daily/Weekly Consultation Trends ==========");
        Report.cprintln("");

        // Count by day of week
        int[] weekdayCounts = new int[7]; // Mon=0, Sun=6
        String[] weekdays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        // Count by date
        ClinicADT<LocalDate> dates = new MyClinicADT<>();
        ClinicADT<Integer> dailyCounts = new MyClinicADT<>();

        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            LocalDate date = c.getConsultationDate().toLocalDate();
            int dayOfWeek = date.getDayOfWeek().getValue() - 1; // Convert to 0-6
            weekdayCounts[dayOfWeek]++;

            // Daily counts
            int dateIndex = findDateIndex(dates, date);
            if (dateIndex == -1) {
                dates.add(date);
                dailyCounts.add(1);
            } else {
                dailyCounts.set(dateIndex, dailyCounts.get(dateIndex) + 1);
            }
        }

        // Weekly distribution
        Report.cprintln("------ Weekly Distribution ------");
        String line1 = "+------------+-------+----------+";
        Report.cprintln(String.format("| %-10s | %5s | %8s |", "Day", "Count", "Percent"));
        Report.cprintln(line1);
        int totalConsultations = consultations.size();
        for (int i = 0; i < weekdays.length; i++) {
            double percentage = (double) weekdayCounts[i] / totalConsultations * 100;
            Report.cprintln(String.format("| %-10s | %5d | %7.1f%% |", weekdays[i], weekdayCounts[i], percentage));
        }
        Report.cprintln(line1);

        // Peak analysis
        int maxDailyCount = 0;
        LocalDate peakDate = null;
        for (int i = 0; i < dailyCounts.size(); i++) {
            if (dailyCounts.get(i) > maxDailyCount) {
                maxDailyCount = dailyCounts.get(i);
                peakDate = dates.get(i);
            }
        }
        Report.cprintln("");
        Report.cprintln("--------- Peak Analysis ---------");
        Report.cprintf("Busiest day: %s (%d consultations)%n", 
            peakDate != null ? peakDate.toString() : "N/A", maxDailyCount);
        
        // Find busiest day of week
        int maxWeekdayIndex = 0;
        for (int i = 1; i < weekdayCounts.length; i++) {
            if (weekdayCounts[i] > weekdayCounts[maxWeekdayIndex]) {
                maxWeekdayIndex = i;
            }
        }
        Report.cprintf("Busiest weekday: %s (%d consultations)%n", 
            weekdays[maxWeekdayIndex], weekdayCounts[maxWeekdayIndex]);
        Report.printFooter();
    }

    private void diagnosisDistributionReport() {
        Report.printHeader("Diagnosis Distribution Report");
        if (consultations.isEmpty()) {
            System.out.println("No consultations available for diagnosis analysis.");
            return;
        }

        Report.cprintln("============== Diagnosis Distribution Report ==============");
        Report.cprintln("");

        // Count diagnoses
        ClinicADT<String> diagnoses = new MyClinicADT<>();
        ClinicADT<Integer> counts = new MyClinicADT<>();

        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            String diagnosis = c.getDiagnosis();
            if (diagnosis == null || diagnosis.trim().isEmpty() || 
                diagnosis.equals("To be diagnosed during appointment")) {
                diagnosis = "Not Diagnosed";
            }
            
            int index = findDiagnosisIndex(diagnoses, diagnosis);
            if (index == -1) {
                diagnoses.add(diagnosis);
                counts.add(1);
            } else {
                counts.set(index, counts.get(index) + 1);
            }
        }

        // Sort by count (descending)
        for (int i = 0; i < counts.size() - 1; i++) {
            for (int j = 0; j < counts.size() - 1 - i; j++) {
                if (counts.get(j) < counts.get(j + 1)) {
                    // Swap counts
                    Integer tempCount = counts.get(j);
                    counts.set(j, counts.get(j + 1));
                    counts.set(j + 1, tempCount);
                    
                    // Swap diagnoses
                    String tempDiagnosis = diagnoses.get(j);
                    diagnoses.set(j, diagnoses.get(j + 1));
                    diagnoses.set(j + 1, tempDiagnosis);
                }
            }
        }

        // Display top 10 diagnoses
        String line = "+----+--------------------------------+-------+----------+";
        String fmt = "| %2s | %-30s | %5s | %8s |%n";

        Report.cprintln(line);
        Report.cprintf(fmt, "No", "Diagnosis", "Count", "Percent");
        Report.cprintln(line);

        int totalWithDiagnosis = consultations.size();
        int displayCount = Math.min(10, diagnoses.size());
        
        for (int i = 0; i < displayCount; i++) {
            double percentage = (double) counts.get(i) / totalWithDiagnosis * 100;
                Report.cprintf(fmt,
                String.valueOf(i + 1),
                cut(diagnoses.get(i), 30),
                counts.get(i),
                String.format("%.1f%%", percentage));
        }
        Report.cprintln(line);

        Report.cprintln("");
        Report.cprintln("========= Summary =========");
        Report.cprintln(String.format("Total unique diagnoses: %d", diagnoses.size()));
        Report.cprintln(String.format("Total consultations: %d", totalWithDiagnosis));
        if (diagnoses.size() > 0) {
            Report.cprintln(String.format("Most common: %s (%d cases, %.1f%%)",
                    diagnoses.get(0), counts.get(0), (double) counts.get(0) / totalWithDiagnosis * 100));
        }
                Report.printFooter();
    }

    // =========================
    // === SUMMARY DISPLAY METHODS ===
    // =========================

    private void displayPatientSummary(ClinicADT<Consultation> consultations) {
        if (consultations.isEmpty()) return;
        
        System.out.println("\n--- Patient Summary ---");
        System.out.printf("Total consultations: %d%n", consultations.size());
        
        // Find date range
        LocalDate earliest = null, latest = null;
        for (int i = 0; i < consultations.size(); i++) {
            LocalDate date = consultations.get(i).getConsultationDate().toLocalDate();
            if (earliest == null || date.isBefore(earliest)) earliest = date;
            if (latest == null || date.isAfter(latest)) latest = date;
        }
        
        if (earliest != null) {
            System.out.printf("Period: %s to %s%n", earliest, latest);
            long daysBetween = ChronoUnit.DAYS.between(earliest, latest) + 1;
            System.out.printf("Frequency: %.1f consultations per week%n", 
                consultations.size() * 7.0 / daysBetween);
        }
    }

    private void displayDoctorSummary(ClinicADT<Consultation> consultations) {
        if (consultations.isEmpty()) return;
        
        System.out.println("\n--- Doctor Summary ---");
        System.out.printf("Total consultations: %d%n", consultations.size());
        
        // Count by status
        int pending = 0, processed = 0, consulting = 0;
        for (int i = 0; i < consultations.size(); i++) {
            Consultation.Status status = consultations.get(i).getStatus();
            switch (status) {
                case PENDING -> pending++;
                case PROCESSED -> processed++;
                case CONSULTING -> consulting++;
            }
        }
        
        System.out.printf("Pending: %d, Consulting: %d, Processed: %d%n", 
            pending, consulting, processed);
        
        // Workload assessment
        String workload;
        if (consultations.size() > 20) workload = "High";
        else if (consultations.size() > 10) workload = "Medium";
        else workload = "Light";
        
        System.out.printf("Workload: %s%n", workload);
    }

    private void displayDateRangeSummary(ClinicADT<Consultation> consultations, LocalDate start, LocalDate end) {
        if (consultations.isEmpty()) return;
        
        System.out.println("\n--- Date Range Summary ---");
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        double avgPerDay = (double) consultations.size() / days;
        
        System.out.printf("Total consultations: %d%n", consultations.size());
        System.out.printf("Period: %d days%n", days);
        System.out.printf("Average per day: %.1f%n", avgPerDay);
        
        // Peak day analysis
        ClinicADT<LocalDate> dates = new MyClinicADT<>();
        ClinicADT<Integer> counts = new MyClinicADT<>();
        
        for (int i = 0; i < consultations.size(); i++) {
            LocalDate date = consultations.get(i).getConsultationDate().toLocalDate();
            int index = findDateIndex(dates, date);
            if (index == -1) {
                dates.add(date);
                counts.add(1);
            } else {
                counts.set(index, counts.get(index) + 1);
            }
        }
        
        // Find peak day
        int maxCount = 0;
        LocalDate peakDate = null;
        for (int i = 0; i < counts.size(); i++) {
            if (counts.get(i) > maxCount) {
                maxCount = counts.get(i);
                peakDate = dates.get(i);
            }
        }
        
        if (peakDate != null) {
            System.out.printf("Busiest day: %s (%d consultations)%n", peakDate, maxCount);
        }
    }

    // =========================
    // === HELPER METHODS FOR SEARCH ===
    // =========================

    private int findDoctorIndex(ClinicADT<String> doctorIds, String doctorId) {
        for (int i = 0; i < doctorIds.size(); i++) {
            if (doctorIds.get(i).equals(doctorId)) {
                return i;
            }
        }
        return -1;
    }

    private int findPatientIndex(ClinicADT<String> patientIds, String patientId) {
        for (int i = 0; i < patientIds.size(); i++) {
            if (patientIds.get(i).equals(patientId)) {
                return i;
            }
        }
        return -1;
    }

    private int findDateIndex(ClinicADT<LocalDate> dates, LocalDate date) {
        for (int i = 0; i < dates.size(); i++) {
            if (dates.get(i).equals(date)) {
                return i;
            }
        }
        return -1;
    }

    private int findDiagnosisIndex(ClinicADT<String> diagnoses, String diagnosis) {
        for (int i = 0; i < diagnoses.size(); i++) {
            if (diagnoses.get(i).equalsIgnoreCase(diagnosis)) {
                return i;
            }
        }
        return -1;
    }

    private String getRatingForWorkload(int consultationCount) {
        if (consultationCount > 50) return "Excellent";
        if (consultationCount > 30) return "Good";
        if (consultationCount > 15) return "Average";
        if (consultationCount > 5) return "Fair";
        return "Low";
    }

    // =========================
    // === EXISTING PROCESS CONSULTATION ===
    // =========================
    
    private void processConsultation() {
        System.out.println("\n=== Process Consultation ===");

        // If no one is currently "called"/locked, let the user pick a CONSULTING appt
        Appointment called = appointmentControl.getCalled();
        if (called == null) {
            var consultingList = appointmentControl.getConsultingAppointments();
            if (consultingList == null || consultingList.size() == 0) {
                System.out.println("No patient is currently in CONSULTING status.");
                return;
            }

            // List all consulting patients
            String line = "+----+--------+----------------------+----------------------+---------------------+";
            String hdr  = "| %-2s | %-6s | %-20s | %-20s | %-19s |%n";
            String row  = "| %-2d | %-6d | %-20s | %-20s | %-19s |%n";
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            System.out.println("\nPatients in CONSULTING status:");
            System.out.println(line);
            System.out.printf(hdr, "No", "ID", "Patient", "Doctor", "Scheduled Time");
            System.out.println(line);
            for (int i = 0; i < consultingList.size(); i++) {
                Appointment a = consultingList.get(i);
                System.out.printf(row, i + 1, a.getAppointmentId(),
                        cut(a.getPatientName(), 20),
                        cut(a.getDoctorName(), 20),
                        a.getScheduledDateTime().format(dtf));
            }
            System.out.println(line);

            int choice;
            while (true) {
                choice = getInt("Select No. to process (0=cancel): ");
                if (choice == 0) return;
                if (choice >= 1 && choice <= consultingList.size()) break;
                System.out.println("Invalid selection.");
            }
            called = consultingList.get(choice - 1);

            // Lock this appointment as the one we're processing now
            boolean ok = appointmentControl.beginConsultation(called.getAppointmentId());
            if (!ok) {
                System.out.println("Unable to begin consultation for this appointment.");
                return;
            }
        }

        // Mirror the "currently consulting" banner (optional)
        consultationControl.showConsultingFromAppointment(called.getPatientId());

        // Prepare an active Consultation record in your Consultation module
        boolean locked = consultationControl.setExternalCalledForAppointment(
                called.getPatientId(),
                called.getPatientName(),
                called.getDoctorName(),
                called.getDoctorId(),
                called.getScheduledDateTime(),
                /* createIfMissing = */ true
        );

        if (!locked) {
            System.out.println("Unable to prepare consultation for this appointment.");
            return;
        }
        
        consultationControl.showConsultingFromAppointment(called.getPatientId());

        // === Minimal capture: symptoms + whether treatment is needed ===
        String symptoms = prompt("Enter symptoms (or 0 to cancel): ");
        if ("0".equals(symptoms)) return;

        boolean treatmentNeeded = yesNo("Treatment needed? (y/n): ");

        // 1) Complete on the Appointment side (routes to TREATMENT or PENDING_PAYMENT
        //    and enqueues for treatment when needed)
        boolean updated = appointmentControl.completeConsultation(symptoms, treatmentNeeded);
        if (!updated) {
            System.out.println("Failed to complete consultation.");
            return;
        }

        // 2) Mark the Consultation entity as processed & persist/unlock in your Consultation module
        consultationControl.processCalledWithSymptomsOnly(symptoms, treatmentNeeded);

        // Final user feedback
        if (treatmentNeeded) {
            System.out.println("\nStatus changed to TREATMENT. Patient sent to Treatment Waiting Queue.");
        } else {
            System.out.println("\nNo treatment required. Status changed to PENDING PAYMENT.");
        }
    }

    // =========================
    // === OTHER UI METHODS ===
    // =========================
    
    private void showConsultingBanner() {
        Appointment called = appointmentControl.getCalled();
        if (called == null) {
            System.out.println("\nCurrently consulting: -");
            return;
        }

        // Ensure a Consultation exists + is locked/CONSULTING immediately
       consultationControl.setExternalCalledForAppointment(
                called.getPatientId(),
                called.getPatientName(),
                called.getDoctorName(),
                called.getDoctorId(),
                called.getScheduledDateTime(),
                /* createIfMissing = */ false   // <<-- no side-effects on banner
        );

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        System.out.println("\nCurrently consulting: "
                + "Appt #" + called.getAppointmentId()
                + "  " + called.getPatientName() + " (" + called.getPatientId() + ")"
                + "  " + called.getScheduledDateTime().format(fmt)
                + "  Dr. " + called.getDoctorName());
    }

    private void checkDoctorAvailability() {
        LocalDate d = pickDate("Select date to check:");
        if (d == null) return;
        consultationControl.showDoctorScheduleForDate(d.atTime(8, 0));
    }

    private void showWorkingHours() {
        System.out.println("\nWorking hours:");
        System.out.println("  Morning : 08:00–12:00");
        System.out.println("  Lunch   : 12:00–13:00 (no consultations)");
        System.out.println("  Afternoon: 13:00–17:00");
        System.out.println("  Gap     : 17:00–18:00 (no consultations)");
        System.out.println("  Night   : 18:00–22:00");
    }

    private void consultationsSortedByDate() {
        if (consultations == null || consultations.size() == 0) {
            System.out.println("\nNo consultations to show.");
            return;
        }
        
        // Use existing sorting method
        ClinicADT<Consultation> copy = sortConsultationsByDate();
        
        String line = "+------+--------------+----------------------+----------------------+----------------------+-----------+";
        String fmt  = "| %-4s | %-12s | %-20s | %-20s | %-20s | %-9s |%n";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        Report.cprintln("========== Consultation Analysis (Sorted by Date) ==========");
        System.out.println(line);
        System.out.printf(fmt, "ID", "Patient ID", "Patient Name", "Doctor", "Date & Time", "Duration");
        System.out.println(line);
        
        for (int i = 0; i < copy.size(); i++) {
            Consultation c = copy.get(i);
            String dt = (c.getConsultationDate() == null) ? "N/A" : c.getConsultationDate().format(dtf);
            System.out.printf(fmt, c.getId(), c.getPatientId(), cut(c.getPatientName(),20),
                    cut(c.getDoctorName(),20), dt, CONSULTATION_DURATION_MIN + " min");
        }
        System.out.println(line);
    }

    private void displayConsultationDetails(ClinicADT<Consultation> consultationList) {
        String format = "| %-4s | %-12s | %-20s | %-15s | %-16s | %-8s | %-8s | %-10s |\n";
        String line = "+------+--------------+----------------------+-----------------+------------------+----------+----------+------------+";
        final int PAGE = 10;

        System.out.println(line);
        System.out.printf(format, "ID", "Patient ID", "Patient Name", "Doctor", "Date", "Start", "End", "Status");
        System.out.println(line);

        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int count = 0;

        for (int i = 0; i < consultationList.size(); i++) {
            Consultation c = consultationList.get(i);
            LocalDateTime start = c.getConsultationDate();
            LocalDateTime end = start.plusMinutes(CONSULTATION_DURATION_MIN);

            String date = (start == null) ? "N/A" : start.format(df);
            String startStr = (start == null) ? "N/A" : start.toLocalTime().format(tf);
            String endStr   = (start == null) ? "N/A" : end.toLocalTime().format(tf);

            System.out.printf(format,
                    c.getId(),
                    c.getPatientId(),
                    cut(c.getPatientName(), 20),
                    cut(c.getDoctorName(), 15),
                    date,
                    startStr,
                    endStr,
                    c.getStatus().toString());

            count++;
            if (count % PAGE == 0 && i < consultationList.size() - 1) {
                System.out.println(line);
                if (!promptContinuePage(count, consultationList.size())) break;
                System.out.println(line);
                System.out.printf(format, "ID", "Patient ID", "Patient Name", "Doctor", "Date", "Start", "End", "Status");
                System.out.println(line);
            }
        }
        System.out.println(line);
    }

    private boolean promptContinuePage(int shown, int total) {
        System.out.print("Shown " + shown + "/" + total + ". Press ENTER to show next 10 (or type q to quit): ");
        String in = sc.nextLine();
        return !(in != null && in.trim().equalsIgnoreCase("q"));
    }

    // =========================
    // ========= UTILS =========
    // =========================
    
    private String prompt(String m) { 
        System.out.print(m); 
        return sc.nextLine().trim(); 
    }

    private boolean yesNo(String m) {
        while (true) {
            System.out.print(m);
            String s = sc.nextLine().trim().toLowerCase();
            if (s.equals("y")) return true;
            if (s.equals("n")) return false;
            System.out.println("Enter y or n.");
        }
    }

    private int getInt(String m) {
        while (true) {
            try {
                System.out.print(m);
                return Integer.parseInt(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Enter a number.");
            }
        }
    }

    private static String cut(String s, int n) { 
        if (s == null) return ""; 
        return s.length() <= n ? s : s.substring(0, Math.max(0, n - 1)) + "..."; 
    }

    private LocalDate pickDate(String prompt) {
        System.out.println("\n" + prompt);
        System.out.print("1) Today  2) Tomorrow  3) Enter yyyy-MM-dd: ");
        String s = sc.nextLine().trim();
        if ("1".equals(s)) return LocalDate.now();
        if ("2".equals(s)) return LocalDate.now().plusDays(1);
        if ("3".equals(s)) {
            System.out.print("Date (yyyy-MM-dd): ");
            try {
                return LocalDate.parse(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Invalid date.");
                return null;
            }
        }
        return null;
    }
}