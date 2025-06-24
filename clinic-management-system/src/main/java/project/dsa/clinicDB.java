package project.dsa;

import java.sql.*;

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

