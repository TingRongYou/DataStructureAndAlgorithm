package entity;

public class Patient {
    private static int idCounter = 1000;

    private final String id;
    private final String name;
    private final int age;
    private final String gender;
    private final String icNumber; // --- New field for IC Number ---
    private final String contact;

    // Constructor for new patient (modified to include icNumber)
    public Patient(String name, int age, String gender, String icNumber, String contact) {
        this.id = "P" + (idCounter++);
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.icNumber = icNumber; // Initialize new field
        this.contact = contact;
    }

    // Constructor for loading from file (modified to include icNumber)
    public Patient(String id, String name, int age, String gender, String icNumber, String contact) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.icNumber = icNumber; // Initialize new field
        this.contact = contact;
    }

    public static void setIdCounter(int nextId) {
        idCounter = nextId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getIcNumber() { return icNumber; } // --- New getter for IC Number ---
    public String getContact() { return contact; }

    @Override
    public String toString() {
        return "Patient ID: " + id + ", Name: " + name + ", Age: " + age +
               ", Gender: " + gender + ", IC Number: " + icNumber + ", Contact: " + contact;
    }

    // Modified to include icNumber in the file string
    public String toFileString() {
        return id + "," + name + "," + age + "," + gender + "," + icNumber + "," + contact;
    }
}