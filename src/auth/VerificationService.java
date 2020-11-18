package auth;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface VerificationService extends Remote {
	
	void shutdown() throws RemoteException;

	boolean shouldShutdown() throws RemoteException;

	//returns new session key if authentication succeeds
	long generateSession(String username, String password, String serviceName) throws RemoteException, AuthException;
	
	//test code! enrollment is not a part of this implementation
    public void setValues(String user, String[] permissions,String password) throws RemoteException;
    public void unsetValues(String user) throws RemoteException;
    
	boolean verify(String username, String serviceName, long sessionKey) throws RemoteException, AuthException;
	String[] getPermissions(long sessionKey) throws RemoteException, AuthException;
	
	void endSession(long sessionKey) throws RemoteException, AuthException;
}
