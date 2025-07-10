package tarumtclinicmanagementsystem;

<<<<<<< HEAD
/**
 * Team Members: Ting Rong You, Yong Chong Xin, Anson Chang, Lim Wen Liang
 */

import java.time.DayOfWeek;
=======
import java.util.Comparator;
>>>>>>> 799e9b2fc56aec04e669a1dff14c09088c36bc85

public class DoctorControl {
    private ClinicADT<Doctor> doctorList;

    public DoctorControl() {
        doctorList = new MyClinicADT<>();
    }

    // ✅ Updated method without manual availability
    public void addDoctor(String name, int room, String gender, String icNumber, String phoneNum) {
        if (room < 1 || room > 10) {
            System.out.println("Room must be between 1 and 10.");
            return;
        }

        if (isRoomOccupied(room)) {
            System.out.println("Room already occupied.");
            return;
        }

        DutySchedule schedule = generateWeeklyShift(doctorList.size());
        Doctor doctor = new Doctor(name, room, gender, icNumber, phoneNum, schedule);
        doctorList.add(doctor);
        System.out.println("Doctor registered:");
        System.out.println(doctor);
        schedule.printScheduleTable(name);
    }




    private boolean isRoomOccupied(int roomNumber) {
        for (int i = 0; i < doctorList.size(); i++) {
            if (doctorList.get(i).getRoomNumber() == roomNumber) {
                return true;
            }
        }
        return false;
    }

    private DutySchedule generateWeeklyShift(int index) {
        DutySchedule schedule = new DutySchedule();
        DayOfWeek[] days = DayOfWeek.values();
        Session[] shifts = { Session.MORNING, Session.AFTERNOON, Session.NIGHT };

        int rest1 = index % 7;
        int rest2 = (rest1 + 3) % 7;

        for (int i = 0; i < 7; i++) {
            DayOfWeek day = days[i];
            if (i == rest1 || i == rest2) {
                schedule.setDaySession(day, Session.REST);
            } else {
                Session shift = shifts[(index + i) % 3];
                schedule.setDaySession(day, shift);
            }
        }

        return schedule;
    }

   public void displayAllDoctors() {
        if (doctorList.isEmpty()) {
            System.out.println("No doctors available.");
            return;
        }

        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
        System.out.printf("| %-10s | %-14s | %-6s | %-10s | %-10s | %-16s | %-12s |\n",
                          "Doctor ID", "Name", "Room", "Available", "Gender", "IC Number", "Phone");
        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");

        for (int i = 0; i < doctorList.size(); i++) {
            Doctor doc = doctorList.get(i);
            System.out.printf("| %-10s | %-14s | %-6d | %-10s | %-10s | %-16s | %-12s |\n",
                              doc.getId(), doc.getName(), doc.getRoomNumber(),
                              doc.isAvailable() ? "Yes" : "No",
                              doc.getGender(), doc.getIcNumber(), doc.getPhoneNumber());
        }

        System.out.println("+------------+----------------+--------+------------+------------+------------------+--------------+");
    }



    public Doctor getDoctorByIndex(int index) {
        if (index >= 0 && index < doctorList.size()) {
            return doctorList.get(index);
        }
        return null;
    }

   public void displayDoctorScheduleTable(int index) {
        Doctor doctor = getDoctorByIndex(index);
        if (doctor != null) {
            System.out.println();
            doctor.getDutySchedule().printScheduleTable(doctor.getName());
        } else {
            System.out.println("Invalid doctor index.");
        }
    }


    public int getDoctorCount() {
        return doctorList.size();
    }
    
    public Doctor getDoctorById(String id) {
        for (int i = 0; i < doctorList.size(); i++) {
            if (doctorList.get(i).getId().equalsIgnoreCase(id)) {
                return doctorList.get(i);
            }
        }
        return null;
    }

    // ✅ Report 1: Display All Doctors Sorted by Name
    public void printDoctorsSortedByName() {
        if (doctorList.isEmpty()) {
            System.out.println("No doctors available.");
            return;
        }

        ClinicADT<Doctor> sorted = new MyClinicADT<>();
        for (int i = 0; i < doctorList.size(); i++) {
            sorted.add(doctorList.get(i));
        }

        sorted.sort(new Comparator<Doctor>() {
            @Override
            public int compare(Doctor d1, Doctor d2) {
                return d1.getName().compareToIgnoreCase(d2.getName());
            }
        });

        System.out.println("=== Doctor List (Sorted by Name) ===");
        for (int i = 0; i < sorted.size(); i++) {
            System.out.println(sorted.get(i));
        }
    }

    // ✅ Report 2: Display Only Available Doctors
    public void printAvailableDoctors() {
        boolean found = false;
        System.out.println("=== Available Doctors ===");
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor doc = doctorList.get(i);
            if (doc.isAvailable()) {
                System.out.println(doc);
                found = true;
            }
        }
        if (!found) {
            System.out.println("No doctors are currently available.");
        }
    }
}

