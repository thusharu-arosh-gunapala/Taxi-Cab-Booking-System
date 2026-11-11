# Database design and setup

The application is mapped to the taxi-booking schema. Flyway applies the single [V1__taxi_booking_schema.sql](../src/main/resources/db/migration/V1__taxi_booking_schema.sql) migration, which creates the independent administrators table and driver application/account fields, and copies any legacy administrator credentials while preserving passenger records. Hibernate uses `ddl-auto=validate`, so it checks the schema but does not create, change, or delete database tables.

## Tables used by the site

| Table | Site functionality |
|---|---|
| `passengers` | Account registration and sign-in; passwords are stored as BCrypt hashes. |
| `administrators` | Separate website-operator accounts, credentials, and admin authority; not passenger accounts. |
| `favourite_locations` | Passenger-owned home, work, and other saved places. |
| `bookings` | Passenger ride requests, route/fare details, scheduling, status, and cancellation information. |
| `trips` | Start and completion history for bookings. |
| `promotions` | Promotion rules, expiry, discount limits, and usage counts. Promotion codes are not preloaded. |
| `payments` | Passenger booking payments, discounts, method/status, and provider references. Card data is not stored. |
| `receipts` | Receipts created only after payment confirmation. |
| `refunds` | Passenger refund requests; a request does not mean a refund has been approved or paid. |
| `drivers` | Driver applications, identity/contact/licence details, private document keys, application status/reason, BCrypt password hash, unique driver reference, credential-email status, forced password change, and online status. Applicants default to pending, inactive, and offline. |
| `vehicles` | Fleet vehicles, category, make/model/year, private photo key, availability, registration plate, and optional assigned driver. |
| `driver_password_reset_tokens` | One-time driver password-reset token hashes and expiration timestamps; plaintext reset tokens are emailed and never stored. |

Foreign keys connect locations/bookings to passengers, trips and payments to bookings, and receipts/refunds to payments. Application queries scope passenger information to the signed-in passenger.

## Create and connect the database with MySQL Workbench

MySQL Workbench is the GUI client; install and start **MySQL Server 8.x** as well. In this setup, use Workbench to create the database and inspect it. Spring Boot/Flyway applies the complete V1 schema, including the separate administrator accounts, when the app connects.

### A. Create a Workbench connection

1. Open MySQL Workbench and click the **+** beside **MySQL Connections**.
2. Set **Connection Name** to `Taxi Booking Local`.
3. Set **Connection Method** to `Standard (TCP/IP)`, **Hostname** to `127.0.0.1`, and **Port** to `3306` (use the port configured for your MySQL Server if different).
4. Enter the MySQL username you created during server setup. Click **Store in Vault...** only if you want Workbench to remember your local password.
5. Click **Test Connection**, enter the password if prompted, confirm success, then save and open the connection.

### B. Create an empty application database in Workbench

1. In the Workbench SQL editor, click **Create a new SQL tab**.
2. Paste and execute this query using the lightning-bolt button:

   ```sql
   CREATE DATABASE taxi_booking
       CHARACTER SET utf8mb4
       COLLATE utf8mb4_unicode_ci;
   ```

3. In the **SCHEMAS** pane, click **Refresh**. Confirm that `taxi_booking` appears.
4. **Do not create the tables manually, and do not execute/import Flyway migration files in Workbench.** Flyway applies the single V1 schema when the app connects. Running both methods would cause a table-already-exists error.

### C. Point the app to this database

Open the project folder `Taxi Booking Application` in VS Code. Open **Terminal → New Terminal** and run these commands in the same PowerShell terminal. Replace the sample username/password with the credentials for the MySQL Workbench connection you just tested:

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/taxi_booking"
   $env:DB_USERNAME = "your-local-mysql-user"
   $env:DB_PASSWORD = "your-local-mysql-password"
   .\mvnw.cmd spring-boot:run
   ```

Do not put real database passwords in `application.properties` or commit them. In the startup output, confirm Flyway reports applying the taxi schema migration and that the application started.

### D. Verify the tables in Workbench

1. Return to Workbench and refresh the **SCHEMAS** pane.
2. Expand `taxi_booking` → **Tables**. You should see `passengers`, `administrators`, `favourite_locations`, `bookings`, `trips`, `promotions`, `payments`, `receipts`, `refunds`, `drivers`, `vehicles`, `driver_password_reset_tokens`, and `flyway_schema_history`.
3. Open [http://localhost:8081](http://localhost:8081). The app uses the configured MySQL database because you started it with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
4. In a Workbench SQL tab, select the schema with:

   ```sql
   USE taxi_booking;
   SELECT id, full_name, email, created_at FROM passengers;
   ```

   This confirms a registration reached MySQL. Do not query or expose `password_hash` except when specifically debugging authentication; it contains sensitive credential hashes.
5. To create the first separate admin account in this MySQL database, open [http://localhost:8081/admin/setup](http://localhost:8081/admin/setup) on this computer after the app starts. Enter the admin name, unique email, and a password of at least 8 characters. The setup page is localhost-only and is disabled after the first administrator exists. The app stores the account in `administrators`; it stores the password as a BCrypt hash.

   Both account types sign in at `/login`; successful authentication routes admins to `/admin` and passengers to `/bookings`. In another Workbench SQL tab, verify only non-sensitive account fields:

   ```sql
   USE taxi_booking;
   SELECT id, full_name, email, created_at
   FROM administrators;
   ```

   Confirm the operator email appears.    Never select, copy, or share `password_hash`. Admins can add/remove other operators in the admin panel, but cannot remove themselves or the last administrator.

6. The admin panel provides customer account review, separate administrator operator management, read-only booking and payment overviews, refund-request review, and driver/vehicle operations. Refund execution is not provided: an operator must review and process it through the actual payment provider.

7. Driver applications at `/driver/apply` collect driver/NIC/address/licence-expiry information, vehicle details, and three required JPG/PNG/WebP images (5 MB maximum each). Driver and vehicle records are stored together; files live outside static resources under `DRIVER_UPLOAD_DIR`, while only random private file keys are stored in the database. Duplicate email, NIC, licence, or vehicle plate values are rejected. Admins can search/filter by status, review the applicant and private documents, then approve or reject with a reason. Approval creates credentials on the existing driver record, BCrypt-hashes the temporary password, and queues HTML email after commit. Credential email status is stored so failed delivery can be retried by rotating and resending credentials. A driver must change the temporary password before reaching the dashboard; no separate driver authentication system is used. SMTP settings and manual Postman/browser checks are documented in the root [README](../README.md#driver-application-and-portal). Automatic ride dispatch still uses the external driver integration.

MySQL data remains stored when the app stops. The `$env:` settings last only for this PowerShell terminal; set them again in a new terminal session. A server already running with H2 will not switch databases when variables are added elsewhere: stop that server and relaunch it from the configured terminal. To run against H2 instead, use a fresh terminal without those database variables; H2 is temporary development/test storage and is cleared when the app stops.

The `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables are read by `application.properties`. To run against H2 instead, open a fresh PowerShell window without those variables; H2 is temporary development/test storage and is cleared when the app stops.

## Existing databases and safety

For a new database, create it empty and let Flyway apply the one V1 migration. V1 now contains both the previous V1 schema and driver-account changes that were previously in V2. **Do not point this version directly at an existing database that has already recorded V1 or V2.** The V1 checksum changed and V2 is no longer present, so Flyway will reject that database until its schema and migration history are deliberately reconciled. Back up the database first using Workbench's **Server → Data Export** and verify that the backup can be restored.

For an existing database, stop the application and inspect `taxi_booking.flyway_schema_history` in Workbench. Do not delete the database or modify that table by hand. A database with both the old V1 and V2 successfully applied will have prior driver fields and may contain an unused `driver_activation_tokens` table; the current temporary-password flow no longer uses activation tokens. Preserve account data and do not drop old tables as part of this feature. A database with only old V1 may need driver-account additions and backfill before its migration history can be reconciled. Since existing databases vary, do not run the combined V1 SQL on an existing schema or blindly repair Flyway metadata. Back up and verify a restorable copy first, then use a migration plan specific to its tables and exact Flyway history. After confirming the existing schema matches the consolidated V1, a Flyway CLI compatible with this project can be configured for the same DB to align the checksum and mark removed V2 as deleted:

   ```powershell
   $env:FLYWAY_URL = $env:DB_URL
   $env:FLYWAY_USER = $env:DB_USERNAME
   $env:FLYWAY_PASSWORD = $env:DB_PASSWORD
   $env:FLYWAY_LOCATIONS = "filesystem:src/main/resources/db/migration"
   flyway repair
   ```

`repair` changes migration metadata only; it does not execute SQL. Do not start the updated app until the actual schema is verified and the repair has completed. Then start it and confirm Flyway validation and JPA schema validation both succeed. Test this procedure on a restored backup first. If the history state differs, a table is partially present, or repair reports errors, stop and get a tailored migration plan rather than dropping tables or editing migration history manually.

The supplied payment text files are not imported. Promotions are not seeded, and no administrator promotion-management UI is exposed. Refund approval/processing and automatic fleet-based dispatch are also not implemented. For future schema changes, add new numbered migrations rather than editing applied migrations. A real MySQL server was not available for this verification, so verify your own local connection from the Flyway startup logs.

## Payment configuration (optional)

Stripe Checkout requires `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, and `APP_BASE_URL`. Cash confirmation requires a trusted driver integration and `DRIVER_INTEGRATION_TOKEN`. Do not enable live payments until these integrations are configured and tested. Details are in the payment setup section of the root README.
