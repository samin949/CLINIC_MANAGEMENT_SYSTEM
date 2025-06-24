package project.dsa;

import java.sql.*;
import java.util.*;

public class Doctor {

    public Connection connection;

    public Doctor(Connection connection) {
        this.connection = connection;
    }

    // ========================================================================
    // 1. Login/Password/View profile/Change password logic same as receptionist
    // ========================================================================

    public Map<String, Object> doctorLogin(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Username and password must not be empty.");
            return result;
        }

        String sql = "SELECT LOGIN_ID, DOCTOR_ID FROM doctor_login WHERE USERNAME = '"
                + username.replace("'", "''") + "' AND PASSWORD = '" + password.replace("'", "''") + "'";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int doctorId = rs.getInt("DOCTOR_ID");

                result.put("success", true);
                result.put("message", "Login successful.");
                result.put("doctor_id", doctorId);
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

    public Map<String, Object> getDoctorInfo(int doctorId) {
        Map<String, Object> info = new HashMap<>();

        String sql = "SELECT NAME, CONTACT_INFO, SPECIALIZATION FROM doctors WHERE DOCTOR_ID = " + doctorId;

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
                        email = parts[0].trim(); // first part is the email
                    }
                }
                info.put("email", email);
                info.put("speciality", rs.getString("SPECIALIZATION"));
            } else {
                info.put("success", false);
                info.put("message", "Doctor not found");
            }
        } catch (SQLException e) {
            info.put("success", false);
            info.put("message", "SQL error: " + e.getMessage());
        }

        return info;
    }

    public Map<String, Object> changePassword(int doctorId, String newPassword) {
        Map<String, Object> result = new HashMap<>();

        if (newPassword == null || newPassword.isEmpty()) {
            result.put("success", false);
            result.put("message", "New password cannot be empty");
            return result;
        }

        try {
            Statement stmt = connection.createStatement();

            String sql = "UPDATE doctor_login SET password = '" + newPassword + "' WHERE doctor_id = "
                    + doctorId;

            int rowsUpdated = stmt.executeUpdate(sql);

            if (rowsUpdated > 0) {
                result.put("success", true);
                result.put("message", "Password updated successfully");
            } else {
                result.put("success", false);
                result.put("message", "Doctor ID not found");
            }

            stmt.close();

        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "Database error: " + e.getMessage());
        }

        return result;
    }

    public String getPasswordById(int doctorId) {
        String password = null;
        String sql = "SELECT password FROM doctor_login WHERE doctor_id = " + doctorId;

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

    public List<Map<String, Object>> getPatientHistoryByName(String name, int doctorId) {
    List<Map<String, Object>> historyList = new ArrayList<>();

    String sql = "SELECT a.APPOINTMENT_ID, p.PATIENT_ID, p.NAME, p.CONTACT_INFO, " +
                 "a.APPOINTMENT_DATE, pay.PAYMENT_STATUS, pay.AMOUNT " +
                 "FROM patients p " +
                 "JOIN appointments a ON p.PATIENT_ID = a.PATIENT_ID " +
                 "LEFT JOIN payments pay ON a.APPOINTMENT_ID = pay.APPOINTMENT_ID " +
                 "WHERE LOWER(p.NAME) LIKE LOWER('%" + name.replace("'", "''") + "%') " +
                 "AND a.DOCTOR_ID = " + doctorId;

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("appointment_id", rs.getInt("APPOINTMENT_ID"));  // ✅ newly added
            row.put("patient_id", rs.getInt("PATIENT_ID"));
            row.put("name", rs.getString("NAME"));
            row.put("contact_info", rs.getString("CONTACT_INFO"));
            row.put("appointment_date", rs.getString("APPOINTMENT_DATE"));
            row.put("payment_status", rs.getString("PAYMENT_STATUS"));
            row.put("amount", rs.getBigDecimal("AMOUNT"));  // Assuming AMOUNT is numeric
            historyList.add(row);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return historyList;
}




public List<Map<String, Object>> getPrescriptions(int patientId, int doctorId) {
    List<Map<String, Object>> prescriptionList = new ArrayList<>();

    String sql = "SELECT diagnosis, medication_details, dosage, instructions " +
                 "FROM prescriptions " +
                 "WHERE patient_id = " + patientId + " AND doctor_id = " + doctorId;

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("diagnosis", rs.getString("diagnosis"));
            row.put("medication_details", rs.getString("medication_details"));
            row.put("dosage", rs.getString("dosage"));
            row.put("instructions", rs.getString("instructions"));
            prescriptionList.add(row);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return prescriptionList;
}

public Map<String, Object> createPrescription(int doctorId, int patientId, String diagnosis, String medicationDetails, String dosage, String instructions) {
    Map<String, Object> result = new HashMap<>();

    if (diagnosis == null || diagnosis.trim().isEmpty() ||
        medicationDetails == null || medicationDetails.trim().isEmpty() ||
        dosage == null || dosage.trim().isEmpty() ||
        instructions == null || instructions.trim().isEmpty()) {
        result.put("success", false);
        result.put("message", "All fields must be filled.");
        return result;
    }

    // Escape single quotes to avoid SQL errors
    diagnosis = diagnosis.replace("'", "''");
    medicationDetails = medicationDetails.replace("'", "''");
    dosage = dosage.replace("'", "''");
    instructions = instructions.replace("'", "''");

    String sql = "INSERT INTO prescriptions (doctor_id, patient_id, diagnosis, medication_details, dosage, instructions) VALUES (" +
                 doctorId + ", " + patientId + ", '" + diagnosis + "', '" + medicationDetails + "', '" + dosage + "', '" + instructions + "')";

    try (Statement stmt = connection.createStatement()) {
        int rows = stmt.executeUpdate(sql);

        if (rows > 0) {
            result.put("success", true);
            result.put("message", "Prescription added successfully.");
        } else {
            result.put("success", false);
            result.put("message", "Failed to add prescription.");
        }

    } catch (SQLException e) {
        result.put("success", false);
        result.put("message", "SQL error: " + e.getMessage());
    }

    return result;
}

public Map<String, Object> addCheckupDetails(int patientId, int appointmentId, String bloodPressure, String sugarLevel, String temperature, String heartRate) {
    Map<String, Object> result = new HashMap<>();

    // Validate inputs
    if (bloodPressure == null || sugarLevel == null || temperature == null || heartRate == null ||
        bloodPressure.trim().isEmpty() || sugarLevel.trim().isEmpty() ||
        temperature.trim().isEmpty() || heartRate.trim().isEmpty()) {
        result.put("success", false);
        result.put("message", "All fields must be filled.");
        return result;
    }

    // Check if appointment exists (to avoid FK constraint violation)
    if (!appointmentExists(appointmentId)) {
        result.put("success", false);
        result.put("message", "Invalid appointment ID: appointment does not exist.");
        return result;
    }

    // Append units if missing
    if (!bloodPressure.trim().endsWith("mmHg")) {
        bloodPressure = bloodPressure.trim() + " mmHg";
    }
    if (!sugarLevel.trim().endsWith("mg/dL")) {
        sugarLevel = sugarLevel.trim() + " mg/dL";
    }
    if (!temperature.trim().endsWith("°C")) {
        temperature = temperature.trim() + " °C";
    }
    if (!heartRate.trim().endsWith("Bpm")) {
        heartRate = heartRate.trim() + " Bpm";
    }

    // Escape single quotes
    bloodPressure = bloodPressure.replace("'", "''");
    sugarLevel = sugarLevel.replace("'", "''");
    temperature = temperature.replace("'", "''");
    heartRate = heartRate.replace("'", "''");

    String sql = "INSERT INTO checkup_details (patient_id, appointment_id, blood_pressure, sugar_level, temperature, heart_rate) " +
                 "VALUES (" + patientId + ", " + appointmentId + ", '" + bloodPressure + "', '" + sugarLevel + "', '" + temperature + "', '" + heartRate + "')";

    try (Statement stmt = connection.createStatement()) {
        int rows = stmt.executeUpdate(sql);

        if (rows > 0) {
            result.put("success", true);
            result.put("message", "Checkup details added successfully.");
        } else {
            result.put("success", false);
            result.put("message", "Failed to add checkup details.");
        }

    } catch (SQLException e) {
        result.put("success", false);
        result.put("message", "SQL error: " + e.getMessage());
        System.out.println(e.getMessage());
    }

    return result;
}

private boolean appointmentExists(int appointmentId) {
    String query = "SELECT 1 FROM appointments WHERE appointment_id = " + appointmentId;
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {
        return rs.next();
    } catch (SQLException e) {
        System.out.println("Error checking appointment existence: " + e.getMessage());
        return false;
    }
}


public Map<String, Object> processNextPatient(int doctorId) {
    Map<String, Object> result = new HashMap<>();

    try {
        // 1. Find the earliest 'waiting' patient in the queue for this doctor
        String query = "SELECT * FROM (" +
               "SELECT q.queue_id, q.appointment_id, q.patient_id, q.enqueue_time, p.name, p.contact_info " +
               "FROM queue q " +
               "JOIN appointments a ON q.appointment_id = a.appointment_id " +
               "JOIN patients p ON q.patient_id = p.patient_id " +
               "WHERE q.status = 'waiting' AND a.doctor_id = " + doctorId + " " +
               "ORDER BY q.enqueue_time ASC" +
               ") WHERE ROWNUM = 1";


        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                int queueId = rs.getInt("queue_id");

                // 2. Immediately mark as completed
                String update = "UPDATE queue SET status = 'completed' WHERE queue_id = " + queueId;
                int updated = stmt.executeUpdate(update);

                if (updated > 0) {
                    // 3. Prepare return result
                    result.put("success", true);
                    result.put("message", "Processed and marked patient as completed.");
                    result.put("queue_id", queueId);
                    result.put("appointment_id", rs.getInt("appointment_id"));
                    result.put("patient_id", rs.getInt("patient_id"));
                    result.put("enqueue_time", rs.getTimestamp("enqueue_time"));
                    result.put("patient_name", rs.getString("name"));
                    result.put("contact_info", rs.getString("contact_info"));
                    result.put("status", "completed");
                } else {
                    result.put("success", false);
                    result.put("message", "Could not update queue status.");
                }
            } else {
                result.put("success", false);
                result.put("message", "No waiting patients in the queue.");
            }
        }

    } catch (SQLException e) {
        result.put("success", false);
        result.put("message", "SQL Error: " + e.getMessage());
    }

    return result;
}

public String getCurrentPatient(int doctorId) {
    String currentPatient = null;

    try {
        String query = "SELECT * FROM (" +
                       "SELECT p.name " +
                       "FROM queue q " +
                       "JOIN appointments a ON q.appointment_id = a.appointment_id " +
                       "JOIN patients p ON q.patient_id = p.patient_id " +
                       "WHERE q.status = 'waiting' AND a.doctor_id = " + doctorId + " " +
                       "ORDER BY q.enqueue_time ASC" +
                       ") WHERE ROWNUM = 1";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                currentPatient = rs.getString("name");
            }

        }
    } catch (SQLException e) {
        System.err.println("SQL Error: " + e.getMessage());
    }

    return currentPatient;
}

public int getQueueCount(int doctorId) {
    int count = 0;
    String sql = "SELECT COUNT(*) AS total FROM queue q " +
                 "JOIN appointments a ON q.appointment_id = a.appointment_id " +
                 "WHERE q.status = 'waiting' AND a.doctor_id = " + doctorId;

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        if (rs.next()) {
            count = rs.getInt("total");
        }

    } catch (SQLException e) {
        System.err.println("SQL error in getQueueCount: " + e.getMessage());
    }

    return count;
}





}
