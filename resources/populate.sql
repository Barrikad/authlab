DROP TABLE IF EXISTS user_data, role_data, permission_data, role_permission, user_role;

CREATE TABLE IF NOT EXISTS user_data(username varchar(30),password varbinary(500), salt varbinary(500));
CREATE TABLE user_role(username varchar(30),role_id integer);
CREATE TABLE role_data(role_id integer,role_title varchar(30),parent_id integer);
CREATE TABLE role_permission(role_id integer,permission_id integer);
CREATE TABLE permission_data(permission_id integer,device varchar(30),operation varchar(30));

INSERT INTO role_data (role_id, role_title, parent_id)
VALUES (1, 'ADMIN', null),
        (2,'TECHNICIAN',1),
        (3,'POWER',1),
        (4,'ORDINARY',3);


INSERT INTO permission_data (permission_id, device, operation)
VALUES (1,'printer','PRINT'), /*Ordinary*/
        (2,'printer','ABORT'), /*Admin*/
        (3,'printer','QUEUE'), /*Ordinary*/
        (4,'printer','TOPQUEUE'), /*Power*/
        (5,'printer','START'), /*Technician*/
        (6,'printer','STOP'), /*Technician*/
        (7,'printer','RESTART'), /*Technician*/
        (8,'printer','STATUS'), /*Technician*/
        (9,'printer','READCONFIG'), /*Technician*/
        (10,'printer','SETCONFIG'), /*Technician*/
        (11,'printer','SHUTDOWN'), /*Admin*/
        (12,'printer','GETLOG'), /*Admin*/
        (13,'printer','WIPELOG'); /*Admin*/

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


