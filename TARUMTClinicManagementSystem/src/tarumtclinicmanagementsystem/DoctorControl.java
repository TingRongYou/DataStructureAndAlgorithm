package tarumtclinicmanagementsystem;

import java.util.Comparator;

public class DoctorControl {
    private ClinicADT<Doctor> doctorList;

    public DoctorControl() {
        doctorList = new MyClinicADT<>(); // Now using the unified ADT
    }

    public void addDoctor(String id, String name, String schedule, boolean available) {
        doctorList.add(new Doctor(id, name, schedule, available));
        System.out.println("Doctor added.");
    }

    public void removeDoctorByIndex(int index) {
        try {
            doctorList.remove(index);
            System.out.println("Doctor removed.");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Invalid index.");
        }
    }

    public void displayAllDoctors() {
        if (doctorList.isEmpty()) {
            System.out.println("No doctors found.");
            return;
        }

        for (int i = 0; i < doctorList.size(); i++) {
            System.out.println("[" + i + "] " + doctorList.get(i));
        }
    }

    public void updateDoctorSchedule(int index, String newSchedule) {
        try {
            Doctor doc = doctorList.get(index);
            doc.setSchedule(newSchedule);
            System.out.println("Schedule updated.");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Doctor not found.");
        }
    }

    public void updateDoctorAvailability(int index, boolean available) {
        try {
            Doctor doc = doctorList.get(index);
            doc.setAvailability(available);
            System.out.println("Availability updated.");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Doctor not found.");
        }
    }

    public int getDoctorCount() {
        return doctorList.size();
    }

    public Doctor getDoctorByIndex(int index) {
        if (index < 0 || index >= doctorList.size()) {
            return null;
        }
        return doctorList.get(index);
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
