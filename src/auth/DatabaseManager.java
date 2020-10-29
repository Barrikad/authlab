package auth;

import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.Properties;

public class DatabaseManager {

    private String jdbcDriver;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public DatabaseManager() throws ClassNotFoundException, IOException {
        Properties prop = new Properties();
        prop.load(getClass().getClassLoader().getResourceAsStream("database.properties"));
        jdbcDriver=prop.getProperty("jdbcDriver");
        dbUrl=prop.getProperty("dbUrl");
        dbUser=prop.getProperty("dbUser");
        dbPassword=prop.getProperty("dbPassword");
        Class.forName(jdbcDriver);
    }

    public boolean validateUser(String user, byte[] password) {
        PreparedStatement stmt = null;
        ResultSet rst = null;
        Connection conn = null;
        boolean result = false;
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            String query = " SELECT password FROM user_data WHERE username=?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, user);

            rst = stmt.executeQuery();
            rst.next();
            result = Arrays.equals(rst.getBytes(1), password);
        } catch (
                SQLException e) {

            System.err.println(e.getMessage());
        } finally {
            if (rst != null) {
                try {
                    rst.close();
                } catch (SQLException e) { /* ignored */}
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) { /* ignored */}
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) { /* ignored */}
            }
        }
        return result;
    }

    public void insertUser(String user, byte[] password) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            String query = " INSERT INTO user_data (username, password)"
                    + " VALUES (?, ?)";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, user);
            stmt.setBytes(2, password);

            stmt.execute();
        } catch (
                SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) { /* ignored */}
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) { /* ignored */}
            }
        }
    }

    //for tests only
    public void deleteUser(String user) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            String query = " DELETE FROM user_data WHERE username=?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, user);

            stmt.executeUpdate();
            conn.close();
        } catch (
                SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) { /* ignored */}
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) { /* ignored */}
            }
        }
    }

    public void updateUserPassword(String user, byte[] password) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            String query = "UPDATE user_data "
                    + "SET password = ? "
                    + "WHERE username = ?";
            stmt = conn.prepareStatement(query);
            stmt.setBytes(1, password);
            stmt.setString(2, user);

            stmt.execute();
        } catch (
                SQLException e) {
            System.err.println(e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) { /* ignored */}
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) { /* ignored */}
            }
        }
    }
}
