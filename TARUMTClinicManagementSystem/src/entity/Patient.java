package entity;

public class Patient {
    private static int idCounter = 1000;

    private final String id;
    private final String name;
    private final int age;
    private final String gender;
    private final String icNumber; // IC Number (e.g., NRIC or MyKad)
    private final String contact;

    // Constructor for creating a new patient (auto-generated ID)
    public Patient(String name, int age, String gender, String icNumber, String contact) {
        this.id = "P" + (idCounter++);
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.icNumber = icNumber;
        this.contact = contact;
    }

    // Constructor for loading a patient from file (uses stored ID)
    public Patient(String id, String name, int age, String gender, String icNumber, String contact) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.icNumber = icNumber;
        this.contact = contact;

        // Ensure idCounter stays ahead of the highest existing ID
        try {
            int numeric = Integer.parseInt(id.substring(1)); // Extract number from "P###"
            if (numeric >= idCounter) {
                idCounter = numeric + 1;
            }
        } catch (NumberFormatException ignored) {}
    }

    // Static method to manually set/reset ID counter (for testing or file restoration)
    public static void setIdCounter(int nextId) {
        idCounter = nextId;
    }

    // === Getters ===
    public String getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getIcNumber() { return icNumber; }
    public String getContact() { return contact; }

    // === Display Formatting ===
    @Override
    public String toString() {
        return "Patient ID: " + id + ", Name: " + name + ", Age: " + age +
               ", Gender: " + gender + ", IC Number: " + icNumber + ", Contact: " + contact;
    }

    // Format for saving to file
    public String toFileString() {
        return id + "," + name + "," + age + "," + gender + "," + icNumber + "," + contact;
    }
}
