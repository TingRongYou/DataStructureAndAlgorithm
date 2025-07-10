/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtclinicmanagementsystem;

/**
 *
 * @author User
 */
/**
 * Enum to represent the working sessions of a doctor.
 * Each session includes start time, end time, and a recess period.
 */
public enum Session {
    // Define session constants with working and recess hours
    MORNING("08:00", "14:00", "10:30", "11:00"),
    AFTERNOON("14:00", "20:00", "16:30", "17:00"),
    NIGHT("20:00", "08:00", "02:30", "03:00"),
    REST("-", "-", "-", "-");
    
    // Fields to hold the session times
    private final String startTime;
    private final String endTime;
    private final String recessStart;
    private final String recessEnd;

    Session(String startTime, String endTime, String recessStart, String recessEnd) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.recessStart = recessStart;
        this.recessEnd = recessEnd;
    }

    // Getters for session components

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getRecessStart() {
        return recessStart;
    }

    public String getRecessEnd() {
        return recessEnd;
    }

    /**
     * Returns the working period of the session as a formatted string.
     * Example: "08:00–14:00"
     */
    public String getWorkTime() {
        return startTime + "-" + endTime;   // Use ASCII hyphen
    }
    /**
     * Returns the recess period as a formatted string.
     * Example: "10:30–11:00"
     */
    public String getRecessTime() {
        return recessStart + "-" + recessEnd; // Use ASCII hyphen
    }
}






