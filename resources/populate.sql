DROP TABLE IF EXISTS user_data, role_data, permission_data, role_permission, user_role;

CREATE TABLE IF NOT EXISTS user_data(username varchar(30),password varbinary(500), salt varbinary(500));
CREATE TABLE user_role(username varchar(30),role_id integer);
CREATE TABLE role_data(role_id integer,role_title varchar(30),parent_id integer);
CREATE TABLE role_permission(role_id integer,permission_id integer);
CREATE TABLE permission_data(permission_id integer,service varchar(30),permission varchar(30));

INSERT INTO role_data (role_id, role_title, parent_id)
VALUES (1, 'ADMIN', null),
        (2,'TECHNICIAN',1),
        (3,'POWER',1),
        (4,'ORDINARY',3);


INSERT INTO permission_data (permission_id, service, permission)
VALUES (1,'PRINTER','PRINT'), /*Ordinary*/
        (2,'PRINTER','ABORT'), /*Admin*/
        (3,'PRINTER','QUEUE'), /*Ordinary*/
        (4,'PRINTER','TOPQUEUE'), /*Power*/
        (5,'PRINTER','START'), /*Technician*/
        (6,'PRINTER','STOP'), /*Technician*/
        (7,'PRINTER','RESTART'), /*Technician*/
        (8,'PRINTER','STATUS'), /*Technician*/
        (9,'PRINTER','READCONFIG'), /*Technician*/
        (10,'PRINTER','SETCONFIG'), /*Technician*/
        (11,'PRINTER','SHUTDOWN'), /*Admin*/
        (12,'PRINTER','GETLOG'), /*Admin*/
        (13,'PRINTER','WIPELOG'); /*Admin*/

INSERT INTO role_permission (role_id, permission_id)
VALUES
        /*Admin User permissions*/
        (1,2),
        (1,11),
        (1,12),
        (1,13),
        /*Technician User permissions*/
        (2,5),
        (2,6),
        (2,7),
        (2,8),
        (2,9),
        (2,10),
        /*Power User permissions*/
        (3,4),
        /*Ordinary User permissions*/
        (4,1),
        (4,3);


INSERT INTO user_role (username, role_id)
VALUES ('Alice',1),
        ('Bob',2),
        ('Cecilia',3),
        ('David',4),
        ('Erica',4),
        ('Fred',4),
        ('George',4);


