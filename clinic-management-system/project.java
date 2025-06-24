package project.dsa;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.InputMismatchException;

class clinicDB { 
    String dbURL = "jdbc:oracle:thin:@localhost:1521:orcl";
    String username = "system";
    String password = "123";
    static Connection connection = null;

    // Method to connect to the database
    public void connect() {
        try {
            connection = DriverManager.getConnection(dbURL, username, password);
            System.out.println("Connected to ClinicDB");
        } catch (SQLException e) {
            System.out.println("ClinicDB connection failed:");
            System.out.println(e);
        }
    }
}

// Method to fetch patient data and return it in a HashMap
class doctor extends clinicDB {
    public HashMap<Integer, String> getPatientData() {
        HashMap<Integer, String> patientMap = new HashMap<>();

        String read = "SELECT p.patient_id, p.name, p.age, p.sex, p.contact_info, " +
                      "a.appointment_id, TO_CHAR(a.appointment_date, 'YYYY-MM-DD HH24:MI') AS appointment_date, " +
                      "d.doctor_id, d.name AS doctor_name " +
                      "FROM patients p " +
                      "LEFT JOIN appointments a ON p.patient_id = a.patient_id " +
                      "LEFT JOIN doctors d ON a.doctor_id = d.doctor_id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(read)) {

            while (rs.next()) {
                int patientId = rs.getInt("patient_id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                String sex = rs.getString("sex");
                String contactInfo = rs.getString("contact_info");
                int appointmentId = rs.getInt("appointment_id");
                String appointmentDate = rs.getString("appointment_date");
                int doctorId = rs.getInt("doctor_id");
                String doctorName = rs.getString("doctor_name");

                // Formatting the patient information
                String patientDetails = "Name: " + name + ", Age: " + age + ", Sex: " + sex + 
                                        ", Contact Info: " + contactInfo;

                if (appointmentId != 0) { // Check if there is an appointment
                    patientDetails += ", Appointment ID: " + appointmentId + 
                                      ", Date: " + appointmentDate +
                                      ", Doctor: " + doctorId + " - " + doctorName;
                } else {
                    patientDetails += ", No appointment scheduled.";
                }

                patientMap.put(patientId, patientDetails);  // Store data in HashMap
            }

        } catch (SQLException e) {
            System.out.println("Error fetching patient data:");
            e.printStackTrace();
        }

        return patientMap;
    }

    public void displayPatientData(Scanner sc) {
        HashMap<Integer, String> patientData = getPatientData();

        System.out.print("Enter Patient Id: ");
        int target = sc.nextInt();
        sc.nextLine(); // fix scanner bug

        if (patientData.containsKey(target)) {
            System.out.println("Patient Details: " + patientData.get(target));
        } else {
            System.out.println("Patient not found.");
        }
    }

    public void createPrescription(Scanner sc) {
        System.out.println("Enter Patient ID:");
        int patientId = sc.nextInt(); sc.nextLine();
        System.out.println("Enter Doctor ID:");
        int doctorId = sc.nextInt(); sc.nextLine();
        System.out.println("Enter Diagnosis:");
        String diagnosis = sc.nextLine();
        System.out.println("Enter Medication Details:");
        String medication = sc.nextLine();
        System.out.println("Enter Dosage:");
        String dosage = sc.nextLine();
        System.out.println("Enter Instructions:");
        String instructions = sc.nextLine();

        String insertQuery = "INSERT INTO prescriptions (patient_id, doctor_id, diagnosis, medication_details, dosage, instructions) " +
                "VALUES ('" + patientId + "', '" + doctorId + "', '" + diagnosis + "', '" + medication + "', '" + dosage + "', '" + instructions + "')";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(insertQuery);
            System.out.println("Prescription added successfully!");
        } catch (InputMismatchException | SQLException e) {
            System.out.println("Error inserting prescription:");
            e.printStackTrace();
        }
    }

    public void addCheckUpDetails(Scanner sc) {
        System.out.println("Enter Check-up Details:");

        System.out.print("Enter Patient ID: ");
        String patientId = sc.nextLine();

        System.out.print("Enter Appointment ID: ");
        String appointmentId = sc.nextLine();

        System.out.print("Enter Blood Pressure: ");
        String bloodPressure = sc.nextLine() +" mmHg";  

        System.out.print("Enter Sugar Level: ");
        String sugarLevel = sc.nextLine() + " mg/dL";

        System.out.print("Enter Temperature: ");
        String temperature = sc.nextLine() + "°F";

        System.out.print("Enter Heart Rate: ");
        String heartRate = sc.nextLine() + " bpm";

        String insertQuery = "INSERT INTO checkup_details (patient_id, appointment_id, blood_pressure, sugar_level, temperature, heart_rate) " +
                             "VALUES ('" + patientId + "', '" + appointmentId + "', '" + bloodPressure + "', '" + sugarLevel + "', '" + temperature + "', '" + heartRate + "')";

        try (Statement stmt = connection.createStatement()) {
            int rowsInserted = stmt.executeUpdate(insertQuery);
            if (rowsInserted > 0) {
                System.out.println("Check-up details added successfully!");
            } else {
                System.out.println("Failed to add check-up details.");
            }
        } catch (InputMismatchException | SQLException e) {
            System.out.println("Error inserting check-up details:");
            e.printStackTrace();
        }
    }
    public void processNextPatient() {
        System.out.println("Processing the next patient in the queue...");
    
        // SQL query to fetch the first patient in the queue (FIFO order)
        String query = "SELECT patient_id, TO_CHAR(appointment_date, 'YYYY-MM-DD HH24:MI:SS') AS appointment_time " +
                       "FROM appointments WHERE appointment_date >= TRUNC(SYSDATE) " +
                       "ORDER BY appointment_date ASC FETCH FIRST 1 ROWS ONLY";
    
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
    
            if (rs.next()) {
                int patientId = rs.getInt("patient_id");
                String appointmentTime = rs.getString("appointment_time");
    
                System.out.println("Attending Patient ID: " + patientId + ", Appointment Time: " + appointmentTime);
    
                // Remove the processed patient from the database
                String deleteQuery = "DELETE FROM appointments WHERE patient_id = " + patientId +
                                     " AND TO_CHAR(appointment_date, 'YYYY-MM-DD HH24:MI:SS') = '" + appointmentTime + "'";
                int rowsAffected = stmt.executeUpdate(deleteQuery);
    
                if (rowsAffected > 0) {
                    System.out.println("Patient removed from queue.");
                } else {
                    System.out.println("Error removing patient from queue.");
                }
            } else {
                System.out.println("No patients in the queue.");
            }
    
        } catch (SQLException e) {
            System.out.println("Error processing patient:");
            e.printStackTrace();
        }
    }
}




class receptionist extends clinicDB {

    // Make the getCurrentTime method static so it can be used without creating an object of receptionist
    public static String getCurrentTime() {
        LocalDateTime datetime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = datetime.format(formatter);   
        return formattedDateTime;
    }

    public void viewPatientHistory(Scanner sc) {
    System.out.print("Enter Patient ID to view history: ");
    int patientId = sc.nextInt();
    sc.nextLine();  // consume newline

    String query = "SELECT " +
                   "PATIENTS.patient_id, " +
                   "PATIENTS.name, " +
                   "PATIENTS.contact_info, " +
                   "APPOINTMENTS.appointment_date, " +
                   "PAYMENTS.amount, " +
                   "PAYMENTS.payment_status " +
                   "FROM PATIENTS " +
                   "JOIN APPOINTMENTS ON PATIENTS.patient_id = APPOINTMENTS.patient_id " +
                   "LEFT JOIN PAYMENTS ON PAYMENTS.appointment_id = APPOINTMENTS.appointment_id " +
                   "WHERE PATIENTS.patient_id = " + patientId + " " +
                   "ORDER BY APPOINTMENTS.appointment_date";

    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

        boolean found = false;
        System.out.println("Patient History for ID: " + patientId);
        System.out.println("--------------------------------------------------");
        while (rs.next()) {
            found = true;
            System.out.println("Patient Name: " + rs.getString("name"));
            System.out.println("Contact Info: " + rs.getString("contact_info"));
            System.out.println("Appointment Date: " + rs.getTimestamp("appointment_date"));
            Double paymentAmount = rs.getDouble("amount");
            if (rs.wasNull()) paymentAmount = null; // handle null payments
            System.out.println("Payment Amount: " + (paymentAmount != null ? paymentAmount : "N/A"));
            System.out.println("Payment Status: " + (rs.getString("payment_status") != null ? rs.getString("payment_status") : "N/A"));
            System.out.println("--------------------------------------------------");
        }
        if (!found) {
            System.out.println("No history found for this patient.");
        }

    } catch (SQLException e) {
        System.out.println("Error fetching patient history:");
        e.printStackTrace();
    }
}


    public void appointment(Scanner sc) {
        String currentDateTime = getCurrentTime();
        System.out.println("Enter Patient ID:");
        int patientId = sc.nextInt(); sc.nextLine();
        System.out.println("Enter Doctor ID:");
        int doctorId = sc.nextInt(); sc.nextLine();

        String insertQuery = "INSERT INTO appointments (patient_id, doctor_id, appointment_date) " +
        "VALUES ('" + patientId + "', '" + doctorId + "', TO_DATE('" + currentDateTime + "', 'YYYY-MM-DD HH24:MI:SS'))";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(insertQuery);
            System.out.println("Doctor assigned successfully!");
        } catch (InputMismatchException | SQLException e) {
            System.out.println("Error assigning doctor:");
            System.out.println(e);
        }
    }

    public void registerPatient(Scanner sc) {
        System.out.println("Enter patient name:");
        String name = sc.nextLine();
        System.out.println("Enter patient age:");
        int age = sc.nextInt();
        sc.nextLine();  // Fix Scanner bug left by nextInt()
        System.out.println("Enter patient sex:");
        String sex = sc.nextLine();
        System.out.println("Enter patient contact_info email and number:");
        String contactinfo = sc.nextLine();

        String insert = "INSERT INTO patients (name, age, sex, contact_info) " +
                "VALUES (INITCAP('" + name + "'), " + age + ", INITCAP('" + sex + "'), '" + contactinfo + "')";


        try (Statement stmt = connection.createStatement()) {
            // Execute the insert statement
            int rowsAffected = stmt.executeUpdate(insert); // inserts data and fetches the number of rows affected
            if (rowsAffected > 0) {
                System.out.println("Patient data inserted successfully!");
            } else {
                System.out.println("Failed to insert patient data, try again!");
            }
        } catch (InputMismatchException | SQLException e) {
            // Handle SQL exception
            System.out.println("Error inserting patient data:");
            e.printStackTrace();
        }
    }

    public void makePayment(Scanner sc) {
        System.out.println("Enter Patient ID:");
        int patientId = sc.nextInt(); sc.nextLine();
        System.out.println("Enter Appointment ID:");
        int appointmentId = sc.nextInt(); sc.nextLine();
        System.out.println("Enter Payment Amount:");
        double amount = sc.nextDouble(); sc.nextLine();
        System.out.println("Enter Payment Method (Cash, Card, Insurance):");
        String method = sc.nextLine();
    
        try (Statement stmt = connection.createStatement()) {
            // Step 1: Insert payment into database
            String insertQuery = "INSERT INTO payments (patient_id, appointment_id, amount, payment_status, payment_method) " +
                                 "VALUES (" + patientId + ", " + appointmentId + ", " + amount + ", 'Completed', INITCAP('" + method + "'))";
            stmt.executeUpdate(insertQuery); // executeUpdate for INSERT queries
    
            // Step 2: Retrieve the newly inserted payment_id and generate receipt
            String fetchQuery = "SELECT pa.payment_id, p.patient_id, pa.amount, pa.payment_method, pa.payment_status " +
                                "FROM payments pa JOIN patients p ON pa.patient_id = p.patient_id " +
                                "WHERE pa.patient_id = " + patientId + " AND pa.appointment_id = " + appointmentId + 
                                " ORDER BY pa.payment_id DESC FETCH FIRST 1 ROW ONLY"; // Fetch latest payment
    
            try (ResultSet rs = stmt.executeQuery(fetchQuery)) {
                if (rs.next()) {
                    int paymentId = rs.getInt("payment_id");
    
                    // Print Receipt
                    System.out.println("\n===== RECEIPT =====");
                    System.out.println("Patient ID: " + rs.getInt("patient_id"));
                    System.out.println("Amount Paid: " + rs.getDouble("amount") + " Rupees");
                    System.out.println("Payment Method: " + rs.getString("payment_method"));
                    System.out.println("Payment Status: " + rs.getString("payment_status"));
                    System.out.println("Date: " + (getCurrentTime()));
                    System.out.println("Queue Number: " + paymentId);
                    System.out.println("====================");
                    System.out.println("Thank you for using our services!\n");
                } else {
                    System.out.println("Error retrieving Payment ID.");
                }
            }
        } catch (InputMismatchException | SQLException e) {
            System.out.println("Error processing payment:");
            e.printStackTrace();
        }
    }
    
    
    

    public void calculateQueue() {
        System.out.println("Displaying patient queue order for today...");

        // Use the getCurrentTime method to get the current date in the required format
        String currentDateTime = getCurrentTime();  // Extract just the date time part (yyyy-MM-dd)

        // SQL query to fetch patients with appointments on the current date
        String query = "SELECT patient_id, appointment_date " + 
                   "FROM appointments " +
                   "WHERE appointment_date >= TRUNC(SYSDATE) " +
                   "ORDER BY appointment_date ASC";
    

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            // Queue to store patient IDs
            Queue<String> patientQueue = new LinkedList<>();

            while (rs.next()) {
                int patientId = rs.getInt("patient_id");
                Timestamp appointmentTime = rs.getTimestamp("appointment_date");

                // Add the patient ID to the queue(for inserting only)
                patientQueue.offer("Patient ID: " + patientId + ", Appointment Time: " + appointmentTime);
            }
            // Display the patients in the queue
            if (patientQueue.isEmpty()) {
                System.out.println("No patients for today.");
            } else {
                System.out.println("Current patient's queue:");
                while (!patientQueue.isEmpty()) {
                    System.out.println(patientQueue.poll());
                }
            }

        } catch (InputMismatchException | SQLException e) {
            System.out.println("Error displaying queue:");
            e.printStackTrace();
        }
    }
    

    
}
