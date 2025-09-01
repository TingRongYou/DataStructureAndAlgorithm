package entity;

import tarumtclinicmanagementsystem.DutySchedule;

public class Doctor {
    private static int idCounter = 1000;

    private String id;
    private String name;
    private int roomNumber;
    private String gender;
    private String icNumber;
    private String phoneNumber;
    private DutySchedule dutySchedule;

    // Constructor for new doctor with auto-generated ID
    public Doctor(String name, int roomNumber, String gender, String icNumber, String phoneNumber, DutySchedule dutySchedule) {
        this.id = "D" + (idCounter++);
        this.name = name;
        this.roomNumber = roomNumber;
        this.gender = gender;
        this.icNumber = icNumber;
        this.phoneNumber = phoneNumber;
        this.dutySchedule = dutySchedule;
    }

    // Constructor for loading doctor from file with manual ID
    public Doctor(String id, String name, int roomNumber, String gender, String icNumber, String phoneNumber, DutySchedule dutySchedule) {
        this.id = id;
        this.name = name;
        this.roomNumber = roomNumber;
        this.gender = gender;
        this.icNumber = icNumber;
        this.phoneNumber = phoneNumber;
        this.dutySchedule = dutySchedule;

        // Adjust idCounter to avoid duplicates for new doctors
        try {
            int numeric = Integer.parseInt(id.replaceAll("[^0-9]", ""));
            if (numeric >= idCounter) {
                idCounter = numeric + 1;
            }
        } 
        catch (NumberFormatException ignored) {
        }
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getRoomNumber() { return roomNumber; }
    public String getGender() { return gender; }
    public String getIcNumber() { return icNumber; }
    public String getPhoneNumber() { return phoneNumber; }
    public DutySchedule getDutySchedule() { return dutySchedule; }

    // Setters (only phone and duty schedule for now)
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setDutySchedule(DutySchedule dutySchedule) {
        this.dutySchedule = dutySchedule;
    }

    // Check if the doctor is currently on duty
    public boolean isAvailable() {
        return dutySchedule.isOnDutyNow();
    }

    // Text representation for display
    @Override
    public String toString() {
        return "Doctor ID: " + id + ", Name: " + name + ", Room: " + roomNumber;
    }

    // String format for saving to file (no duty schedule, store separately if needed)
    public String toFileString() {
        return id + "," + name + "," + roomNumber + "," + gender + "," + icNumber + "," + phoneNumber;
    }
}
