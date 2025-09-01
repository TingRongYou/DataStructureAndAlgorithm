package boundary;

import adt.ClinicADT;
import adt.MyClinicADT;
import control.AppointmentControl;
import control.PaymentControl;
import control.PharmacyControl;
import control.PharmacyQueueControl;
import entity.Appointment;
import entity.MedicalTreatment;
import entity.Payment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import utility.Report;

/**
 * Payment UI – pick a PENDING_PAYMENT appointment, auto-calc (consultation + treatment + medicine),
 * append receipt (with META), search (3 features), reports (5 features), and method bar chart.
 * Uses only your ADT/iterator; no java.util collections.
 * Enforces business rule: IF medicine fee > 0 THEN treatment fee must be included.
 */
public class PaymentUI {
    private final Scanner sc = new Scanner(System.in);

    private final PharmacyControl pharmacy;
    private final AppointmentControl apptCtrl;
    private final PharmacyQueueControl pharmQueue;
    private final ClinicADT<MedicalTreatment> treatments; // infer treatment from this
    private final PaymentControl control = new PaymentControl();

    private static final int WIDTH = 120;
    private static final int RECORDS_PER_PAGE = 10;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PaymentUI(PharmacyControl pharmacyControl,
                     AppointmentControl appointmentControl,
                     PharmacyQueueControl pharmacyQueue,
                     ClinicADT<MedicalTreatment> allTreatments) {
        this.pharmacy = pharmacyControl;
        this.apptCtrl = appointmentControl;
        this.pharmQueue = pharmacyQueue;
        this.treatments = allTreatments;
    }
    
    public void patientPaymentMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("              PATIENT - PAYMENTS");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Process Payment for Pending Appointment");
            System.out.println(" 2) View Saved Receipt by ID");
            System.out.println(" 0) Back");
            System.out.print("Choice: ");
            int choice; try { choice = Integer.parseInt(sc.nextLine().trim()); } catch (Exception e) { choice = -1; }
            switch (choice) {
                case 1 -> processPayment();
                case 2 -> viewReceiptById();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    
    public void adminPaymentMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("               ADMIN - PAYMENTS");
            System.out.println("=".repeat(60));
            System.out.println(" 1) View Saved Receipt by ID");
            System.out.println(" 2) Search");
            System.out.println(" 3) Reports");
            System.out.println(" 0) Back");
            System.out.print("Choice: ");
            int choice; try { choice = Integer.parseInt(sc.nextLine().trim()); } catch (Exception e) { choice = -1; }
            switch (choice) {
                case 1 -> viewReceiptById();
                case 2 -> searchMenu();
                case 3 -> reportMenu();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ===== MAIN MENU =====
    public void run() {
        int choice;
        do {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("             [PAYMENT / BILLING] MANAGEMENT");
            System.out.println("=".repeat(60));
            System.out.println("1. Process Payment for a Pending Appointment");
            System.out.println("2. View Saved Receipt by ID");
            System.out.println("3. Search");
            System.out.println("4. Reports");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            try { choice = Integer.parseInt(sc.nextLine().trim()); } catch (Exception e) { choice = -1; }

            switch (choice) {
                case 1 -> processPayment();
                case 2 -> viewReceiptById();
                case 3 -> searchMenu();     
                case 4 -> reportMenu();
                case 0 -> System.out.println("Returning...");
                default -> System.out.println("Invalid choice.");
            }
        } while (choice != 0);
    }

    // ===== Option 1: Process a pending payment =====
    private void processPayment() {
        Appointment a = pickPendingPaymentAppointment();
        if (a == null) { System.out.println("No appointment selected."); return; }

        pharmQueue.reload();
        int apptId = a.getAppointmentId();

        // Calculate medicine fee from Pharmacy queues (READY + DISPENSED)
        double medicineFee = pharmQueue.getTotalMedicineFeeForAppointment(apptId, pharmacy);
        String medBreakdown = pharmQueue.getMedicineBreakdownForAppointment(apptId);
        if (medicineFee > 0) {
            System.out.println("\nMedicine breakdown:");
            System.out.print(medBreakdown);
        }

        // Enforce rule:
        // - consultation: always
        // - treatment: included if (a) there was a treatment recorded OR (b) any medicine is dispensed
        boolean hasTreatment =
                (a.isTreatmentNeeded())
             || inferTreatmentFromRecords(a)
             || (medicineFee > 0.0);

        double previewTotal = new Payment(hasTreatment, medicineFee, Payment.PaymentMethod.CASH, 0.0).getTotal();

        System.out.println("\n=== Bill Preview ===");
        String line = "+----------+------------+----------------------+----------------------+---------------------+";
        String hdr  = "| Appt ID  | Patient ID | Patient Name         | Doctor               | Scheduled Time      |";
        String row  = "| %-8d | %-10s | %-20s | %-20s | %-19s |";
        System.out.println(line); System.out.println(hdr); System.out.println(line);
        System.out.println(String.format(row,
                a.getAppointmentId(), a.getPatientId(),
                cut(a.getPatientName(),20), cut(a.getDoctorName(),20),
                a.getScheduledDateTime().format(DTF)));
        System.out.println(line);

        System.out.printf("%nConsultation : RM %6.2f%n", Payment.CONSULTATION_FEE);
        System.out.printf("Treatment    : %sRM %6.2f%n",
                (hasTreatment ? "" : "(skip) "),
                (hasTreatment ? Payment.TREATMENT_FEE : 0.0));
        System.out.printf("Medicine     : %sRM %6.2f%n",
                (medicineFee > 0.0 ? "" : "(skip) "),
                medicineFee);
        System.out.println("--------------------------------");
        System.out.printf("Current total: RM %6.2f%n", previewTotal);

        Payment.PaymentMethod method = selectPaymentMethod();
        double amountPaid = processPaymentAmount(method, previewTotal);
        if (amountPaid == 0.0 && method != Payment.PaymentMethod.CASH) {
            System.out.println("Payment canceled.");
            return;
        }

        Payment p = control.buildPaymentWithDoctor(
                hasTreatment, medicineFee, method, amountPaid,
                a.getAppointmentId(), a.getPatientId(), a.getPatientName(),
                a.getDoctorId(), a.getDoctorName()
        );

        apptCtrl.markCompleted(a.getAppointmentId());

        String block = control.loadReceiptBlock(p.getReceiptId());
        System.out.println("\n" + (block != null ? block : control.renderReceiptLayout(p, "TARUMTClinic")));

        System.out.println("Receipt ID: " + p.getReceiptId() + " (saved to src/textFile/receipts.txt)");
        System.out.println("Appointment #" + a.getAppointmentId() + " is now COMPLETED.");
    }

    // ===== Option 2: View receipt with pagination =====
    private void viewReceiptById() {
        MyClinicADT<PaymentControl.PaymentRecord> allRecords = getSortedReceiptList();
        if (allRecords.size() == 0) {
            System.out.println("\nNo receipts found.");
            return;
        }

        int currentPage = 0;
        int totalPages = (allRecords.size() + RECORDS_PER_PAGE - 1) / RECORDS_PER_PAGE;

        while (true) {
            displayReceiptPage(allRecords, currentPage, totalPages);
            System.out.print("\nEnter No. OR Receipt ID (Enter=next page, P=prev page, 0=cancel): ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Operation canceled.");
                return;
            }
            if (input.isEmpty()) { // next page
                if (currentPage < totalPages - 1) currentPage++;
                else System.out.println("Already at last page.");
                continue;
            }
            if (input.equalsIgnoreCase("P")) { // prev page
                if (currentPage > 0) currentPage--; else System.out.println("Already at first page.");
                continue;
            }

            PaymentControl.PaymentRecord selectedRecord = null;
            if (input.matches("\\d+")) {
                try {
                    int num = Integer.parseInt(input);
                    int globalIndex = currentPage * RECORDS_PER_PAGE + num - 1;
                    if (num >= 1 && num <= RECORDS_PER_PAGE && globalIndex < allRecords.size()) {
                        selectedRecord = allRecords.get(globalIndex);
                    } else {
                        System.out.println("Invalid number for current page.");
                        continue;
                    }
                } catch (Exception e) {
                    System.out.println("Invalid input.");
                    continue;
                }
            } else {
                selectedRecord = findRecordByReceiptId(input);
                if (selectedRecord == null) {
                    System.out.println("Receipt ID not found.");
                    continue;
                }
            }
            if (selectedRecord != null) {
                displayReceiptDetails(selectedRecord);
                break; // exit after showing receipt
            }
        }
    }

    /** Display a page of receipts */
    private void displayReceiptPage(MyClinicADT<PaymentControl.PaymentRecord> allRecords, int currentPage, int totalPages) {
        int startIndex = currentPage * RECORDS_PER_PAGE;
        int endIndex = Math.min(startIndex + RECORDS_PER_PAGE, allRecords.size());

        String line = "+----+------------------+------------+----------------------+----------+----------------------+---------------------+";
        String hdr  = "| No | Receipt          | Appt ID    | Patient Name         | Dr.ID    | Doctor Name          | Date/Time           |";
        String row  = "| %-2d | %-16s | %-10s | %-20s | %-8s | %-20s | %-19s |";

        System.out.println("\n=== Available Receipts (Page " + (currentPage + 1) + " of " + totalPages + ") ===");
        System.out.println(line);
        System.out.println(hdr);
        System.out.println(line);

        for (int i = startIndex; i < endIndex; i++) {
            PaymentControl.PaymentRecord r = allRecords.get(i);

            String patientName = "N/A";
            if (r.patientName != null && !r.patientName.trim().isEmpty()) {
                patientName = r.patientName.trim();
            } else if (r.apptId > 0) {
                Appointment appt = findAppointmentById(r.apptId);
                if (appt != null && appt.getPatientName() != null && !appt.getPatientName().isBlank()) {
                    patientName = appt.getPatientName();
                }
            }

            String doctorName = "-";
            String doctorId   = "-";
            if (r.apptId > 0) {
                Appointment appt = findAppointmentById(r.apptId);
                if (appt != null) {
                    if (appt.getDoctorName() != null && !appt.getDoctorName().isBlank()) doctorName = appt.getDoctorName();
                    if (appt.getDoctorId() != null && !appt.getDoctorId().isBlank())   doctorId   = appt.getDoctorId();
                }
            }

            int displayNum = (i - startIndex) + 1;
            System.out.println(String.format(row,
                    displayNum,
                    cut(r.receiptId != null ? r.receiptId : "N/A", 16),
                    (r.apptId < 0 ? "-" : String.valueOf(r.apptId)),
                    cut(patientName, 20),
                    cut(doctorId, 8),
                    cut(doctorName, 20),
                    r.dateTime.format(DTF)
            ));
        }
        System.out.println(line);

        System.out.println("Showing " + (startIndex + 1) + "-" + endIndex + " of " + allRecords.size() + " records");
    }

    /** Display detailed receipt information */
    private void displayReceiptDetails(PaymentControl.PaymentRecord rec) {
        String docNm = "-";
        if (rec.apptId > 0) {
            Appointment ap = findAppointmentById(rec.apptId);
            if (ap != null && ap.getDoctorName() != null && !ap.getDoctorName().isBlank()) {
                docNm = ap.getDoctorName();
            }
        }

        String line = "+------------+----------------------+---------------------+------------+------------+----------------------+";
        String hdr  = "| Receipt ID | Patient Name         | Date/Time           | Appt ID    | Method     | Doctor Name          |";
        String row  = "| %-10s | %-20s | %-19s | %-10s | %-10s | %-20s |";

        System.out.println();
        System.out.println(Report.center("Receipt Summary"));
        System.out.println(Report.center(line));
        System.out.println(Report.center(hdr));
        System.out.println(Report.center(line));
        System.out.println(Report.center(String.format(
                row,
                (rec.receiptId == null ? "-" : cut(rec.receiptId, 10)),
                cut((rec.patientName == null || rec.patientName.isEmpty()) ? "N/A" : rec.patientName, 20),
                rec.dateTime.format(DTF),
                (rec.apptId <= 0 ? "-" : String.valueOf(rec.apptId)),
                rec.method == null ? "-" : rec.method.name(),
                cut(docNm, 20)
        ))); // fixed extra ')'
        System.out.println(Report.center(line));

        String block = control.loadReceiptBlock(rec.receiptId);
        if (block != null) {
            System.out.println();
            System.out.println(block);
        } else {
            System.out.println("Receipt layout not found in file.");
        }
    }

    /** Get sorted receipt list (newest first) */
    private MyClinicADT<PaymentControl.PaymentRecord> getSortedReceiptList() {
        ClinicADT<PaymentControl.PaymentRecord> all = control.loadAllRecords();
        MyClinicADT<PaymentControl.PaymentRecord> list = new MyClinicADT<>();
        ClinicADT.MyIterator<PaymentControl.PaymentRecord> it = all.iterator();
        while (it.hasNext()) list.add(it.next());

        // Selection sort by date desc (newest first)
        for (int i = 0; i < list.size(); i++) {
            int best = i;
            for (int j = i + 1; j < list.size(); j++) {
                if (list.get(j).dateTime.isAfter(list.get(best).dateTime)) best = j;
            }
            if (best != i) {
                PaymentControl.PaymentRecord tmp = list.get(i);
                list.set(i, list.get(best));
                list.set(best, tmp);
            }
        }
        return list;
    }

    // ====== SEARCH MENU (included) ======
    private void searchMenu() {
        int c;
        do {
            System.out.println("\n=== Search ===");
            System.out.println("1. By Payment Method");
            System.out.println("2. By Date (YYYY-MM-DD)");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            try { c = Integer.parseInt(sc.nextLine().trim()); } catch (Exception e) { c = -1; }

            switch (c) {
                case 1 -> searchByMethod();
                case 2 -> searchByDate();
                case 0 -> {}
                default -> System.out.println("Invalid choice.");
            }
        } while (c != 0);
    }

    // ====== ADT-backed search helpers ======
    private PaymentControl.PaymentRecord findRecordByReceiptId(String rid) {
        ClinicADT<PaymentControl.PaymentRecord> all = control.loadAllRecords();
        ClinicADT<PaymentControl.PaymentRecord> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<PaymentControl.PaymentRecord> it = all.iterator();
        while (it.hasNext()) sorted.add(it.next());

        ClinicADT.MyComparator<PaymentControl.PaymentRecord> comparator =
            new ClinicADT.MyComparator<PaymentControl.PaymentRecord>() {
                @Override
                public int compare(PaymentControl.PaymentRecord a, PaymentControl.PaymentRecord b) {
                    if (a.receiptId == null && b.receiptId == null) return 0;
                    if (a.receiptId == null) return 1;
                    if (b.receiptId == null) return -1;
                    return a.receiptId.compareToIgnoreCase(b.receiptId);
                }
            };
        sorted.sort(comparator);

        PaymentControl.PaymentRecord key = new PaymentControl.PaymentRecord();
        key.receiptId = rid;

        int idx = ((MyClinicADT<PaymentControl.PaymentRecord>) sorted).search(key, comparator);
        return (idx >= 0) ? sorted.get(idx) : null;
    }

    private Appointment findAppointmentById(int apptId) {
        if (apptId <= 0 || apptCtrl == null) return null;

        ClinicADT<Appointment> all = apptCtrl.getAll();
        ClinicADT<Appointment> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> it = all.iterator();
        while (it.hasNext()) sorted.add(it.next());

        ClinicADT.MyComparator<Appointment> comparator = new ClinicADT.MyComparator<Appointment>() {
            @Override public int compare(Appointment a, Appointment b) {
                return Integer.compare(a.getAppointmentId(), b.getAppointmentId());
            }
        };
        sorted.sort(comparator);

        // If you have a way to construct a key with the ID set, use it.
        // Otherwise, linear scan is fine here since it's only for display enrichment.
        ClinicADT.MyIterator<Appointment> it2 = sorted.iterator();
        while (it2.hasNext()) {
            Appointment a = it2.next();
            if (a.getAppointmentId() == apptId) return a;
        }
        return null;
    }

    private void searchByMethod() {
        Payment.PaymentMethod m = pickMethod();
        if (m == null) return;

        ClinicADT<PaymentControl.PaymentRecord> all = control.loadAllRecords();
        ClinicADT<PaymentControl.PaymentRecord> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<PaymentControl.PaymentRecord> it = all.iterator();
        while (it.hasNext()) sorted.add(it.next());

        ClinicADT.MyComparator<PaymentControl.PaymentRecord> comparator =
            new ClinicADT.MyComparator<PaymentControl.PaymentRecord>() {
                @Override
                public int compare(PaymentControl.PaymentRecord a, PaymentControl.PaymentRecord b) {
                    if (a.method == null && b.method == null) return 0;
                    if (a.method == null) return 1;
                    if (b.method == null) return -1;
                    return a.method.compareTo(b.method);
                }
            };
        sorted.sort(comparator);

        PaymentControl.PaymentRecord key = new PaymentControl.PaymentRecord();
        key.method = m;
        int idx = ((MyClinicADT<PaymentControl.PaymentRecord>) sorted).search(key, comparator);

        MyClinicADT<PaymentControl.PaymentRecord> out = new MyClinicADT<>();
        if (idx >= 0) {
            int first = idx;
            while (first > 0 && sorted.get(first - 1).method == m) first--;
            int last = idx;
            while (last < sorted.size() - 1 && sorted.get(last + 1).method == m) last++;
            for (int i = first; i <= last; i++) out.add(sorted.get(i));
        }
        printRecordListCentered("Payments by " + m.getDisplayName(), out);
    }

    private void searchByDate() {
        System.out.print("Date (YYYY-MM-DD): ");
        String s = sc.nextLine().trim();
        LocalDate d;
        try { d = LocalDate.parse(s); } catch (Exception e) { System.out.println("Bad date."); return; }

        ClinicADT<PaymentControl.PaymentRecord> all = control.loadAllRecords();
        ClinicADT<PaymentControl.PaymentRecord> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<PaymentControl.PaymentRecord> it = all.iterator();
        while (it.hasNext()) sorted.add(it.next());

        ClinicADT.MyComparator<PaymentControl.PaymentRecord> comparator =
            new ClinicADT.MyComparator<PaymentControl.PaymentRecord>() {
                @Override
                public int compare(PaymentControl.PaymentRecord a, PaymentControl.PaymentRecord b) {
                    LocalDate da = a.dateTime.toLocalDate();
                    LocalDate db = b.dateTime.toLocalDate();
                    return da.compareTo(db);
                }
            };
        sorted.sort(comparator);

        PaymentControl.PaymentRecord key = new PaymentControl.PaymentRecord();
        key.dateTime = d.atStartOfDay();
        int idx = ((MyClinicADT<PaymentControl.PaymentRecord>) sorted).search(key, comparator);

        MyClinicADT<PaymentControl.PaymentRecord> out = new MyClinicADT<>();
        if (idx >= 0) {
            int first = idx;
            while (first > 0 && sorted.get(first - 1).dateTime.toLocalDate().equals(d)) first--;
            int last = idx;
            while (last < sorted.size() - 1 && sorted.get(last + 1).dateTime.toLocalDate().equals(d)) last++;
            for (int i = first; i <= last; i++) out.add(sorted.get(i));
        }
        printRecordListCentered("Payments on " + d, out);
    }

    // ===== Option 4: Reports (5 features) =====
    private void reportMenu() {
        int c;
        do {
            System.out.println("\n=== Reports ===");
            System.out.println("1. Daily Transactions (pick date)");
            System.out.println("2. Patient Payment History (by Patient ID)");
            System.out.println("3. Payment Method Comparison (bar chart)");
            System.out.println("4. Date Range Summary");
            System.out.println("5. Top Spenders (by total amount)");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            try { c = Integer.parseInt(sc.nextLine().trim()); } catch (Exception e) { c = -1; }

            switch (c) {
                case 1 -> reportDaily();
                case 2 -> reportPatientHistory();
                case 3 -> reportMethodBarChart();
                case 4 -> reportDateRangeSummary();
                case 5 -> reportTopSpenders();
                case 0 -> {}
                default -> System.out.println("Invalid choice.");
            }
        } while (c != 0);
    }

    private void reportDaily() {
        System.out.print("Date (YYYY-MM-DD): ");
        LocalDate d;
        try { d = LocalDate.parse(sc.nextLine().trim()); } catch (Exception e) { System.out.println("Bad date."); return; }
        Report.printHeader("Daily Transaction Report");
        ClinicADT<PaymentControl.PaymentRecord> all = control.loadAllRecords();
        MyClinicADT<PaymentControl.PaymentRecord> out = new MyClinicADT<>();
        double total = 0.0;
        int count = 0;

        ClinicADT.MyIterator<PaymentControl.PaymentRecord> it = all.iterator();
        while (it.hasNext()) {
            PaymentControl.PaymentRecord r = it.next();
            if (r.dateTime.toLocalDate().equals(d)) {
                out.add(r);
                total += r.total;
                count++;
            }
        }

        printRecordListCentered("Daily Transactions - " + d, out);
        System.out.println(Report.center(String.format("Total Transactions: %d", count)));
        System.out.println(Report.center(String.format("       Grand Total        : RM %.2f", total)));
        Report.printFooter();
    }

    private void reportPatientHistory() {
        System.out.print("Patient ID: ");
        String pid = sc.nextLine().trim();
        ClinicADT<PaymentControl.PaymentRecord> all = control.loadAllRecords();
        MyClinicADT<PaymentControl.PaymentRecord> out = new MyClinicADT<>();
        double total = 0.0;

        ClinicADT.MyIterator<PaymentControl.PaymentRecord> it = all.iterator();
        while (it.hasNext()) {
            PaymentControl.PaymentRecord r = it.next();
            if (r.patientId.equalsIgnoreCase(pid)) {
                out.add(r);
                total += r.total;
            }
        }
        Report.printHeader("Patient Payment History");
        printRecordListCentered("Payment History - Patient " + pid, out);
        System.out.println(Report.center(String.format("Total Paid: RM %.2f", total)));
        Report.printFooter();
    }

    private void reportMethodBarChart() {
        Report.printHeader("Payment Method Comparison Report");
        ClinicADT<PaymentControl.PaymentRecord> all = control.loadAllRecords();
        int m = Payment.PaymentMethod.values().length;
        String[] labels = new String[m];
        int[] counts = new int[m];

        for (int i = 0; i < m; i++) labels[i] = Payment.PaymentMethod.values()[i].name();

        ClinicADT.MyIterator<PaymentControl.PaymentRecord> it = all.iterator();
        while (it.hasNext()) {
            PaymentControl.PaymentRecord r = it.next();
            for (int i = 0; i < m; i++) if (r.method == Payment.PaymentMethod.values()[i]) { counts[i]++; break; }
        }
        renderVerticalBarChart("Payment Method Comparison (Count)", labels, counts, 10);
        Report.printFooter();
    }

    private void reportDateRangeSummary() {
        System.out.print("Start date (YYYY-MM-DD): ");
        LocalDate s, e;
        try { s = LocalDate.parse(sc.nextLine().trim()); } catch (Exception ex) { System.out.println("Bad date."); return; }
        System.out.print("End date (YYYY-MM-DD): ");
        try { e = LocalDate.parse(sc.nextLine().trim()); } catch (Exception ex) { System.out.println("Bad date."); return; }
        Report.printHeader("Report Date Range Summary");

        if (e.isBefore(s)) { System.out.println("End before start."); return; }

        ClinicADT<PaymentControl.PaymentRecord> all = control.loadAllRecords();
        int count = 0;
        double total = 0.0;

        ClinicADT.MyIterator<PaymentControl.PaymentRecord> it = all.iterator();
        while (it.hasNext()) {
            PaymentControl.PaymentRecord r = it.next();
            LocalDate d = r.dateTime.toLocalDate();
            if ((d.isAfter(s) || d.equals(s)) && (d.isBefore(e) || d.equals(e))) {
                count++;
                total += r.total;
            }
        }
        System.out.println(Report.center("Date Range Summary"));
        System.out.println(Report.center("-------------------"));
        System.out.println(Report.center("From: " + s + "   To: " + e));
        System.out.println(Report.center(String.format("Transactions: %d", count)));
        System.out.println(Report.center(String.format("Grand Total : RM %.2f", total)));
        Report.printFooter();
    }

    private void reportTopSpenders() {
        Report.printHeader("Top Spender Report");
        ClinicADT<PaymentControl.PaymentRecord> all = control.loadAllRecords();

        MyClinicADT<String> keys   = new MyClinicADT<>();
        MyClinicADT<String> names  = new MyClinicADT<>();
        MyClinicADT<String> ids    = new MyClinicADT<>();
        MyClinicADT<Double> totals = new MyClinicADT<>();

        double unknownTotal = 0.0;
        int unknownCount = 0;

        ClinicADT.MyIterator<PaymentControl.PaymentRecord> it = all.iterator();
        while (it.hasNext()) {
            PaymentControl.PaymentRecord r = it.next();

            String pid = (r.patientId == null) ? "" : r.patientId.trim();
            String pname = (r.patientName == null) ? "" : r.patientName.trim();

            String key, dispName, dispId;

            if (!pid.isEmpty()) {
                key = "ID:" + pid;
                dispName = pname.isEmpty() ? pid : pname;
                dispId = pid;
            } else if (!pname.isEmpty()) {
                key = "N:" + pname;
                dispName = pname;
                dispId = "-";
            } else {
                unknownTotal += r.total;
                unknownCount++;
                continue;
            }

            int idx = indexOf(keys, key);
            if (idx < 0) {
                keys.add(key);
                names.add(dispName);
                ids.add(dispId);
                totals.add(r.total);
            } else {
                totals.set(idx, totals.get(idx) + r.total);
            }
        }

        int n = keys.size();
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;

        int limit = Math.min(10, n);
        for (int i = 0; i < limit; i++) {
            int best = i;
            for (int j = i + 1; j < n; j++) {
                if (totals.get(order[j]) > totals.get(order[best])) best = j;
            }
            int tmp = order[i]; order[i] = order[best]; order[best] = tmp;
        }

        String line = "+----+----------------------+------------+--------------+";
        String hdr  = "| No | Patient Name         | Patient ID | Total (RM)   |";
        String row  = "| %-2d | %-20s | %-10s | %12.2f |";
        System.out.println(Report.center(line));
        System.out.println(Report.center(hdr));
        System.out.println(Report.center(line));

        int printed = 0;
        for (int k = 0; k < limit; k++) {
            int idx = order[k];
            System.out.println(Report.center(String.format(
                    row, (printed + 1), cut(names.get(idx), 20), cut(ids.get(idx), 10), totals.get(idx)
            )));
            printed++;
        }

        if (printed == 0 && unknownCount > 0) {
            System.out.println(Report.center(String.format(row, 1, "(unknown)", "-", unknownTotal)));
            printed = 1;
        }

        System.out.println(Report.center(line));

        if (unknownCount > 0 && printed > 0) {
            System.out.println(Report.center(String.format(
                "(Note: %d legacy receipt%s without patient info totaling RM %.2f were omitted from ranking.)",
                unknownCount, (unknownCount == 1 ? "" : "s"), unknownTotal
            )));
        }
        Report.printFooter();
    }

    // ====== Selection & inference helpers ======
    private Appointment pickPendingPaymentAppointment() {
        ClinicADT<Appointment> pending = new MyClinicADT<>();
        ClinicADT.MyIterator<Appointment> it = apptCtrl.getAll().iterator();
        while (it.hasNext()) {
            Appointment a = it.next();
            if (a.getStatus() == Appointment.AppointmentStatus.PENDING_PAYMENT) pending.add(a);
        }
        if (pending.size() == 0) {
            System.out.println("No patients in PENDING PAYMENT.");
            return null;
        }

        String line = "+----+----------+------------+----------------------+----------------------+---------------------+";
        String hdr  = "| No | Appt ID  | Patient ID | Patient Name         | Doctor               | Scheduled Time      |";
        String row  = "| %-2d | %-8d | %-10s | %-20s | %-20s | %-19s |";

        System.out.println("\nPatients Pending Payment:");
        System.out.println(line); System.out.println(hdr); System.out.println(line);
        for (int i = 0; i < pending.size(); i++) {
            Appointment a = pending.get(i);
            System.out.println(String.format(row,
                    (i + 1),
                    a.getAppointmentId(),
                    a.getPatientId(),
                    cut(a.getPatientName(),20),
                    cut(a.getDoctorName(),20),
                    a.getScheduledDateTime().format(DTF)));
        }
        System.out.println(line);

        int sel;
        while (true) {
            System.out.print("Select No. (0=cancel): ");
            try {
                sel = Integer.parseInt(sc.nextLine().trim());
                if (sel == 0) return null;
                if (sel >= 1 && sel <= pending.size()) break;
            } catch (Exception ignored) {}
            System.out.println("Invalid selection.");
        }
        return pending.get(sel - 1);
    }

    /** Heuristic: treatment considered if a MedicalTreatment exists for the same patient
        on/after appt time OR same calendar day. */
    private boolean inferTreatmentFromRecords(Appointment a) {
        if (treatments == null || treatments.size() == 0) return false;
        LocalDate apptDay = a.getScheduledDateTime().toLocalDate();
        LocalDateTime apptTs = a.getScheduledDateTime();

        ClinicADT.MyIterator<MedicalTreatment> it = treatments.iterator();
        while (it.hasNext()) {
            MedicalTreatment t = it.next();
            if (!t.getPatientId().equalsIgnoreCase(a.getPatientId())) continue;
            if (t.getTreatmentDateTime().toLocalDate().isEqual(apptDay)) return true;
            if (!t.getTreatmentDateTime().isBefore(apptTs)) return true;
        }
        return false;
    }
    
    // ===== Payment flow helpers =====

    private Payment.PaymentMethod selectPaymentMethod() {
        System.out.println("\n=== Select Payment Method ===");
        Payment.PaymentMethod[] methods = control.getAvailablePaymentMethods();
        for (int i = 0; i < methods.length; i++) System.out.printf("%d. %s%n", i + 1, methods[i].getDisplayName());
        while (true) {
            System.out.print("Select payment method (1-" + methods.length + "): ");
            try {
                int choice = Integer.parseInt(sc.nextLine().trim());
                Payment.PaymentMethod selected = control.getPaymentMethodByChoice(choice);
                if (selected != null) {
                    System.out.printf("Selected: %s%n", selected.getDisplayName());
                    return selected;
                }
            } catch (Exception ignored) {}
            System.out.println("Invalid choice.\n");
        }
    }
    
    private Payment.PaymentMethod pickMethod() {
        
        Payment.PaymentMethod[] methods = control.getAvailablePaymentMethods();
        System.out.println("\n=== Pick Payment Method ===");
        for (int i = 0; i < methods.length; i++) System.out.printf("%d. %s%n", i + 1, methods[i].getDisplayName());
        while (true) {
            System.out.print("Choice (1-" + methods.length + ", 0=cancel): ");
            try {
                int c = Integer.parseInt(sc.nextLine().trim());
                if (c == 0) return null;
                Payment.PaymentMethod m = control.getPaymentMethodByChoice(c);
                if (m != null) return m;
            } catch (Exception ignored) {}
            System.out.println("Invalid selection.");
        }
    }

    private double processPaymentAmount(Payment.PaymentMethod paymentMethod, double totalAmount) {
        System.out.printf("%nTotal Amount: RM %.2f%n", totalAmount);
        if (paymentMethod == Payment.PaymentMethod.CASH) {
            while (true) {
                System.out.print("Enter cash amount: RM ");
                try {
                    double amountPaid = Double.parseDouble(sc.nextLine().trim());
                    if (amountPaid >= totalAmount) {
                        double change = amountPaid - totalAmount;
                        if (change > 0) System.out.printf("Change: RM %.2f%n", change);
                        System.out.println("Cash payment accepted!");
                        return amountPaid;
                    } else {
                        System.out.printf("Insufficient amount. Need RM %.2f more.%n", totalAmount - amountPaid);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number.\n");
                }
            }
        } else {
            System.out.printf("Amount to be charged: RM %.2f%n", totalAmount);
            System.out.print("Confirm payment? (y/n): ");
            String s = sc.nextLine().trim().toLowerCase();
            if (s.equals("y")) {
                System.out.println("Payment processed successfully!");
                return totalAmount;
            } else {
                return 0.0; // canceled
            }
        }
    } 

    // ====== Common printing helpers ======
    private static String cut(String s, int w) {
        if (s == null) return "";
        if (w <= 0) return "";
        if (s.length() <= w) return s;
        if (w <= 3) return "...".substring(0, w);
        return s.substring(0, w - 3) + "...";
    }

    private static int indexOf(MyClinicADT<String> list, String key) {
        for (int i=0;i<list.size();i++) if (list.get(i).equalsIgnoreCase(key)) return i;
        return -1;
    }

    private void printRecordListCentered(String title, ClinicADT<PaymentControl.PaymentRecord> recs) {
        String line = "+---------------------+----------+------------+----------------------+--------------+-----------------+------------+";
        String hdr  = "| Date/Time           | Receipt  | Appt ID    | Patient Name         | Patient ID   | Method          | Total (RM) |";
        String row  = "| %-19s | %-8s | %-10s | %-20s | %-12s | %-15s | %10.2f |";

        System.out.println();
        System.out.println(Report.center(title));
        System.out.println(Report.center("-".repeat(title.length())));
        System.out.println(Report.center(line));
        System.out.println(Report.center(hdr));
        System.out.println(Report.center(line));

        if (recs == null || recs.size() == 0) {
            String empty = "| (no records)                                                                                 |";
            System.out.println(Report.center(empty));
            System.out.println(Report.center(line));
            return;
        }

        ClinicADT.MyIterator<PaymentControl.PaymentRecord> it = recs.iterator();
        while (it.hasNext()) {
            PaymentControl.PaymentRecord r = it.next();
            System.out.println(Report.center(String.format(row,
                    r.dateTime.toString().replace('T',' '),
                    cut(r.receiptId,8),
                    (r.apptId < 0 ? "-" : String.valueOf(r.apptId)),
                    cut(r.patientName,20),
                    cut(r.patientId,12),
                    (r.method == null ? "-" : r.method.name()),
                    r.total
            )));
        }
        System.out.println(Report.center(line));
    }

    /** Centered vertical bar chart with a small top cap "-" for each bar. */
    private void renderVerticalBarChart(String title, String[] labels, int[] counts, int maxHeight) {
        if (labels == null || counts == null || labels.length != counts.length || labels.length == 0) return;

        int max = 0; for (int c : counts) if (c > max) max = c;
        if (max == 0) max = 1;
        int unit = (max + maxHeight - 1) / maxHeight;

        final int colW = 12;
        final String bar = "||";
        final String sp  = "  ";

        System.out.println();
        System.out.println(Report.center(title));
        System.out.println(Report.center("-".repeat(title.length())));

        String top = "";
        for (int i = 0; i < counts.length; i++) top += padCenter(String.valueOf(counts[i]), colW);
        System.out.println(Report.center(top));

        String caps = "";
        for (int i = 0; i < counts.length; i++) caps += padCenter("--", colW);
        System.out.println(Report.center(caps));

        for (int h = maxHeight; h >= 1; h--) {
            String r = "";
            for (int i = 0; i < counts.length; i++) {
                int barH = (counts[i] + unit - 1) / unit;
                r += padCenter((barH >= h) ? bar : sp, colW);
            }
            System.out.println(Report.center(r));
        }

        String base = repeat("-", labels.length * colW);
        System.out.println(Report.center(base));

        String labLine = "";
        for (int i = 0; i < labels.length; i++) labLine += padCenter(labels[i], colW);
        System.out.println(Report.center(labLine));
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
        StringBuilder sb = new StringBuilder(n * s.length());
        for (int i=0;i<n;i++) sb.append(s);
        return sb.toString();
    }
}
