package project.dsa;

import java.sql.*;
import java.util.*;

import static spark.Spark.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class BackendServer {

    // Shared DB connection object
    static clinicDB db = new clinicDB();

    // Role-based service objects
    static Receptionist receptionist;
    static Doctor doctor; // Placeholder for future use

    public static void main(String[] args) {

        // Step 1: Connect to database
        db.connect();
        if (clinicDB.connection == null) {
            System.err.println("Database connection failed. Exiting...");
            stop();
            return;
        }

        // Step 2: Initialize services
        receptionist = new Receptionist(clinicDB.connection);
        doctor = new Doctor(clinicDB.connection); 

        // Step 3: Set port and enable CORS
        port(4567);
        enableCORS("*", "*", "*");

        // Step 4: Setup API routes
        setupReceptionistRoutes();
        setupDoctorRoutes(); // Placeholder

        // Step 5: Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
            System.out.println("Server stopped.");
        }));
    }

    // =============================
    // Receptionist API Endpoints
    // =============================
    public static void setupReceptionistRoutes() {

        // Get patient history by name
        get("/patientHistory", (req, res) -> {
            String nameQuery = req.queryParams("name");

            if (nameQuery == null || nameQuery.trim().isEmpty()) {
                res.status(400);
                return "Missing 'name' query parameter";
            }

            List<Map<String, Object>> patientData = receptionist.getPatientHistoryByName(nameQuery);
            JSONArray jsonArray = new JSONArray(patientData);

            res.type("application/json");
            return jsonArray.toString();
        });

        // Get all patients
        get("/allPatients", (req, res) -> {
            List<Map<String, Object>> patientList = receptionist.getAllPatients();
            JSONArray jsonArray = new JSONArray(patientList);

            res.type("application/json");
            return jsonArray.toString();
        });

        // GET /patients?query=someQuery --> fetch patients by query string
        get("/patients", (req, res) -> {
            String query = req.queryParams("query");
            if (query == null || query.trim().isEmpty()) {
                res.status(400);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Query parameter 'query' is required.")).toString();
            }
            Map<String, String> data = new HashMap<>();
            data.put("request_type", "getPatients");
            data.put("query", query.trim());

            Map<String, Object> result = receptionist.registerAppointFromAPI(data);

            res.type("application/json");
            return new JSONObject(result).toString();
        });

        // GET /specialties --> fetch all specialties
        get("/specialties", (req, res) -> {
            Map<String, String> data = new HashMap<>();
            data.put("request_type", "getSpecialties");

            Map<String, Object> result = receptionist.registerAppointFromAPI(data);

            res.type("application/json");
            return new JSONObject(result).toString();
        });

        // GET /doctors?specialization=Cardiology --> fetch doctors by specialization
        get("/doctors", (req, res) -> {
            String specialization = req.queryParams("specialization");
            if (specialization == null || specialization.trim().isEmpty()) {
                res.status(400);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Query parameter 'specialization' is required.")).toString();
            }
            Map<String, String> data = new HashMap<>();
            data.put("request_type", "getDoctors");
            data.put("specialization", specialization.trim());

            Map<String, Object> result = receptionist.registerAppointFromAPI(data);

            res.type("application/json");
            return new JSONObject(result).toString();
        });

        // Register new patient
        post("/registerPatient", (req, res) -> {
            String body = req.body();
            JSONObject json = new JSONObject(body);

            Map<String, String> patientData = new HashMap<>();
            patientData.put("name", json.optString("name", "").trim());
            patientData.put("age", json.optString("age", "").trim());
            patientData.put("sex", json.optString("sex", "").trim());
            patientData.put("contact_info", json.optString("contact_info", "").trim());

            Map<String, Object> result = receptionist.registerPatientFromAPI(patientData);

            if (Boolean.TRUE.equals(result.get("success"))) {
                res.status(201);
            } else {
                res.status(400);
            }

            res.type("application/json");
            return new JSONObject(result).toString();
        });

        // Register Appointment (NEW ENDPOINT)

        post("/registerAppointment", (req, res) -> {
            String body = req.body();
            JSONObject json = new JSONObject(body);

            String patientId = json.optString("patient_id", "").trim();
            String doctorId = json.optString("doctor_id", "").trim();
            String appointmentDate = json.optString("appointment_date", "").trim();

            if (patientId.isEmpty() || doctorId.isEmpty() || appointmentDate.isEmpty()) {
                res.status(400);
                return new JSONObject(Map.of(
                        "success", false,
                        "message",
                        "Missing required fields: patient_id, doctor_id, and appointment_date must be provided"))
                        .toString();
            }

            Map<String, String> appointmentData = new HashMap<>();
            appointmentData.put("request_type", "register"); // Added request_type here
            appointmentData.put("patient_id", patientId);
            appointmentData.put("doctor_id", doctorId);
            appointmentData.put("appointment_date", appointmentDate);

            // Call the correct method name here (registerAppointFromAPI)
            Map<String, Object> result = receptionist.registerAppointFromAPI(appointmentData);

            if (Boolean.TRUE.equals(result.get("success"))) {
                res.status(201);
            } else {
                res.status(400);
            }

            res.type("application/json");
            return new JSONObject(result).toString();
        });

        // GET for searching patients by query param
        get("/searchPatients", (req, res) -> {
            String query = req.queryParams("q");
            res.type("application/json");

            Map<String, Object> result = receptionist.searchPatients(query);

            if (Boolean.TRUE.equals(result.get("success"))) {
                res.status(200);
            } else {
                res.status(400);
            }
            return new JSONObject(result).toString();
        });

        // POST for making payments with JSON body
        post("/makePayment", (req, res) -> {
            res.type("application/json");
            try {
                JSONObject json = new JSONObject(req.body());

                Map<String, String> data = new HashMap<>();
                for (String key : json.keySet()) {
                    data.put(key, json.getString(key));
                }

                Map<String, Object> result = receptionist.makePayment(data);

                if (Boolean.TRUE.equals(result.get("success"))) {
                    res.status(200);
                } else {
                    res.status(400);
                }

                return new JSONObject(result).toString();

            } catch (Exception e) {
                res.status(500);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Server error: " + e.getMessage())).toString();
            }
        });

        post("/receptionist/login", (req, res) -> {
            res.type("application/json");
            try {
                JSONObject json = new JSONObject(req.body());
                String username = json.optString("username", "").trim();
                String password = json.optString("password", "").trim();

                if (username.isEmpty() || password.isEmpty()) {
                    res.status(400);
                    return new JSONObject(Map.of(
                            "success", false,
                            "message", "Username and password are required.")).toString();
                }

                // Call your receptionist login method (assumed to return Map<String,Object>)
                Map<String, Object> loginResult = receptionist.receptionistLogin(username, password);

                if (Boolean.TRUE.equals(loginResult.get("success"))) {
                    res.status(200);
                } else {
                    res.status(401); // Unauthorized
                }

                return new JSONObject(loginResult).toString();

            } catch (Exception e) {
                res.status(500);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Server error: " + e.getMessage())).toString();
            }
        });

        // GET /receptionist/info?receptionist_id=123
        get("/receptionist/info", (req, res) -> {
            res.type("application/json");

            String idStr = req.queryParams("receptionist_id");
            if (idStr == null || idStr.trim().isEmpty()) {
                res.status(400);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Query parameter 'receptionist_id' is required.")).toString();
            }

            int receptionistId;
            try {
                receptionistId = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                res.status(400);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Invalid receptionist_id format.")).toString();
            }

            Map<String, Object> info = receptionist.getReceptionistInfo(receptionistId);

            if (Boolean.TRUE.equals(info.get("success"))) {
                res.status(200);
            } else {
                res.status(404);
            }

            return new JSONObject(info).toString();
        });

        post("/receptionist/changePassword", (req, res) -> {
            res.type("application/json");

            try {
                JSONObject json = new JSONObject(req.body());
                int receptionistId = json.optInt("receptionist_id", -1);
                String currentPassword = json.optString("current_password", "").trim();
                String newPassword = json.optString("new_password", "").trim();

                if (receptionistId <= 0 || currentPassword.isEmpty() || newPassword.isEmpty()) {
                    res.status(400);
                    return new JSONObject(Map.of(
                            "success", false,
                            "message", "Missing or invalid parameters.")).toString();
                }

                // Fetch current password from DB
                String storedPassword = receptionist.getPasswordById(receptionistId); // create this method

                if (storedPassword == null) {
                    res.status(404);
                    return new JSONObject(Map.of(
                            "success", false,
                            "message", "Receptionist not found.")).toString();
                }

                if (!storedPassword.equals(currentPassword)) {
                    res.status(403);
                    return new JSONObject(Map.of(
                            "success", false,
                            "message", "Current password is incorrect.")).toString();
                }

                // Update password
                Map<String, Object> result = receptionist.changePassword(receptionistId, newPassword);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    res.status(200);
                } else {
                    res.status(400);
                }

                return new JSONObject(result).toString();

            } catch (Exception e) {
                res.status(500);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Server error: " + e.getMessage())).toString();
            }
        });

    }

    // =============================
    // Doctor API Endpoints (Future)
    // =============================
    public static void setupDoctorRoutes() {
        

    // Role-based service objects

        // Placeholder for future doctor-related endpoints
        // POST /doctor/login
        post("/doctor/login", (req, res) -> {
            res.type("application/json");
            try {
                JSONObject json = new JSONObject(req.body());
                String username = json.optString("username", "").trim();
                String password = json.optString("password", "").trim();

                if (username.isEmpty() || password.isEmpty()) {
                    res.status(400);
                    return new JSONObject(Map.of(
                            "success", false,
                            "message", "Username and password are required.")).toString();
                }

                // Call doctor login method
                Map<String, Object> loginResult = doctor.doctorLogin(username, password);

                if (Boolean.TRUE.equals(loginResult.get("success"))) {
                    res.status(200);
                } else {
                    res.status(401); // Unauthorized
                }

                return new JSONObject(loginResult).toString();

            } catch (Exception e) {
                res.status(500);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Server error: " + e.getMessage())).toString();
            }
        });

        // GET /doctor/info?doctor_id=123
        get("/doctor/info", (req, res) -> {
            res.type("application/json");

            String idStr = req.queryParams("doctor_id");
            if (idStr == null || idStr.trim().isEmpty()) {
                res.status(400);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Query parameter 'doctor_id' is required.")).toString();
            }

            int doctorId;
            try {
                doctorId = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                res.status(400);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Invalid doctor_id format.")).toString();
            }

            Map<String, Object> info = doctor.getDoctorInfo(doctorId);

            if (Boolean.TRUE.equals(info.get("success"))) {
                res.status(200);
            } else {
                res.status(404);
            }

            return new JSONObject(info).toString();
        });

        // POST /doctor/changePassword
        post("/doctor/changePassword", (req, res) -> {
            res.type("application/json");

            try {
                JSONObject json = new JSONObject(req.body());
                int doctorId = json.optInt("doctor_id", -1);
                String currentPassword = json.optString("current_password", "").trim();
                String newPassword = json.optString("new_password", "").trim();

                if (doctorId <= 0 || currentPassword.isEmpty() || newPassword.isEmpty()) {
                    res.status(400);
                    return new JSONObject(Map.of(
                            "success", false,
                            "message", "Missing or invalid parameters.")).toString();
                }

                // Fetch current password from DB
                String storedPassword = doctor.getPasswordById(doctorId);

                if (storedPassword == null) {
                    res.status(404);
                    return new JSONObject(Map.of(
                            "success", false,
                            "message", "Doctor not found.")).toString();
                }

                if (!storedPassword.equals(currentPassword)) {
                    res.status(403);
                    return new JSONObject(Map.of(
                            "success", false,
                            "message", "Current password is incorrect.")).toString();
                }

                // Update password
                Map<String, Object> result = doctor.changePassword(doctorId, newPassword);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    res.status(200);
                } else {
                    res.status(400);
                }

                return new JSONObject(result).toString();

            } catch (Exception e) {
                res.status(500);
                return new JSONObject(Map.of(
                        "success", false,
                        "message", "Server error: " + e.getMessage())).toString();
            }
        });

      // http://localhost:4567/patientHistory?name=Ali
   get("/patientPrescriptionHistory", (req, res) -> {
    String name = req.queryParams("name");
    String doctorIdParam = req.queryParams("doctorId"); // get doctorId from query param

    if (name == null || name.trim().isEmpty()) {
        res.status(400);
        return new JSONObject(Map.of("error", "Missing or empty 'name' query parameter")).toString();
    }

    if (doctorIdParam == null || doctorIdParam.trim().isEmpty()) {
        res.status(400);
        return new JSONObject(Map.of("error", "Missing or empty 'doctorId' query parameter")).toString();
    }

    int doctorId;
    try {
        doctorId = Integer.parseInt(doctorIdParam);
    } catch (NumberFormatException e) {
        res.status(400);
        return new JSONObject(Map.of("error", "'doctorId' must be an integer")).toString();
    }

    List<Map<String, Object>> history = doctor.getPatientHistoryByName(name, doctorId);

    JSONArray jsonArray = new JSONArray();
    for (Map<String, Object> record : history) {
        jsonArray.put(new JSONObject(record));
    }

    res.type("application/json");
    return jsonArray.toString();
});


get("/prescriptions", (req, res) -> {
    String patientIdParam = req.queryParams("patient_id");
    String doctorIdParam = req.queryParams("doctor_id");

    if (patientIdParam == null || doctorIdParam == null) {
        res.status(400);
        return new JSONObject(Map.of("error", "Missing 'patient_id' or 'doctor_id' query parameter")).toString();
    }

    int patientId, doctorId;
    try {
        patientId = Integer.parseInt(patientIdParam);
        doctorId = Integer.parseInt(doctorIdParam);
    } catch (NumberFormatException e) {
        res.status(400);
        return new JSONObject(Map.of("error", "Invalid 'patient_id' or 'doctor_id' format")).toString();
    }

    List<Map<String, Object>> prescriptions = doctor.getPrescriptions(patientId, doctorId);

    JSONArray jsonArray = new JSONArray();
    for (Map<String, Object> record : prescriptions) {
        jsonArray.put(new JSONObject(record));
    }

    res.type("application/json");
    return jsonArray.toString();
});

post("/createPrescription", (req, res) -> {
    res.type("application/json");

    JSONObject data = new JSONObject(req.body());

    int doctorId = data.getInt("doctor_id");
    int patientId = data.getInt("patient_id");
    String diagnosis = data.getString("diagnosis");
    String medicationDetails = data.getString("medication_details");
    String dosage = data.getString("dosage");
    String instructions = data.getString("instructions");

    Map<String, Object> result = doctor.createPrescription(doctorId, patientId, diagnosis, medicationDetails, dosage, instructions);

    JSONObject json = new JSONObject(result);
    return json.toString();
});


post("/addCheckupDetails", (req, res) -> {
    try {
        JSONObject body = new JSONObject(req.body());

        int patientId = body.getInt("patient_id");
        int appointmentId = body.getInt("appointment_id");
        String bloodPressure = body.getString("blood_pressure");
        String sugarLevel = body.getString("sugar_level");
        String temperature = body.getString("temperature");
        String heartRate = body.getString("heart_rate");

        Map<String, Object> result = doctor.addCheckupDetails(patientId, appointmentId, bloodPressure, sugarLevel, temperature, heartRate);

        res.type("application/json");
        return new JSONObject(result).toString();

    } catch (Exception e) {
        System.out.println(e.getMessage());
        res.status(400);
        return new JSONObject(Map.of(
            "success", false,
            "message", "Invalid input or server error: " + e.getMessage()
        )).toString();
    }
});


// Add processNextPatient route
    post("/doctor/processNextPatient", (req, res) -> {
        res.type("application/json");
        try {
            JSONObject json = new JSONObject(req.body());
            int doctorId = json.getInt("doctorId");

            // Call your Doctor class method (assumed to be accessible as 'doctor')
            Map<String, Object> result = doctor.processNextPatient(doctorId);

            return new JSONObject(result).toString();

        } catch (Exception e) {
            res.status(500);
            return new JSONObject(Map.of(
                "success", false,
                "message", "Server error: " + e.getMessage()
            )).toString();
        }
    });


   get("/doctor/currentPatientName", (req, res) -> {
    res.type("application/json");
    try {
        int doctorId = Integer.parseInt(req.queryParams("doctorId"));
        String currentPatient = doctor.getCurrentPatient(doctorId);  //   Use doctor object

        if (currentPatient != null) {
            return new JSONObject(Map.of(
                "success", true,
                "current_patient", currentPatient
            )).toString();
        } else {
            return new JSONObject(Map.of(
                "success", false,
                "message", "No current patient in queue."
            )).toString();
        }

    } catch (Exception e) {
        res.status(500);
        return new JSONObject(Map.of(
            "success", false,
            "message", "Error: " + e.getMessage()
        )).toString();
    }
});

get("/doctor/queueCount", (req, res) -> {
    res.type("application/json");

    String doctorIdParam = req.queryParams("doctorId");
    if (doctorIdParam == null || doctorIdParam.trim().isEmpty()) {
        res.status(400);
        return new JSONObject(Map.of(
                "success", false,
                "message", "Missing 'doctorId' query parameter."
        )).toString();
    }

    int doctorId;
    try {
        doctorId = Integer.parseInt(doctorIdParam);
    } catch (NumberFormatException e) {
        res.status(400);
        return new JSONObject(Map.of(
                "success", false,
                "message", "Invalid 'doctorId' format. It must be an integer."
        )).toString();
    }

    try {
        int count = doctor.getQueueCount(doctorId); // You already created this method

        res.status(200);
        return new JSONObject(Map.of(
                "success", true,
                "queue_count", count
        )).toString();

    } catch (Exception e) {
        res.status(500);
        return new JSONObject(Map.of(
                "success", false,
                "message", "Server error: " + e.getMessage()
        )).toString();
    }
});





    }

    // =============================
    // Enable CORS (for frontend)
    // =============================
    public static void enableCORS(final String origin, final String methods, final String headers) {

        options("/*", (request, response) -> {
            String reqHeaders = request.headers("Access-Control-Request-Headers");
            if (reqHeaders != null) {
                response.header("Access-Control-Allow-Headers", reqHeaders);
            }

            String reqMethod = request.headers("Access-Control-Request-Method");
            if (reqMethod != null) {
                response.header("Access-Control-Allow-Methods", reqMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Allow-Methods", methods);
            response.header("Access-Control-Allow-Headers", headers);
        });
    }
}
