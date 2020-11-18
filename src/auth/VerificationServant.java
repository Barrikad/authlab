package auth;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VerificationServant extends UnicastRemoteObject implements VerificationService {
    private static final long serialVersionUID = 8868835734121491443L;
    private boolean shutdown = false;
    private Map<Long, String[]> sessions;
    private MessageDigest hasher;
    private SecureRandom random;
    private DatabaseManager databaseManager;

    public VerificationServant() throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        super();
        sessions = new HashMap<>();
        hasher = MessageDigest.getInstance("SHA-512");
        random = new SecureRandom();
        this.databaseManager = new DatabaseManager();
       
    }

    private void updateValues(String user, String currentPassword, String newPassword) throws AuthException {
        if (validLogin(user,currentPassword)) {
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
        }else {
        	throw new AuthException("failed to authenticate user");
        }
    }

    //returns false if login is invalid or a database error happens
    private boolean validLogin(String user, String password) {
        byte[] salt;
		try {
			salt = databaseManager.getSalt(user);
		} catch (SQLException e) {
			return false;
		}
        if(salt.length == 0 || password.length() == 0) {
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
        //unexport self as last action
        if (shutdown) {
            UnicastRemoteObject.unexportObject(this, true);
        }
        return shutdown;
    }
    
    @Override
    //test code! enrollment is not a part of this implementation
    public void setValues(String user, String[] permissions,String password) throws RemoteException{
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        hasher.reset();
        hasher.update(salt);
        hasher.update(password.getBytes());
        try {
			databaseManager.insertUser(user, permissions, hasher.digest(),salt);
		} catch (SQLException e) {
			e.printStackTrace();
		}
        hasher.reset();
    }
    
    @Override
    //test code! enrollment is not a part of this implementation
    public void unsetValues(String user) throws RemoteException{
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
    public synchronized long generateSession(String username, String password, String serviceName) throws RemoteException, AuthException {
        if (!validLogin(username, password)) {
            throw new AuthException("Username or password not valid");
        }

        long key = random.nextLong();
        
        String permissions = "";
		try {
			permissions = databaseManager.getPermissions(username);
		} catch (SQLException e) {
			e.printStackTrace();
		}
        String[] sessionVals = {username, serviceName, permissions};
        sessions.put(key, sessionVals);
        return key;
    }

    @Override
    public synchronized boolean verify(String username, String serviceName, long sessionKey) throws RemoteException, AuthException {
        String[] session = sessions.get(sessionKey);
        return session != null && session[0].equals(username) && session[1].equals(serviceName);

    }

	@Override
	public void endSession(long sessionKey) throws RemoteException, AuthException {
		sessions.remove(sessionKey);
	}

	@Override
	public String[] getPermissions(long sessionKey) throws RemoteException, AuthException {
		String[] session = sessions.get(sessionKey);
		if(session == null) {
			throw new AuthException("no session found");
		}
		String[] pers = session[2].split(";");
		for(String str : pers) {
			String[] temp = str.split(":");
			//find service matching session
			if(temp[0].equals(session[1])) {
				return temp[1].split(",");
			}
		}
		throw new AuthException("no permissions found!");
	}

}
