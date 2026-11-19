# User accounts and administration

The backend supports public registration and login using BCrypt-hashed passwords and an HTTP session. New registrations and accounts created by the super-admin are always regular `USER` accounts. Only requests authenticated as `SUPER_ADMIN` can access `/api/admin/users`.

## Create the initial super-admin

The initial super-admin is inserted directly into the `app_users` database table. After that account exists, super-admins can promote or demote other users in the **Manage users** page. Generate a BCrypt hash with the same encoder used by the application:

1. From `backend`, run `.\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt"`.
2. In PowerShell, run `$classpath = "target\classes;" + (Get-Content target\classpath.txt -Raw)`.
3. Run `jshell --class-path $classpath`, then enter:

   ```java
   import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
   new BCryptPasswordEncoder().encode("replace-with-a-strong-password")
   ```

4. Insert the generated hash (not the plain-text password) into the database:

   ```sql
   INSERT INTO app_users (name, email, password_hash, role, enabled)
   VALUES ('Administrator', 'admin@example.com', '<generated-bcrypt-hash>', 'SUPER_ADMIN', TRUE);
   ```

Do not expose or commit the generated password or hash. The current super-admin account cannot be changed in the admin UI, new accounts are always regular users, and super-admin accounts cannot be deleted. At least one enabled super-admin account must remain.

## API

- `POST /api/auth/register` — create a regular user.
- `POST /api/auth/login` — start an authenticated session.
- `GET /api/auth/me` — get the current account.
- `PUT /api/auth/me` — update your own name, email, or password; the account role cannot be changed here.
- `POST /api/auth/logout` — end the current session.
- `POST /api/history` — calculate and save a comparison for the signed-in user.
- `GET /api/history` — list only the signed-in user's saved comparisons.
- `GET /api/history/searches` — get up to five recent unique product searches for suggestions.
- `GET /api/admin/users` — list users (super-admin only).
- `POST /api/admin/users` — create a regular user (super-admin only).
- `PUT /api/admin/users/{id}` — update a user's name, email, password, enabled status, or role (`USER`/`SUPER_ADMIN`) (super-admin only).
- `DELETE /api/admin/users/{id}` — delete a regular user (super-admin only).

State-changing requests require the CSRF token returned by `GET /api/auth/csrf` in the `X-XSRF-TOKEN` header and the session cookies. Passwords must be at least 8 characters and no more than 72 UTF-8 bytes.
