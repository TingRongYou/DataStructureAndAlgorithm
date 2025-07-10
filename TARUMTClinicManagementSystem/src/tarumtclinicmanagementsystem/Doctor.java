/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author Acer
 */


public class Doctor {
    private static int idCounter = 1000;

    private String id;
    private String name;
    private int roomNumber;
    private String gender;
    private String icNumber;
    private String phoneNumber;
    private DutySchedule dutySchedule;

    public Doctor(String name, int roomNumber, String gender, String icNumber, String phoneNumber, DutySchedule dutySchedule) {
        this.id = "D" + (idCounter++);
        this.name = name;
        this.roomNumber = roomNumber;
        this.gender = gender;
        this.icNumber = icNumber;
        this.phoneNumber = phoneNumber;
        this.dutySchedule = dutySchedule;
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
}






