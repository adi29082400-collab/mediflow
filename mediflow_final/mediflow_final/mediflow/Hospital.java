import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Hospital {

    public void addPatient(Patient patient) {

        String checkQuery =
                "SELECT id FROM patients WHERE id = ?";

        String insertQuery =
                "INSERT INTO patients " +
                "(id, name, age, gender, disease, phone, address, medical_history, allergies) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection con = DatabaseConnection.getConnection();

        if (con == null) {
            System.out.println("\nDatabase Connection Failed!");
            return;
        }

        try (
            PreparedStatement checkStmt =
                    con.prepareStatement(checkQuery)
        ) {

            checkStmt.setInt(1, patient.getId());

            try (ResultSet rs = checkStmt.executeQuery()) {

                if (rs.next()) {
                    System.out.println("\nPatient ID already exists!");
                    return;
                }
            }

            try (
                PreparedStatement insertStmt =
                        con.prepareStatement(insertQuery)
            ) {

                insertStmt.setInt(1, patient.getId());
                insertStmt.setString(2, patient.getName());
                insertStmt.setInt(3, patient.getAge());
                insertStmt.setString(4, patient.getGender());
                insertStmt.setString(5, patient.getDisease());
                insertStmt.setString(6, patient.getPhoneNumber());
                insertStmt.setString(7, patient.getAddress());
                insertStmt.setString(8, patient.getMedicalHistory());
                insertStmt.setString(9, patient.getAllergies());

                insertStmt.executeUpdate();

                System.out.println("\nPatient Added Successfully!");
            }

        } catch (SQLException e) {

            System.out.println("\nDatabase Error!");
            e.printStackTrace();

        } finally {

            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void viewPatients() {

        String query = "SELECT * FROM patients";

        Connection con = DatabaseConnection.getConnection();

        if (con == null) {
            System.out.println("\nDatabase Connection Failed!");
            return;
        }

        try (
            PreparedStatement stmt =
                    con.prepareStatement(query);
            ResultSet rs =
                    stmt.executeQuery()
        ) {

            System.out.println(
                    "\n================================================================================================================"
            );

            System.out.printf(
                    "%-8s %-18s %-6s %-10s %-15s %-15s %-20s %-25s %-20s%n",
                    "ID",
                    "NAME",
                    "AGE",
                    "GENDER",
                    "DISEASE",
                    "PHONE",
                    "ADDRESS",
                    "MEDICAL HISTORY",
                    "ALLERGIES"
            );

            System.out.println(
                    "================================================================================================================"
            );

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.printf(
                        "%-8d %-18s %-6d %-10s %-15s %-15s %-20s %-25s %-20s%n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("gender"),
                        rs.getString("disease"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("medical_history"),
                        rs.getString("allergies")
                );
            }

            System.out.println(
                    "================================================================================================================"
            );

            if (!found) {
                System.out.println("\nNo Patients Found!");
            }

        } catch (SQLException e) {

            System.out.println("\nDatabase Error!");
            e.printStackTrace();

        } finally {

            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void searchPatient(int id) {

        String query =
                "SELECT * FROM patients WHERE id = ?";

        Connection con = DatabaseConnection.getConnection();

        if (con == null) {
            System.out.println("\nDatabase Connection Failed!");
            return;
        }

        try (
            PreparedStatement stmt =
                    con.prepareStatement(query)
        ) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    System.out.println("\n===== PATIENT FOUND =====");

                    System.out.println(
                            "ID                : " +
                            rs.getInt("id")
                    );

                    System.out.println(
                            "Name              : " +
                            rs.getString("name")
                    );

                    System.out.println(
                            "Age               : " +
                            rs.getInt("age")
                    );

                    System.out.println(
                            "Gender            : " +
                            rs.getString("gender")
                    );

                    System.out.println(
                            "Disease           : " +
                            rs.getString("disease")
                    );

                    System.out.println(
                            "Phone             : " +
                            rs.getString("phone")
                    );

                    System.out.println(
                            "Address           : " +
                            rs.getString("address")
                    );

                    System.out.println(
                            "Medical History   : " +
                            rs.getString("medical_history")
                    );

                    System.out.println(
                            "Allergies         : " +
                            rs.getString("allergies")
                    );

                } else {

                    System.out.println("\nPatient Not Found!");
                }
            }

        } catch (SQLException e) {

            System.out.println("\nDatabase Error!");
            e.printStackTrace();

        } finally {

            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void searchPatient(String name, String phoneNumber) {

        String query =
                "SELECT * FROM patients " +
                "WHERE name = ? AND phone = ?";

        Connection con = DatabaseConnection.getConnection();

        if (con == null) {
            System.out.println("\nDatabase Connection Failed!");
            return;
        }

        try (
            PreparedStatement stmt =
                    con.prepareStatement(query)
        ) {

            stmt.setString(1, name);
            stmt.setString(2, phoneNumber);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    System.out.println("\n===== PATIENT FOUND =====");

                    System.out.println(
                            "ID                : " +
                            rs.getInt("id")
                    );

                    System.out.println(
                            "Name              : " +
                            rs.getString("name")
                    );

                    System.out.println(
                            "Age               : " +
                            rs.getInt("age")
                    );

                    System.out.println(
                            "Gender            : " +
                            rs.getString("gender")
                    );

                    System.out.println(
                            "Disease           : " +
                            rs.getString("disease")
                    );

                    System.out.println(
                            "Phone             : " +
                            rs.getString("phone")
                    );

                    System.out.println(
                            "Address           : " +
                            rs.getString("address")
                    );

                    System.out.println(
                            "Medical History   : " +
                            rs.getString("medical_history")
                    );

                    System.out.println(
                            "Allergies         : " +
                            rs.getString("allergies")
                    );

                } else {

                    System.out.println("\nPatient Not Found!");
                }
            }

        } catch (SQLException e) {

            System.out.println("\nDatabase Error!");
            e.printStackTrace();

        } finally {

            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void updatePatient(
            int id,
            String name,
            int age,
            String gender,
            String disease,
            String phoneNumber,
            String address,
            String medicalHistory,
            String allergies) {

        String query =
                "UPDATE patients " +
                "SET name = ?, " +
                "age = ?, " +
                "gender = ?, " +
                "disease = ?, " +
                "phone = ?, " +
                "address = ?, " +
                "medical_history = ?, " +
                "allergies = ? " +
                "WHERE id = ?";

        Connection con = DatabaseConnection.getConnection();

        if (con == null) {
            System.out.println("\nDatabase Connection Failed!");
            return;
        }

        try (
            PreparedStatement stmt =
                    con.prepareStatement(query)
        ) {

            stmt.setString(1, name);
            stmt.setInt(2, age);
            stmt.setString(3, gender);
            stmt.setString(4, disease);
            stmt.setString(5, phoneNumber);
            stmt.setString(6, address);
            stmt.setString(7, medicalHistory);
            stmt.setString(8, allergies);
            stmt.setInt(9, id);

            int rows = stmt.executeUpdate();

            if (rows > 0) {

                System.out.println(
                        "\nPatient Updated Successfully!"
                );

            } else {

                System.out.println(
                        "\nPatient Not Found!"
                );
            }

        } catch (SQLException e) {

            System.out.println("\nDatabase Error!");
            e.printStackTrace();

        } finally {

            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void deletePatient(int id) {

        String query =
                "DELETE FROM patients WHERE id = ?";

        Connection con = DatabaseConnection.getConnection();

        if (con == null) {
            System.out.println("\nDatabase Connection Failed!");
            return;
        }

        try (
            PreparedStatement stmt =
                    con.prepareStatement(query)
        ) {

            stmt.setInt(1, id);

            int rows = stmt.executeUpdate();

            if (rows > 0) {

                System.out.println(
                        "\nPatient Deleted Successfully!"
                );

            } else {

                System.out.println(
                        "\nPatient Not Found!"
                );
            }

        } catch (SQLException e) {

            System.out.println("\nDatabase Error!");
            e.printStackTrace();

        } finally {

            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
