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

	@Override
	public synchronized String print(String filename, String printer, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		//"np" for not-protected
		String[] entry = {user,filename,"np"};
		printQueue.get(printer).add(entry);
		
		String lEntry = user + "; print; " + printer + "; " + filename;
		log.add(lEntry);
		return "job added to printer";
	}


	@Override
	public synchronized String printProtected(String filename, String printer, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		//"np" for not-protected
		String[] entry = {user,filename,"p"};
		printQueue.get(printer).add(entry);
		
		String lEntry = user + "; printProtected; " + printer + "; " + filename;
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
		List<String[]> queue = new ArrayList<>();
		for(String[] elem : printQueue.get(printer)) {
			//check if element is protected 
			if(!elem[0].equals(user) && elem[2].equals("p")) {
				String[] cencored = {"censored","censored","p"};
				queue.add(cencored);
			}else {
				queue.add(elem);
			}
		}
		
		String lEntry = user + "; queue; " + printer;
		log.add(lEntry);
		return queue;
	}

	@Override
	public synchronized String topQueue(String printer, int job, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		List<String[]> queue = printQueue.get(printer);
		
		//find first nonprotected job
		int index = 0;
		for(; index < queue.size(); index++) {
			if(queue.get(index)[2].equals("np")) {
				break;
			}
		}
		
		String lEntry = user + "; topQueue; " + printer + "; " + queue.get(job)[1];
		log.add(lEntry);
		
		if(index < job) {
			Collections.swap(queue, index, job);
			return "Job swapped with job at position " + String.valueOf(index); 
		}else {
			return "Could not swap with protected jobs";
		}
	}

	@Override
	public synchronized String start(long sessionKey) throws RemoteException, AuthException {
		String user = checkUser(sessionKey);
		System.out.println("Starting printer");
		online = true;
		
		String lEntry = user + "; start";
		log.add(lEntry);
		return "Printer started";
	}

	@Override
	public synchronized String stop(long sessionKey) throws RemoteException, AuthException {
		String user = checkUser(sessionKey);
		for(String printer : printQueue.keySet()) {
			for(String[] elem : printQueue.get(printer)) {
				if(elem[2].equals("p")) {
					throw new AuthException("Can't stop server while protected processes are running");
				}
			}
		}
		System.out.println("Stopping printer");
		online = false;
		for(String printer : printQueue.keySet()) {
			printQueue.get(printer).clear();
		}
		
		String lEntry = user + "; stop";
		log.add(lEntry);
		return "Printer stopped";
	}

	@Override
	public synchronized String restart(long sessionKey) throws RemoteException, AuthException {
		String user = checkUser(sessionKey);
		stop(sessionKey);
		start(sessionKey);
		
		//accountability ensure by start and stop
		return "Printer restarted";
	}

	@Override
	public synchronized String status(String printer, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
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
		
		String lEntry = user + "; readConfig; " + parameter;
		log.add(lEntry);
		return config.get(parameter);
	}

	@Override
	public synchronized String setConfig(String parameter, String value, long sessionKey) throws RemoteException, AuthException, DisabledException {
		checkOnline();
		String user = checkUser(sessionKey);
		config.put(parameter, value);
		
		String lEntry = user + "; setConfig; " + parameter + "; " + value;
		log.add(lEntry);
		return "Parameter " + parameter + " set to " + value;
	}

	@Override
	public synchronized String shutdown(long sessionKey) throws RemoteException, AuthException {
		String user = checkUser(sessionKey);
		if(!user.equals("admin")) {
			throw new AuthException("only admin can end protected jobs");
		}
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
	public synchronized void stopProtected(long sessionKey) throws RemoteException, AuthException {
		String user = checkUser(sessionKey);
		if(!user.equals("admin")) {
			throw new AuthException("only admin can end protected jobs");
		}
		System.out.println("Stopping printer");
		online = false;
		for(String printer : printQueue.keySet()) {
			printQueue.get(printer).clear();
		}
		
		String lEntry = user + "; stopProtected";
		log.add(lEntry);
	}

	@Override
	public synchronized List<String> getLog(long sessionKey) throws RemoteException, AuthException {
		String user = checkUser(sessionKey);
		if(!user.equals("admin")) {
			throw new AuthException("only admin can end protected jobs");
		}
		
		String lEntry = user + "; getLog";
		log.add(lEntry);
		return log;
	}

	@Override
	public synchronized void wipeLog(long sessionKey) throws RemoteException, AuthException {
		String user = checkUser(sessionKey);
		if(!user.equals("admin")) {
			throw new AuthException("only admin can end protected jobs");
		}
		log.clear();
		String lEntry = user + "; wipeLog";
		log.add(lEntry);
	}
}
