# Skytech CRM Backend

Production-oriented REST API for Skytech CRM, built with Java 21, Spring Boot 3, stateless JWT security, PostgreSQL, Flyway, JPA/Hibernate, MapStruct, Arkesel SMS, and JavaMailSender.

## Prerequisites

- Java 21
- A Supabase project (or PostgreSQL 15+ locally)
- Optional for local development: Arkesel SMS and Google Workspace SMTP credentials for real outbound messages
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
| `PORT` | no | HTTP port supplied by the hosting platform; defaults to `8080` |
| `JWT_SECRET` | yes | HMAC key, minimum 32 random bytes |
| `AUTH_OTP_ENABLED` | no | Enables the second login step; temporarily defaults to `false` |
| `CORS_ALLOWED_ORIGINS` | no | Comma-separated frontend origins; defaults to `http://localhost:3000` |
| `COMMUNICATION_SMS_PROVIDER` | for SMS | Must be `arkesel` |
| `ARKESEL_SMS_API_URL` | for SMS | Arkesel v2 send endpoint; defaults to `https://sms.arkesel.com/api/v2/sms/send` |
| `ARKESEL_SMS_API_KEY` | for SMS | Secret Arkesel API key; backend/Railway only |
| `ARKESEL_SMS_SENDER_ID` | for SMS | Arkesel-approved sender ID, at most 11 characters |
| `ARKESEL_SMS_SANDBOX` | no | Use `true` while testing and `false` for real delivery |
| `ARKESEL_DEFAULT_COUNTRY_CODE` | no | Country code added to local numbers; defaults to Ghana `233` |
| `MAIL_HOST` / `MAIL_PORT` | for email | SMTP endpoint; defaults to `smtp.gmail.com:587` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | for email | Google Workspace address and app password |
| `MAIL_FROM` | for email | Verified sender; defaults to `info@skytechghana.com` |
| `INVOICE_ISSUER_NAME` | yes | Legal/display name frozen into newly created invoices |
| `INVOICE_ISSUER_EMAIL` | recommended | Issuer contact email printed on invoices |
| `INVOICE_ISSUER_PHONE` | recommended | Issuer phone printed on invoices |
| `INVOICE_ISSUER_ADDRESS` | recommended | Issuer address printed on invoices |
| `INVOICE_ISSUER_TAX_ID` | where applicable | Tax/registration identifier printed on invoices |
| `INVOICE_PAYMENT_INSTRUCTIONS` | recommended | Bank or payment instructions printed on invoices |
| `MAIL_HEALTH_ENABLED` | no | Enables the SMTP health probe after email is configured; defaults to `false` |
| `STRIPE_SECRET_KEY` | no | Reserved for the inactive billing integration |
| `APP_TIME_ZONE` | no | Scheduler time zone; defaults to `Africa/Accra` |

Outbound failures are logged and do not fail CRM requests. In development, leaving the Arkesel API key and SMTP password blank is safe, although OTP delivery will not reach the user. SMS recipients are normalized to international digits; Ghanaian local numbers beginning with `0` use country code `233` by default.

The `info@skytechghana.com` Google Workspace mailbox is intended for OTPs, individual notifications, and invoice delivery. Do not use Gmail for heavy email broadcasts: its daily quotas and anti-abuse controls are unsuitable for bulk campaigns. Select the SMS channel to send bulk campaigns through Arkesel, or move email broadcasts to a dedicated transactional/bulk-email SMTP provider before enabling high-volume email delivery.

The Stripe SDK client is wired from `STRIPE_SECRET_KEY`, but no billing operation invokes it yet. Feature gates intentionally allow all plans until billing is activated.

Invoice and automation frontend request examples, lifecycle rules, PDF handling, and status behavior are documented in [`docs/FRONTEND_INVOICES_AUTOMATIONS.md`](docs/FRONTEND_INVOICES_AUTOMATIONS.md).

The August 2026 authentication, lead/deal, personal automation, broadcast, dashboard/task, and invoice contract changes are documented in [`docs/FRONTEND_CONTRACT_2026_08.md`](docs/FRONTEND_CONTRACT_2026_08.md).

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

1. `POST /api/v1/auth/login` validates email/password. While `AUTH_OTP_ENABLED=false`, it immediately returns `requires_otp: false`, the access token, refresh token, and user.
2. When `AUTH_OTP_ENABLED=true`, login sends a six-digit OTP and returns `requires_otp: true`; `POST /api/v1/auth/verify-otp` then returns the tokens.
3. Send `Authorization: Bearer ACCESS_TOKEN` on protected requests.
4. `POST /api/v1/auth/refresh` rotates access credentials from a valid, non-revoked refresh token.
5. `POST /api/v1/auth/logout` revokes the stored refresh-token hash.

Role checks are enforced in the service layer. Leads and deals are restricted to assigned agents, while administrators and managers receive the broader views described by the API contract.

The OTP bypass is intended only for the current development/demo period. Re-enable it later by setting `AUTH_OTP_ENABLED=true` in Railway and redeploying; no code rollback is required. The frontend should inspect `data.requires_otp`: when false, store `data.access_token` and `data.refresh_token` and enter the CRM immediately; when true, display the existing OTP screen.

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

   Store the result, database password, Arkesel API key, Google app password, and future Stripe key in the deployment platform's encrypted secret manager. Never copy production values into `.env.example`, Git, Docker images, frontend code, tickets, or chat messages.

3. **Configure every production environment variable.** Set `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, and `APP_TIME_ZONE`. Configure Arkesel and email before the first real login because OTP delivery requires at least one working channel. `CORS_ALLOWED_ORIGINS` must contain only the real HTTPS frontend origins, comma-separated, with no wildcard.

4. **Configure `info@skytechghana.com` email delivery.** Confirm that the address is a Google Workspace mailbox, enable two-step verification for it, and create a Google app password (Google Account -> Security -> App passwords). In Railway set `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME=info@skytechghana.com`, `MAIL_PASSWORD` to the 16-character app password, and `MAIL_FROM=info@skytechghana.com`. Do not use the normal mailbox password. Ensure SPF/DKIM are enabled for `skytechghana.com`. Set `MAIL_HEALTH_ENABLED=true` only after the credentials work, then send a real OTP and invoice email. Gmail is acceptable for transactional messages but not heavy email broadcasts; connect a dedicated transactional/bulk-email provider before sending those.

5. **Configure Arkesel SMS.** In Arkesel, create an API key and register/approve a sender ID. The API permits no more than 11 sender-ID characters, so `Skytech Ghana` is invalid; use the exact shorter value approved by Arkesel, such as `Skytech` or `SkytechGH`. In Railway set `COMMUNICATION_SMS_PROVIDER=arkesel`, `ARKESEL_SMS_API_URL=https://sms.arkesel.com/api/v2/sms/send`, `ARKESEL_SMS_API_KEY` to the secret API key, `ARKESEL_SMS_SENDER_ID` to that approved value, `ARKESEL_SMS_SANDBOX=true`, and `ARKESEL_DEFAULT_COUNTRY_CODE=233`. Test OTP and opted-in broadcast delivery, then change `ARKESEL_SMS_SANDBOX=false` for production. Confirm consent, opt-out, credits, and Ghana messaging requirements before bulk sending. These values never belong in Vercel or frontend code.

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
