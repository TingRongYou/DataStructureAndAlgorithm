/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author User
 */
public class Test {
    public static void main(String[] args) {
    try {
        System.out.println("=== Clinic Consultation Management System ===");
        ConsultationUI ui = new ConsultationUI();
        ui.run();
    } catch (Exception e) {
        System.err.println("Fatal error: " + e.getMessage());
    }
}

}
