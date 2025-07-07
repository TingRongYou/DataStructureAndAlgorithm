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
    private String id;
    private String name;
    private String schedule;
    private boolean isAvailable;

    public Doctor(String id, String name, String schedule, boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.schedule = schedule;
        this.isAvailable = isAvailable;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSchedule() {
        return schedule;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public void setAvailability(boolean available) {
        this.isAvailable = available;
    }

    @Override
    public String toString() {
        return "Doctor ID: " + id + ", Name: " + name + ", Schedule: " + schedule +
               ", Available: " + (isAvailable ? "Yes" : "No");
    }
}

