# Frontend contract update

All examples below are the `data` portion of the standard API envelope. JSON uses snake case.

## Authentication

`POST /api/v1/auth/login` returns populated tokens and user data when OTP is disabled:

```json
{
  "requires_otp": false,
  "user_id": "uuid",
  "access_token": "jwt",
  "refresh_token": "jwt",
  "user": { "id": "uuid", "email": "agent@example.com", "role": "AGENT" }
}
```

Access tokens remain short-lived. Keep the refresh token and call `POST /api/v1/auth/refresh`
with `{ "refresh_token": "jwt" }`; it remains valid for exactly seven days unless the user logs
out or logs in again. A successful refresh returns a new `access_token` and does not replace or
invalidate the current refresh token.

## Leads and automatic pipeline deals

Lead create and update accept multiple assignees:

```json
{
  "first_name": "Ama",
  "company_name": "Example Ltd",
  "assigned_to": ["agent-uuid-1", "agent-uuid-2"],
  "category": "Tech"
}
```

Use `category` as the canonical field. `industry` remains a temporary alias, but if both are sent
they must match. Accepted values are:

- `Hospitality`
- `Retail & E-commerce`
- `Education`
- `Tourism & Logistics`
- `Real estate & construction`
- `Healthcare`
- `Tech`
- `NGO`
- `Religion`
- `Other`

Creating a lead also creates its single `PROSPECTING` deal transactionally. Do not make a second
deal-create request. Deal responses now include `customer_first_name`, `customer_last_name`,
`customer_email`, `customer_phone`, `customer_company`, `customer_address`, and
`customer_category`; use these fields to prefill invoices.

## Personal automations

Create personal automations with top-level `contact_ids` and an exact ISO date:

```json
{
  "automation_type": "PERSONAL",
  "name": "Personal follow-up",
  "active": true,
  "contact_ids": ["lead-uuid-1", "lead-uuid-2"],
  "trigger_config": { "date": "2026-08-20" },
  "steps": [
    { "channel": "SMS", "message": "Your reminder" },
    { "channel": "EMAIL", "subject": "Reminder", "message": "Your reminder" }
  ]
}
```

`contact_ids` must reference contacts in the current tenant. Delivery respects each lead's
`sms_opt_in`/`email_opt_in` and available phone/email. Responses include `execution_state`,
`next_run_at`, `last_executed_at`, `failure_reason`, and `recipient_count`. One-time personal
automations move from `WAITING` to `COMPLETED` or `FAILED` when the scheduler processes them.

## Broadcasts

Audience sources are combined as a union and deduplicated:

```json
{
  "name": "Prospecting follow-up",
  "channel": "SMS",
  "message_content": "Hello",
  "contact_ids": ["explicit-lead-uuid"],
  "segment_filter": {
    "stages": ["PROSPECTING", "NEGOTIATION"],
    "lead_ids": ["another-lead-uuid"]
  },
  "scheduled_at": "2026-08-20T10:00:00Z"
}
```

Stage audiences come from deals currently in those stages. Future broadcasts are returned
immediately as `WAITING`, appear in list/recent-activity responses, and are dispatched every 15
minutes. The terminal state is `SENT` or `FAILED`; inspect `recipient_count` and
`failure_details`. Only opted-in, reachable contacts in the current tenant are delivered.

## Dashboard and tasks

Dashboard `period` accepts `today`, `this_week`, `this_month`, or `three_months`. Agents are always
scoped to themselves. Managers and administrators can omit `user_id` for all tenant users or pass
`user_id=<uuid>` for one user on overview and top-deals requests.

Task status updates accept status, reason, or both:

```json
{ "reason": "Waiting for customer documents" }
```

A reason-only request preserves the current status. The returned task exposes
`completion_reason`; managers and administrators also receive an in-app notification when another
user submits a reason.

## Invoices

Send only source values: deal, recipient overrides, dates, currency, tax rate, discount, notes,
terms, and the complete `items` collection. Never calculate or send subtotal, tax amount, line
amount, total, amount paid, or balance due—the backend owns all calculations.

Draft updates are complete replacements and must include the latest `version` returned by the API.
Include every line and sub-line that should remain. A stale version returns HTTP 409; a missing
version returns HTTP 400.

`GET /api/v1/invoices/{id}/pdf` returns `application/pdf` as a downloadable attachment. Sending
transitions through `SENDING` to `SENT` or `SEND_FAILED`; inspect `last_send_error` on failure.
`POST /api/v1/invoices/{id}/payments` creates the matching payment deal log in the same transaction,
so the frontend must not create a second deal log.
