package project.dsa;

import java.sql.*;
import java.util.*;

public class Receptionist {

    public Connection connection;

    public Receptionist(Connection connection) {
        this.connection = connection;
    }

    // =========================================
    // 1. Search Patient History by Name
    // =========================================
    public List<Map<String, Object>> getPatientHistoryByName(String name) {
        List<Map<String, Object>> results = new ArrayList<>();

        String sql = "SELECT PATIENTS.name, PATIENTS.contact_info, " +
                     "APPOINTMENTS.appointment_date, " +
                     "PAYMENTS.amount, PAYMENTS.payment_status " +
                     "FROM PATIENTS " +
                     "JOIN APPOINTMENTS ON PATIENTS.patient_id = APPOINTMENTS.patient_id " +
                     "LEFT JOIN PAYMENTS ON PAYMENTS.appointment_id = APPOINTMENTS.appointment_id " +
                     "WHERE LOWER(PATIENTS.name) LIKE '%" + name.toLowerCase() + "%' " +
                     "ORDER BY APPOINTMENTS.appointment_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("name", rs.getString("name"));
                record.put("contact_info", rs.getString("contact_info"));

                Timestamp apptDate = rs.getTimestamp("appointment_date");
                record.put("appointment_date", apptDate != null ? apptDate.toString() : "N/A");

                Double amount = rs.getDouble("amount");
                if (rs.wasNull()) amount = null;
                record.put("amount", amount != null ? amount : "N/A");

                String status = rs.getString("payment_status");
                record.put("payment_status", status != null ? status : "N/A");

                results.add(record);
            }

        } catch (SQLException e) {
            System.err.println("SQL error in getPatientHistoryByName: " + e.getMessage());
        }

        return results;
    }

    // =========================================
    // 2. Get All Patients
    // =========================================
    public List<Map<String, Object>> getAllPatients() {
        List<Map<String, Object>> patients = new ArrayList<>();

        String sql = "SELECT patient_id, name, age, sex, contact_info FROM PATIENTS ORDER BY name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> patient = new HashMap<>();
                patient.put("patient_id", rs.getInt("patient_id"));
                patient.put("name", rs.getString("name"));
                patient.put("age", rs.getInt("age"));
                patient.put("sex", rs.getString("sex"));
                patient.put("contact_info", rs.getString("contact_info"));
                patients.add(patient);
            }

        } catch (SQLException e) {
            System.err.println("SQL error in getAllPatients: " + e.getMessage());
        }

        return patients;
    }

    // =========================================
    // 3. Register New Patient
    // =========================================
    public Map<String, Object> registerPatientFromAPI(Map<String, String> data) {
        Map<String, Object> result = new HashMap<>();

        try {
            String name = data.get("name");
            int age = Integer.parseInt(data.get("age"));
            String sex = data.get("sex");
            String contactInfo = data.get("contact_info");
            

            String sql = "INSERT INTO patients (name, age, sex, contact_info) VALUES (" +
                         "INITCAP('" + name + "'), " +
                         age + ", " +
                         "INITCAP('" + sex + "'), " +
                         "'" + contactInfo + "')";
            System.out.println(sql);

            try (Statement stmt = connection.createStatement()) {
                int rowsAffected = stmt.executeUpdate(sql);
                if (rowsAffected > 0) {
                    result.put("success", true);
                    result.put("message", "Patient data inserted successfully.");
                } else {
                    result.put("success", false);
                    result.put("message", "Failed to insert patient data.");
                }
            }

        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "Invalid age value.");
        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "SQL error: " + e.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Unexpected error: " + e.getMessage());
        }

        return result;
    }

    // =========================================
// 4b. Make Appointment & Fetch Data (Combined GET/POST)
// =========================================
public Map<String, Object> registerAppointFromAPI(Map<String, String> data) {
    Map<String, Object> result = new HashMap<>();

    try {
        String requestType = data.get("request_type");
        if (requestType == null) {
            result.put("success", false);
            result.put("message", "request_type parameter is required.");
            System.out.println("eror");
            return result;

        }

        if ("getPatients".equalsIgnoreCase(requestType)) {
            // autocomplete patient search by name, age, sex, contact (query param)
            String query = data.get("query");
            if (query == null || query.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Search query is required.");
                return result;
            }
            query = query.toLowerCase().trim();

            String sql = "SELECT patient_id, name, age, sex, contact_info FROM patients " +
             "WHERE LOWER(name) LIKE '%" + query.toLowerCase() + "%' " +
             "OR TO_CHAR(age) LIKE '%" + query + "%' " +
             "OR LOWER(sex) LIKE '%" + query.toLowerCase() + "%' " +
             "OR LOWER(contact_info) LIKE '%" + query.toLowerCase() + "%' " +
             "ORDER BY name";


            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                List<Map<String, Object>> patients = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> patient = new HashMap<>();
                    patient.put("patient_id", rs.getInt("patient_id"));
                    patient.put("name", rs.getString("name"));
                    patient.put("age", rs.getInt("age"));
                    patient.put("sex", rs.getString("sex"));
                    patient.put("contact_info", rs.getString("contact_info"));
                    patients.add(patient);
                }

                result.put("success", true);
                result.put("patients", patients);
            }

        } else if ("getSpecialties".equalsIgnoreCase(requestType)) {
            // fetch all distinct specialties
            String sql = "SELECT DISTINCT specialization FROM doctors ORDER BY specialization";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                List<String> specialties = new ArrayList<>();
                while (rs.next()) {
                    specialties.add(rs.getString("specialization"));
                }

                result.put("success", true);
                result.put("specialties", specialties);
            }

        } else if ("getDoctors".equalsIgnoreCase(requestType)) {
            // fetch doctors by specialty
            String specialty = data.get("specialization");
            if (specialty == null || specialty.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Specialization is required.");
                return result;
            }

            String sql = "SELECT doctor_id, name FROM doctors WHERE specialization = '" + specialty + "' ORDER BY name";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                List<Map<String, Object>> doctors = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> doctor = new HashMap<>();
                    doctor.put("doctor_id", rs.getInt("doctor_id"));
                    doctor.put("name", rs.getString("name"));
                    doctors.add(doctor);
                }

                result.put("success", true);
                result.put("doctors", doctors);
            }

        } else if ("register".equalsIgnoreCase(requestType)) {
            // register appointment (insert)
            int patientId = Integer.parseInt(data.get("patient_id"));
            int doctorId = Integer.parseInt(data.get("doctor_id"));
            String appointmentDateStr = data.get("appointment_date");

            if (appointmentDateStr == null || appointmentDateStr.isEmpty()) {
                result.put("success", false);
                result.put("message", "Appointment date is required.");
                return result;
            }

            String formatted = appointmentDateStr.replace("T", " ");
            if (formatted.length() == 16) formatted += ":00";

            String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_date) " +
                         "VALUES (" + patientId + ", " + doctorId + ", TO_TIMESTAMP('" + formatted + "', 'YYYY-MM-DD HH24:MI:SS'))";

            try (Statement stmt = connection.createStatement()) {
                int rows = stmt.executeUpdate(sql);
                if (rows > 0) {
                    result.put("success", true);
                    result.put("message", "Appointment registered successfully.");
                } else {
                    result.put("success", false);
                    result.put("message", "Failed to register appointment.");
                }
            }

        } else {
            result.put("success", false);
            result.put("message", "Invalid request_type parameter.");
        }

    } catch (NumberFormatException e) {
        result.put("success", false);
        result.put("message", "Invalid number format: " + e.getMessage());
    } catch (SQLException e) {
        result.put("success", false);
        result.put("message", "SQL error: " + e.getMessage());
    } catch (Exception e) {
        result.put("success", false);
        result.put("message", "Unexpected error: " + e.getMessage());
    }

    return result;
}





// =========================================
// Combined GET/POST Handler for Payment & Patient Search
// =========================================
public Map<String, Object> searchPatients(String query) {
    Map<String, Object> result = new HashMap<>();
    if (query == null || query.trim().isEmpty()) {
        result.put("success", false);
        result.put("message", "Search query is required.");
        return result;
    }

    query = query.toLowerCase().trim();

    String sql = "SELECT p.patient_id, p.name, p.age, p.sex, p.contact_info, a.appointment_id, a.appointment_date "
           + "FROM patients p LEFT JOIN appointments a ON p.patient_id = a.patient_id "
           + "WHERE LOWER(p.name) LIKE '%" + query.toLowerCase() + "%' "
           + "OR CAST(p.age AS VARCHAR2(10)) LIKE '%" + query.toLowerCase() + "%' "
           + "OR LOWER(p.sex) LIKE '%" + query.toLowerCase() + "%' "
           + "ORDER BY p.name FETCH FIRST 10 ROWS ONLY";



    try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

        List<Map<String, Object>> patients = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> patient = new HashMap<>();
            patient.put("patient_id", rs.getInt("patient_id"));
            patient.put("name", rs.getString("name"));
            patient.put("age", rs.getInt("age"));
            patient.put("sex", rs.getString("sex"));
            patient.put("contact_info", rs.getString("contact_info"));

            int appId = rs.getInt("appointment_id");
            if (rs.wasNull())
                appId = -1; // no appointment

            patient.put("appointment_id", appId == -1 ? null : appId);

            Timestamp ts = rs.getTimestamp("appointment_date");
            patient.put("appointment_date", ts != null ? ts.toString().replace(" ", "T") : null);

            patients.add(patient);
        }

        result.put("success", true);
        result.put("patients", patients);

    } catch (SQLException e) {
        result.put("success", false);
        result.put("message", "SQL error: " + e.getMessage());
    }

    return result;
}


public Map<String, Object> makePayment(Map<String, String> data) {
    Map<String, Object> result = new HashMap<>();
    try {
        // Validate inputs
        String patientIdStr = data.get("patient_id");
        String appointmentIdStr = data.get("appointment_id");
        String amountStr = data.get("amount");
        String method = data.get("payment_method");

        if (patientIdStr == null || appointmentIdStr == null || amountStr == null || method == null
                || patientIdStr.trim().isEmpty() || appointmentIdStr.trim().isEmpty() || amountStr.trim().isEmpty()
                || method.trim().isEmpty()) {
            result.put("success", false);
            result.put("message",
                    "All payment fields are required: patient_id, appointment_id, amount, payment_method.");
            return result;
        }

        int patientId = Integer.parseInt(patientIdStr);
        int appointmentId = Integer.parseInt(appointmentIdStr);
        double amount = Double.parseDouble(amountStr);
        String paymentMethod = method.trim();

        String insertQuery = "INSERT INTO payments (patient_id, appointment_id, amount, payment_status, payment_method) "
                + "VALUES (" + patientId + ", " + appointmentId + ", " + amount + ", 'Completed', INITCAP('"
                + paymentMethod + "'))";

        try (Statement stmt = connection.createStatement()) {
            int rows = stmt.executeUpdate(insertQuery);
            if (rows > 0) {
                result.put("success", true);
                result.put("message", "Payment processed successfully.");
            } else {
                result.put("success", false);
                result.put("message", "Payment processing failed.");
            }
        }

    } catch (NumberFormatException e) {
        result.put("success", false);
        result.put("message", "Invalid number format: " + e.getMessage());
    } catch (SQLException e) {
        result.put("success", false);
        result.put("message", "SQL error: " + e.getMessage());
    } catch (Exception e) {
        result.put("success", false);
        result.put("message", "Unexpected error: " + e.getMessage());
    }

    return result;
}

public Map<String, Object> receptionistLogin(String username, String password) {
    Map<String, Object> result = new HashMap<>();

    if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
        result.put("success", false);
        result.put("message", "Username and password must not be empty.");
        return result;
    }

    String sql = "SELECT LOGIN_ID, RECEPTIONIST_ID FROM receptionist_login WHERE USERNAME = '" 
                 + username.replace("'", "''") + "' AND PASSWORD = '" + password.replace("'", "''") + "'";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        if (rs.next()) {
            int receptionistId = rs.getInt("RECEPTIONIST_ID");

            result.put("success", true);
            result.put("message", "Login successful.");
            result.put("receptionist_id", receptionistId);
        } else {
            result.put("success", false);
            result.put("message", "Invalid username or password.");
        }

    } catch (SQLException e) {
        result.put("success", false);
        result.put("message", "SQL error: " + e.getMessage());
    }

    return result;
}

public Map<String, Object> getReceptionistInfo(int receptionistId) {
    Map<String, Object> info = new HashMap<>();

    String sql = "SELECT NAME, CONTACT_INFO FROM receptionists WHERE RECEPTIONIST_ID = " + receptionistId;

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        if (rs.next()) {
            info.put("success", true);
            info.put("name", rs.getString("NAME"));

            String contactInfo = rs.getString("CONTACT_INFO");
            String email = null;
            if (contactInfo != null) {
                String[] parts = contactInfo.split(",");
                if (parts.length > 0) {
                    email = parts[0].trim();  // first part is the email
                }
            }
            info.put("email", email);
        } else {
            info.put("success", false);
            info.put("message", "Receptionist not found");
        }
    } catch (SQLException e) {
        info.put("success", false);
        info.put("message", "SQL error: " + e.getMessage());
    }

    return info;
}

public Map<String, Object> changePassword(int receptionistId, String newPassword) {
    Map<String, Object> result = new HashMap<>();

    if (newPassword == null || newPassword.isEmpty()) {
        result.put("success", false);
        result.put("message", "New password cannot be empty");
        return result;
    }

    try {
        Statement stmt = connection.createStatement();

        
        String sql = "UPDATE receptionist_login SET password = '" + newPassword + "' WHERE receptionist_id = " + receptionistId;

        int rowsUpdated = stmt.executeUpdate(sql);

        if (rowsUpdated > 0) {
            result.put("success", true);
            result.put("message", "Password updated successfully");
        } else {
            result.put("success", false);
            result.put("message", "Receptionist ID not found");
        }

        stmt.close();

    } catch (SQLException e) {
        result.put("success", false);
        result.put("message", "Database error: " + e.getMessage());
    }

    return result;
}

public String getPasswordById(int receptionistId) {
    String password = null;
    String sql = "SELECT password FROM receptionist_login WHERE receptionist_id = " + receptionistId;

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        if (rs.next()) {
            password = rs.getString("password");
        }
    } catch (SQLException e) {
        System.err.println("SQL error in getPasswordById: " + e.getMessage());
    }

    return password;
}







}
