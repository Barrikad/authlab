package auth;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface VerificationService extends Remote {
	
	void shutdown() throws RemoteException;

	boolean shouldShutdown() throws RemoteException;

	//returns new session key if authentication succeeds
	long generateSession(String username, String password, String serviceName) throws RemoteException, AuthException;
	
	boolean verify(String username, String serviceName, long sessionKey) throws RemoteException, AuthException;

	void endSession(long sessionKey) throws RemoteException, AuthException;
}
