package auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

//It is assumed that only the verifier would be able to connect to the manager and use these methods
public class DatabaseManager {

    private String jdbcDriver;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    class DBData{
        private Integer role_id;
        private String role_title;
        private Integer parent_id;
        private String device;
        private String operation;

        DBData(){}

        DBData (DBData dbData){
            role_id=dbData.role_id;
            role_title=dbData.role_title;
            parent_id=dbData.parent_id;
            device=dbData.device;
            operation=dbData.operation;
        }
    }


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

    public void insertUser(String user, byte[] password, byte[] salt) throws SQLException {
        PreparedStatement stmt = null;
        Connection conn = null;
        
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = " INSERT INTO user_data (username, password, salt)"
                + " VALUES (?, ?, ?)";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, user);
        //remove last ',' in permissionsF
        stmt.setBytes(2, password);
        stmt.setBytes(3, salt);

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

    public Map getRoles() throws SQLException{

        Map<Integer,String> roles = new HashMap<>();
        Map<String,List<String>> rolesWithPermissions = new HashMap<>();
        List<DBData> dbDataList = new ArrayList<>();

        PreparedStatement stmt = null;
        ResultSet rst = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = "select role_id,role_title, parent_id, device, operation " +
                "from role_data natural join role_permission natural join permission_data;";
        stmt = conn.prepareStatement(query);
        rst = stmt.executeQuery();

        while ( rst.next() )
        {
            DBData dbData = new DBData();
            dbData.role_id = rst.getInt("role_id");
            dbData.role_title = rst.getString("role_title");
            dbData.parent_id = rst.getInt("parent_id");
            dbData.device = rst.getString("device");
            dbData.operation = rst.getString("operation");
            dbDataList.add(dbData);
            roles.putIfAbsent(dbData.role_id,dbData.role_title);

        }
        rst.close();
        stmt.close();
        conn.close();

        for (Map.Entry<Integer,String> entry: roles.entrySet()){
            List<String> operationsForRole= new ArrayList<>();
            this.getOperations(operationsForRole,
                    dbDataList.stream().map(DBData::new).collect(Collectors.toList()), entry.getKey());
            rolesWithPermissions.put(entry.getValue(),operationsForRole);
        }
        return rolesWithPermissions;
    }

    public void getOperations(List<String> operationsForRole, List<DBData> dbList, Integer roleID){
        for(DBData dbData: dbList){
            if (dbData.role_id==roleID) {
                operationsForRole.add(dbData.operation);
            }
        }

        dbList=dbList.stream()
                .filter(entry->entry.role_id!=roleID)
                .collect(Collectors.toList());

        for(DBData dbData: dbList){
            if (dbData.parent_id==roleID)
                getOperations(operationsForRole,
                        dbList,
                        dbData.role_id);
        }
    }
}
