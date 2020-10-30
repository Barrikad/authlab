##INSTRUCTIONS TO CREATE DATABASE

#   Install MySQL

#   Launch a terminal window
#   sudo mysql
#   CREATE USER '<username>'@'localhost' IDENTIFIED BY '<password>';
#   GRANT ALL PRIVILEGES ON * . * TO '<username>'@'localhost';
#   FLUSH PRIVILEGES;
#   CREATE DATABASE auth;
#   USE auth;
#   CREATE TABLE user_data(username varchar(30), password varbinary(500), salt varbinary(500));