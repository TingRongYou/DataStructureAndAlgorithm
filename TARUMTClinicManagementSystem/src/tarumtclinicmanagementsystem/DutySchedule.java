package tarumtclinicmanagementsystem;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;

public class DutySchedule {
    private EnumMap<DayOfWeek, Session> schedule;

    public DutySchedule() {
        schedule = new EnumMap<>(DayOfWeek.class);
    }

    // Set session for a specific day
    public void setDaySession(DayOfWeek day, Session session) {
        schedule.put(day, session);
    }

    // Get session for a day, defaulting to REST if none set
    public Session getSessionForDay(DayOfWeek day) {
        return schedule.getOrDefault(day, Session.REST);
    }

    // ✅ Checks if the doctor is currently on duty (considering recess time)
    public boolean isOnDutyNow() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now = LocalTime.now();

        Session session = schedule.get(today);
        if (session == null || session == Session.REST) return false;

        try {
            LocalTime start = LocalTime.parse(session.getStartTime());
            LocalTime end = LocalTime.parse(session.getEndTime());
            LocalTime recessStart = LocalTime.parse(session.getRecessStart());
            LocalTime recessEnd = LocalTime.parse(session.getRecessEnd());

            boolean inShift = (start.isBefore(end))
                    ? !now.isBefore(start) && !now.isAfter(end)
                    : now.isAfter(start) || now.isBefore(end); // overnight shift

            boolean inRecess = (recessStart.isBefore(recessEnd))
                    ? !now.isBefore(recessStart) && !now.isAfter(recessEnd)
                    : now.isAfter(recessStart) || now.isBefore(recessEnd); // overnight recess

            return inShift && !inRecess;
        } catch (Exception e) {
            return false;
        }
    }

    // Prints the doctor's duty schedule as a table
    public void printScheduleTable(String doctorName) {
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now().withNano(0);
        DayOfWeek today = currentDate.getDayOfWeek();

        // Table Header
        System.out.println("+=============================================================================================+");
        System.out.printf("| SCHEDULE FOR DR. %-28s ON %-10s | DATE: %-10s | TIME: %-8s |\n",
                doctorName.toUpperCase(), today, currentDate, currentTime);
        System.out.println("+-------------+--------------+--------------------+------------------+-----------------+");
        System.out.printf("| %-11s | %-12s | %-18s | %-16s | %-15s |\n",
                "DAY", "SESSION", "WORK TIME", "RECESS TIME", "ON DUTY NOW");
        System.out.println("+-------------+--------------+--------------------+------------------+-----------------+");

        for (DayOfWeek day : DayOfWeek.values()) {
            Session session = schedule.getOrDefault(day, Session.REST);
            String workTime = session == Session.REST ? "---" : session.getWorkTime();
            String recessTime = session == Session.REST ? "---" : session.getRecessTime();

            String onDuty = "-";
            if (day == today) {
                onDuty = isOnDutyNow() ? "YES" : "NO";
            }

            System.out.printf("| %-11s | %-12s | %-18s | %-16s | %-15s |\n",
                    day, session.name(), workTime, recessTime, onDuty);
        }

        System.out.println("+-------------+--------------+--------------------+------------------+-----------------+");
    }
}
