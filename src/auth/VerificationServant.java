package auth;

import auth.enums.Permission;
import auth.enums.Role;
import auth.enums.Service;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;

public class VerificationServant extends UnicastRemoteObject implements VerificationService {
    private static final long serialVersionUID = 8868835734121491443L;
    private boolean shutdown = false;
    private Map<Long, String[]> sessions;
    private Map<Role, Map<Permission, Service>> roles;
    private MessageDigest hasher;
    private SecureRandom random;
    private DatabaseManager databaseManager;

    public VerificationServant() throws IOException, NoSuchAlgorithmException, ClassNotFoundException, SQLException {
        super();
        sessions = new HashMap<>();
        hasher = MessageDigest.getInstance("SHA-512");
        random = new SecureRandom();
        databaseManager = new DatabaseManager();
        roles = databaseManager.getRoles();
    }

    private void updateValues(String user, String currentPassword, String newPassword) throws AuthException {
        if (validLogin(user, currentPassword)) {
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            hasher.reset();
            hasher.update(salt);
            hasher.update(newPassword.getBytes());
            try {
                databaseManager.updateUserPassword(user, hasher.digest(), salt);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            hasher.reset();
        } else {
            throw new AuthException("failed to authenticate user");
        }
    }

    // returns false if login is invalid or a database error happens
    private boolean validLogin(String user, String password) {
        byte[] salt;
        try {
            salt = databaseManager.getSalt(user);
        } catch (SQLException e) {
            return false;
        }
        if (salt.length == 0 || password.length() == 0) {
            return false;
        }
        hasher.reset();
        hasher.update(salt);
        hasher.update(password.getBytes());
        boolean result;
        try {
            result = databaseManager.validateUser(user, hasher.digest());
        } catch (SQLException e) {
            return false;
        }
        hasher.reset();
        return result;
    }

    @Override
    public synchronized boolean shouldShutdown() throws RemoteException {
        // unexport self as last action
        if (shutdown) {
            UnicastRemoteObject.unexportObject(this, true);
        }
        return shutdown;
    }

    @Override
    // test code! enrollment is not a part of this implementation
    public void setValues(String user, String password) throws RemoteException {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        hasher.reset();
        hasher.update(salt);
        hasher.update(password.getBytes());
        try {
            databaseManager.insertUser(user, hasher.digest(), salt);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        hasher.reset();
    }

    @Override
    // test code! enrollment is not a part of this implementation
    public void unsetValues(String user) throws RemoteException {
        try {
            databaseManager.deleteUser(user);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void shutdown() throws RemoteException {
        shutdown = true;
    }

    @Override
    public synchronized long generateSession(String username, String password, Service serviceName)
            throws RemoteException, AuthException {
        if (!validLogin(username, password)) {
            throw new AuthException("Username or password not valid");
        }

        long key = random.nextLong();

        Role role = null;
        try {
            role = databaseManager.getUserRole(username, serviceName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String[] sessionVals = { username, role.toString(), serviceName.toString() };
        sessions.put(key, sessionVals);
        return key;
    }

    @Override
    public synchronized boolean verify(String username, Service serviceName, long sessionKey)
            throws RemoteException, AuthException {
        String[] session = sessions.get(sessionKey);
        return session != null && session[0].equals(username) && session[2].equals(serviceName.toString());

    }

    @Override
    public void endSession(long sessionKey) throws RemoteException, AuthException {
        sessions.remove(sessionKey);
    }

    @Override
    public List<Permission> getPermissions(long sessionKey) throws RemoteException, AuthException {
        String[] session = sessions.get(sessionKey);
        if (session == null) {
            throw new AuthException("no session found");
        }
        List<Permission> result = new ArrayList<>();
        for (Map.Entry<Permission, Service> entry : roles.get(Role.valueOf(session[1])).entrySet()) {
            if (entry.getValue().toString().equals(session[2]))
                result.add(entry.getKey());
        }

        if (result.isEmpty())
            throw new AuthException("no permissions found!");
        else
            return result;
    }
}
