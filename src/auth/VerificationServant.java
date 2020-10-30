package auth;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VerificationServant extends UnicastRemoteObject implements VerificationService {
    private static final long serialVersionUID = 8868835734121491443L;
    private boolean shutdown = false;
    private Map<Long, String[]> sessions;
    private Set<Login> logins;
    private MessageDigest hasher;
    private SecureRandom random;

    public VerificationServant() throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        super();
        logins = new HashSet<>();
        sessions = new HashMap<>();
        hasher = MessageDigest.getInstance("SHA-512");
        random = new SecureRandom();
        Login admin = new Login(hasher, random);
        Login user = new Login(hasher, random);
        
        //TEST CODE! ENROLLMENT OF USERS IS NOT A PART OF THIS IMPLEMENTATION
        admin.setValues("admin", "password");
        user.setValues("user", "1234");
        logins.add(admin);
        logins.add(user);
        //-------------------------------------------------------------------
    }

    private class Login {
        public String user;
        public byte[] salt;
        private MessageDigest hasher;
        private SecureRandom random;
        private DatabaseManager databaseManager;

        public Login(MessageDigest hasher, SecureRandom random) throws ClassNotFoundException, IOException {
            this.hasher = hasher;
            this.random = random;
            this.databaseManager = new DatabaseManager();
        }

        public void setValues(String user, String password) {
            this.user = user;
            this.salt = new byte[16];
            random.nextBytes(salt);
            hasher.reset();
            hasher.update(salt);
            hasher.update(password.getBytes());
            databaseManager.insertUser(user, hasher.digest());
            hasher.reset();
        }

        public void updateValues(String user, String currentPassword, String newPassword) {
            hasher.reset();
            hasher.update(salt);
            hasher.update(currentPassword.getBytes());
            if (databaseManager.validateUser(user, hasher.digest())) {
                hasher.reset();
                hasher.update(salt);
                hasher.update(newPassword.getBytes());
                databaseManager.updateUserPassword(user, hasher.digest());
            }
            hasher.reset();
        }

        public boolean validLogin(String user, String password) {
            if (!user.equals(this.user)) {
                return false;
            }
            hasher.reset();
            hasher.update(salt);
            hasher.update(password.getBytes());
            boolean result = databaseManager.validateUser(user, hasher.digest());
            hasher.reset();
            return result;
        }
    }

    @Override
    public synchronized boolean shouldShutdown() throws RemoteException {
        //unexport self as last action
        if (shutdown) {
            UnicastRemoteObject.unexportObject(this, true);
        }
        return shutdown;
    }

    @Override
    public synchronized void shutdown() throws RemoteException {
        shutdown = true;
    }

    private boolean userExists(String user, String password) {
        for (Login lg : logins) {
            if (lg.validLogin(user, password)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized long generateSession(String username, String password, String serviceName) throws RemoteException, AuthException {
        if (!userExists(username, password)) {
            throw new AuthException("Username or password not valid");
        }

        long key = random.nextLong();
        String[] sessionVals = {username, serviceName};
        sessions.put(key, sessionVals);
        return key;
    }

    @Override
    public synchronized boolean verify(String username, String serviceName, long sessionKey) throws RemoteException, AuthException {
        String[] session = sessions.get(sessionKey);
        return session != null && session[0].equals(username) && session[1].equals(serviceName);

    }

}
