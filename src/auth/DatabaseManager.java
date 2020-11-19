package auth;

import auth.enums.Permission;
import auth.enums.Role;
import auth.enums.Service;

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

    class DBData {
        private Integer role_id;
        private Role role_title;
        private Integer parent_id;
        private String service;
        private Permission permission;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DBData)) return false;
            DBData dbData = (DBData) o;
            return role_id.equals(dbData.role_id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(role_id);
        }
    }


    public DatabaseManager() throws ClassNotFoundException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("resources/database.properties"));
        jdbcDriver = prop.getProperty("jdbcDriver");
        dbUrl = prop.getProperty("dbUrl");
        dbUser = prop.getProperty("dbUser");
        dbPassword = prop.getProperty("dbPassword");
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

    public Role getUserRole(String username, Service serviceName) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rst = null;
        Connection conn = null;
        String result;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = " SELECT role_title " +
                "FROM user_data natural join user_role natural join role_data natural join " +
                "role_permission natural join permission_data " +
                "WHERE username=? and service=?";
        stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        stmt.setString(2, serviceName.toString());
        rst = stmt.executeQuery();
        rst.next();
        result = rst.getString(1);

        rst.close();
        stmt.close();
        conn.close();

        return Role.valueOf(result);
    }

    public Map<Role, Map<Permission, Service>> getRoles() throws SQLException {

        Map<Integer, Role> roles = new HashMap<>();
        Map<Role, Map<Permission, Service>> rolesWithPermissions = new HashMap<>();
        List<DBData> dbDataList = new ArrayList<>();

        PreparedStatement stmt = null;
        ResultSet rst = null;
        Connection conn = null;

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        String query = "select role_id,role_title, parent_id, service, permission " +
                "from role_data natural join role_permission natural join permission_data";
        stmt = conn.prepareStatement(query);
        rst = stmt.executeQuery();

        while (rst.next()) {
            DBData dbData = new DBData();
            dbData.role_id = rst.getInt("role_id");
            dbData.role_title = Role.valueOf(rst.getString("role_title"));
            dbData.parent_id = rst.getInt("parent_id");
            dbData.service = rst.getString("service");
            dbData.permission = Permission.valueOf(rst.getString("permission"));
            dbDataList.add(dbData);
            roles.putIfAbsent(dbData.role_id, dbData.role_title);
        }

        rst.close();
        stmt.close();
        conn.close();

        for (Map.Entry<Integer, Role> entry : roles.entrySet()) {
            Map<Permission,Service> permissionsForRole = new HashMap<>();
            this.getPermissionsFromRole(permissionsForRole, dbDataList, entry.getKey());
            rolesWithPermissions.put(entry.getValue(), permissionsForRole);
        }
        return rolesWithPermissions;
    }

    private void getPermissionsFromRole(Map<Permission,Service> permissionsForRole, List<DBData> dbList, Integer roleID) {
        for (DBData dbData : dbList) {
            if (dbData.role_id.equals(roleID)) {
                permissionsForRole.put(dbData.permission,Service.valueOf(dbData.service));
            }
        }
        for (DBData dbData : dbList.stream().distinct().collect(Collectors.toList())) {
            if (dbData.parent_id.equals(roleID))
                getPermissionsFromRole(permissionsForRole, dbList, dbData.role_id);
        }
    }
}
