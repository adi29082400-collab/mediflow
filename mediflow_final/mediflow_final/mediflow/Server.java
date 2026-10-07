import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT), 0
        );

        server.createContext("/api/login", Server::handleLogin);
        server.createContext("/api/patients", Server::handlePatients);
        server.createContext("/api/doctors", Server::handleDoctors);

        // MEDICINE AVAILABILITY API
        server.createContext("/api/medicines", Server::handleMedicines);

        server.createContext(
                "/api/appointments",
                Server::handleAppointments
        );

        server.createContext("/", Server::serveWebsite);

        server.setExecutor(null);

        System.out.println("=================================");
        System.out.println("       MEDIFLOW SERVER");
        System.out.println("=================================");
        System.out.println("Server Started Successfully!");
        System.out.println("Open: http://localhost:" + PORT);
        System.out.println("=================================");

        server.start();
    }


    // =========================================================
    // LOGIN
    // =========================================================

    private static void handleLogin(
            HttpExchange exchange)
            throws IOException {

        String method =
                exchange.getRequestMethod();

        if (method.equals("OPTIONS")) {

            sendResponse(
                    exchange,
                    200,
                    "{}"
            );

            return;
        }

        if (!method.equals("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "{\"error\":\"Method not allowed\"}"
            );

            return;
        }

        try {

            String body =
                    readRequestBody(exchange);

            Map<String, String> data =
                    parseFormData(body);

            String username =
                    data.get("username");

            String password =
                    data.get("password");

            if (username == null ||
                    password == null ||
                    username.isEmpty() ||
                    password.isEmpty()) {

                sendResponse(
                        exchange,
                        400,
                        "{\"error\":\"Username and password are required\"}"
                );

                return;
            }

            String query = """
                    SELECT username, role
                    FROM users
                    WHERE username = ?
                    AND password = ?
                    """;

            try (
                    Connection con =
                            DatabaseConnection.getConnection()
            ) {

                if (con == null) {

                    sendResponse(
                            exchange,
                            500,
                            "{\"error\":\"Database connection failed\"}"
                    );

                    return;
                }

                try (
                        PreparedStatement stmt =
                                con.prepareStatement(query)
                ) {

                    stmt.setString(
                            1,
                            username
                    );

                    stmt.setString(
                            2,
                            password
                    );

                    try (
                            ResultSet rs =
                                    stmt.executeQuery()
                    ) {

                        if (rs.next()) {

                            String role =
                                    rs.getString("role");

                            String json =
                                    "{"
                                            + "\"username\":\""
                                            + escapeJson(
                                            rs.getString("username")
                                    )
                                            + "\","
                                            + "\"role\":\""
                                            + escapeJson(role)
                                            + "\""
                                            + "}";

                            sendResponse(
                                    exchange,
                                    200,
                                    json
                            );

                        } else {

                            sendResponse(
                                    exchange,
                                    401,
                                    "{\"error\":\"Invalid username or password\"}"
                            );
                        }
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "{\"error\":\"Server error\"}"
            );
        }
    }


    // =========================================================
    // PATIENT API ROUTER
    // =========================================================

    private static void handlePatients(
            HttpExchange exchange)
            throws IOException {

        String method =
                exchange.getRequestMethod();

        String path =
                exchange
                        .getRequestURI()
                        .getPath();

        if (method.equals("OPTIONS")) {

            sendResponse(
                    exchange,
                    200,
                    "{}"
            );

            return;
        }

        try {

            // GET ALL PATIENTS
            if (method.equals("GET") &&
                    path.equals("/api/patients")) {

                getAllPatients(exchange);

                return;
            }


            // SEARCH PATIENT
            if (method.equals("GET") &&
                    path.equals("/api/patients/search")) {

                searchPatientByNamePhone(exchange);

                return;
            }


            // GET PATIENT BY ID
            if (method.equals("GET") &&
                    path.startsWith("/api/patients/")) {

                String idText =
                        path.substring(
                                "/api/patients/".length()
                        );

                int id =
                        Integer.parseInt(idText);

                getPatient(
                        exchange,
                        id
                );

                return;
            }


            // ADD PATIENT
            if (method.equals("POST") &&
                    path.equals("/api/patients")) {

                addPatient(exchange);

                return;
            }


            // UPDATE PATIENT
            if (method.equals("PUT") &&
                    path.startsWith("/api/patients/")) {

                String idText =
                        path.substring(
                                "/api/patients/".length()
                        );

                int id =
                        Integer.parseInt(idText);

                updatePatient(
                        exchange,
                        id
                );

                return;
            }


            // DELETE PATIENT
            if (method.equals("DELETE") &&
                    path.startsWith("/api/patients/")) {

                String idText =
                        path.substring(
                                "/api/patients/".length()
                        );

                int id =
                        Integer.parseInt(idText);

                deletePatient(
                        exchange,
                        id
                );

                return;
            }


            sendResponse(
                    exchange,
                    404,
                    "{\"error\":\"Patient API endpoint not found\"}"
            );

        } catch (NumberFormatException e) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Invalid patient ID\"}"
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "{\"error\":\"Server error\"}"
            );
        }
    }


    // =========================================================
    // ADD PATIENT
    // =========================================================

    private static void addPatient(
            HttpExchange exchange)
            throws Exception {

        String body =
                readRequestBody(exchange);

        Map<String, String> data =
                parseFormData(body);

        String idText =
                data.get("id");

        String name =
                data.get("name");

        String ageText =
                data.get("age");

        String gender =
                data.get("gender");

        String disease =
                data.get("disease");

        String phone =
                data.get("phone");

        String address =
                data.get("address");

        String medicalHistory =
                data.get("medicalHistory");

        String allergies =
                data.get("allergies");


        if (idText == null ||
                idText.trim().isEmpty()) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Patient ID is required\"}"
            );

            return;
        }


        if (name == null ||
                name.trim().isEmpty()) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Patient name is required\"}"
            );

            return;
        }


        if (ageText == null ||
                ageText.trim().isEmpty()) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Patient age is required\"}"
            );

            return;
        }


        int id;
        int age;

        try {

            id =
                    Integer.parseInt(
                            idText.trim()
                    );

            age =
                    Integer.parseInt(
                            ageText.trim()
                    );

        } catch (NumberFormatException e) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Invalid patient ID or age\"}"
            );

            return;
        }


        if (id <= 0) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Patient ID must be greater than 0\"}"
            );

            return;
        }


        if (age <= 0 ||
                age > 120) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Patient age must be between 1 and 120\"}"
            );

            return;
        }


        String checkQuery =
                "SELECT id FROM patients WHERE id = ?";


        String insertQuery = """
                INSERT INTO patients
                (
                    id,
                    name,
                    age,
                    gender,
                    disease,
                    phone,
                    address,
                    medical_history,
                    allergies
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;


        try (
                Connection con =
                        DatabaseConnection.getConnection()
        ) {

            if (con == null) {

                sendResponse(
                        exchange,
                        500,
                        "{\"error\":\"Database connection failed\"}"
                );

                return;
            }


            try (
                    PreparedStatement stmt =
                            con.prepareStatement(checkQuery)
            ) {

                stmt.setInt(
                        1,
                        id
                );

                try (
                        ResultSet rs =
                                stmt.executeQuery()
                ) {

                    if (rs.next()) {

                        sendResponse(
                                exchange,
                                409,
                                "{\"error\":\"Patient ID already exists\"}"
                        );

                        return;
                    }
                }
            }


            try (
                    PreparedStatement stmt =
                            con.prepareStatement(insertQuery)
            ) {

                stmt.setInt(
                        1,
                        id
                );

                stmt.setString(
                        2,
                        name
                );

                stmt.setInt(
                        3,
                        age
                );

                stmt.setString(
                        4,
                        gender
                );

                stmt.setString(
                        5,
                        disease
                );

                stmt.setString(
                        6,
                        phone
                );

                stmt.setString(
                        7,
                        address
                );

                stmt.setString(
                        8,
                        medicalHistory
                );

                stmt.setString(
                        9,
                        allergies
                );

                stmt.executeUpdate();
            }
        }


        sendResponse(
                exchange,
                201,
                "{"
                        + "\"message\":\"Patient added successfully\","
                        + "\"id\":"
                        + id
                        + "}"
        );
    }


    // =========================================================
    // GET ALL PATIENTS
    // =========================================================

    private static void getAllPatients(
            HttpExchange exchange)
            throws Exception {

        String query =
                "SELECT * FROM patients";

        StringBuilder json =
                new StringBuilder("[");

        try (
                Connection con =
                        DatabaseConnection.getConnection();

                PreparedStatement stmt =
                        con.prepareStatement(query);

                ResultSet rs =
                        stmt.executeQuery()
        ) {

            boolean first =
                    true;

            while (rs.next()) {

                if (!first) {

                    json.append(",");
                }

                first =
                        false;

                json.append(
                        patientJson(rs)
                );
            }
        }

        json.append("]");

        sendResponse(
                exchange,
                200,
                json.toString()
        );
    }


    // =========================================================
    // GET PATIENT BY ID
    // =========================================================

    private static void getPatient(
            HttpExchange exchange,
            int id)
            throws Exception {

        String query =
                "SELECT * FROM patients WHERE id = ?";

        try (
                Connection con =
                        DatabaseConnection.getConnection();

                PreparedStatement stmt =
                        con.prepareStatement(query)
        ) {

            stmt.setInt(
                    1,
                    id
            );

            try (
                    ResultSet rs =
                            stmt.executeQuery()
            ) {

                if (rs.next()) {

                    sendResponse(
                            exchange,
                            200,
                            patientJson(rs)
                    );

                } else {

                    sendResponse(
                            exchange,
                            404,
                            "{\"error\":\"Patient not found\"}"
                    );
                }
            }
        }
    }


    // =========================================================
    // SEARCH PATIENT
    // =========================================================

    private static void searchPatientByNamePhone(
            HttpExchange exchange)
            throws Exception {

        String queryString =
                exchange
                        .getRequestURI()
                        .getRawQuery();

        Map<String, String> params =
                parseFormData(queryString);

        String idText =
                params.get("id");

        String name =
                params.get("name");


        if (idText != null &&
                !idText.trim().isEmpty()) {

            int id;

            try {

                id =
                        Integer.parseInt(
                                idText.trim()
                        );

            } catch (NumberFormatException e) {

                sendResponse(
                        exchange,
                        400,
                        "{\"error\":\"Invalid patient ID\"}"
                );

                return;
            }


            String query =
                    "SELECT * FROM patients WHERE id = ?";

            try (
                    Connection con =
                            DatabaseConnection.getConnection();

                    PreparedStatement stmt =
                            con.prepareStatement(query)
            ) {

                stmt.setInt(
                        1,
                        id
                );

                try (
                        ResultSet rs =
                                stmt.executeQuery()
                ) {

                    if (rs.next()) {

                        sendResponse(
                                exchange,
                                200,
                                "[" + patientJson(rs) + "]"
                        );

                    } else {

                        sendResponse(
                                exchange,
                                404,
                                "{\"error\":\"Patient not found\"}"
                        );
                    }
                }
            }

            return;
        }


        if (name != null &&
                !name.trim().isEmpty()) {

            String query = """
                    SELECT *
                    FROM patients
                    WHERE LOWER(name) LIKE LOWER(?)
                    ORDER BY name, id
                    """;

            StringBuilder json =
                    new StringBuilder("[");

            try (
                    Connection con =
                            DatabaseConnection.getConnection();

                    PreparedStatement stmt =
                            con.prepareStatement(query)
            ) {

                stmt.setString(
                        1,
                        "%" + name.trim() + "%"
                );

                try (
                        ResultSet rs =
                                stmt.executeQuery()
                ) {

                    boolean first =
                            true;

                    while (rs.next()) {

                        if (!first) {

                            json.append(",");
                        }

                        first =
                                false;

                        json.append(
                                patientJson(rs)
                        );
                    }
                }
            }


            json.append("]");


            if (json.toString().equals("[]")) {

                sendResponse(
                        exchange,
                        404,
                        "{\"error\":\"Patient not found\"}"
                );

                return;
            }


            sendResponse(
                    exchange,
                    200,
                    json.toString()
            );

            return;
        }


        sendResponse(
                exchange,
                400,
                "{\"error\":\"Enter patient ID or patient name\"}"
        );
    }

        // =========================================================
    // UPDATE PATIENT
    // =========================================================

    private static void updatePatient(
            HttpExchange exchange,
            int id)
            throws Exception {

        String body =
                readRequestBody(exchange);

        Map<String, String> data =
                parseFormData(body);

        String name =
                data.get("name");

        String ageText =
                data.get("age");

        String gender =
                data.get("gender");

        String disease =
                data.get("disease");

        String phone =
                data.get("phone");

        String address =
                data.get("address");

        String medicalHistory =
                data.get("medicalHistory");

        String allergies =
                data.get("allergies");


        if (name == null ||
                ageText == null) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Required patient fields are missing\"}"
            );

            return;
        }


        int age;

        try {

            age =
                    Integer.parseInt(
                            ageText.trim()
                    );

        } catch (NumberFormatException e) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Invalid age\"}"
            );

            return;
        }


        String query = """
                UPDATE patients
                SET
                    name = ?,
                    age = ?,
                    gender = ?,
                    disease = ?,
                    phone = ?,
                    address = ?,
                    medical_history = ?,
                    allergies = ?
                WHERE id = ?
                """;


        try (
                Connection con =
                        DatabaseConnection.getConnection();

                PreparedStatement stmt =
                        con.prepareStatement(query)
        ) {

            stmt.setString(
                    1,
                    name
            );

            stmt.setInt(
                    2,
                    age
            );

            stmt.setString(
                    3,
                    gender
            );

            stmt.setString(
                    4,
                    disease
            );

            stmt.setString(
                    5,
                    phone
            );

            stmt.setString(
                    6,
                    address
            );

            stmt.setString(
                    7,
                    medicalHistory
            );

            stmt.setString(
                    8,
                    allergies
            );

            stmt.setInt(
                    9,
                    id
            );


            int rows =
                    stmt.executeUpdate();


            if (rows > 0) {

                sendResponse(
                        exchange,
                        200,
                        "{\"message\":\"Patient updated successfully\"}"
                );

            } else {

                sendResponse(
                        exchange,
                        404,
                        "{\"error\":\"Patient not found\"}"
                );
            }
        }
    }


    // =========================================================
    // DELETE PATIENT
    // =========================================================

    private static void deletePatient(
            HttpExchange exchange,
            int id)
            throws Exception {

        String query =
                "DELETE FROM patients WHERE id = ?";


        try (
                Connection con =
                        DatabaseConnection.getConnection();

                PreparedStatement stmt =
                        con.prepareStatement(query)
        ) {

            stmt.setInt(
                    1,
                    id
            );


            int rows =
                    stmt.executeUpdate();


            if (rows > 0) {

                sendResponse(
                        exchange,
                        200,
                        "{\"message\":\"Patient deleted successfully\"}"
                );

            } else {

                sendResponse(
                        exchange,
                        404,
                        "{\"error\":\"Patient not found\"}"
                );
            }
        }
    }


    // =========================================================
    // DOCTOR API
    // =========================================================

    private static void handleDoctors(
            HttpExchange exchange)
            throws IOException {

        String method =
                exchange.getRequestMethod();


        if (method.equals("OPTIONS")) {

            sendResponse(
                    exchange,
                    200,
                    "{}"
            );

            return;
        }


        if (!method.equals("GET")) {

            sendResponse(
                    exchange,
                    405,
                    "{\"error\":\"Method not allowed\"}"
            );

            return;
        }


        try {

            getDoctors(exchange);

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "{\"error\":\"Unable to load doctors\"}"
            );
        }
    }


    // =========================================================
    // GET DOCTORS
    // =========================================================

    private static void getDoctors(
            HttpExchange exchange)
            throws Exception {

        String query = """
                SELECT
                    doctor_id,
                    doctor_name,
                    specialization,
                    department,
                    phone,
                    room_no,
                    availability
                FROM doctors
                ORDER BY doctor_name
                """;


        StringBuilder json =
                new StringBuilder("[");


        try (
                Connection con =
                        DatabaseConnection.getConnection();

                PreparedStatement stmt =
                        con.prepareStatement(query);

                ResultSet rs =
                        stmt.executeQuery()
        ) {

            boolean first =
                    true;


            while (rs.next()) {

                if (!first) {

                    json.append(",");
                }

                first =
                        false;


                json.append("{");


                json.append("\"doctorId\":")
                        .append(
                                rs.getInt(
                                        "doctor_id"
                                )
                        )
                        .append(",");


                json.append("\"doctorName\":\"")
                        .append(
                                escapeJson(
                                        rs.getString(
                                                "doctor_name"
                                        )
                                )
                        )
                        .append("\",");


                json.append("\"specialization\":\"")
                        .append(
                                escapeJson(
                                        rs.getString(
                                                "specialization"
                                        )
                                )
                        )
                        .append("\",");


                json.append("\"department\":\"")
                        .append(
                                escapeJson(
                                        rs.getString(
                                                "department"
                                        )
                                )
                        )
                        .append("\",");


                json.append("\"phone\":\"")
                        .append(
                                escapeJson(
                                        rs.getString(
                                                "phone"
                                        )
                                )
                        )
                        .append("\",");


                json.append("\"roomNo\":\"")
                        .append(
                                escapeJson(
                                        rs.getString(
                                                "room_no"
                                        )
                                )
                        )
                        .append("\",");


                json.append("\"availability\":\"")
                        .append(
                                escapeJson(
                                        rs.getString(
                                                "availability"
                                        )
                                )
                        )
                        .append("\"");


                json.append("}");
            }
        }


        json.append("]");


        sendResponse(
                exchange,
                200,
                json.toString()
        );
    }


    // =========================================================
    // MEDICINE AVAILABILITY API
    // =========================================================

    private static void handleMedicines(
            HttpExchange exchange)
            throws IOException {

        String method =
                exchange.getRequestMethod();


        if (method.equals("OPTIONS")) {

            sendResponse(
                    exchange,
                    200,
                    "{}"
            );

            return;
        }


        if (!method.equals("GET")) {

            sendResponse(
                    exchange,
                    405,
                    "{\"error\":\"Method not allowed\"}"
            );

            return;
        }


        try {

            String queryString =
                    exchange
                            .getRequestURI()
                            .getRawQuery();


            Map<String, String> params =
                    parseFormData(queryString);


            String name =
                    params.get("name");


            if (name == null ||
                    name.trim().isEmpty()) {

                getAllMedicines(exchange);

            } else {

                searchMedicines(
                        exchange,
                        name.trim()
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "{\"error\":\"Unable to load medicines\"}"
            );
        }
    }


    // =========================================================
    // GET ALL MEDICINES
    // =========================================================

    private static void getAllMedicines(
            HttpExchange exchange)
            throws Exception {

        String query = """
                SELECT
                    medicine_id,
                    medicine_name,
                    category,
                    stock_quantity,
                    location,
                    availability
                FROM medicines
                ORDER BY medicine_name
                """;


        sendMedicineQuery(
                exchange,
                query,
                null
        );
    }


    // =========================================================
    // SEARCH MEDICINE BY NAME
    // =========================================================

    private static void searchMedicines(
            HttpExchange exchange,
            String name)
            throws Exception {

        String query = """
                SELECT
                    medicine_id,
                    medicine_name,
                    category,
                    stock_quantity,
                    location,
                    availability
                FROM medicines
                WHERE LOWER(medicine_name) LIKE LOWER(?)
                ORDER BY medicine_name
                """;


        sendMedicineQuery(
                exchange,
                query,
                "%" + name + "%"
        );
    }


    // =========================================================
    // EXECUTE MEDICINE QUERY
    // =========================================================

    private static void sendMedicineQuery(
            HttpExchange exchange,
            String query,
            String searchName)
            throws Exception {

        StringBuilder json =
                new StringBuilder("[");


        try (
                Connection con =
                        DatabaseConnection.getConnection()
        ) {

            if (con == null) {

                sendResponse(
                        exchange,
                        500,
                        "{\"error\":\"Database connection failed\"}"
                );

                return;
            }


            try (
                    PreparedStatement stmt =
                            con.prepareStatement(query)
            ) {

                if (searchName != null) {

                    stmt.setString(
                            1,
                            searchName
                    );
                }


                try (
                        ResultSet rs =
                                stmt.executeQuery()
                ) {

                    boolean first =
                            true;


                    while (rs.next()) {

                        if (!first) {

                            json.append(",");
                        }

                        first =
                                false;


                        json.append(
                                medicineJson(rs)
                        );
                    }
                }
            }
        }


        json.append("]");


        sendResponse(
                exchange,
                200,
                json.toString()
        );
    }


    // =========================================================
    // MEDICINE JSON
    // =========================================================

    private static String medicineJson(
            ResultSet rs)
            throws SQLException {

        int stockQuantity =
                rs.getInt(
                        "stock_quantity"
                );


        String availability =
                rs.getString(
                        "availability"
                );


        // Even if the database availability value was entered
        // incorrectly, zero stock should always show Out of Stock.
        if (stockQuantity <= 0) {

            availability =
                    "Out of Stock";
        }


        return "{"
                + "\"medicineId\":"
                + rs.getInt(
                "medicine_id"
        )
                + ","

                + "\"medicineName\":\""
                + escapeJson(
                rs.getString(
                        "medicine_name"
                )
        )
                + "\","

                + "\"category\":\""
                + escapeJson(
                rs.getString(
                        "category"
                )
        )
                + "\","

                + "\"stockQuantity\":"
                + stockQuantity
                + ","

                + "\"location\":\""
                + escapeJson(
                rs.getString(
                        "location"
                )
        )
                + "\","

                + "\"availability\":\""
                + escapeJson(
                availability
        )
                + "\""

                + "}";
    }


    // =========================================================
    // APPOINTMENT API ROUTER
    // =========================================================

    private static void handleAppointments(
            HttpExchange exchange)
            throws IOException {

        String method =
                exchange.getRequestMethod();


        String path =
                exchange
                        .getRequestURI()
                        .getPath();


        if (method.equals("OPTIONS")) {

            sendResponse(
                    exchange,
                    200,
                    "{}"
            );

            return;
        }


        try {

            // GET APPOINTMENTS FOR PATIENT
            if (method.equals("GET") &&
                    path.equals("/api/appointments")) {

                getAppointments(exchange);

                return;
            }


            // GET ONE APPOINTMENT
            if (method.equals("GET") &&
                    path.startsWith(
                            "/api/appointments/"
                    )) {

                String idText =
                        path.substring(
                                "/api/appointments/"
                                        .length()
                        );


                int id =
                        Integer.parseInt(
                                idText
                        );


                getAppointment(
                        exchange,
                        id
                );

                return;
            }


            // BOOK APPOINTMENT
            if (method.equals("POST") &&
                    path.equals("/api/appointments")) {

                bookAppointment(exchange);

                return;
            }


            // UPDATE APPOINTMENT STATUS
            if (method.equals("PUT") &&
                    path.startsWith(
                            "/api/appointments/"
                    )) {

                String idText =
                        path.substring(
                                "/api/appointments/"
                                        .length()
                        );


                int appointmentId =
                        Integer.parseInt(
                                idText
                        );


                updateAppointmentStatus(
                        exchange,
                        appointmentId
                );

                return;
            }


            sendResponse(
                    exchange,
                    404,
                    "{\"error\":\"Appointment API endpoint not found\"}"
            );


        } catch (NumberFormatException e) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Invalid appointment ID\"}"
            );


        } catch (Exception e) {

            e.printStackTrace();


            sendResponse(
                    exchange,
                    500,
                    "{\"error\":\"Appointment server error\"}"
            );
        }
    }


    // =========================================================
    // BOOK APPOINTMENT
    // =========================================================

    private static void bookAppointment(
            HttpExchange exchange)
            throws Exception {

        String body =
                readRequestBody(exchange);


        Map<String, String> data =
                parseFormData(body);


        String patientIdText =
                data.get("patientId");

        String patientName =
                data.get("patientName");

        String doctorIdText =
                data.get("doctorId");

        String appointmentDate =
                data.get("appointmentDate");

        String appointmentTime =
                data.get("appointmentTime");

        String reason =
                data.get("reason");


        if (patientIdText == null ||
                patientName == null ||
                doctorIdText == null ||
                appointmentDate == null ||
                appointmentTime == null) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Required appointment fields are missing\"}"
            );

            return;
        }


        int patientId =
                Integer.parseInt(
                        patientIdText
                );


        int doctorId =
                Integer.parseInt(
                        doctorIdText
                );


        try (
                Connection con =
                        DatabaseConnection.getConnection()
        ) {

            if (con == null) {

                sendResponse(
                        exchange,
                        500,
                        "{\"error\":\"Database connection failed\"}"
                );

                return;
            }


            // Make sure the patient exists.
            String patientQuery =
                    "SELECT id FROM patients WHERE id = ?";


            try (
                    PreparedStatement stmt =
                            con.prepareStatement(
                                    patientQuery
                            )
            ) {

                stmt.setInt(
                        1,
                        patientId
                );


                try (
                        ResultSet rs =
                                stmt.executeQuery()
                ) {

                    if (!rs.next()) {

                        sendResponse(
                                exchange,
                                404,
                                "{\"error\":\"Patient not found\"}"
                        );

                        return;
                    }
                }
            }


            // Make sure the doctor exists and is available.
            String doctorQuery = """
                    SELECT
                        doctor_name,
                        availability
                    FROM doctors
                    WHERE doctor_id = ?
                    """;


            String doctorName;


            try (
                    PreparedStatement stmt =
                            con.prepareStatement(
                                    doctorQuery
                            )
            ) {

                stmt.setInt(
                        1,
                        doctorId
                );


                try (
                        ResultSet rs =
                                stmt.executeQuery()
                ) {

                    if (!rs.next()) {

                        sendResponse(
                                exchange,
                                404,
                                "{\"error\":\"Doctor not found\"}"
                        );

                        return;
                    }


                    doctorName =
                            rs.getString(
                                    "doctor_name"
                            );


                    String availability =
                            rs.getString(
                                    "availability"
                            );


                    if (availability == null ||
                            !availability.equalsIgnoreCase(
                                    "Available"
                            )) {

                        sendResponse(
                                exchange,
                                409,
                                "{\"error\":\"Doctor is currently unavailable\"}"
                        );

                        return;
                    }
                }
            }


            String insertQuery = """
                    INSERT INTO appointments
                    (
                        patient_id,
                        patient_name,
                        doctor_id,
                        appointment_date,
                        appointment_time,
                        reason,
                        status
                    )
                    VALUES (?, ?, ?, ?, ?, ?, 'Pending')
                    """;


            int appointmentId;


            try (
                    PreparedStatement stmt =
                            con.prepareStatement(
                                    insertQuery,
                                    Statement.RETURN_GENERATED_KEYS
                            )
            ) {

                stmt.setInt(
                        1,
                        patientId
                );

                stmt.setString(
                        2,
                        patientName
                );

                stmt.setInt(
                        3,
                        doctorId
                );

                stmt.setDate(
                        4,
                        Date.valueOf(
                                appointmentDate
                        )
                );

                stmt.setTime(
                        5,
                        Time.valueOf(
                                appointmentTime.length() == 5
                                        ? appointmentTime + ":00"
                                        : appointmentTime
                        )
                );

                stmt.setString(
                        6,
                        reason
                );


                stmt.executeUpdate();


                try (
                        ResultSet keys =
                                stmt.getGeneratedKeys()
                ) {

                    if (keys.next()) {

                        appointmentId =
                                keys.getInt(1);

                    } else {

                        sendResponse(
                                exchange,
                                500,
                                "{\"error\":\"Could not create appointment ID\"}"
                        );

                        return;
                    }
                }
            }


            String json =
                    "{"
                            + "\"appointmentId\":"
                            + appointmentId
                            + ","

                            + "\"patientId\":"
                            + patientId
                            + ","

                            + "\"patientName\":\""
                            + escapeJson(
                            patientName
                    )
                            + "\","

                            + "\"doctorId\":"
                            + doctorId
                            + ","

                            + "\"doctorName\":\""
                            + escapeJson(
                            doctorName
                    )
                            + "\","

                            + "\"appointmentDate\":\""
                            + escapeJson(
                            appointmentDate
                    )
                            + "\","

                            + "\"appointmentTime\":\""
                            + escapeJson(
                            appointmentTime
                    )
                            + "\","

                            + "\"reason\":\""
                            + escapeJson(
                            reason == null
                                    ? ""
                                    : reason
                    )
                            + "\","

                            + "\"status\":\"Pending\""

                            + "}";


            sendResponse(
                    exchange,
                    201,
                    json
            );
        }
    }

        // =========================================================
    // UPDATE APPOINTMENT STATUS
    // =========================================================

    private static void updateAppointmentStatus(
            HttpExchange exchange,
            int appointmentId)
            throws Exception {

        String body =
                readRequestBody(exchange);

        Map<String, String> data =
                parseFormData(body);

        String status =
                data.get("status");


        if (status == null ||
                status.trim().isEmpty()) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Appointment status is required\"}"
            );

            return;
        }


        status =
                status.trim();


        boolean validStatus =
                status.equalsIgnoreCase("Pending") ||
                status.equalsIgnoreCase("Confirmed") ||
                status.equalsIgnoreCase("Completed") ||
                status.equalsIgnoreCase("Cancelled");


        if (!validStatus) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Invalid appointment status\"}"
            );

            return;
        }


        // Normalize status capitalization.
        if (status.equalsIgnoreCase("Pending")) {

            status =
                    "Pending";

        } else if (status.equalsIgnoreCase("Confirmed")) {

            status =
                    "Confirmed";

        } else if (status.equalsIgnoreCase("Completed")) {

            status =
                    "Completed";

        } else {

            status =
                    "Cancelled";
        }


        String query = """
                UPDATE appointments
                SET status = ?
                WHERE appointment_id = ?
                """;


        try (
                Connection con =
                        DatabaseConnection.getConnection();

                PreparedStatement stmt =
                        con.prepareStatement(query)
        ) {

            stmt.setString(
                    1,
                    status
            );

            stmt.setInt(
                    2,
                    appointmentId
            );


            int rowsUpdated =
                    stmt.executeUpdate();


            if (rowsUpdated == 0) {

                sendResponse(
                        exchange,
                        404,
                        "{\"error\":\"Appointment not found\"}"
                );

                return;
            }
        }


        String json =
                "{"
                        + "\"message\":\"Appointment status updated successfully\","
                        + "\"appointmentId\":"
                        + appointmentId
                        + ","
                        + "\"status\":\""
                        + escapeJson(status)
                        + "\""
                        + "}";


        sendResponse(
                exchange,
                200,
                json
        );
    }


    // =========================================================
    // GET APPOINTMENTS FOR PATIENT
    // =========================================================

    private static void getAppointments(
            HttpExchange exchange)
            throws Exception {

        String queryString =
                exchange
                        .getRequestURI()
                        .getRawQuery();


        Map<String, String> params =
                parseFormData(queryString);


        String patientIdText =
                params.get("patientId");


        if (patientIdText == null ||
                patientIdText.trim().isEmpty()) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Patient ID is required\"}"
            );

            return;
        }


        int patientId;

        try {

            patientId =
                    Integer.parseInt(
                            patientIdText.trim()
                    );

        } catch (NumberFormatException e) {

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"Invalid patient ID\"}"
            );

            return;
        }


        String query = """
                SELECT
                    a.appointment_id,
                    a.patient_id,
                    a.patient_name,
                    a.doctor_id,
                    d.doctor_name,
                    d.specialization,
                    a.appointment_date,
                    a.appointment_time,
                    a.reason,
                    a.status
                FROM appointments a
                JOIN doctors d
                    ON a.doctor_id = d.doctor_id
                WHERE a.patient_id = ?
                ORDER BY
                    a.appointment_date DESC,
                    a.appointment_time DESC
                """;


        StringBuilder json =
                new StringBuilder("[");


        try (
                Connection con =
                        DatabaseConnection.getConnection();

                PreparedStatement stmt =
                        con.prepareStatement(query)
        ) {

            stmt.setInt(
                    1,
                    patientId
            );


            try (
                    ResultSet rs =
                            stmt.executeQuery()
            ) {

                boolean first =
                        true;


                while (rs.next()) {

                    if (!first) {

                        json.append(",");
                    }


                    first =
                            false;


                    json.append(
                            appointmentJson(rs)
                    );
                }
            }
        }


        json.append("]");


        sendResponse(
                exchange,
                200,
                json.toString()
        );
    }


    // =========================================================
    // GET ONE APPOINTMENT
    // =========================================================

    private static void getAppointment(
            HttpExchange exchange,
            int appointmentId)
            throws Exception {

        String query = """
                SELECT
                    a.appointment_id,
                    a.patient_id,
                    a.patient_name,
                    a.doctor_id,
                    d.doctor_name,
                    d.specialization,
                    a.appointment_date,
                    a.appointment_time,
                    a.reason,
                    a.status
                FROM appointments a
                JOIN doctors d
                    ON a.doctor_id = d.doctor_id
                WHERE a.appointment_id = ?
                """;


        try (
                Connection con =
                        DatabaseConnection.getConnection();

                PreparedStatement stmt =
                        con.prepareStatement(query)
        ) {

            stmt.setInt(
                    1,
                    appointmentId
            );


            try (
                    ResultSet rs =
                            stmt.executeQuery()
            ) {

                if (rs.next()) {

                    sendResponse(
                            exchange,
                            200,
                            appointmentJson(rs)
                    );

                } else {

                    sendResponse(
                            exchange,
                            404,
                            "{\"error\":\"Appointment not found\"}"
                    );
                }
            }
        }
    }


    // =========================================================
    // PATIENT JSON
    // =========================================================

    private static String patientJson(
            ResultSet rs)
            throws SQLException {

        return "{"

                + "\"id\":"
                + rs.getInt(
                "id"
        )
                + ","

                + "\"name\":\""
                + escapeJson(
                rs.getString(
                        "name"
                )
        )
                + "\","

                + "\"age\":"
                + rs.getInt(
                "age"
        )
                + ","

                + "\"gender\":\""
                + escapeJson(
                rs.getString(
                        "gender"
                )
        )
                + "\","

                + "\"disease\":\""
                + escapeJson(
                rs.getString(
                        "disease"
                )
        )
                + "\","

                + "\"phone\":\""
                + escapeJson(
                rs.getString(
                        "phone"
                )
        )
                + "\","

                + "\"address\":\""
                + escapeJson(
                rs.getString(
                        "address"
                )
        )
                + "\","

                + "\"medicalHistory\":\""
                + escapeJson(
                rs.getString(
                        "medical_history"
                )
        )
                + "\","

                + "\"allergies\":\""
                + escapeJson(
                rs.getString(
                        "allergies"
                )
        )
                + "\""

                + "}";
    }


    // =========================================================
    // APPOINTMENT JSON
    // =========================================================

    private static String appointmentJson(
            ResultSet rs)
            throws SQLException {

        /*
         * patient_id can become NULL when a patient is deleted
         * because the database foreign key uses ON DELETE SET NULL.
         *
         * We therefore handle SQL NULL properly instead of returning
         * patientId = 0.
         */

        int patientId =
                rs.getInt(
                        "patient_id"
                );

        boolean patientIdIsNull =
                rs.wasNull();


        StringBuilder json =
                new StringBuilder();


        json.append("{");


        json.append("\"appointmentId\":")
                .append(
                        rs.getInt(
                                "appointment_id"
                        )
                )
                .append(",");


        json.append("\"patientId\":");

        if (patientIdIsNull) {

            json.append("null");

        } else {

            json.append(
                    patientId
            );
        }

        json.append(",");


        json.append("\"patientName\":\"")
                .append(
                        escapeJson(
                                rs.getString(
                                        "patient_name"
                                )
                        )
                )
                .append("\",");


        json.append("\"doctorId\":")
                .append(
                        rs.getInt(
                                "doctor_id"
                        )
                )
                .append(",");


        json.append("\"doctorName\":\"")
                .append(
                        escapeJson(
                                rs.getString(
                                        "doctor_name"
                                )
                        )
                )
                .append("\",");


        json.append("\"specialization\":\"")
                .append(
                        escapeJson(
                                rs.getString(
                                        "specialization"
                                )
                        )
                )
                .append("\",");


        json.append("\"appointmentDate\":\"")
                .append(
                        escapeJson(
                                rs.getString(
                                        "appointment_date"
                                )
                        )
                )
                .append("\",");


        json.append("\"appointmentTime\":\"")
                .append(
                        escapeJson(
                                rs.getString(
                                        "appointment_time"
                                )
                        )
                )
                .append("\",");


        json.append("\"reason\":\"")
                .append(
                        escapeJson(
                                rs.getString(
                                        "reason"
                                )
                        )
                )
                .append("\",");


        json.append("\"status\":\"")
                .append(
                        escapeJson(
                                rs.getString(
                                        "status"
                                )
                        )
                )
                .append("\"");


        json.append("}");


        return json.toString();
    }


    // =========================================================
    // SERVE WEBSITE
    // =========================================================

    private static void serveWebsite(
            HttpExchange exchange)
            throws IOException {

        String path =
                exchange
                        .getRequestURI()
                        .getPath();


        if (path.equals("/")) {

            path =
                    "/index.html";
        }


        // Prevent paths such as ../../some-file
        if (path.contains("..")) {

            sendResponse(
                    exchange,
                    403,
                    "{\"error\":\"Forbidden\"}"
            );

            return;
        }


        Path filePath =
                Paths.get(
                        "web",
                        path.substring(1)
                );


        if (!Files.exists(filePath) ||
                Files.isDirectory(filePath)) {

            sendResponse(
                    exchange,
                    404,
                    "{\"error\":\"Page not found\"}"
            );

            return;
        }


        byte[] data =
                Files.readAllBytes(
                        filePath
                );


        String contentType =
                getContentType(
                        filePath.toString()
                );


        exchange
                .getResponseHeaders()
                .set(
                        "Content-Type",
                        contentType
                );


        exchange.sendResponseHeaders(
                200,
                data.length
        );


        try (
                OutputStream os =
                        exchange.getResponseBody()
        ) {

            os.write(
                    data
            );
        }
    }


    // =========================================================
    // READ REQUEST BODY
    // =========================================================

    private static String readRequestBody(
            HttpExchange exchange)
            throws IOException {

        try (
                InputStream input =
                        exchange.getRequestBody()
        ) {

            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

        // =========================================================
    // PARSE FORM / QUERY / SIMPLE JSON DATA
    // =========================================================

    private static Map<String, String> parseFormData(
            String body) {

        Map<String, String> data =
                new HashMap<>();


        if (body == null ||
                body.trim().isEmpty()) {

            return data;
        }


        body =
                body.trim();


        // -----------------------------------------------------
        // SIMPLE JSON BODY
        // Example:
        // {"status":"Confirmed"}
        // -----------------------------------------------------

        if (body.startsWith("{") &&
                body.endsWith("}")) {

            String json =
                    body.substring(
                            1,
                            body.length() - 1
                    ).trim();


            if (json.isEmpty()) {

                return data;
            }


            StringBuilder current =
                    new StringBuilder();


            boolean insideQuotes =
                    false;

            boolean escaped =
                    false;


            List<String> pairs =
                    new ArrayList<>();


            for (
                    int i = 0;
                    i < json.length();
                    i++
            ) {

                char c =
                        json.charAt(i);


                if (escaped) {

                    current.append(c);

                    escaped =
                            false;

                    continue;
                }


                if (c == '\\' &&
                        insideQuotes) {

                    current.append(c);

                    escaped =
                            true;

                    continue;
                }


                if (c == '"') {

                    insideQuotes =
                            !insideQuotes;

                    current.append(c);

                    continue;
                }


                if (c == ',' &&
                        !insideQuotes) {

                    pairs.add(
                            current.toString()
                    );

                    current.setLength(0);

                } else {

                    current.append(c);
                }
            }


            pairs.add(
                    current.toString()
            );


            for (String pair : pairs) {

                String[] parts =
                        pair.split(
                                ":",
                                2
                        );


                if (parts.length != 2) {

                    continue;
                }


                String key =
                        parts[0].trim();


                String value =
                        parts[1].trim();


                if (key.startsWith("\"") &&
                        key.endsWith("\"")) {

                    key =
                            key.substring(
                                    1,
                                    key.length() - 1
                            );
                }


                if (value.startsWith("\"") &&
                        value.endsWith("\"")) {

                    value =
                            value.substring(
                                    1,
                                    value.length() - 1
                            );


                    value =
                            value.replace(
                                    "\\\"",
                                    "\""
                            );


                    value =
                            value.replace(
                                    "\\n",
                                    "\n"
                            );


                    value =
                            value.replace(
                                    "\\r",
                                    "\r"
                            );


                    value =
                            value.replace(
                                    "\\\\",
                                    "\\"
                            );
                }


                data.put(
                        key,
                        value
                );
            }


            return data;
        }


        // -----------------------------------------------------
        // FORM DATA / QUERY STRING
        //
        // Example:
        // status=Confirmed
        //
        // Example:
        // name=Paracetamol
        // -----------------------------------------------------

        String[] pairs =
                body.split("&");


        for (String pair : pairs) {

            String[] parts =
                    pair.split(
                            "=",
                            2
                    );


            if (parts.length == 2) {

                String key =
                        URLDecoder.decode(
                                parts[0],
                                StandardCharsets.UTF_8
                        );


                String value =
                        URLDecoder.decode(
                                parts[1],
                                StandardCharsets.UTF_8
                        );


                data.put(
                        key,
                        value
                );

            } else if (parts.length == 1) {

                String key =
                        URLDecoder.decode(
                                parts[0],
                                StandardCharsets.UTF_8
                        );


                data.put(
                        key,
                        ""
                );
            }
        }


        return data;
    }


    // =========================================================
    // ESCAPE TEXT FOR JSON
    // =========================================================

    private static String escapeJson(
            String value) {

        if (value == null) {

            return "";
        }


        return value

                .replace(
                        "\\",
                        "\\\\"
                )

                .replace(
                        "\"",
                        "\\\""
                )

                .replace(
                        "\n",
                        "\\n"
                )

                .replace(
                        "\r",
                        "\\r"
                )

                .replace(
                        "\t",
                        "\\t"
                );
    }


    // =========================================================
    // STATIC FILE CONTENT TYPE
    // =========================================================

    private static String getContentType(
            String file) {


        String lowerFile =
                file.toLowerCase();


        if (lowerFile.endsWith(".html")) {

            return "text/html; charset=UTF-8";
        }


        if (lowerFile.endsWith(".css")) {

            return "text/css; charset=UTF-8";
        }


        if (lowerFile.endsWith(".js")) {

            return "application/javascript; charset=UTF-8";
        }


        if (lowerFile.endsWith(".json")) {

            return "application/json; charset=UTF-8";
        }


        if (lowerFile.endsWith(".png")) {

            return "image/png";
        }


        if (lowerFile.endsWith(".jpg") ||
                lowerFile.endsWith(".jpeg")) {

            return "image/jpeg";
        }


        if (lowerFile.endsWith(".gif")) {

            return "image/gif";
        }


        if (lowerFile.endsWith(".svg")) {

            return "image/svg+xml";
        }


        if (lowerFile.endsWith(".ico")) {

            return "image/x-icon";
        }


        return "application/octet-stream";
    }


    // =========================================================
    // SEND HTTP RESPONSE
    // =========================================================

    private static void sendResponse(
            HttpExchange exchange,
            int status,
            String response)
            throws IOException {


        if (response == null) {

            response =
                    "";
        }


        byte[] data =
                response.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange
                .getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );


        // -----------------------------------------------------
        // CORS
        // -----------------------------------------------------

        exchange
                .getResponseHeaders()
                .set(
                        "Access-Control-Allow-Origin",
                        "*"
                );


        exchange
                .getResponseHeaders()
                .set(
                        "Access-Control-Allow-Methods",
                        "GET, POST, PUT, DELETE, OPTIONS"
                );


        exchange
                .getResponseHeaders()
                .set(
                        "Access-Control-Allow-Headers",
                        "Content-Type"
                );


        exchange.sendResponseHeaders(
                status,
                data.length
        );


        try (
                OutputStream os =
                        exchange.getResponseBody()
        ) {

            os.write(
                    data
            );
        }
    }


    // =========================================================
    // END OF SERVER
    // =========================================================

}