package control;

import adt.ClinicADT;
import adt.MyClinicADT;
import entity.Medicine;
import utility.Report;
import utility.Validation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class PharmacyControl {
    private final ClinicADT<Medicine> medicineList = new MyClinicADT<>();

    // === DATA FILES ===
    private final String medicineFilePath = "src/textFile/medicine.txt";
    private final String dispenseLogPath = "src/textFile/dispense_log.txt";
    private final String restockLogPath  = "src/textFile/restock_log.txt";

    public PharmacyControl() { loadFromFile(); }

    // === Fixed table format with consistent column widths ===
    private static final String LINE =
            "+-------+----------------------+----------+----------+------------+--------------------+---------------------------+----------------+";
    private static final String ROWFMT =
            "| %-5s | %-20s | %8s | %-8s | %10s | %-22s | %-25s | %-10s |";

    // ---------- Centering wrappers ----------
    private static void cLine(String s) { Report.cprintln(s); }
    private static void cPrintf(String fmt, Object... args) { Report.cprintf(fmt, args); }

    private void printHeader() {
        cLine(LINE);
        cPrintf(ROWFMT, "ID", "Name", "Quantity", "Unit", "Price(MYR)", "Usage", "Intake/Day", "Expiry");
        cLine(LINE);
    }

    private void printLine() { cLine(LINE); }

    private void printSingleMedicine(Medicine m) {
        printHeader();
        cLine(formatMedicineRow(m));
        printLine();
    }

    // Helper method to format a single medicine row consistently
    private String formatMedicineRow(Medicine m) {
        return String.format(ROWFMT,
                m.getId(),
                fit(m.getName(), 20),
                m.getQuantity(),
                fit(m.getUnit(), 8),
                String.format("%.2f", m.getPricePerUnit()),
                fit(m.getUsage(), 22),
                fit(m.getIntakeMeasurePerDay(), 25),
                fit(m.getExpiration(), 10)
        );
    }

    // ===== Billing/Payment table (reuse the same widths for alignment) =====
    private static final String LINE_BILLING = LINE;
    private static final String ROWFMT_BILLING = ROWFMT;

    public void printBillingHeader() {
        cLine(LINE_BILLING);
        cPrintf(ROWFMT_BILLING, "ID", "Name", "Quantity", "Unit", "Price(MYR)", "Usage", "Intake/Day", "Expiry");
        cLine(LINE_BILLING);
    }

    public void printBillingRow(Medicine m) {
        cLine(formatMedicineRow(m));
    }

    public void printBillingTable() {
        printBillingHeader();
        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) printBillingRow(it.next());
        cLine(LINE_BILLING);
    }

    // --- Add Medicine ---
    public void addMedicine(Medicine med) {
        String nameError = Validation.validateMedicineName(med.getName());
        String qtyError  = Validation.validateMedicineQuantity(med.getQuantity());
        String unitError = Validation.validateMedicineUnit(med.getUnit());
        String usageError = Validation.validateMedicineUsage(med.getUsage());
        String expirationError = Validation.validateMedicineExpiry(med.getExpiration());
        String priceError = (med.getPricePerUnit() < 0) ? "Price per unit must be >= 0." : null;
        String intakeMeasureError = (med.getIntakeMeasurePerDay() == null || med.getIntakeMeasurePerDay().isBlank())
                ? "Intake measure per day is required." : null;

        if (nameError != null || qtyError != null || unitError != null || usageError != null
                || expirationError != null || priceError != null || intakeMeasureError != null) {
            Report.cprintln("Failed to add medicine due to validation errors:");
            if (nameError != null) Report.cprintln(" - " + nameError);
            if (qtyError != null) Report.cprintln(" - " + qtyError);
            if (unitError != null) Report.cprintln(" - " + unitError);
            if (usageError != null) Report.cprintln(" - " + usageError);
            if (expirationError != null) Report.cprintln(" - " + expirationError);
            if (priceError != null) Report.cprintln(" - " + priceError);
            if (intakeMeasureError != null) Report.cprintln(" - " + intakeMeasureError);
            return;
        }

        medicineList.add(med);
        saveToFile();
        Report.cprintln("");
        Report.cprintln("Medicine added successfully!");
        Report.cprintln("");
        printSingleMedicine(med);
    }

    // --- Dispense Medicine ---
    public boolean dispenseMedicineById(String id, int amount) {
        Medicine m = getMedicineById(id);
        if (m != null) {
            String error = Validation.validateDispenseQuantity(m.getQuantity(), amount);
            if (error != null) {
                Report.cprintln(error);
                return false;
            }
            m.setQuantity(m.getQuantity() - amount);
            Report.cprintln(amount + " units of " + m.getName() + " dispensed.");
            saveToFile();
            double unit = m.getPricePerUnit();
            double total = unit * amount;
            appendLog(dispenseLogPath, String.format("%s,%s,%s,%d,%.2f,%.2f",
                    nowIso(), m.getId(), m.getName(), amount, unit, total));
            return true;
        } else {
            Report.cprintln("Medicine not found.");
        }
        return false;
    }

    // --- Restock Medicine ---
    public boolean restockMedicineById(String id, int amount) {
        Medicine m = getMedicineById(id);
        if (m != null) {
            String qtyError = Validation.validateMedicineQuantity(amount);
            if (qtyError != null) {
                Report.cprintln(qtyError);
                return false;
            }
            int before = m.getQuantity();
            m.setQuantity(before + amount);
            System.out.println("Medicine restocked: " + amount + " added to " + m.getName());
            saveToFile();
            appendLog(restockLogPath, String.format("%s,%s,%s,%d,%d",
                    nowIso(), m.getId(), m.getName(), amount, m.getQuantity()));
            return true;
        }
        Report.cprintln("Medicine not found.");
        return false;
    }

    // --- Remove Medicine ---
    public boolean removeMedicineById(String id) {
        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        int index = 0;
        while (it.hasNext()) {
            if (it.next().getId().equalsIgnoreCase(id)) {
                medicineList.remove(index);
                Report.cprintln("Medicine removed: " + id);
                saveToFile();
                return true;
            }
            index++;
        }
        Report.cprintln("Medicine not found.");
        return false;
    }

    // --- Display All Stock (centered, no pagination) ---
    public void displayStock() {
        if (medicineList.isEmpty()) {
            Report.cprintln("No medicines in stock.");
            return;
        }

        printHeader();
        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            Medicine m = it.next();
            cLine(formatMedicineRow(m));
        }
        printLine();
    }

    // --- Print Low Stock and Restock Prompt (centered) ---
    public void printLowStockMedicines(int threshold, Scanner sc) {
        Report.printHeader("Low Stock Report (≤ " + threshold + ")");
        boolean found = false;
        printHeader();

        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            Medicine m = it.next();
            if (m.getQuantity() <= threshold) {
                cLine(formatMedicineRow(m));
                found = true;
            }
        }

        if (!found) {
            cLine(String.format("| %-" + (LINE.length() - 4) + "s |",
                    "All medicines are sufficiently stocked."));
            printLine();
            Report.printFooter();
            return;
        }

        printLine();
        Report.printFooter();

        System.out.print(("Do you want to restock any medicine? (y/n): "));
        String ans = sc.nextLine().trim().toLowerCase();
        if (!ans.equals("y")) return;

        System.out.print(Report.center("Enter Medicine ID to restock: "));
        String id = sc.nextLine().trim().toUpperCase();
        Medicine m = getMedicineById(id);
        if (m == null) {
            Report.cprintln("Invalid Medicine ID.");
            return;
        }

        System.out.print(Report.center("Enter quantity to add: "));
        try {
            int qty = Integer.parseInt(sc.nextLine());
            String qtyError = Validation.validateMedicineQuantity(qty);
            if (qtyError == null) {
                restockMedicineById(id, qty);
            } else {
                Report.cprintln(qtyError);
            }
        } catch (NumberFormatException e) {
            Report.cprintln("Invalid quantity input.");
        }
    }

    // --- All Medicines Sorted by Name (centered) ---
    public void printAllMedicinesSortedByNameReport() {
        Report.printHeader("All Medicines (Sorted by Name)");
        if (medicineList.isEmpty()) {
            Report.cprintln("No medicines available.");
            Report.printFooter();
            return;
        }

        // Copy into a working list
        ClinicADT<Medicine> sorted = new MyClinicADT<>();
        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) sorted.add(it.next());
        sorted.sort(new ClinicADT.MyComparator<Medicine>() {
            @Override
            public int compare(Medicine a, Medicine b) {
                String x = (a == null || a.getName() == null) ? "" : a.getName().trim();
                String y = (b == null || b.getName() == null) ? "" : b.getName().trim();
                return x.compareToIgnoreCase(y);
            }
        });

        cLine(LINE);
        cPrintf(ROWFMT, "ID", "Name", "Quantity", "Unit",
                "Price(MYR)", "Usage", "Intake/Day", "Expiry");
        cLine(LINE);

        ClinicADT.MyIterator<Medicine> sortedIt = sorted.iterator();
        while (sortedIt.hasNext()) {
            cLine(formatMedicineRow(sortedIt.next()));
        }
        cLine(LINE);

        Report.printFooter();
    }

    // --- Expiration Report ---
    public void expirationReport() {
        Report.printHeader("Expiration Report");

        int expiredCount = 0;
        int within6Count = 0;
        int after6Count = 0;

        LocalDate today = LocalDate.now();
        LocalDate sixMonthsLater = today.plusMonths(6);

        // Partition medicines into 3 groups
        ClinicADT<Medicine> expiredMeds = new MyClinicADT<>();
        ClinicADT<Medicine> within6Meds = new MyClinicADT<>();
        ClinicADT<Medicine> after6Meds = new MyClinicADT<>();

        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            Medicine m = it.next();
            try {
                LocalDate expDate = LocalDate.parse(m.getExpiration());

                if (expDate.isBefore(today)) {
                    expiredMeds.add(m); expiredCount++;
                } else if (!expDate.isAfter(sixMonthsLater)) {
                    within6Meds.add(m); within6Count++;
                } else {
                    after6Meds.add(m);  after6Count++;
                }
            } 
            catch (Exception ignored) {
            }
        }

        String border    = "+-------+----------------------+------------+";
        String headerFmt = "| %-5s | %-20s | %-10s |";
        String rowFmt    = "| %-5s | %-20s | %-10s |";

        // Expired
        Report.cprintln("");
        Report.cprintln("[Expired Medicines]");
        if (expiredCount > 0) {
            cLine(border);
            cPrintf(headerFmt, "ID", "Name", "Expiry");
            cLine(border);
            ClinicADT.MyIterator<Medicine> eit = expiredMeds.iterator();
            while (eit.hasNext()) {
                Medicine m = eit.next();
                cLine(String.format(rowFmt, m.getId(), fit(m.getName(), 20), m.getExpiration()));
            }
            cLine(border);
        } else {
            Report.cprintln("No expired medicines found.");
        }

        // Within 6 months
        Report.cprintln("");
        Report.cprintln("[Medicines Expiring Within 6 Months]");
        if (within6Count > 0) {
            cLine(border);
            cPrintf(headerFmt, "ID", "Name", "Expiry");
            cLine(border);
            ClinicADT.MyIterator<Medicine> wit = within6Meds.iterator();
            while (wit.hasNext()) {
                Medicine m = wit.next();
                cLine(String.format(rowFmt, m.getId(), fit(m.getName(), 20), m.getExpiration()));
            }
            cLine(border);
        } else {
            Report.cprintln("No medicines expiring within 6 months.");
        }

        // After 6 months
        Report.cprintln("");
        Report.cprintln("[Medicines Expiring After 6 Months]");
        if (after6Count > 0) {
            cLine(border);
            cPrintf(headerFmt, "ID", "Name", "Expiry");
            cLine(border);
            ClinicADT.MyIterator<Medicine> ait = after6Meds.iterator();
            while (ait.hasNext()) {
                Medicine m = ait.next();
                cLine(String.format(rowFmt, m.getId(), fit(m.getName(), 20), m.getExpiration()));
            }
            cLine(border);
        } else {
            Report.cprintln("No medicines expiring after 6 months.");
        }

        // Summary chart
        printSummaryChart(expiredCount, within6Count, after6Count);
        Report.printFooter();
    }

    // --- Daily Dispense Report (for given date) ---
    public void dailyDispenseReport(LocalDate date) {
        Report.printHeader("Daily Dispense Report (" + date + ")");

        double grandTotal = 0.0;

        String border = "+--------------+----------+----------------------+--------+------------+------------+";
        String rowFmt  = "| %-12s | %-8s | %-20s | %6s | %10s | %10s |";

        cLine(border);
        cPrintf(rowFmt, "Time", "ID", "Name", "Qty", "Unit(MYR)", "Total");
        cLine(border);

        try (BufferedReader br = new BufferedReader(new FileReader(dispenseLogPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length < 6) continue;

                try {
                    LocalDateTime ts = LocalDateTime.parse(p[0]);
                    if (!ts.toLocalDate().equals(date)) continue;

                    String timeStr = ts.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    String id = p[1];
                    String name = p[2];
                    int qty = Integer.parseInt(p[3]);
                    double unit = Double.parseDouble(p[4]);
                    double total = Double.parseDouble(p[5]);

                    grandTotal += total;

                    cLine(String.format(rowFmt,
                            timeStr, id, fit(name, 20), qty,
                            String.format("%.2f", unit), String.format("%.2f", total)));
                } 
                catch (Exception ignored) {
                }
            }
        } catch (IOException ignored) {}

        cLine(border);
        cLine(String.format(rowFmt, "Grand Total", "", "", "", "", String.format("%.2f", grandTotal)));
        cLine(border);

        Report.printFooter();
    }

    // --- Restock Report (last 14 days from today) ---
    public void restockReportLast14Days() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(13);

        Report.printHeader("Restock Report (" + start + " to " + end + ")");

        final String border = "+---------------------+----------+----------------------+--------+----------+";
        final String rowFmt = "| %-19s | %-8s | %-20s | %6s | %8s |";
        final DateTimeFormatter outTs = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        cLine(border);
        cPrintf(rowFmt, "DateTime", "ID", "Name", "Added", "Balance");
        cLine(border);

        int totalAdded = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(restockLogPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length < 5) continue;

                try {
                    LocalDateTime ts = LocalDateTime.parse(p[0]);
                    LocalDate d = ts.toLocalDate();
                    if (d.isBefore(start) || d.isAfter(end)) continue;

                    String id = p[1].trim();
                    String name = p[2].trim();
                    int added = safeInt(p[3]);
                    int after = safeInt(p[4]);

                    totalAdded += added;

                    cLine(String.format(rowFmt,
                            ts.format(outTs), id, fit(name, 20),
                            String.valueOf(added), String.valueOf(after)));
                } catch (Exception ignored) {
                    // skip malformed row
                }
            }
        } catch (IOException ignored) {}

        cLine(border);
        cLine(String.format(rowFmt, "", "", "Total Added", String.valueOf(totalAdded), ""));
        cLine(border);

        Report.printFooter();
    }

    // --- File Operations ---
    private void saveToFile() {
        ensureParentDir(medicineFilePath);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(medicineFilePath), StandardCharsets.UTF_8))) {
            ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
            while (it.hasNext()) {
                Medicine m = it.next();
                writer.printf("%s,%s,%d,%s,%s,%s,%.2f,%s,%s%n",
                        m.getId(), m.getName(), m.getQuantity(), m.getUnit(), m.getUsage(), m.getExpiration(),
                        m.getPricePerUnit(), m.getIntakeMethod(), m.getIntakeMeasurePerDay());
            }
        } catch (IOException e) {
            Report.cprintln("Error saving to file: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        File file = new File(medicineFilePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 9) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    int qty = Integer.parseInt(parts[2].trim());
                    String unit = parts[3].trim();
                    String usage = parts[4].trim();
                    String expiration = parts[5].trim();
                    double price = Double.parseDouble(parts[6].trim());
                    String intakeMethod = parts[7].trim();
                    String intakeMeasurePerDay = parts[8].trim();

                    medicineList.add(new Medicine(id, name, qty, unit, usage, expiration,
                            price, intakeMethod, intakeMeasurePerDay));
                } else if (parts.length == 6) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    int qty = Integer.parseInt(parts[2].trim());
                    String unit = parts[3].trim();
                    String usage = parts[4].trim();
                    String expiration = parts[5].trim();

                    double price = 0.0;
                    String intakeMethod = "ORAL_AFTER_MEAL";
                    String intakeMeasurePerDay = "1 unit/day";

                    medicineList.add(new Medicine(id, name, qty, unit, usage, expiration,
                            price, intakeMethod, intakeMeasurePerDay));
                }
            }
        } catch (IOException | NumberFormatException e) {
            Report.cprintln("Error loading from file: " + e.getMessage());
        }
    }

    // --- Accessors ---
    public Medicine getMedicineById(String id) {
        ClinicADT.MyIterator<Medicine> it = medicineList.iterator();
        while (it.hasNext()) {
            Medicine m = it.next();
            if (m.getId().equalsIgnoreCase(id)) return m;
        }
        return null;
    }

    public ClinicADT<Medicine> getAllMedicines() { return medicineList; }
    public Medicine getMedicineAt(int index) { return medicineList.get(index); }
    public int getSize() { return medicineList.size(); }
    public boolean isEmpty() { return medicineList.isEmpty(); }

    // --- Summary chart (centered with aligned colons) ---
    private void printSummaryChart(int expired, int within6, int after6) {
        final String title = "Summary Frequency Bar Chart";
        final String underline = "=".repeat(title.length());

        Report.cprintln("");
        Report.cprintln(title);
        Report.cprintln(underline);

        final int labelWidth = 10;   // width before colon
        final int barMaxChars = 40;
        final int rowTargetWidth = labelWidth + 2 + barMaxChars;

        printSummaryRow("Expired",   expired,  labelWidth, barMaxChars, rowTargetWidth);
        printSummaryRow("Within 6M", within6,  labelWidth, barMaxChars, rowTargetWidth);
        printSummaryRow("After 6M",  after6,   labelWidth, barMaxChars, rowTargetWidth);
    }

    private void printSummaryRow(String label, int count, int labelWidth, int barMaxChars, int rowTargetWidth) {
        String bars = "| ".repeat(Math.max(0, count));
        if (bars.length() > barMaxChars) {
            bars = bars.substring(0, Math.max(0, barMaxChars - 3)) + "...";
        }
        String left = String.format("%-" + labelWidth + "s : ", label);
        String row  = left + bars;
        if (row.length() < rowTargetWidth) {
            row = row + " ".repeat(rowTargetWidth - row.length());
        }
        Report.cprintln(row);
    }

    // --- Misc helpers ---
    private static void appendLog(String path, String line) {
        ensureParentDir(path);
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(path, true), StandardCharsets.UTF_8))) {
            pw.println(line);
        } catch (IOException ignored) {}
    }

    private static String nowIso() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // Use ASCII "..." to keep consoles consistent across platforms
    private static String fit(String s, int w) {
        if (s == null) s = "";
        if (s.length() <= w) return s;
        return s.substring(0, Math.max(0, w - 3)) + "...";
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static void ensureParentDir(String path) {
        try {
            File f = new File(path);
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
        } catch (Exception ignored) {}
    }
}
