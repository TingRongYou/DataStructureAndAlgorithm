/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

public class Doctor {
    private static int idCounter = 1000;

    private String id;
    private String name;
    private int roomNumber;
    private String gender;
    private String icNumber;
    private String phoneNumber;
    private DutySchedule dutySchedule;

   // Auto-ID constructor
    public Doctor(String name, int roomNumber, String gender, String icNumber, String phoneNumber, DutySchedule dutySchedule) {
        this.id = "D" + (idCounter++);
        this.name = name;
        this.roomNumber = roomNumber;
        this.gender = gender;
        this.icNumber = icNumber;
        this.phoneNumber = phoneNumber;
        this.dutySchedule = dutySchedule;
    }

    // Manual ID (for loading from file)
    public Doctor(String id, String name, int roomNumber, String gender, String icNumber, String phoneNumber, DutySchedule dutySchedule) {
        this.id = id;
        this.name = name;
        this.roomNumber = roomNumber;
        this.gender = gender;
        this.icNumber = icNumber;
        this.phoneNumber = phoneNumber;
        this.dutySchedule = dutySchedule;

        try {
            int numeric = Integer.parseInt(id.replaceAll("[^0-9]", ""));
            if (numeric >= idCounter) {
                idCounter = numeric + 1;
            }
        } catch (NumberFormatException ignored) {}
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getRoomNumber() { return roomNumber; }
    public String getGender() { return gender; }
    public String getIcNumber() { return icNumber; }
    public String getPhoneNumber() { return phoneNumber; }
    public DutySchedule getDutySchedule() { return dutySchedule; }

    public boolean isAvailable() {
        return dutySchedule.isOnDutyNow();
    }

    @Override
    public String toString() {
        return "Doctor ID: " + id + ", Name: " + name + ", Room: " + roomNumber;
    }

    public String toFileString() {
        return id + "," + name + "," + roomNumber + "," + gender + "," + icNumber + "," + phoneNumber;
    }
}







