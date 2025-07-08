package tarumtclinicmanagementsystem;

/**
 * Team Members: Ting Rong You, Yong Chong Xin, Anson Chang, Lim Wen Liang
 */
public class DoctorControl {
    private MyList<Doctor> doctorList;

    public DoctorControl() {
        doctorList = new MyList<>();
    }

    public void addDoctor(String id, String name, String schedule, boolean available) {
        doctorList.add(new Doctor(id, name, schedule, available));
        System.out.println("Doctor added.");
    }

    public void removeDoctorByIndex(int index) {
        boolean removed = doctorList.remove(index);
        if (removed)
            System.out.println("Doctor removed.");
        else
            System.out.println("Invalid index.");
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
        Doctor doc = doctorList.get(index);
        if (doc != null) {
            doc.setSchedule(newSchedule);
            System.out.println("Schedule updated.");
        } else {
            System.out.println("Doctor not found.");
        }
    }

    public void updateDoctorAvailability(int index, boolean available) {
        Doctor doc = doctorList.get(index);
        if (doc != null) {
            doc.setAvailability(available);
            System.out.println("Availability updated.");
        } else {
            System.out.println("Doctor not found.");
        }
    }

    public int getDoctorCount() {
        return doctorList.size();
    }

    // ✅ Added: Retrieve doctor object safely by index
    public Doctor getDoctorByIndex(int index) {
        if (index < 0 || index >= doctorList.size()) {
            return null;
        }
        return doctorList.get(index);
    }
}
