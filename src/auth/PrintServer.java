package auth;

import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class PrintServer implements Runnable {
	private final static int portNumber = 2019;
	private final static String name = "printer";

	public static void main(String[] args) throws Exception {
		PrintServant printServant = new PrintServant();
		
		Registry registry = LocateRegistry.createRegistry(portNumber);
		registry.bind(name, printServant);
		
		//busy-wait, might wanna change this
		while(!printServant.shouldShutdown()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				;//pass if interrupted, doesn't matter
			}
		}
		try {
		UnicastRemoteObject.unexportObject(registry, true);
		System.out.println("Stopping rmi server");
		}catch(NoSuchObjectException e) {
			System.out.println("Server already stopped");
		}
	}

	@Override
	public void run() {
		String[] args = {};
		try {
			main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
