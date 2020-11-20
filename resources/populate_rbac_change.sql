/* It is assumed that the new users are already in the database. */

/* Remove role from Bob but keep him in database for record reasons. */
DELETE FROM user_role WHERE username='Bob';

/* Update role of George. */
UPDATE user_role
SET role_id=2
    WHERE username='George';

/* Give roles to the new employees. */
INSERT INTO user_role (username, role_id)
VALUES ('Henry',4),
        ('Ida',3);




