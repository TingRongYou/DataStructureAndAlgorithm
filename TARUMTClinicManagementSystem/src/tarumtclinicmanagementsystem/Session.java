package tarumtclinicmanagementsystem;

/**
 * Enum to represent the working sessions of a doctor.
 * Each session includes start time, end time, and a recess period.
 */
public enum Session {
    // Session constants: start time, end time, recess start, recess end
    MORNING("08:00", "14:00", "10:30", "11:00"),
    AFTERNOON("14:00", "20:00", "16:30", "17:00"),
    NIGHT("20:00", "08:00", "02:30", "03:00"),
    REST("-", "-", "-", "-"); // Off-duty day

    // Fields
    private final String startTime;
    private final String endTime;
    private final String recessStart;
    private final String recessEnd;

    // Constructor
    Session(String startTime, String endTime, String recessStart, String recessEnd) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.recessStart = recessStart;
        this.recessEnd = recessEnd;
    }

    // Accessors
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getRecessStart() { return recessStart; }
    public String getRecessEnd() { return recessEnd; }

    /**
     * Returns the working period of the session as a string.
     * Example: "08:00-14:00"
     */
    public String getWorkTime() {
        return startTime + "-" + endTime;
    }

    /**
     * Returns the recess period of the session as a string.
     * Example: "10:30-11:00"
     */
    public String getRecessTime() {
        return recessStart + "-" + recessEnd;
    }
}
