package auth;

import java.rmi.*;
import java.util.List;


public interface PrintService extends Remote{
	
	//Attempts to bind session to user
	public String login(String username, long sessionKey) throws RemoteException, AuthException;
	
	//Print file with specified printer
	//Return status from printer
	public String print(String filename, String printer, long sessionKey) throws RemoteException,AuthException, DisabledException;
	
	//Starts a protected print job
	//A protected job can not stopped or suppressed by another client
	public String printProtected(String filename, String printer, long sessionKey) throws RemoteException,AuthException, DisabledException;
	
	//Removes given job from queue of printer
	public String abort(String printer, int job, long sessionKey) throws RemoteException,AuthException, DisabledException;
	
	//Returns the queue of the printer
	//Hides jobs not owned by client
	public List<String[]> queue(String printer, long sessionKey) throws RemoteException,AuthException, DisabledException;
	
	//Moves job to top of queue
	//Returns info about rearrangement
	//Only rearranges jobs owned by client
	public String topQueue(String printer, int job, long sessionKey) throws RemoteException,AuthException, DisabledException;
	
	//Starts printer services
	public String start(long sessionKey) throws RemoteException,AuthException;
	
	//Stops printer services
	//Clears queues of all printers
	//Can't be used if protected jobs are queued
	public String stop(long sessionKey) throws RemoteException,AuthException;
	
	//Stops and starts printer services
	//This will clear queues
	//Can't be used if protected jobs are queued
	public String restart(long sessionKey) throws RemoteException,AuthException;
	
	//Returns status of given printer
	public String status(String printer, long sessionKey) throws RemoteException,AuthException, DisabledException;
	
	//Returns value of parameter in config
	public String readConfig(String parameter, long sessionKey) throws RemoteException,AuthException, DisabledException;
	
	//Changes value of parameter in config
	public String setConfig(String parameter, String value, long sessionKey) throws RemoteException, AuthException, DisabledException;
	
	//Shuts down server
	//Server can't be restarted after this command
	//Will not save state or consider protected jobs
	public String shutdown(long sessionKey) throws RemoteException, AuthException;
	
	//nonremote shutdown-message to rmi
	public boolean shouldShutdown() throws RemoteException;
	
	//admin command. Can not be run by other users than admin
	//stops and wipes even protected processes
	public void stopProtected(long sessionKey) throws RemoteException, AuthException;

	//admin command
	//returns log
	public List<String> getLog(long sessionKey) throws RemoteException, AuthException;
	
	//admin command
	//wipe log
	public void wipeLog(long sessionKey) throws RemoteException, AuthException;
}
