package auth;

import auth.enums.Permission;
import auth.enums.Role;
import auth.enums.Service;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface VerificationService extends Remote {
	
	void shutdown() throws RemoteException;

	boolean shouldShutdown() throws RemoteException;

	//returns new session key if authentication succeeds
	long generateSession(String username, String password, Service serviceName) throws RemoteException, AuthException;
	
	//test code! enrollment is not a part of this implementation
    public void setValues(String user, String password) throws RemoteException;
    public void unsetValues(String user) throws RemoteException;
    
	boolean verify(String username, Service serviceName, long sessionKey) throws RemoteException, AuthException;
	List<Permission> getPermissions(long sessionKey) throws RemoteException, AuthException;

	void endSession(long sessionKey) throws RemoteException, AuthException;


}
