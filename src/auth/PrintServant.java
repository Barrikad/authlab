package auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
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
	//maps roles to users
	private Map<String,Integer> roles;
	//maps sessionkeys to users
	private Map<Long,String> sessions;
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
		roles = new HashMap<String,Integer>();
		printQueue = new HashMap<String,List<String[]>>();
		sessions = new HashMap<Long,String>();
		config = new HashMap<>();
		log = new ArrayList<>();
		getAllUserRoles();
		
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
	//get user roles from the policy file
    private void getAllUserRoles() {
    	try {
    		String pathname = "account.txt";
			File filename = new File(pathname);
			InputStreamReader reader = new InputStreamReader(new FileInputStream(filename));
			BufferedReader br = new BufferedReader(reader);
			String line = "";
			line = br.readLine();
			String[] arr = line.split("\\s+");
			roles.put(arr[0], Integer. parseInt(arr[1]));
			while(line != null) {
				line = br.readLine();
				if (line != null) {
					String[] arr2 = line.split("\\s+");
					roles.put(arr2[0], Integer. parseInt(arr2[1]));
				}
			}
    		
    	}catch (Exception e) {
			e.printStackTrace();
		}
    }
    //Check user's authority, 1 means highest authority, can access every service, using a Hierarchical structure, 4 levels in total
    private void checkUserRole(String userName,int serviceLevel) throws DisabledException {
    	roles.get(userName);
		if(roles.get(userName)>serviceLevel) {
			throw new DisabledException("You don't have the authority to use this service");
		}
	}
	@Override
	public synchronized String print(String filename, String printer, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();	
		String user = checkUser(sessionKey);
		checkUserRole(user,4);
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
		checkUserRole(user,3);
		List<String[]> queue = printQueue.get(printer);
		
		String lEntry = user + "; queue; " + printer;
		log.add(lEntry);
		return queue;
	}

	@Override
	public synchronized String topQueue(String printer, int job, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		checkUserRole(user,3);
		List<String[]> queue = printQueue.get(printer);
		
		String lEntry = user + "; topQueue; " + printer + "; " + queue.get(job)[1];
		log.add(lEntry);
		
		Collections.swap(queue, 0, job);
		return "Job swapped with job at position 0"; 
	}

	@Override
	public synchronized String start(long sessionKey) throws RemoteException, AuthException, DisabledException {
		String user = checkUser(sessionKey);
		checkUserRole(user,2);
		online = true;
		
		String lEntry = user + "; start";
		log.add(lEntry);
		return "Printer started";
	}

	@Override
	public synchronized String stop(long sessionKey) throws RemoteException, AuthException, DisabledException {
		String user = checkUser(sessionKey);
		checkUserRole(user,2);
		online = false;
		for(String printer : printQueue.keySet()) {
			printQueue.get(printer).clear();
		}
		
		String lEntry = user + "; stop";
		log.add(lEntry);
		return "Printer stopped";
	}

	@Override
	public synchronized String restart(long sessionKey) throws RemoteException, AuthException, DisabledException {
		String user = checkUser(sessionKey);
		checkUserRole(user,3);
		stop(sessionKey);
		start(sessionKey);
		
		//accountability ensured by start and stop
		return "Printer restarted";
	}

	@Override
	public synchronized String status(String printer, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		checkUserRole(user,2);
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
		checkUserRole(user,2);
		String lEntry = user + "; readConfig; " + parameter;
		log.add(lEntry);
		return config.get(parameter);
	}

	@Override
	public synchronized String setConfig(String parameter, String value, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		checkUserRole(user,2);
		config.put(parameter, value);
		
		String lEntry = user + "; setConfig; " + parameter + "; " + value;
		log.add(lEntry);
		return "Parameter " + parameter + " set to " + value;
	}

	@Override
	public synchronized String shutdown(long sessionKey) throws RemoteException, AuthException {
		String user = checkUser(sessionKey);
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
		checkUserRole(user,1);
		String lEntry = user + "; getLog";
		log.add(lEntry);
		return log;
	}

	@Override
	public synchronized void wipeLog(long sessionKey) throws RemoteException, AuthException, DisabledException {
		String user = checkUser(sessionKey);
		checkUserRole(user,1);
		log.clear();
		String lEntry = user + "; wipeLog";
		log.add(lEntry);
	}

	@Override
	public void logout(long sessionKey) throws RemoteException, AuthException {
		sessions.remove(sessionKey);
	}
}
