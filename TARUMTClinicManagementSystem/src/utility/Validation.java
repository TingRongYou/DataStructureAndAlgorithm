package utility;

import control.PharmacyControl;
import static java.awt.SystemColor.control;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class Validation {
    private static final Pattern IC_PATTERN = Pattern.compile("^\\d{6}-\\d{2}-\\d{4}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01\\d{8,9}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z ]+$");
    private static final Pattern MEDICINE_UNIT_PATTERN = Pattern.compile("^(mg|ml|g)$", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Generic validations
    public static boolean isNullOrEmpty(String input) {
        return input == null || input.trim().isEmpty();
    }

    public static boolean isPositiveNumber(int number) {
        return number > 0;
    }

    public static boolean isInRange(int number, int min, int max) {
        return number >= min && number <= max;
    }

    // Specific domain validations
    public static boolean isValidIC(String icNumber) {
        return !isNullOrEmpty(icNumber) && IC_PATTERN.matcher(icNumber).matches();
    }

    public static boolean isValidPhone(String phoneNumber) {
        return !isNullOrEmpty(phoneNumber) && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    public static boolean isValidName(String name) {
        return !isNullOrEmpty(name) && NAME_PATTERN.matcher(name).matches();
    }

    public static boolean isValidGender(String gender) {
        return !isNullOrEmpty(gender) && (gender.equalsIgnoreCase("M") || gender.equalsIgnoreCase("F"));
    }

    public static boolean isValidAge(int age) {
        return isInRange(age, 1, 150);
    }

    public static boolean isValidRoomNumber(int room) {
        return isInRange(room, 1, 10);
    }

    public static boolean isValidMedicineUnit(String unit) {
        return !isNullOrEmpty(unit) && MEDICINE_UNIT_PATTERN.matcher(unit).matches();
    }

    public static boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isFutureDateTime(LocalDateTime dateTime) {
        return dateTime.isAfter(LocalDateTime.now());
    }

    //specific validations 
    public static boolean isWorkingHour(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        // Working hours: 8-12, 13-17, 18-22 (skip lunch 12-13)
        return (hour >= 8 && hour < 12) || 
               (hour >= 13 && hour < 17) || 
               (hour >= 18 && hour < 22);
    }
    
    public static boolean isValidPatientId(String id){
        // P followed by 4 digits (e.i. P1001)
        return !isNullOrEmpty(id) && id.matches("^P\\d{4}$");
    }
    
    public static boolean isValidDoctorId(String id){
        return !isNullOrEmpty(id) && id.matches("^D\\d{4}$"); 
    }
    
    public static boolean isValidTreatmentId(String id){
        return !isNullOrEmpty(id) && id.matches("^T\\\\d{5}$");
    }

    // Validations with error messages
    public static String validateMalaysianIC(String icNumber) {
        if (isNullOrEmpty(icNumber)) return "IC number cannot be empty";
        if (!isValidIC(icNumber)) return "Invalid IC format. Please use YYMMDD-XX-XXXX";
        if (!isValidMalaysianIC(icNumber)) return "Invalid IC number. Date portion is not valid";
        return null;
    }
    
    public static boolean isValidMalaysianIC(String icNumber) {
        if (!isValidIC(icNumber)) return false;

        try {
            // Extract birth date parts
            String[] parts = icNumber.split("-");
            int year = Integer.parseInt(parts[0].substring(0, 2));
            int month = Integer.parseInt(parts[0].substring(2, 4));
            int day = Integer.parseInt(parts[0].substring(4, 6));

            // Validate date components
            if (month < 1 || month > 12) return false;
            if (day < 1 || day > 31) return false;

            // Additional checks for specific months
            if ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) {
                return false;
            }
            if (month == 2) {
                // Simple leap year check 
                boolean isLeap = (year % 4 == 0);
                if (day > (isLeap ? 29 : 28)) return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String validatePhone(String phoneNumber) {
        if (isNullOrEmpty(phoneNumber)) return "Phone number cannot be empty";
        if (!isValidPhone(phoneNumber)) return "Invalid phone number. Use Malaysian format (01xxxxxxxx or 01xxxxxxxxx)";
        return null;
    }

    public static String validateName(String name) {
        if (isNullOrEmpty(name)) return "Name cannot be empty";
        if (!isValidName(name)) return "Name can only contain letters and spaces";
        return null;
    }

    public static String validateGender(String gender) {
        if (isNullOrEmpty(gender)) return "Gender cannot be empty";
        if (!isValidGender(gender)) return "Gender must be M or F";
        return null;
    }

    public static String validateAge(int age) {
        if (!isValidAge(age)) return "Age must be between 1 and 150";
        return null;
    }

    public static String validateAgeAndICConsistency(int age, String icNumber) {
        int currentYear = Year.now().getValue(); // e.g., 2025
        int expectedBirthYear = currentYear - age;
        int expectedYY = expectedBirthYear % 100;

        int birthYearLastTwo = Integer.parseInt(icNumber.substring(0, 2));

        if (birthYearLastTwo != expectedYY) {
            return String.format(
                "Age %d does not match IC birth year (Expected YY=%02d, but got %02d)",
                age, expectedYY, birthYearLastTwo
            );
        }
        return null; // Valid
    }


    public static String validateRoomNumber(int room) {
        if (!isValidRoomNumber(room)) return "Room number must be between 1 and 10";
        return null;
    }

    public static String validateDate(String dateStr) {
        if (isNullOrEmpty(dateStr)) return "Date cannot be empty";
        if (!isValidDate(dateStr)) return "Invalid date format. Please use yyyy-MM-dd";
        return null;
    }

    public static String validateDateTime(LocalDateTime dateTime) {
        if (!isFutureDateTime(dateTime)) return "Date/time must be in the future";
        if (!isWorkingHour(dateTime)) return "Date/time must be during working hours (8AM-12PM, 1PM-5PM, 6PM-10PM)";
        return null;
    }
    
    public static String validatePatientId(String id){
        if(isNullOrEmpty(id)) return "Patient ID cannot be empty";
        if(!isValidPatientId(id)) return "Invalid Patient ID format. Should be in PXXX format";
        return null;
    }
    
    public static String validateDoctorId(String id){
        if(isNullOrEmpty(id)) return "Doctor ID cannot be empty";
        if(!isValidDoctorId(id)) return "Invalid Doctor ID format. Should be in PXXX format";
        return null;
    }
    
    public static String validateTreatmentId(String id){
        if (isNullOrEmpty(id)) return "Treatment ID cannot be empty";
        if (!isValidTreatmentId(id)) return "Invalid Treatment ID format. Should be TXXXXX digit.";
        return null;
    }
    
    public static String validateFutureDateTime(LocalDateTime dateTime){
        if (dateTime.isBefore(LocalDateTime.now())) return "Date/time must be in future.";
        return null;
    }
    
    public static String validateWorkingHours(LocalDateTime dateTime){
        int hour = dateTime.getHour();
        if (!((hour >= 8 && hour < 12) || (hour >= 13 && hour < 17) || (hour >= 18 && hour < 22)))
            return "Must be during working hours (8AM-12PM, 1PM-5PM, 6PM-10PM)";
        return null;
    }
    
    public static String validateMedicineName(String name){
        if (isNullOrEmpty(name)) return "Medicine name cannot be empty";
        if (!name.matches("^[a-zA-Z][a-zA-Z0-9 -]*$"))
            return "Name can only contain letters, numbers, spaces and hyphens";
        if (name.length() > 50) return "Name cannot exceed 50 characters";
        return null;
    }
    
    public static String validateMedicineQuantity(int quantity){
        if (!isPositiveNumber(quantity)) return "Quantity must be positive";
        if (quantity > 1000) return "Quantity cannot exceed 1000 units";
        return null;
    }
    
    public static String validateMedicineUnit(String unit) {
        if (isNullOrEmpty(unit)) return "Unit cannot be empty";
        if (!MEDICINE_UNIT_PATTERN.matcher(unit).matches()) 
            return "Unit must be mg, ml, or g";
        return null;
    }
    
    public static String validateMedicineUsage(String usage){
        if (isNullOrEmpty(usage)) return "Usage instructions cannot be empty";
        if (usage.length() > 100) return "Usage cannot exceed 100 characters";
        return null;
    }
    
    public static String validateDispenseQuantity(int currentStock, int requested){
        if (!isPositiveNumber(requested)) return "Quantity must be positive";
        if (requested > currentStock) return "Cannot dispense more than current stock (" + currentStock + ")";
        return null;
    }
}
