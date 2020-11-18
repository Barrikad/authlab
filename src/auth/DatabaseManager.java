package auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.Properties;

//It is assumed that only the verifier would be able to connect to the manager and use these methods
public class DatabaseManager {

    private String jdbcDriver;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public DatabaseManager() throws ClassNotFoundException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("resources/database.properties"));
        jdbcDriver=prop.getProperty("jdbcDriver");
        dbUrl=prop.getProperty("dbUrl");
        dbUser=prop.getProperty("dbUser");
        dbPassword=prop.getProperty("dbPassword");
        Class.forName(jdbcDriver);
    }
    
    //just throw the error and assume it's a failed login
    public byte[] getSalt(String user) throws SQLException {
    	PreparedStatement stmt = null;
        ResultSet rst = null;
        Connection conn = null;
        byte[] result = {};
        
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = " SELECT salt FROM user_data WHERE username=?";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, user);

        rst = stmt.executeQuery();
        rst.next();
        result = rst.getBytes(1);
        
        rst.close();
        stmt.close();
        conn.close();
        return result;
    }

    public boolean validateUser(String user, byte[] password) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rst = null;
        Connection conn = null;
        boolean result = false;
        
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = " SELECT password FROM user_data WHERE username=?";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, user);

        rst = stmt.executeQuery();
        rst.next();
        result = Arrays.equals(rst.getBytes(1), password);
        
        rst.close();
        stmt.close();
        conn.close();
        return result;
    }

    public void insertUser(String user, String[] permissions, byte[] password, byte[] salt) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
        
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        String permissionsF = "printer:";
        for(String p : permissions) {
        	permissionsF += p + ",";
        }
        String query = " INSERT INTO user_data (username, permissions, password, salt)"
                + " VALUES (?, ?, ?, ?)";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, user);
        //remove last ',' in permissionsF
        stmt.setString(2, permissionsF.substring(0, permissionsF.length()-1));
        stmt.setBytes(3, password);
        stmt.setBytes(4, salt);

        stmt.execute();
        
        stmt.close();
        conn.close();
    }

    public void deleteUser(String user) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
    
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = " DELETE FROM user_data WHERE username=?";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, user);

        stmt.executeUpdate();
        
        stmt.close();
        conn.close();
    }

    public void updateUserPassword(String user, byte[] password, byte[] salt) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
        
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = "UPDATE user_data "
                + "SET password = ? AND salt = ? "
                + "WHERE username = ?";
        stmt = conn.prepareStatement(query);
        stmt.setBytes(1, password);
        stmt.setBytes(2, salt);
        stmt.setString(3, user);

        stmt.execute();
        
        stmt.close();
        conn.close();
    }
    
    public String getPermissions(String username) throws SQLException {
    	PreparedStatement stmt = null;
        ResultSet rst = null;
        Connection conn = null;
        String result = "";
        
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = " SELECT permissions FROM user_data WHERE username=?";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);

        rst = stmt.executeQuery();
        rst.next();
        result = rst.getString(1);
        
        rst.close();
        stmt.close();
        conn.close();
        return result;
    }
}
