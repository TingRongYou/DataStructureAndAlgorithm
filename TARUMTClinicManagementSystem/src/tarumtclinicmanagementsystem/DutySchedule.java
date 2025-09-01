package tarumtclinicmanagementsystem;

import adt.ClinicADT;
import adt.MyClinicADT;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public class DutySchedule {

    /** Simple pair of (day, session). */
    private static final class DaySession {
        final DayOfWeek day;
        final Session session;
        DaySession(DayOfWeek day, Session session) {
            this.day = day;
            this.session = session;
        }
    }

    private final ClinicADT<DaySession> entries = new MyClinicADT<>();

    /** Replace or add the session for a given day. */
    public void setDaySession(DayOfWeek day, Session session) {
        int idx = indexOf(day);
        DaySession ds = new DaySession(day, session);
        if (idx >= 0) {
            entries.set(idx, ds);
        } else {
            entries.add(ds);
        }
    }

    /** Get session for a day, defaulting to REST if none set. */
    public Session getSessionForDay(DayOfWeek day) {
        int idx = indexOf(day);
        return (idx >= 0) ? entries.get(idx).session : Session.REST;
    }

    /**Checks if the doctor is currently on duty (considering recess time). */
    public boolean isOnDutyNow() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now = LocalTime.now();

        Session session = getSessionForDay(today);
        if (session == null || session == Session.REST) return false;

        try {
            LocalTime start       = LocalTime.parse(session.getStartTime());
            LocalTime end         = LocalTime.parse(session.getEndTime());
            LocalTime recessStart = LocalTime.parse(session.getRecessStart());
            LocalTime recessEnd   = LocalTime.parse(session.getRecessEnd());

            // In-shift (handles overnight shifts where end < start)
            boolean inShift = (start.isBefore(end))
                    ? !now.isBefore(start) && !now.isAfter(end)
                    : now.isAfter(start) || now.isBefore(end);

            // In-recess (handles overnight recess where recessEnd < recessStart)
            boolean inRecess = (recessStart.isBefore(recessEnd))
                    ? !now.isBefore(recessStart) && !now.isAfter(recessEnd)
                    : now.isAfter(recessStart) || now.isBefore(recessEnd);

            return inShift && !inRecess;
        } catch (Exception e) {
            return false;
        }
    }

    /** Pretty table, same output as before. */
    public void printScheduleTable(String doctorName) {
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now().withNano(0);
        DayOfWeek today = currentDate.getDayOfWeek();

        System.out.println("+======================================================================================+");
        System.out.printf("| SCHEDULE FOR DR. %-16s ON %-10s | DATE: %-10s | TIME: %-9s |\n",
                doctorName.toUpperCase(), today, currentDate, currentTime);
        System.out.println("+-------------+--------------+--------------------+------------------+-----------------+");
        System.out.printf("| %-11s | %-12s | %-18s | %-16s | %-15s |\n",
                "DAY", "SESSION", "WORK TIME", "RECESS TIME", "ON DUTY NOW");
        System.out.println("+-------------+--------------+--------------------+------------------+-----------------+");

        for (DayOfWeek d : DayOfWeek.values()) {
            Session s = getSessionForDay(d);
            String workTime   = (s == Session.REST) ? "---" : s.getWorkTime();
            String recessTime = (s == Session.REST) ? "---" : s.getRecessTime();
            String onDuty = (d == today) ? (isOnDutyNow() ? "YES" : "NO") : "-";

            System.out.printf("| %-11s | %-12s | %-18s | %-16s | %-15s |\n",
                    d, s.name(), workTime, recessTime, onDuty);
        }

        System.out.println("+-------------+--------------+--------------------+------------------+-----------------+");
    }

    // -------- internals --------

    /** Find index of the entry for a given day using iterator. */
    private int indexOf(DayOfWeek day) {
        int index = 0;
        ClinicADT.MyIterator<DaySession> it = entries.iterator();
        while (it.hasNext()) {
            DaySession ds = it.next();
            if (ds.day == day) {
                return index;
            }
            index++;
        }
        return -1;
    }
}
