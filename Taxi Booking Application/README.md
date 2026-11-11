# Taxi Booking Web App

Shared Spring Boot application for the existing taxi booking project. It uses Java 17, Maven, Spring MVC, Thymeleaf, Spring Security, Spring Data JPA, and Flyway migrations.

## Requirements

- Java 17+
- MySQL 8+ for persistent local data, or no database setup for the default in-memory H2 development database

## Run locally

From this directory in PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Open [http://localhost:8081](http://localhost:8081). The default profile uses an in-memory H2 database; its data is discarded when the application stops. The default port can be changed with `SERVER_PORT`.

The Maven wrapper is included, so a separate Maven installation is not required. Open this directory as the project root in IntelliJ IDEA or VS Code.

## Configure MySQL

For step-by-step MySQL Workbench setup, including creating a connection, schema, and checking the app-created tables, see [docs/database-design.md](docs/database-design.md).

Create a new, empty local database, then configure the connection using environment variables. Do not import the Flyway schema manually and do not put real credentials in `application.properties` or commit them.

```sql
CREATE DATABASE taxi_booking
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

PowerShell example:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/taxi_booking"
$env:DB_USERNAME = "your-local-user"
$env:DB_PASSWORD = "your-local-password"
.\mvnw.cmd spring-boot:run
```

Flyway applies the versioned schema migrations on startup, beginning with [V1__taxi_booking_schema.sql](src/main/resources/db/migration/V1__taxi_booking_schema.sql). It creates a separate `administrators` table and migrates any legacy admin credentials from `passengers` without deleting passenger records. Later migrations add driver email delivery tracking. Hibernate validates the mapped schema and is not allowed to create, update, or delete tables. MySQL connectivity has not been verified unless you configure a real MySQL server and test it yourself. Existing databases with recorded migrations need the careful backup and reconciliation steps in [docs/database-design.md](docs/database-design.md); do not simply replace their migration files and restart.

See [docs/database-design.md](docs/database-design.md) for schema and migration guidance, [docs/migration-report.md](docs/migration-report.md) for the original-project inventory and file dispositions, and the [team OOP, CRUD, and viva guide](docs/team-member-oop-crud-viva-guide.md) for individual assignment preparation.

## Set up the admin operator

Administrators use separate accounts, but passengers and operators share the same sign-in page at `/login`. The application routes each successfully authenticated account to its own dashboard: passengers to `/bookings`, administrators to `/admin`. To create the first administrator without terminal credentials, open [http://localhost:8081/admin/setup](http://localhost:8081/admin/setup) on the same computer as the application, then choose the administrator name, email, and password. The setup page is available only from localhost and closes permanently once the first administrator has been created. Sign in at `/login` with those credentials. Legacy administrator credentials are copied into the separate administrators table by V1, retaining the existing password hash.

The administrator account is persisted through Spring Data JPA in the database selected by `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. When connecting to MySQL, start the app with those variables pointing to your Workbench server; Flyway creates the `administrators` table and the local setup page writes the account there. By default, the app uses temporary in-memory H2; accounts in H2 do not survive an app restart. In Workbench you can verify the account with `SELECT id, full_name, email, created_at FROM administrators;`. Do not insert an admin password directly in SQL; the application hashes it before storage. See [docs/database-design.md](docs/database-design.md) for the full setup and verification steps.

Choose an email not used by a passenger or driver. Additional operator accounts can be created by an admin at **Administrator operators**; each operator has a separate administrator email and password. The panel prevents self-removal and removal of the last admin. Keep administrator access limited to trusted operators. A fixed built-in admin password is intentionally not provided.

The admin panel provides customer account review, read-only booking and payment overviews, refund-request review, separate administrator operator management, and driver/vehicle operations. Admins land directly on this operations dashboard and do not see passenger ride navigation. Failed sign-in returns to the shared `/login` page. Refund execution is not provided: an operator must review the request and process it through the payment provider. The panel does not automatically approve or claim a refund has been issued.

## Driver application and portal

The public **Drive with us** form at `/driver/apply` collects driver identity/contact/licence details, vehicle information, and required profile, licence, and vehicle images. Each image must be JPG, PNG, or WebP and no larger than 5 MB. Files are stored outside static/public resources in `DRIVER_UPLOAD_DIR` (default `./var/driver-uploads`) and are served only through admin-authorized document routes. Applicants start with `PENDING`; duplicate email, NIC, licence, or vehicle plate values are rejected. Admins can search/filter applications, inspect details and documents, and approve or reject with an internal reason under **Admin → Drivers**.

After approval, the app creates the driver account in the shared `drivers` table, hashes a cryptographically random temporary password with BCrypt, sets `mustChangePassword`, and queues an encrypted copy of the temporary password in the database in the same transaction. Email is attempted only after commit; failures remain pending and retry automatically every five minutes. The driver signs in at `/login` with their application email and must change the temporary password before any `/driver/**` dashboard or availability route is allowed. A manual retry uses the same password and does not create an account or rotate credentials. Rejection sends a status/reason email without activating a driver account. Configure `MAIL_CREDENTIAL_ENCRYPTION_KEY` before approving or rejecting applications; it must remain stable across restarts while there are pending email retries.

### Mailtrap SMTP setup

The project already includes `spring-boot-starter-mail`; SMTP credentials are read from environment variables, not Java source. To use the SMTP server values supplied for this project, set the following in a PowerShell terminal before starting Spring Boot:

```powershell
$env:MAIL_HOST = "live.smtp.mailtrap.io"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "apismtp@mailtrap.io"
$env:MAIL_PASSWORD = "<newly-rotated-Mailtrap-token>"
$env:MAIL_FROM = "serendibride@admin.com"
$env:APP_BASE_URL = "http://localhost:8081"
$env:MAIL_CREDENTIAL_ENCRYPTION_KEY = "<base64-encoded-32-byte-key>"
$env:DRIVER_UPLOAD_DIR = ".\var\driver-uploads"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

Generate the encryption key once and keep it in a safe local secret store. This command uses a cryptographically secure random-number generator and prints standard Base64 for exactly 32 random bytes:

```powershell
$keyBytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($keyBytes)
    $env:MAIL_CREDENTIAL_ENCRYPTION_KEY = [Convert]::ToBase64String($keyBytes)
    $env:MAIL_CREDENTIAL_ENCRYPTION_KEY
} finally {
    $rng.Dispose()
    [Array]::Clear($keyBytes, 0, $keyBytes.Length)
}
```

Run the app in the same PowerShell session after generating the key so the environment variable is inherited. Never generate a replacement key automatically. The key is loaded and validated the first time a driver-email payload needs encryption or decryption; this allows the website to start when that specific feature has not been configured yet, while producing a clear error if driver email delivery requires the missing/invalid key. The key must decode to exactly 32 bytes. Do not regenerate or change the key while encrypted driver-email outbox records exist: those records will remain intact, but cannot be decrypted with a different key. Do not commit the key or SMTP password. If an SMTP token has been exposed, revoke it in Mailtrap and use a newly generated token.

In VS Code, open **Run and Debug**, create or select a Java launch configuration for `com.serendib.taxibooking.TaxiBookingApplication`, and set the variables under its `env` object or point `envFile` at a local `.env` file. Include `MAIL_CREDENTIAL_ENCRYPTION_KEY`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM` (and the database variables when using MySQL). Keep actual values in the local environment or ignored `.env` file; never put secrets in a committed `launch.json`, `application.properties`, logs, API responses, or GitHub. Start the configuration after setting values. The supported property bindings are in [application.properties](src/main/resources/application.properties).

The application loads the key from `MAIL_CREDENTIAL_ENCRYPTION_KEY`, Base64-decodes it, checks the 32-byte length, and creates an AES key using `SecretKeySpec`. Outbox values continue to use AES/GCM/NoPadding with a fresh 12-byte nonce prepended to the ciphertext and 128-bit authentication tag, all Base64-encoded, so existing records remain compatible when the same key is retained. If the original key was lost or changed, restore it from the secure backup used when the data was encrypted. If it cannot be recovered, the prior plaintext cannot be derived from the encrypted database value; do not overwrite or delete those rows as a workaround. Keep them for audit/recovery, restore access with your existing driver password-reset process, and only remove/re-encrypt outbox data through an explicitly planned migration with the original key.

Mailtrap Email Testing inboxes use their own sandbox host, port, username, and password. If you want all test messages captured in an Email Testing inbox instead of using the provided live SMTP endpoint, copy the inbox SMTP values from Mailtrap and set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, and `MAIL_PASSWORD` to those values. Mailtrap's live SMTP endpoint may deliver to real recipients and may require an authorized sender/domain; verify your account and sender policy first.

`application.properties` supplies the host/port/username/from defaults and enables SMTP auth, STARTTLS, and 10-second connect/read/write timeouts. Secrets have no source-code defaults. If SMTP is missing or unavailable, the application reports delivery as pending without logging credentials; the retry worker keeps trying.

### Test email endpoint and inbox checks

The test route is registered only with the `dev` profile and requires an authenticated administrator session. In Postman:

1. Start the app with the `dev` profile and sign in as an administrator at `http://localhost:8081/login`.
2. Keep the authenticated `JSESSIONID` cookie. Include a valid CSRF token from the signed-in session as the `X-CSRF-TOKEN` header (the application uses Spring Security's normal session-based CSRF protection).
3. Send `POST http://localhost:8081/api/email/test` with `Content-Type: application/json`:

```json
{
  "to": "your-inbox-recipient@example.com",
  "subject": "Mailtrap SMTP Test",
  "message": "Testing email integration."
}
```

4. A successful response is `200` with `{"status":"sent"}`. Invalid input returns `400`; missing SMTP configuration returns `503`; delivery errors return `502`.
5. Open the corresponding Mailtrap inbox and inspect the message, HTML/text parts, recipient, and sender. For driver emails, submit a driver application, approve it from **Admin → Drivers**, then inspect the approval message. Rejection emails can be tested by rejecting another pending application and including a reason.

### Driver workflow checks

### Driver workflow checks

| Action | Route |
|---|---|
| Submit an application | `GET/POST /driver/apply` (multipart form) |
| Review/search/filter applications | `GET /admin/drivers?status=PENDING&search=...` |
| View application and uploaded documents | `GET /admin/drivers/{id}` |
| Approve and create account | `POST /admin/drivers/{id}/approve` |
| Reject with reason | `POST /admin/drivers/{id}/reject` (`reason` form field) |
| Retry a pending email without changing credentials | `POST /admin/drivers/{id}/credentials` |
| Send a development test email (admin, dev profile only) | `POST /api/email/test` |
| Shared login | `POST /login` |
| Mandatory first-login password change | `GET/POST /driver/change-password` |
| Request a password-reset email | `GET/POST /driver/forgot-password` |
| Reset with a single-use emailed link | `GET/POST /driver/reset-password?token=...` |
| Driver dashboard / availability | `GET /driver/dashboard`, `POST /driver/availability` |

Password-reset tokens are high-entropy, stored only as SHA-256 hashes, expire after 30 minutes, and are invalidated after use or when a new reset is requested. The forgot-password response is the same whether an eligible account exists or not. These are browser-form routes and use the application's session and CSRF protection. For manual Postman checks, first sign in through `/login`, retain the session cookie, and include a valid CSRF token for POST requests. To test applicant submission, send multipart fields named `name`, `email`, `phone`, `nicNumber`, `address`, `licenseNumber`, `licenseExpiryDate`, `vehicleCategory`, `vehicleBrand`, `vehicleModel`, `vehicleYear`, `plateNumber`, `vehicleColor`, `profilePhoto`, `licenseImage`, and `vehiclePhoto`. The automated H2 suite exercises approval/email, BCrypt storage, forced password change, password reset, application submission, and duplicate plate rejection with `.\mvnw.cmd test`.

If `localhost:8081` refuses the connection, verify the terminal reports that Spring Boot started, use the configured port, and check that another process is not already using it. If approval shows failed/not configured email, check that SMTP variables were set in the same terminal before startup and use the exact host/port/username/password from the provider. Uploaded images remain on disk when an application is rejected or deleted; remove them only as part of a deliberate retention policy.

**Ride workflow remains incomplete:** nearby-driver map markers use the existing trusted location integration, but booking creation does not dispatch to those drivers. Driver ride offers, acceptance/rejection, atomic assignment, driver trip controls, real-time driver notifications, live browser GPS tracking, earnings, and payout history are not implemented. Do not treat the dashboard as a production driver dispatch app.

## Payment setup

The payment area is available to signed-in passengers at **Payments**. Only completed bookings can be paid. The amount and passenger ownership are read from the saved booking.

## Geoapify maps, place search, routing, and nearby drivers

The booking map uses MapLibre GL with Geoapify raster tiles. Address suggestions use Geoapify geocoding, and fare estimates use Geoapify driving routes. Configure a replacement Geoapify key in the same terminal used to start the app; do not paste it into source files or browser JavaScript:

```powershell
$secureGeoapifyKey = Read-Host "Geoapify API key" -AsSecureString
$keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureGeoapifyKey)
try {
    $env:GEOAPIFY_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer)
    $secureGeoapifyKey.Dispose()
}
.\mvnw.cmd spring-boot:run
```

The key provided in the chat should be rotated in Geoapify because it has been exposed. Geoapify requests are made server-side; map tiles are proxied through `/api/maps/tiles/{z}/{x}/{y}.png` so the key is not embedded in the page. A current Geoapify key and external provider access are required; they were not verified in this environment.

The app's approved/online fleet records do not themselves contain GPS coordinates. A trusted driver app/backend must periodically post each approved driver's current coordinates to `POST /api/integrations/drivers/{driverId}/location`, using header `X-Integration-Token` configured through `DRIVER_INTEGRATION_TOKEN`, and JSON such as `{"latitude":6.9271,"longitude":79.8612}`. Updates are held in application memory for up to 120 seconds and are not written to MySQL. The passenger booking map requests nearby, available drivers every 15 seconds and shows up to 20 within a configurable 5 km radius, filtered by selected vehicle category. Without a trusted live GPS feed, the map correctly shows no nearby drivers; it does not invent or simulate positions. Multi-instance deployments need shared, expiring location storage before enabling live tracking.

- **Cash:** starts as `PENDING`; it becomes paid only after the trusted driver integration confirms collection. Set `DRIVER_INTEGRATION_TOKEN` to a long random secret and configure the driver integration to call `POST /api/integrations/payments/{paymentReference}/cash-collected` with that token and a collection reference.
- **Stripe Checkout:** uses Stripe's hosted page so this app never handles card numbers or CVV. The option remains disabled unless both settings are present:

```powershell
$env:STRIPE_SECRET_KEY = "sk_test_replace-with-your-test-secret"
$env:STRIPE_WEBHOOK_SECRET = "whsec_replace-with-your-webhook-secret"
$env:APP_BASE_URL = "http://localhost:8081"
$env:DRIVER_INTEGRATION_TOKEN = "replace-with-a-long-random-secret"
```

Configure Stripe to send Checkout events to `POST /api/payments/stripe/webhook`. For local development, Stripe CLI can forward events to `http://localhost:8081/api/payments/stripe/webhook`. The app retrieves the session directly from Stripe and verifies the webhook signature before marking a payment successful. No real Stripe account or charge has been tested.

Receipts are created only after verified Stripe success or trusted cash confirmation. Refund submissions are requests only and do not issue refunds; the administrative review/processing flow is not implemented. Promotions are validated and discounted from database records, but no codes are seeded or imported and promotion administration is not implemented. The original flat files are not migrated.

## Implemented

- Passenger registration, BCrypt password storage, login, and logout
- Public driver applications and admin-only driver verification, availability, and vehicle management
- Admin operations for customer account review, separate operator management, booking/payment review, and refund-request review
- Authenticated booking requests, passenger-scoped booking history, edits and cancellations, and driver integration endpoints for trip status
- Saved home/work/other locations
- Fare estimate API, route/map integrations, and interactive booking map
- JPA-backed booking, trip, payment, promotion, refund-request, and receipt records
- Responsive Thymeleaf pages for booking, payment history, receipts, and refund requests

Geoapify requires `GEOAPIFY_API_KEY`; a trusted driver integration and `DRIVER_INTEGRATION_TOKEN` are required for live driver markers. External Geoapify, driver, MySQL, and payment-provider connections have not been verified with real local credentials.

## Verification

Run `.\mvnw.cmd test`. The tests use the in-memory H2 database and cover application/schema startup, passenger and admin authorization, driver application/verification, vehicle assignment and availability, payment and refund pages, fare calculations, promotion discounts, cash confirmation, receipt issuance, and Stripe webhook signature validation. They do not test real MySQL, Stripe, mapping, or a live driver dispatch service. The fleet module stores and manages drivers and vehicles but is not yet wired into automatic booking dispatch; the current booking integration remains external.
