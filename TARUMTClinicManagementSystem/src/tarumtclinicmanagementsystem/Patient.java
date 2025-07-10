package tarumtclinicmanagementsystem;

public class Patient {
    private static int idCounter = 1000;

    private String name;
    private String id;
    private int age;
    private String gender;
    private String contact;

    public Patient(String name, int age, String gender, String contact) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.contact = contact;
        this.id = generateID();
    }

    private String generateID() {
        return "P" + (idCounter++);
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getContact() {
        return contact;
    }

    @Override
    public String toString() {
        return "Patient ID: " + id +
               ", Name: " + name +
               ", Age: " + age +
               ", Gender: " + gender +
               ", Contact: " + contact;
    }

    public String toFileString() {
        return id + "," + name + "," + age + "," + gender + "," + contact;
    }
}
