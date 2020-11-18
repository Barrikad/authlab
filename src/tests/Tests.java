package tests;

import auth.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.*;





@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Tests{
	private PrintService printServer;
	private VerificationService verifier;
	private Registry registry;
	private static final int userNumber = 7;
	private User[] users = new User[userNumber];
	private static final int portNumber = 2019;
	private static final String serviceName = "printer";
	private static final String vName = "verification";
	private static final String file="file.txt";
	private static final String file1="file1.txt";
	private static final String file2="file2.txt";
	private static final String file3="file3.txt";
	private static final String printer1="printer1";
	private static final String printer2="printer2";
	private String[] job1 = new String[2];
	private String[] job2 = new String[2];
	private String[] job3 = new String[2];
	private static final String[] adminR = {"p","q","tq","sa","so","a","r","su","rc","sc"};
	private static final String[] techR = {"sa","so","r","su","rc","a","sc"};
	private static final String[] powerR = {"p","q","tq","r","a"};
	private static final String[] userR = {"p"};

	@BeforeAll
	void setUp() throws NotBoundException, IOException, InterruptedException, AuthException, ClassNotFoundException {

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
		
		//create users
		Random pwGenerator = new Random();
		String[] names = {"Alice","Bob","Cecilia","David","Erica","Fred","George"};
		for(int i = 0; i < userNumber; i++) {
			users[i] = new User();
			users[i].name = names[i];
			users[i].password = String.valueOf(pwGenerator.nextLong());
		}
		//permissions not hardcoded but in a database
		//its just that database is constructed from tests
		users[0].permissions = adminR;
		users[1].permissions = techR;
		users[2].permissions = powerR;
		users[3].permissions = userR;
		users[4].permissions = userR;
		users[5].permissions = userR;
		users[6].permissions = userR;
		
		//connect to printer
		//assuming secured connection with signature from printer
		registry = LocateRegistry.getRegistry(InetAddress.getLocalHost().getHostName(), portNumber);
		printServer = (PrintService) registry.lookup(serviceName);

		//connect to verifier
		verifier = (VerificationService) registry.lookup(vName);
		
		//register to verifier
		//authenticate to verifier
		//login to printer
		for(User user : users) {
			verifier.setValues(user.name, user.permissions, user.password);
			user.sessionKey = verifier.generateSession(user.name,user.password,serviceName);
			printServer.login(user.name, user.sessionKey);
		}

		//define jobs
		job1[0] = users[0].name; job1[1] = file1;
		job2[0] = users[0].name; job2[1] = file2;
		job3[0] = users[0].name; job3[1] = file3;
	}
	
	private class User{
		public String name;
		public String[] permissions;
		public String password;
		public long sessionKey;
	}

	@BeforeEach
	void start() throws RemoteException, AuthException {
		printServer.start(users[0].sessionKey);
	}

	@AfterEach
	void stop() throws RemoteException, AuthException {
		printServer.stop(users[0].sessionKey);
	}

	@AfterAll
	void tearDown() throws AuthException, RemoteException, NotBoundException {
		for(User user : users) {
			verifier.unsetValues(user.name);
		}
		printServer.shutdown(users[0].sessionKey);
		verifier.shutdown();
		registry.unbind(serviceName);
		registry.unbind(vName);
	}

	@Test
	void canPrint() throws RemoteException, AuthException, DisabledException {
		printServer.print(file, printer1, users[0].sessionKey);
		List<String[]> queue = printServer.queue(printer1,users[0].sessionKey);

		String[] expected = {users[0].name, file};
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
	void canDisable() throws RemoteException, AuthException{
		printServer.stop(users[0].sessionKey);
		assertThrows(DisabledException.class,() ->
			printServer.print(file, printer1, users[0].sessionKey)
		);
	}

	@Test
	void canRestartAndWipe() throws RemoteException, AuthException, DisabledException{
		printServer.print(file, printer1, users[0].sessionKey);
		printServer.restart(users[0].sessionKey);
		List<String[]> pQueue = printServer.queue(printer1, users[0].sessionKey);
		assertTrue(pQueue.isEmpty());
	}

	@Test
	void canOrderAndReorder() throws RemoteException, AuthException, DisabledException{
		printServer.print(file1, printer1, users[0].sessionKey);
		printServer.print(file2, printer1, users[0].sessionKey);
		printServer.print(file3, printer1, users[0].sessionKey);
		List<String[]> queue = printServer.queue(printer1, users[0].sessionKey);
		assertArrayEquals(queue.get(0),job1);
		assertArrayEquals(queue.get(1),job2);
		assertArrayEquals(queue.get(2),job3);

		printServer.topQueue(printer1, 2, users[0].sessionKey);
		queue = printServer.queue(printer1, users[0].sessionKey);
		assertArrayEquals(queue.get(0),job3);
	}

	@Test
	void canGetStatus() throws RemoteException, AuthException, DisabledException{
		printServer.print(file1, printer1, users[0].sessionKey);
		printServer.print(file2, printer1, users[0].sessionKey);
		printServer.print(file3, printer1, users[0].sessionKey);
		String status1 = printServer.status(printer1, users[0].sessionKey);
		String status2 = printServer.status(printer2, users[0].sessionKey);
		assertEquals("Running : true; NextJob : file1.txt; QueueSize : 3", status1);
		assertEquals("Running : false; NextJob : null; QueueSize : 0", status2);
	}

	@Test
	void canChangeConfig() throws RemoteException, AuthException, DisabledException{
		printServer.setConfig("Black/White", "true", users[0].sessionKey);
		String value = printServer.readConfig("Black/White", users[0].sessionKey);
		assertEquals("true",value);
	}

	@Test
	void canAbort() throws RemoteException, AuthException, DisabledException{
		printServer.print(file1, printer1, users[0].sessionKey);
		printServer.print(file2, printer1, users[0].sessionKey);
		printServer.print(file3, printer1, users[0].sessionKey);
		printServer.abort(printer1, 1, users[0].sessionKey);
		List<String[]> queue = printServer.queue(printer1, users[0].sessionKey);
		assertArrayEquals(queue.get(0),job1);
		assertArrayEquals(queue.get(1),job3);
		assertEquals(queue.size(),2);
	}

	@Test
	void canFailVerification(){
		assertThrows(AuthException.class, ()->
			verifier.generateSession(users[0].name, "wrong", serviceName)
		);
	}

	@Test
	void canFailPrinterSession(){
		long sKey = 123456L;
		assertThrows(AuthException.class,()->
			printServer.login(users[0].name, sKey)
		);
	}

	@Test
	void actionsAreLogged() throws RemoteException, AuthException, DisabledException{
		printServer.wipeLog(users[0].sessionKey);
		printServer.print(file1, printer1, users[0].sessionKey);
		printServer.queue(printer1, users[0].sessionKey);
		printServer.abort(printer1, 0, users[0].sessionKey);
		printServer.setConfig("fast", "true", users[0].sessionKey);
		printServer.readConfig("fast", users[0].sessionKey);
		printServer.print(file1, printer1, users[0].sessionKey);
		printServer.print(file2, printer1, users[0].sessionKey);
		printServer.topQueue(printer1, 1, users[0].sessionKey);
		printServer.start(users[0].sessionKey);
		printServer.restart(users[0].sessionKey);
		List<String> log = printServer.getLog(users[0].sessionKey);

		String[] excpected = {"Alice; wipeLog",
							  "Alice; print; printer1; file1.txt",
							  "Alice; queue; printer1",
							  "Alice; abort; printer1; file1.txt",
							  "Alice; setConfig; fast; true",
							  "Alice; readConfig; fast",
							  "Alice; print; printer1; file1.txt",
							  "Alice; print; printer1; file2.txt",
							  "Alice; topQueue; printer1; file2.txt",
							  "Alice; start",
							  "Alice; stop",
							  "Alice; start"};
		
		for(int i = 0; i < excpected.length; i++) {
			assertEquals(excpected[i],log.get(i));
		}
		
	}
	
	@Test
	void canLogout() throws RemoteException, AuthException, DisabledException{
		printServer.logout(users[1].sessionKey);
		verifier.endSession(users[1].sessionKey);
		assertThrows(AuthException.class, () -> {
			printServer.print(file, printer1, users[1].sessionKey);
		});
		assertThrows(AuthException.class, () -> {
			printServer.login(users[1].name, users[1].sessionKey);
		});
		
		//clean up
		users[1].sessionKey = verifier.generateSession(users[1].name, users[1].password, serviceName);
		printServer.login(users[1].name, users[1].sessionKey);
	}
}
