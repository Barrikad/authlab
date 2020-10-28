package tests;

import auth.AuthException;
import auth.DisabledException;
import auth.PrintServer;
import auth.PrintService;
import auth.VerificationServer;
import auth.VerificationService;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.junit.jupiter.api.*;

/*
 * TODO:
 * authenticate printer to verifier
 * authenticate verifier to user
 * authenticate printer to user
 * authenticate verifier to printer
 * persistency
 * formal analysis and modeling
 * accountability
 * password changes
 */

/*
 * NOTES:
 * synchronized to avoid protected file being lost
 */


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SuccessTests{
	private PrintService printServer;
	private VerificationService verifier;
	private Registry registry;
	private final int portNumber = 2019;
	private final String serviceName = "printer";
	private final String vName = "verification";
	private final String username = "admin";
	private final String password = "password";
	private final String projectLocation = "C:\\Users\\simon\\eclipse-workspace\\authentication";
	private final String printerClass = "auth.PrintServer";
	private final String verificationClass = "auth.VerificationServer";
	private long sessionKey;
	private long sessionKey2;
	private String[] job1 = {username,"file1.txt","np"};
	private String[] job2 = {username,"file2.txt","np"};
	private String[] job3 = {username,"file3.txt","np"};

	@BeforeAll
	void setUp() throws NotBoundException, IOException, InterruptedException, AuthException {
		
		/*
		Runtime runtime = Runtime.getRuntime();
		Process serverProcess = runtime.exec("java -classpath " + this.projectLocation + " " + this.printerClass);
		Thread.sleep(2000);
		Process verificationProcess = runtime.exec("java -classpath " + this.projectLocation + " " + this.verificationClass);
		Thread.sleep(2000);*/
		
		Thread printThread = new Thread(new PrintServer());
		Thread veriThread = new Thread(new VerificationServer());
		printThread.start();
		Thread.sleep(1000);
		veriThread.start();
		Thread.sleep(1000);
		
		
		//connect to printer
		//assuming secured connection with signature from printer
		registry = LocateRegistry.getRegistry(InetAddress.getLocalHost().getHostName(), this.portNumber);
		printServer = (PrintService) registry.lookup(serviceName);
		
		//connect to verifier
		verifier = (VerificationService) registry.lookup(vName);
		
		//authenticate to verifier
		sessionKey = verifier.generateSession(username,password,serviceName);
		sessionKey2 = verifier.generateSession("user", "1234", serviceName);
		
		printServer.login("user", sessionKey2);
		printServer.login(username,sessionKey);
	}
	
	
	@BeforeEach
	void start() throws RemoteException, AuthException {
		printServer.start(sessionKey);
	}
	
	@AfterEach
	void stop() throws RemoteException, AuthException {
		printServer.stopProtected(sessionKey);
	}
	
	@AfterAll
	void tearDown() throws AuthException, RemoteException, InterruptedException, NotBoundException {
		printServer.shutdown(sessionKey);
		verifier.shutdown();
		registry.unbind(serviceName);
		registry.unbind(vName);
	}
	
	@Test
	void canPrint() throws RemoteException, AuthException, DisabledException {
		printServer.print("file.txt", "printer1",sessionKey);
		List<String[]> queue = printServer.queue("printer1",sessionKey);
		
		String[] expected = {username,"file.txt","np"};
		boolean found = false;
		for(String[] elem : queue) {
			if(Arrays.equals(elem,expected)) {
				found = true;
				break;
			}
		}
		assertTrue(found);
	}
	
	@Test
	void canDisable() throws RemoteException, AuthException, DisabledException{
		printServer.stop(sessionKey);
		assertThrows(DisabledException.class,() -> {
			printServer.print("file.txt", "printer1", sessionKey);
		});
	}
	
	@Test
	void canRestartAndWipe() throws RemoteException, AuthException, DisabledException{
		printServer.print("file.txt", "printer1", sessionKey);
		printServer.restart(sessionKey);
		List<String[]> pQueue = printServer.queue("printer1", sessionKey);
		assertTrue(pQueue.size() == 0);
	}
	
	@Test
	void canOrderAndReorder() throws RemoteException, AuthException, DisabledException{
		printServer.print("file1.txt", "printer1", sessionKey);
		printServer.print("file2.txt", "printer1", sessionKey);
		printServer.print("file3.txt", "printer1", sessionKey);
		List<String[]> queue = printServer.queue("printer1", sessionKey);
		assertArrayEquals(queue.get(0),job1);
		assertArrayEquals(queue.get(1),job2);
		assertArrayEquals(queue.get(2),job3);
		
		printServer.topQueue("printer1", 2, sessionKey);
		queue = printServer.queue("printer1", sessionKey);
		assertArrayEquals(queue.get(0),job3);
	}
	
	@Test
	void canGetStatus() throws RemoteException, AuthException, DisabledException{
		printServer.print("file1.txt", "printer1", sessionKey);
		printServer.print("file2.txt", "printer1", sessionKey);
		printServer.print("file3.txt", "printer1", sessionKey);
		String status1 = printServer.status("printer1", sessionKey);
		String status2 = printServer.status("printer2", sessionKey);
		assertEquals("Running : true; NextJob : file1.txt; QueueSize : 3", status1);
		assertEquals("Running : false; NextJob : null; QueueSize : 0", status2);
	}
	
	@Test
	void canChangeConfig() throws RemoteException, AuthException, DisabledException{
		printServer.setConfig("Black/White", "true", sessionKey);
		String value = printServer.readConfig("Black/White", sessionKey);
		assertEquals("true",value);
	}
	
	@Test
	void canAbort() throws RemoteException, AuthException, DisabledException{
		printServer.print("file1.txt", "printer1", sessionKey);
		printServer.print("file2.txt", "printer1", sessionKey);
		printServer.print("file3.txt", "printer1", sessionKey);
		printServer.abort("printer1", 1, sessionKey);
		List<String[]> queue = printServer.queue("printer1", sessionKey);
		assertArrayEquals(queue.get(0),job1);
		assertArrayEquals(queue.get(1),job3);
		assertEquals(queue.size(),2);
	}
	
	@Test
	void cantWipeProtected() throws RemoteException, AuthException, DisabledException{
		printServer.printProtected("file1.txt", "printer1", sessionKey2);
		printServer.print("file2.txt", "printer1", sessionKey);
		printServer.topQueue("printer1", 1, sessionKey);
		List<String[]> queue = printServer.queue("printer1", sessionKey);
		String[] job = {username,"file2.txt","np"};
		assertArrayEquals(queue.get(1),job);
	}
	
	@Test
	void censorsProtected() throws RemoteException, AuthException, DisabledException{
		printServer.printProtected("file1.txt", "printer1", sessionKey2);
		printServer.print("file2.txt", "printer1", sessionKey);
		List<String[]> queue = printServer.queue("printer1", sessionKey);
		assertFalse(queue.get(0)[0].equals("user") || queue.get(0)[1].equals("file1.txt"));
	}
	
	@Test
	void cantStopProtected() throws RemoteException, AuthException, DisabledException{
		printServer.print("file2.txt", "printer2", sessionKey);
		printServer.printProtected("file1.txt", "printer2", sessionKey2);
		assertThrows(AuthException.class,() ->{
			printServer.stop(sessionKey2);
		});
	}
	
	@Test
	void canFailVerification() throws RemoteException, AuthException, DisabledException{
		assertThrows(AuthException.class, ()->{
			verifier.generateSession("admin", "wrong", "printer");
		});
	}
	
	@Test
	void canFailPrinterSession() throws RemoteException, AuthException, DisabledException{
		long sKey = 123456L;
		assertThrows(AuthException.class,()->{
			printServer.login("user2", sKey);
		});
	}
	
	@Test
	void actionsAreLogged() throws RemoteException, AuthException, DisabledException{
		printServer.wipeLog(sessionKey);
		printServer.print("file1.txt", "printer1", sessionKey);
		printServer.printProtected("file2.txt", "printer1", sessionKey);
		printServer.queue("printer1", sessionKey);
		printServer.abort("printer1", 0, sessionKey);
		printServer.setConfig("fast", "true", sessionKey);
		printServer.readConfig("fast", sessionKey);
		printServer.print("file1.txt", "printer1", sessionKey);
		printServer.topQueue("printer1", 1, sessionKey);
		printServer.stopProtected(sessionKey);
		printServer.start(sessionKey);
		printServer.restart(sessionKey);
		printServer.stop(sessionKey);
		List<String> log = printServer.getLog(sessionKey);
		
		String[] excpected = {"admin; print; printer1; file1.txt",
							  "admin; printProtected; printer1; file2",
							  "admin; queue; printer1",
							  "admin; abort; printer1; file1.txt",
							  "admin; setConfig; fast; true",
							  "admin; readConfig; fast",
							  "admin; print; printer1; file1.txt",
							  "admin; topQueue; printer1; file1.txt",
							  "admin; stopProtected",
							  "admin; start",
							  "admin; restart",
							  "admin; stop"};
		
		for(int i = 0; i < excpected.length; i++) {
			assertEquals(excpected[i],log.get(i));
		}
		
	}
}
