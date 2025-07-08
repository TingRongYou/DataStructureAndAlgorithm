package tarumtclinicmanagementsystem;

import java.time.LocalDate;
import java.util.Scanner;

public class TreatmentUI {
    private final TreatmentControl control;
    private final Scanner scanner;
    
    public TreatmentUI(){
        this.control = new TreatmentControl();
        this.scanner = new Scanner(System.in);
    }
    
    public void run(){
        int choice;
        do{
            System.out.print("\n=== Medical Treatment Management ===");
            System.out.println("1. Add New Treatment");
            System.out.println("2. View Patient Treatment History");
            System.out.println("3. Process Next Follow-Up");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");
            choice = scanner.nextInt();
            
            try{
                choice = scanner.nextInt();
                scanner.nextInt();
                
                switch (choice) {
                    case 1 -> addTreatment();
                    case 2 -> viewPatientHistory();
                    case 3 -> processFollowUp();
                    default -> System.out.println("Invalid!");
                }
            }catch(Exception e){
                System.out.println("Error! Invalid input. Please try again.");
                scanner.nextLine(); 
                choice = -1;
            }
        } while (choice != 0);
    }
    
    private void addTreatment() {
        try{
            System.out.print("Enter Patient ID: ");
            String patientId = scanner.nextLine();
            System.out.print("Enter Diagnosis: ");
            String diagnosis = scanner.nextLine();
            System.out.print("Prescription: ");
            String prescription = scanner.nextLine();
            System.out.print("Follow-up needed? (yes/no): "); //changed the true/false
            boolean isFollowUp = scanner.nextBoolean();
            scanner.nextLine();

            MedicalTreatment treatment = new MedicalTreatment(
                "T" + System.currentTimeMillis(), // generate ID
                patientId, "D001", diagnosis, prescription, LocalDate.now(), isFollowUp
            ); //doctorId to be linked later 
            control.addTreatment(treatment);
            System.out.println("Treatment added.");
        } catch(Exception e){
            System.out.println("Error! " + e.getMessage());
        }
    }
    
    public void viewPatientHistory(){
        System.out.print("Patient ID: ");
        String patientId = scanner.nextLine();
        DynamicArray<MedicalTreatment> treatments = control.getTreatmentsByPatient(patientId);
        
        if(treatments.isEmpty()){
            System.out.println("No treatment record found.");
        } else{
            System.out.println("\n=== Treatment History ===");
            for(int i = 0; i < treatments.size(); i++){
                System.out.println(treatments.get(i));
            }
        }
    }
    private void processFollowUp(){
        try{
            MedicalTreatment nextFollowUp = control.processNextFollowUp();
            if(nextFollowUp == null){
                System.out.println("No follow-ups in queue.");
            }
            else{
                System.out.println("Processing follow-up: " + nextFollowUp);
            }
        }catch (Exception e){
            System.out.println("Error! " + e.getMessage());
        }
    }
}
