# Skytech CRM Backend

Production-oriented REST API for Skytech CRM, built with Java 21, Spring Boot 3, stateless JWT security, PostgreSQL, Flyway, JPA/Hibernate, MapStruct, Twilio, and JavaMailSender.

## Prerequisites

- Java 21
- A Supabase project (or PostgreSQL 15+ locally)
- Optional: Twilio and SMTP credentials for real outbound messages
- Docker, if you prefer a container build

The Maven Wrapper is committed, so a separate Maven installation is not required.

## Supabase setup

1. In Supabase, create or open a project.
2. This project is configured to use its Supabase shared pooler in session mode. The selected JDBC endpoint is:

   ```text
   jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:5432/postgres?sslmode=require
   ```

   The corresponding database username is `postgres.wpqhskjkueajfwzulbih`. Port `5432` selects session mode, which is compatible with this persistent Spring Boot/HikariCP application on Railway and Render.
3. Copy `.env.example` to `.env` and enter the database URL, username, and password. Spring does not load `.env` by itself; export these variables in your shell or configure them in your IDE/runtime.
4. Use a `JWT_SECRET` containing at least 32 random bytes. Do not commit it.

Flyway runs automatically on startup and owns the schema. The configured Hibernate mode is `validate`, so the app never mutates production tables outside migrations.

## Environment variables

| Variable | Required | Purpose |
|---|---:|---|
| `DATABASE_URL` | yes | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | yes | PostgreSQL/Supabase username |
| `DATABASE_PASSWORD` | yes | Database password |
| `DB_POOL_MAX_SIZE` | no | Maximum Hikari connections per app instance; defaults to `5` |
| `DB_POOL_MIN_IDLE` | no | Minimum idle Hikari connections; defaults to `1` |
| `DB_CONNECTION_TIMEOUT_MS` | no | Database connection timeout; defaults to `30000` ms |
| `JWT_SECRET` | yes | HMAC key, minimum 32 random bytes |
| `CORS_ALLOWED_ORIGINS` | no | Comma-separated frontend origins; defaults to `http://localhost:3000` |
| `TWILIO_ACCOUNT_SID` | for SMS | Twilio account SID |
| `TWILIO_AUTH_TOKEN` | for SMS | Twilio auth token |
| `TWILIO_FROM_NUMBER` | for SMS | Twilio sender number |
| `MAIL_HOST` / `MAIL_PORT` | for email | SMTP endpoint |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | for email | SMTP credentials |
| `STRIPE_SECRET_KEY` | no | Reserved for the inactive billing integration |
| `APP_TIME_ZONE` | no | Scheduler time zone; defaults to `Africa/Accra` |

Outbound failures are logged and do not fail CRM requests. In development, leaving Twilio/SMTP blank is therefore safe, although OTP delivery will only be visible through a configured provider.

The Stripe SDK client is wired from `STRIPE_SECRET_KEY`, but no billing operation invokes it yet. Feature gates intentionally allow all plans until billing is activated.

## Run locally

PowerShell example:

```powershell
$env:DATABASE_URL='jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:5432/postgres?sslmode=require'
$env:DATABASE_USERNAME='postgres.wpqhskjkueajfwzulbih'
$env:DATABASE_PASSWORD='your-password'
$env:JWT_SECRET='replace-with-at-least-32-random-bytes'
.\mvnw.cmd spring-boot:run
```

The API is available at `http://localhost:8080/api/v1`. Swagger UI is at `http://localhost:8080/swagger-ui.html`, and health is at `http://localhost:8080/actuator/health`.

No default administrator password is shipped. Create the first user securely using a one-off BCrypt seed, an approved database administration process, or a temporary deployment-only bootstrap routine. Subsequent users are created through `POST /api/v1/users` by an authenticated administrator.

## Authentication

1. `POST /api/v1/auth/login` validates email/password and sends a six-digit OTP.
2. `POST /api/v1/auth/verify-otp` returns a 15-minute access token and seven-day refresh token.
3. Send `Authorization: Bearer ACCESS_TOKEN` on protected requests.
4. `POST /api/v1/auth/refresh` rotates access credentials from a valid, non-revoked refresh token.
5. `POST /api/v1/auth/logout` revokes the stored refresh-token hash.

Role checks are enforced in the service layer. Leads and deals are restricted to assigned agents, while administrators and managers receive the broader views described by the API contract.

## Build and test

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

Container build:

```powershell
docker build -t skytech-crm-backend .
docker run --rm -p 8080:8080 --env-file .env skytech-crm-backend
```

## Operational notes

- Profile images are stored beneath `uploads/profiles`; mount persistent storage there in a container deployment or replace that adapter with object storage.
- Soft-deleted users, leads, and deals are automatically excluded by Hibernate restrictions.
- Deal and task updates support optimistic-lock versions.
- Automation jobs run at 07:00 and 08:00 daily, hourly, every 15 minutes, and midnight using `APP_TIME_ZONE` (default `Africa/Accra`).
- Add all future schema changes as new sequential Flyway migrations; never edit an already-applied migration.

## Production go-live runbook

Complete every item below before exposing the API to real users.

1. **Configure the Supabase PostgreSQL project.** Save the database password in a password manager. This deployment uses the shared session pooler:

   ```text
   jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:5432/postgres?sslmode=require
   ```

   Use `postgres.wpqhskjkueajfwzulbih` as the username. Do not use the Supabase anon key: this backend connects through PostgreSQL JDBC.

2. **Create production secrets.** Generate a JWT secret with at least 32 cryptographically random bytes. PowerShell example:

   ```powershell
   $bytes = New-Object byte[] 48
   [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
   [Convert]::ToBase64String($bytes)
   ```

   Store the result, database password, Twilio token, SMTP password, and future Stripe key in the deployment platform's encrypted secret manager. Never copy production values into `.env.example`, Git, Docker images, frontend code, tickets, or chat messages.

3. **Configure every production environment variable.** Set `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, and `APP_TIME_ZONE`. Configure the Twilio and mail variables before the first login because OTP delivery requires at least one working channel. `CORS_ALLOWED_ORIGINS` must contain only the real HTTPS frontend origins, comma-separated, with no wildcard.

4. **Configure email delivery.** Create an SMTP account with a transactional provider, verify the sending domain, publish its SPF and DKIM DNS records, and set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, and `MAIL_PASSWORD`. Send a real OTP email and verify delivery, spam placement, and sender identity.

5. **Configure Twilio SMS.** Create or select the Twilio production account, obtain an SMS-capable sender number, complete any country-specific sender registration, and set `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and `TWILIO_FROM_NUMBER`. Send test OTP and broadcast messages to opted-in test numbers. Confirm consent, opt-out, and local messaging-law requirements before bulk sending.

6. **Keep Stripe inactive until billing is implemented.** The SDK client is wired, but no billing workflow calls it. Leave `STRIPE_SECRET_KEY` empty or provide a restricted test key. Do not use a live Stripe key until webhook verification, subscription persistence, retry handling, and real feature-gate rules replace the intentional stub.

7. **Run and verify Flyway migrations.** Back up the database before every deployment that introduces a migration. Start one application instance with the production database variables; Flyway will apply `V1` onward and Hibernate will validate the resulting schema. Check the `flyway_schema_history` table and application logs for successful migration and validation before scaling to multiple instances.

8. **Create the first administrator securely.** After Flyway creates the tables, open the Supabase SQL editor and insert one bootstrap administrator. Use a unique email and a temporary high-entropy password:

   ```sql
   INSERT INTO users
     (first_name, last_name, email, password_hash, role, plan_tier, is_active)
   VALUES
     ('Skytech', 'Administrator', 'admin@example.com',
      crypt('REPLACE_WITH_A_LONG_TEMPORARY_PASSWORD', gen_salt('bf', 12)),
      'ADMIN', 'FREE', TRUE);
   ```

   Log in through the OTP flow, create named administrator accounts through the API, then remove or rotate the temporary bootstrap credentials. Never ship a shared default administrator password.

9. **Provide durable profile-photo storage.** The current adapter writes to `/app/uploads/profiles`. In Docker or a container platform, mount a persistent writable volume at `/app/uploads` owned by UID `10001`; otherwise photos disappear when the container is replaced. For multi-instance deployment, replace the local adapter with shared object storage before scaling horizontally.

10. **Deploy behind HTTPS.** Build with `docker build -t skytech-crm-backend .`, deploy the immutable image, expose only the reverse proxy/load balancer publicly, terminate TLS with a valid certificate, and forward requests to port `8080`. Redirect HTTP to HTTPS and restrict database access to the application environment.

11. **Set production health checks and resource limits.** Configure the platform health check as `GET /actuator/health`, add startup/readiness grace time for Flyway, and define CPU, memory, restart, and connection limits. Run only one migration-capable instance during the first startup of a new release.

12. **Protect operational endpoints.** Decide whether public Swagger is acceptable. If not, block `/swagger-ui.html`, `/swagger-ui/**`, and `/v3/api-docs/**` at the production gateway while leaving them available in controlled development environments. Keep only `/actuator/health` public; do not expose additional actuator endpoints without authentication.

13. **Configure logs, monitoring, and alerts.** Centralize application logs, redact secrets and personal data, and alert on repeated 5xx responses, failed restarts, database saturation, rejected logins, scheduler failures, and notification-provider failures. Ensure the runtime clock is synchronized and `APP_TIME_ZONE` matches the business calendar.

14. **Configure backups and recovery.** Enable the appropriate Supabase backup/PITR plan, document retention, and perform a restore rehearsal. Record the recovery-time and recovery-point objectives. Take an on-demand backup before schema migrations and destructive operational work.

15. **Run production smoke tests.** Using dedicated test users for each role, verify login and OTP, refresh and logout, all RBAC boundaries, lead assignment and conversion, pipeline transitions, payments, tasks and overdue processing, comments/replies, calendar reminders, automations, broadcasts, activity logs, dashboard calculations, pagination, filtering, validation errors, conflict responses, and soft-delete visibility.

16. **Complete a security review.** Rotate any credential used during setup, review dependency/security scan results, rate-limit login and OTP endpoints at the gateway, define data-retention and deletion procedures, verify messaging consent, and arrange an external penetration test before storing real customer data.

17. **Prepare rollback.** Keep the previous application image available, document the rollback command, and ensure every database migration has a tested forward-fix strategy. Do not manually edit an already-recorded Flyway migration in production.
