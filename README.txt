INSTRUCTIONS TO CREATE DATABASE

   Install MySQL

   Launch a terminal window
   sudo mysql
   CREATE USER '<username>'@'localhost' IDENTIFIED BY '<password>';
   GRANT ALL PRIVILEGES ON * . * TO '<username>'@'localhost';
   FLUSH PRIVILEGES;
   CREATE DATABASE auth;
   USE auth;
   CREATE TABLE user_data(username varchar(30), permissions varchar(500), password varbinary(500), salt varbinary(500));

DEPENDENCIES
	To run the project you will need JUnit 5 and the MySQL connector for java
	The connector is included in the lib-folder, but it must be included in the build path

RUNNING THE CODE
	There is no main class in the project, and code is rather run through JUnit tests.
	The tests initializes the servers and starts print-sessions with two users.
	A MySQL process must be started manually.
	The properties dbUser and dbPassword in the database.properties file must be changed to credentials for your database
	To run parts of the code just add a test-case, and run the tests.
	The tests will terminate the servers before exiting.

The project has successfully built and all tests passed on both macOS and windows

format for permssion-string is "object:action,action;object:action,action", 
for example "printer:p,q,tq"