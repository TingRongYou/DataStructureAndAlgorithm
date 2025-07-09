package tarumtclinicmanagementsystem;

/**
 * Team Members: Ting Rong You, Yong Chong Xin, Anson Chang, Lim Wen Liang
 */
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
}
