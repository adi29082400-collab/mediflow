import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        Hospital hospital = new Hospital();
        Login login = new Login();

        if (!login.authenticate(sc)) {
            sc.close();
            return;
        }

        String role = login.getRole();

        if (role.equals("ADMIN")) {

            adminMenu(sc, hospital);

        }

        else if (role.equals("PATIENT")) {

            patientMenu();

        }

        sc.close();
    }

    private static void adminMenu(
            Scanner sc,
            Hospital hospital) {

        int choice;

        do {

            System.out.println("\n====================================");
            System.out.println("        MEDIFLOW HOSPITAL");
            System.out.println("     ADMINISTRATOR DASHBOARD");
            System.out.println("====================================");

            System.out.println("1. Add Patient");
            System.out.println("2. View Patients");
            System.out.println("3. Search Patient");
            System.out.println("4. Update Patient");
            System.out.println("5. Delete Patient");
            System.out.println("6. Exit");

            System.out.println("====================================");

            System.out.print("Enter your choice: ");

            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:

                    System.out.println(
                            "\n========== ADD PATIENT =========="
                    );

                    System.out.print("Enter patient ID: ");

                    int id = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Enter patient name: ");

                    String name = sc.nextLine();

                    int age;

                    while (true) {

                        System.out.print("Enter patient age: ");

                        age = sc.nextInt();
                        sc.nextLine();

                        if (age > 0 && age <= 120) {
                            break;
                        }

                        System.out.println(
                                "Invalid Age! Please enter a value between 1 and 120."
                        );
                    }

                    String gender;

                    while (true) {

                        System.out.print(
                                "Enter patient gender (Male/Female/Other): "
                        );

                        gender = sc.nextLine();

                        if (
                                gender.equalsIgnoreCase("Male") ||
                                gender.equalsIgnoreCase("Female") ||
                                gender.equalsIgnoreCase("Other")
                        ) {
                            break;
                        }

                        System.out.println(
                                "Invalid Gender! Please enter Male, Female or Other."
                        );
                    }

                    System.out.print("Enter patient disease: ");

                    String disease = sc.nextLine();

                    String phone;

                    while (true) {

                        System.out.print(
                                "Enter patient phone number: "
                        );

                        phone = sc.nextLine();

                        if (phone.matches("\\d{10}")) {
                            break;
                        }

                        System.out.println(
                                "Invalid Phone Number! Enter exactly 10 digits."
                        );
                    }

                    System.out.print("Enter patient address: ");

                    String address = sc.nextLine();

                    System.out.print("Enter previous medical history: ");

                    String medicalHistory = sc.nextLine();

                    System.out.print("Enter patient allergies: ");

                    String allergies = sc.nextLine();

                    Patient patient = new Patient(
                            id,
                            name,
                            age,
                            gender,
                            disease,
                            phone,
                            address,
                            medicalHistory,
                            allergies
                    );

                    hospital.addPatient(patient);

                    break;

                case 2:

                    hospital.viewPatients();

                    break;

                case 3:

                    System.out.println(
                            "\n========== SEARCH PATIENT =========="
                    );

                    System.out.println("1. Search by Patient ID");
                    System.out.println("2. Search by Name + Phone");

                    System.out.print("Enter your choice: ");

                    int searchChoice = sc.nextInt();
                    sc.nextLine();

                    if (searchChoice == 1) {

                        System.out.print(
                                "Enter Patient ID to Search: "
                        );

                        int searchId = sc.nextInt();
                        sc.nextLine();

                        hospital.searchPatient(searchId);

                    } else if (searchChoice == 2) {

                        System.out.print(
                                "Enter Patient Name: "
                        );

                        String searchName = sc.nextLine();

                        System.out.print(
                                "Enter Patient Phone Number: "
                        );

                        String searchPhone = sc.nextLine();

                        hospital.searchPatient(
                                searchName,
                                searchPhone
                        );

                    } else {

                        System.out.println(
                                "\nInvalid Search Option!"
                        );
                    }

                    break;

                case 4:

                    System.out.println(
                            "\n========== UPDATE PATIENT =========="
                    );

                    System.out.print(
                            "Enter Patient ID to Update: "
                    );

                    int updateId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Enter New Name: ");

                    String newName = sc.nextLine();

                    int newAge;

                    while (true) {

                        System.out.print("Enter New Age: ");

                        newAge = sc.nextInt();
                        sc.nextLine();

                        if (newAge > 0 && newAge <= 120) {
                            break;
                        }

                        System.out.println(
                                "Invalid Age! Enter a value between 1 and 120."
                        );
                    }

                    String newGender;

                    while (true) {

                        System.out.print(
                                "Enter New Gender (Male/Female/Other): "
                        );

                        newGender = sc.nextLine();

                        if (
                                newGender.equalsIgnoreCase("Male") ||
                                newGender.equalsIgnoreCase("Female") ||
                                newGender.equalsIgnoreCase("Other")
                        ) {
                            break;
                        }

                        System.out.println(
                                "Invalid Gender! Please enter Male, Female or Other."
                        );
                    }

                    System.out.print("Enter New Disease: ");

                    String newDisease = sc.nextLine();

                    String newPhone;

                    while (true) {

                        System.out.print(
                                "Enter New Phone Number: "
                        );

                        newPhone = sc.nextLine();

                        if (newPhone.matches("\\d{10}")) {
                            break;
                        }

                        System.out.println(
                                "Invalid Phone Number! Enter exactly 10 digits."
                        );
                    }

                    System.out.print("Enter New Address: ");

                    String newAddress = sc.nextLine();

                    System.out.print(
                            "Enter New Previous Medical History: "
                    );

                    String newMedicalHistory = sc.nextLine();

                    System.out.print("Enter New Allergies: ");

                    String newAllergies = sc.nextLine();

                    hospital.updatePatient(
                            updateId,
                            newName,
                            newAge,
                            newGender,
                            newDisease,
                            newPhone,
                            newAddress,
                            newMedicalHistory,
                            newAllergies
                    );

                    break;

                case 5:

                    System.out.println(
                            "\n========== DELETE PATIENT =========="
                    );

                    System.out.print(
                            "Enter Patient ID to Delete: "
                    );

                    int deleteId = sc.nextInt();
                    sc.nextLine();

                    hospital.deletePatient(deleteId);

                    break;

                case 6:

                    System.out.println(
                            "\nThank you for using MediFlow!"
                    );

                    break;

                default:

                    System.out.println(
                            "\nInvalid Choice! Please select 1-6."
                    );
            }

        } while (choice != 6);
    }

    private static void patientMenu() {

        System.out.println("\n====================================");
        System.out.println("        MEDIFLOW PATIENT VISIT");
        System.out.println("====================================");

        System.out.println("Patient Visit Portal");
        System.out.println();

        System.out.println("Hospital Information");
        System.out.println("Doctor Information");
        System.out.println("Disease Information");
        System.out.println("Contact Information");
        System.out.println("Book Appointment");
        System.out.println("Check Appointment Status");

        System.out.println("====================================");

        System.out.println(
                "\nPatient portal will be connected to the web interface next."
        );
    }
}
