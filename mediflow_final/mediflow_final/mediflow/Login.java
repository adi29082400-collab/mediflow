import java.util.Scanner;

public class Login {

    private final String adminUsername = "admin";
    private final String adminPassword = "1234";

    private String role;

    public boolean authenticate(Scanner sc) {

        System.out.println("====================================");
        System.out.println("          MEDIFLOW LOGIN");
        System.out.println("====================================");

        System.out.println("\n1. Hospital Administrator");
        System.out.println("2. Patient Visit");

        System.out.print("\nSelect option: ");
        String choice = sc.nextLine();

        if (choice.equals("1")) {

            System.out.print("Username: ");
            String user = sc.nextLine();

            System.out.print("Password: ");
            String pass = sc.nextLine();

            if (user.equals(adminUsername) &&
                pass.equals(adminPassword)) {

                role = "ADMIN";

                System.out.println("\nLogin Successful!");
                System.out.println("Role: Hospital Administrator");

                return true;
            }

            System.out.println("\nInvalid Username or Password!");

            return false;
        }

        if (choice.equals("2")) {

            role = "PATIENT";

            System.out.println("\nOpening Patient Visit Portal...");

            return true;
        }

        System.out.println("\nInvalid Option!");

        return false;
    }

    public String getRole() {

        return role;

    }
}
