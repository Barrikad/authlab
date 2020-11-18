package auth;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PrintServant extends UnicastRemoteObject implements PrintService {

	private static final long serialVersionUID = 4197272341926558367L;
	

	private final int portNumber = 2019;
	private final String vName = "verification";
	//maps sessionkeys to users
	private Map<Long,String> sessions;
	//map sessionkeys to permissions
	private Map<Long,String[]> permissions;
	//a list for each printer with jobs and their owners
	private Map<String,List<String[]>> printQueue;
	//config file stand-in
	private Map<String,String> config;
	//log file stand-in
	private List<String> log;
	
	private int printerNum = 5;
	private boolean shutdown = false;
	private boolean online = true;

	public PrintServant() throws RemoteException {
		super();
		permissions = new HashMap<>();
		printQueue = new HashMap<String,List<String[]>>();
		sessions = new HashMap<Long,String>();
		config = new HashMap<>();
		log = new ArrayList<>();
		
		for(int i = 0; i < printerNum; i++) {
			printQueue.put("printer" + String.valueOf(i + 1), new ArrayList<String[]>());
		}
	}
	
	private void checkOnline() throws DisabledException {
		if(!online) {
			throw new DisabledException("Printer is offline. Use start() to enable services");
		}
	}

	private String checkUser(long sessionKey) throws AuthException {
		String user = sessions.get(sessionKey);
		if(user == null) {
			throw new AuthException("No session with given key exists");
		}
		return user;
	}
	
	private void checkPermission(long sessionKey, String permission) throws AuthException {
		String[] userPermissions = permissions.get(sessionKey);
		for (String p : userPermissions) {
			if(p.equals(permission)) {
				return;
			}
		}
		throw new AuthException("Permission denied");
	}
	
	@Override
	public synchronized String print(String filename, String printer, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();	
		String user = checkUser(sessionKey);
		checkPermission(sessionKey,"p");
		String[] entry = {user,filename};
		printQueue.get(printer).add(entry);
		
		String lEntry = user + "; print; " + printer + "; " + filename;
		log.add(lEntry);
		return "job added to printer";
	}

	@Override
	public synchronized String abort(String printer, int job, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		checkPermission(sessionKey,"a");
		List<String[]> queue = printQueue.get(printer);
		String[] removed = queue.remove(job);
		
		String lEntry = user + "; abort; " + printer + "; " + removed[1];
		log.add(lEntry);
		return "Job " + removed[1] + " removed from " + printer;
	}

	@Override
	public synchronized List<String[]> queue(String printer, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		checkPermission(sessionKey,"q");
		List<String[]> queue = printQueue.get(printer);
		
		String lEntry = user + "; queue; " + printer;
		log.add(lEntry);
		return queue;
	}

	@Override
	public synchronized String topQueue(String printer, int job, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		checkPermission(sessionKey,"tq");
		List<String[]> queue = printQueue.get(printer);
		
		String lEntry = user + "; topQueue; " + printer + "; " + queue.get(job)[1];
		log.add(lEntry);
		
		Collections.swap(queue, 0, job);
		return "Job swapped with job at position 0"; 
	}
	
	private void startP() {
		online = true;
	}
	private void stopP() {
		online = false;
		for(String printer : printQueue.keySet()) {
			printQueue.get(printer).clear();
		}
	}

	@Override
	public synchronized String start(long sessionKey) throws RemoteException, AuthException{
		String user = checkUser(sessionKey);
		checkPermission(sessionKey,"sa");
		
		startP();
		
		String lEntry = user + "; start";
		log.add(lEntry);
		return "Printer started";
	}

	@Override
	public synchronized String stop(long sessionKey) throws RemoteException, AuthException {
		String user = checkUser(sessionKey);
		checkPermission(sessionKey,"so");
		
		stopP();
		
		String lEntry = user + "; stop";
		log.add(lEntry);
		return "Printer stopped";
	}

	@Override
	public synchronized String restart(long sessionKey) throws RemoteException, AuthException, DisabledException {
		String user = checkUser(sessionKey);
		checkPermission(sessionKey,"r");
		stopP();
		startP();
		String lEntry = user + "; restart";
		log.add(lEntry);
		//accountability ensured by start and stop
		return "Printer restarted";
	}

	@Override
	public synchronized String status(String printer, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		checkPermission(sessionKey,"su");
		List<String[]> queue = printQueue.get(printer);
		boolean runs = !queue.isEmpty();
		int qSize = queue.size();
		String nextJob;
		if(runs) {
			nextJob = queue.get(0)[1];
		}else {
			nextJob = "null";
		}
		String message = "Running : " + String.valueOf(runs) + "; " +
						 "NextJob : " + nextJob + "; " + 
						 "QueueSize : " + String.valueOf(qSize);
		
		String lEntry = user + "; status; " + printer;
		log.add(lEntry);
		return message;
	}

	@Override
	public synchronized String readConfig(String parameter, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		checkPermission(sessionKey,"rc");
		String lEntry = user + "; readConfig; " + parameter;
		log.add(lEntry);
		return config.get(parameter);
	}

	@Override
	public synchronized String setConfig(String parameter, String value, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		checkPermission(sessionKey,"sc");
		config.put(parameter, value);
		
		String lEntry = user + "; setConfig; " + parameter + "; " + value;
		log.add(lEntry);
		return "Parameter " + parameter + " set to " + value;
	}

	@Override
	public synchronized String shutdown(long sessionKey) throws RemoteException, AuthException {
		String user = checkUser(sessionKey);
		//same permission as stop
		checkPermission(sessionKey,"so");
		shutdown = true;
		
		String lEntry = user + "; shutdown";
		log.add(lEntry);
		return "Server hard shut down";
	}

	@Override
	public synchronized boolean shouldShutdown() throws RemoteException {
		//unexport self as last action
		if(shutdown == true) {
			UnicastRemoteObject.unexportObject(this, true);
		}
		return shutdown;
	}

	@Override
	public synchronized String login(String username, long sessionKey) throws RemoteException, AuthException {
		//connect to verifier
		//assuming secured connection with signature from verifier
		try {
			Registry registry = LocateRegistry.getRegistry(InetAddress.getLocalHost().getHostName(), this.portNumber);
			VerificationService verifier = (VerificationService) registry.lookup(vName);
			
			if(verifier.verify(username, "printer", sessionKey)) {
				sessions.put(sessionKey, username);
//				permissions.put(sessionKey, verifier.getPermissions(sessionKey));
			}else {
				throw new AuthException("user was not verified by database");
			}
		} catch (RemoteException | UnknownHostException | NotBoundException e) {
			e.printStackTrace();
		}
		
		String lEntry = username + "; login";
		log.add(lEntry);
		return "successfully started session with " + username;
	}

	@Override
	public synchronized List<String> getLog(long sessionKey) throws RemoteException, AuthException, DisabledException {
		String user = checkUser(sessionKey);
		//same permission as read config
		checkPermission(sessionKey,"rc");
		String lEntry = user + "; getLog";
		log.add(lEntry);
		return log;
	}

	@Override
	public synchronized void wipeLog(long sessionKey) throws RemoteException, AuthException, DisabledException {
		String user = checkUser(sessionKey);
		//same permission as set config
		checkPermission(sessionKey,"sc");
		log.clear();
		String lEntry = user + "; wipeLog";
		log.add(lEntry);
	}

	@Override
	public void logout(long sessionKey) throws RemoteException, AuthException {
		sessions.remove(sessionKey);
	}
}
