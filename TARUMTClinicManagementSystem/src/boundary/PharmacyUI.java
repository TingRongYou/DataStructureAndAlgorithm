package boundary;

import adt.ClinicADT;
import control.AppointmentControl;
import control.PharmacyControl;
import control.PharmacyQueueControl;
import entity.MedicalTreatment;
import entity.Medicine;
import entity.MedicinePrescription;
import utility.Report;
import utility.Validation;

import java.time.LocalDate;
import java.util.Scanner;

public class PharmacyUI {
    private final PharmacyControl pharmacyControl; 
    private final AppointmentControl appointmentControl;
    private final PharmacyQueueControl queueCtrl;      
    private final Scanner sc = new Scanner(System.in);
    private final ClinicADT<MedicalTreatment> allTreatments;

    public PharmacyUI(PharmacyControl pharmacyControl,
                      PharmacyQueueControl queueCtrl, 
                      AppointmentControl appointmentControl,
                      ClinicADT<MedicalTreatment> allTreatments) {
        this.pharmacyControl   = pharmacyControl;
        this.appointmentControl = appointmentControl;
        this.queueCtrl         = queueCtrl;
        this.allTreatments     = allTreatments;
    }

    // =======================
    // MENUS (NOT CENTERED)
    // =======================
    public void run() {
        int choice;
        do {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("             TARUMT CLINIC PHARMACY MANAGEMENT");
            System.out.println("=".repeat(60));
            System.out.println("1. Add Medicine");
            System.out.println("2. Prepare Dispense Medicine");
            System.out.println("3. View Stock");
            System.out.println("4. Restock Medicine");
            System.out.println("5. Remove Medicine");
            System.out.println("6. Report Features");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            try {
                choice = Integer.parseInt(sc.nextLine().trim());
                switch (choice) {
                    case 1 -> addMedicine();
                    case 2 -> prepareOrDispense();
                    case 3 -> pharmacyControl.displayStock();
                    case 4 -> restockMedicine();
                    case 5 -> removeMedicine();
                    case 6 -> runReportMenu();
                    case 0 -> System.out.println("Exiting Pharmacy Module...");
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                choice = -1;
            }
        } while (choice != 0);
    }
    
    
    public void pharmacistPharmacyMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("            PHARMACIST - PHARMACY");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Add Medicine");
            System.out.println(" 2) Prepare / Dispense Medicine");
            System.out.println(" 3) View Stock");
            System.out.println(" 4) Restock Medicine");
            System.out.println(" 5) Remove Medicine");
            System.out.println(" 0) Back");
            System.out.print("Choice: ");
            int choice; try { choice = Integer.parseInt(sc.nextLine().trim()); } catch (Exception e) { choice = -1; }
            switch (choice) {
                case 1 -> addMedicine();
                case 2 -> prepareOrDispense();
                case 3 -> pharmacyControl.displayStock();
                case 4 -> restockMedicine();
                case 5 -> removeMedicine();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
     
     public void adminPharmacyMenu() {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("              ADMIN - PHARMACY");
            System.out.println("=".repeat(60));
            System.out.println(" 1) Generate Report");
            System.out.println(" 0) Back");
            System.out.print("Choice: ");
            int choice; try { choice = Integer.parseInt(sc.nextLine().trim()); } catch (Exception e) { choice = -1; }
            switch (choice) {
                case 1 -> runReportMenu();
                case 0 -> { System.out.println("Returning..."); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    

    private void runReportMenu() {
        int c;
        do {
            System.out.println("\n=== Report Features ===");
            System.out.println("1. Low Stock Report");
            System.out.println("2. Expiring Medicines Report");
            System.out.println("3. Medicine Inventory Report");
            System.out.println("4. All Medicines (Sorted by Name)");
            System.out.println("5. Daily Dispense Report (Today)");
            System.out.println("6. Restock Report (Last 14 Days)");
            System.out.println("0. Back");
            System.out.print("Choice: ");

            try {
                c = Integer.parseInt(sc.nextLine().trim());
                switch (c) {
                    case 1 -> generateLowStockReport();
                    case 2 -> pharmacyControl.expirationReport();
                    case 3 -> inventoryReport();
                    case 4 -> pharmacyControl.printAllMedicinesSortedByNameReport();
                    case 5 -> pharmacyControl.dailyDispenseReport(LocalDate.now());
                    case 6 -> pharmacyControl.restockReportLast14Days();
                    case 0 -> System.out.println("Returning to main menu...");
                    default -> System.out.println("Invalid choice.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                c = -1;
            }
        } while (c != 0);
    }

    // =======================
    // METHODS
    // =======================
    private void addMedicine() {
        String name, unit, usage, expiration, error;
        int qty;

        do {
            System.out.print("Medicine Name (or 0 to cancel): ");
            name = sc.nextLine().trim();
            if (name.equals("0")) { System.out.println("Operation cancelled."); return; }
            error = Validation.validateMedicineName(name);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        do {
            qty = promptForInt("Quantity (or 0 to cancel): ");
            if (qty == 0) { System.out.println("Operation cancelled."); return; }
            error = Validation.validateMedicineQuantity(qty);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        do {
            System.out.print("Unit (mg/ml/g/tablet/etc.) (or 0 to cancel): ");
            unit = sc.nextLine().trim().toLowerCase();
            if (unit.equals("0")) { System.out.println("Operation cancelled."); return; }
            error = Validation.validateMedicineUnit(unit);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        do {
            System.out.print("Usage / Indication (or 0 to cancel): ");
            usage = sc.nextLine().trim();
            if (usage.equals("0")) { System.out.println("Operation cancelled."); return; }
            error = Validation.validateMedicineUsage(usage);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        do {
            System.out.print("Expiration Date (YYYY-MM-DD) (or 0 to cancel): ");
            expiration = sc.nextLine().trim();
            if (expiration.equals("0")) { System.out.println("Operation cancelled."); return; }
            error = Validation.validateMedicineExpiry(expiration);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        double pricePerUnit;
        while (true) {
            System.out.print("Price per unit (MYR) (or 0 to cancel): ");
            String s = sc.nextLine().trim();
            try {
                pricePerUnit = Double.parseDouble(s);
                if (pricePerUnit == 0.0) { System.out.println("Operation cancelled."); return; }
                if (pricePerUnit < 0) { System.out.println("Price must be >= 0.\n"); continue; }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.\n");
            }
        }

        String intakeMethod = null;
        while (true) {
            System.out.println("Select Intake Method:");
            System.out.println("=====================");
            System.out.println("1. Oral After Meal");
            System.out.println("2. Oral Before Meal");
            System.out.println("3. Oral With Water");
            System.out.println("4. Inhalation");
            System.out.println("5. Topical (Skin)");
            System.out.println("6. Injection");
            System.out.println("7. Ophthalmic (Eye Drops)");
            System.out.print("Choice (0 to cancel): ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "0" -> { System.out.println("Operation cancelled."); return; }
                case "1" -> intakeMethod = "ORAL_AFTER_MEAL";
                case "2" -> intakeMethod = "ORAL_BEFORE_MEAL";
                case "3" -> intakeMethod = "ORAL_WITH_WATER";
                case "4" -> intakeMethod = "INHALATION";
                case "5" -> intakeMethod = "TOPICAL";
                case "6" -> intakeMethod = "INJECTION";
                case "7" -> intakeMethod = "OPHTHALMIC_EYE_DROP";
                default -> { System.out.println("Invalid choice. Please try again.\n"); continue; }
            }
            break;
        }

        String intakeMeasurePerDay;
        if ("OPHTHALMIC_EYE_DROP".equals(intakeMethod)) {
            int drops;
            while (true) {
                System.out.print("How many drops per day? (1-20, or 0 to cancel): ");
                String s = sc.nextLine().trim();
                try {
                    drops = Integer.parseInt(s);
                    if (drops == 0) { System.out.println("Operation cancelled."); return; }
                    if (drops < 1 || drops > 20) { System.out.println("Please enter 1-20.\n"); continue; }
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a whole number.\n");
                }
            }
            intakeMeasurePerDay = drops + " drops/day";
        } else {
            while (true) {
                System.out.print("Intake measure per day (e.g., '1 tablet/day' or just a number) (0 to cancel): ");
                intakeMeasurePerDay = sc.nextLine().trim();
                if (intakeMeasurePerDay.equals("0")) { System.out.println("Operation cancelled."); return; }
                if (intakeMeasurePerDay.isBlank()) { System.out.println("Required.\n"); continue; }
                if (intakeMeasurePerDay.matches("\\d+")) {
                    intakeMeasurePerDay = intakeMeasurePerDay + " unit/day";
                }
                break;
            }
        }

        pharmacyControl.addMedicine(new Medicine(
                name, qty, unit, usage, expiration,
                pricePerUnit, intakeMethod, intakeMeasurePerDay
        ));
    }

    private void prepareOrDispense() {
        queueCtrl.reload();

        ClinicADT<MedicinePrescription> readyQueue = queueCtrl.getReadyQueue();
        if (readyQueue == null || readyQueue.isEmpty()) {
            System.out.println("No prescriptions in the ready queue.");
            return;
        }

        System.out.println("\n=== Pharmacy READY Queue ===");
        String line = "+----+----------------------+------------+----------+----------------------+--------+";
        String hdr  = "| No | Patient              | Patient ID | Med.ID   | Medicine             | Qty    |";
        String row  = "| %-2d | %-20s | %-10s | %-8s | %-20s | %6d |";
        System.out.println(line);
        System.out.println(hdr);
        System.out.println(line);

        int i = 1;
        ClinicADT.MyIterator<MedicinePrescription> it = readyQueue.iterator();
        while (it.hasNext()) {
            MedicinePrescription p = it.next();
            System.out.println(String.format(row,
                    i++,
                    cut(p.getPatientName(),20),
                    p.getPatientId(),
                    p.getMedicineId(),
                    cut(p.getMedicineName(),20),
                    p.getQuantity()
            ));
        }
        System.out.println(line);
        System.out.println("(Total in queue: " + readyQueue.size() + ")");

        System.out.print("Press ENTER to dispense the FRONT prescription, or 0 to cancel: ");
        String in = sc.nextLine().trim();
        if (in.equals("0")) return;

        // Stock check that considers earlier reservations
        MedicinePrescription front = readyQueue.get(0);
        Medicine med   = pharmacyControl.getMedicineById(front.getMedicineId());
        if (med == null) {
            System.out.println("Cannot find medicine in stock: " + front.getMedicineId());
            return;
        }

        int reservedAll = queueCtrl.getReservedQtyForMedicine(front.getMedicineId());
        int reservedOthers = Math.max(0, reservedAll - front.getQuantity());

        int onHand = med.getQuantity();
        int availableForFront = onHand - reservedOthers;

        if (front.getQuantity() > availableForFront) {
            System.out.printf(
                "Blocked: requested %d exceeds available %d after earlier reservations (in-stock: %d, reserved-before: %d).%n",
                front.getQuantity(), Math.max(0, availableForFront), onHand, reservedOthers
            );
            System.out.println("Please restock or reduce the prescribed quantity before this patient reaches the front.");
            return;
        }

        boolean ok = queueCtrl.dispenseFront(this.pharmacyControl);
        if (!ok) {
            System.out.println("Dispense failed (insufficient stock or internal error).");
        }
    }

    private void restockMedicine() {
        if (pharmacyControl.isEmpty()) {
            System.out.println("No medicines available to restock.");
            return;
        }

        displaySimpleList();

        String id;
        Medicine med;
        while(true){
            System.out.print("Enter Medicine ID to restock (or 0 to cancel): ");
            id = sc.nextLine().trim().toUpperCase();
            if (id.equals("0")) { System.out.println("Operation cancelled."); return; }

            med = pharmacyControl.getMedicineById(id);
            if (med == null) System.out.println("Invalid Medicine ID.\n");
            else break;
        }
        int qty;
        String error;
        do {
            qty = promptForInt("Quantity to add (or 0 to cancel): ");
            if (qty == 0) { System.out.println("Operation cancelled."); return; }
            error = Validation.validateMedicineQuantity(qty);
            if (error != null) System.out.println(error + "\n");
        } while (error != null);

        pharmacyControl.restockMedicineById(id, qty);
    }

    private void removeMedicine() {
        if (pharmacyControl.isEmpty()) {
            System.out.println("No medicines to remove.");
            return;
        }

        displaySimpleList();

        String id;
        Medicine med;
        while (true) {
            System.out.print("Enter Medicine ID to remove (or 0 to cancel): ");
            id = sc.nextLine().trim().toUpperCase();
            if (id.equals("0")) { System.out.println("Operation cancelled."); return; }

            med = pharmacyControl.getMedicineById(id);
            if (med == null) System.out.println("Invalid Medicine ID. Please try again.\n");
            else break;
        }
        pharmacyControl.removeMedicineById(id);
    }

    private void generateLowStockReport() {
        while (true) {
            int threshold = promptForInt("Enter low stock threshold (e.g., 5) or 0 to cancel: ");
            if (threshold == 0) { System.out.println("Operation cancelled."); return; }
            if (threshold < 0) { System.out.println("Please enter a positive number or 0 to cancel."); continue; }
            pharmacyControl.printLowStockMedicines(threshold, sc);
            break;
        }
    }

    private int promptForInt(String message) {
        while (true) {
            System.out.print(message);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.\n");
            }
        }
    }

    // Paginated simple list (10 records at a time) — now using Scanner only
    private void displaySimpleList() {
        ClinicADT<Medicine> list = pharmacyControl.getAllMedicines();

        if (list.isEmpty()) {
            System.out.println("No medicines in stock.");
            return;
        }

        final int PAGE_SIZE = 10;
        final String format = "| %-6s | %-20s | %-8s |%n";
        final String line   = "+--------+----------------------+----------+";

        int total = list.size();
        int shown = 0;

        ClinicADT.MyIterator<Medicine> it = list.iterator();

        while (it.hasNext()) {
            System.out.println(line);
            System.out.printf(format, "ID", "Name", "Quantity");
            System.out.println(line);

            int pageCount = 0;
            while (it.hasNext() && pageCount < PAGE_SIZE) {
                Medicine m = it.next();
                System.out.printf(format, m.getId(), m.getName(), m.getQuantity());
                pageCount++;
                shown++;
            }

            System.out.println(line);

            if (shown >= total) break;

            System.out.printf("Showing %d/%d - Press ENTER for next %d, or 'q' to quit: ",
                    shown, total, PAGE_SIZE);
            String s = sc.nextLine();
            if (s != null && s.trim().equalsIgnoreCase("q")) break;
        }
    }

    public void inventoryReport() {
        Report.printHeader("Medicine Inventory Report");
        Report.cprintln("=== Medicine Inventory Report ===");
        pharmacyControl.displayStock();
        Report.printFooter();
    }

    private static String cut(String s, int n){
        if (s == null) return "";
        return (s.length() <= n) ? s : s.substring(0, Math.max(0, n - 1)) + "…";
    }
}
