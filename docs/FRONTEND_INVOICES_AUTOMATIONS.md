# Frontend integration: invoices and automations

All JSON endpoints require `Authorization: Bearer <access_token>` and return the standard Skytech API envelope. JSON property names use `snake_case`. The PDF endpoint returns raw `application/pdf` bytes instead of the JSON envelope.

## Invoice lifecycle

The supported lifecycle is:

`DRAFT -> ISSUED -> SENDING -> SENT -> PARTIALLY_PAID -> PAID`

Delivery failures become `SEND_FAILED` and can be sent again. An issued invoice with no payments can become `VOID`. Only drafts can be edited or deleted. If a process stops while sending, the same invoice can be retried after 10 minutes.

### Routes

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/invoices` | Paginated list. Supports `search`, `status`, `deal_id`, `page`, and `size`. |
| `POST` | `/api/v1/invoices` | Create a calculated draft. |
| `GET` | `/api/v1/invoices/{id}` | Get an invoice, line items, and payments. |
| `PUT` | `/api/v1/invoices/{id}` | Replace editable draft data. |
| `DELETE` | `/api/v1/invoices/{id}` | Delete a draft. |
| `POST` | `/api/v1/invoices/{id}/issue` | Freeze the invoice number and issue date. |
| `GET` | `/api/v1/invoices/{id}/pdf` | Download the generated PDF. |
| `POST` | `/api/v1/invoices/{id}/send` | Queue the PDF as an email attachment. |
| `POST` | `/api/v1/invoices/{id}/payments` | Record a partial or full payment. |
| `POST` | `/api/v1/invoices/{id}/void` | Void an issued unpaid invoice. |

Agents only see and modify invoices for deals assigned to them. Managers and administrators can access all invoices.

### Create or update a draft

```json
{
  "deal_id": "0e6cbf03-cb72-4f73-8f25-a4623d25650b",
  "recipient_name": "Ama Mensah",
  "recipient_company": "Acme Limited",
  "recipient_email": "billing@acme.example",
  "recipient_address": "Accra, Ghana",
  "due_date": "2026-08-17",
  "currency": "GHS",
  "tax_rate": 15,
  "discount_amount": 50,
  "notes": "Thank you for your business.",
  "terms": "Payment is due within 14 days.",
  "items": [
    {
      "description": "CRM implementation",
      "quantity": 1,
      "unit_price": 5000
    },
    {
      "description": "Monthly support",
      "quantity": 3,
      "unit_price": 500
    }
  ],
  "version": 0
}
```

`recipient_name`, company, email, and address are populated from the deal's lead when omitted. The frontend must not calculate or submit subtotal, tax amount, total, paid amount, or balance. The backend calculates them using decimal arithmetic. `version` is returned by the backend and should be sent on draft updates to detect concurrent edits.

Treat a successful `PUT` as a full replacement of the draft and send all current line items.

### Issue and download

Issuing assigns a unique number such as `INV-2026-000042`. If no due date was supplied, the backend defaults it to 14 days after issue. The number, line-item snapshot, issuer details, and financial values then become authoritative.

For Axios, request the PDF as a blob:

```js
const response = await api.get(`/api/v1/invoices/${invoiceId}/pdf`, {
  responseType: "blob"
});
const url = URL.createObjectURL(response.data);
const anchor = document.createElement("a");
anchor.href = url;
anchor.download = `${invoice.invoice_number}.pdf`;
anchor.click();
URL.revokeObjectURL(url);
```

### Send by email

```json
{
  "email": "billing@acme.example",
  "subject": "Invoice INV-2026-000042 from Skytech",
  "message": "Please find your invoice attached."
}
```

All three fields are optional; the saved recipient email and default subject/body are used when omitted. The immediate response normally has `status: "SENDING"`. Poll `GET /api/v1/invoices/{id}` until it becomes `SENT`, `PARTIALLY_PAID`, `PAID`, or `SEND_FAILED`. Show `last_send_error` for a failed delivery and allow the user to retry.

Email credentials are currently optional. If SMTP and `MAIL_FROM` are not configured, invoice creation, issue, PDF download, and payments still work, while sending ends in `SEND_FAILED` without crashing the request.

### Record payment

```json
{
  "amount": 2500,
  "payment_mode": "BANK_TRANSFER",
  "reference": "BANK-TX-39201",
  "paid_at": "2026-08-03T12:00:00Z"
}
```

`payment_mode` must be `MOMO`, `BANK_TRANSFER`, `CASH`, or `CHEQUE`. A payment cannot exceed `balance_due`. Recording it also creates the corresponding `PAYMENT` deal log and updates the deal's total paid, arrears, and paid-in-full state. Do not separately create another payment deal log for the same invoice payment.

## Automation builder

The backend already exposes frontend CRUD for automations. `ADMIN` and `MANAGER` can use it; `AGENT` receives `403`.

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/automations/options` | Supported types, channels, step fields, and trigger requirements. |
| `GET` | `/api/v1/automations` | Paginated list. |
| `POST` | `/api/v1/automations` | Create. |
| `GET` | `/api/v1/automations/{id}` | Read. |
| `PUT` | `/api/v1/automations/{id}` | Update. |
| `DELETE` | `/api/v1/automations/{id}` | Delete. |
| `PUT` | `/api/v1/automations/{id}/toggle` | Activate or deactivate. |
| `GET` | `/api/v1/automations/birthday-configs` | Birthday workflows. |
| `GET` | `/api/v1/automations/holiday-configs` | Holiday workflows. |
| `GET` | `/api/v1/automations/payment-workflows` | Payment workflows. |

### Birthday automation

```json
{
  "automation_type": "BIRTHDAY",
  "name": "Birthday greeting",
  "active": true,
  "trigger_config": {},
  "steps": [
    {
      "channel": "BOTH",
      "subject": "Happy birthday",
      "message": "Skytech wishes you a happy birthday."
    }
  ]
}
```

The backend checks lead birthdays every day at 8 AM. The frontend does not supply a date for this automation; birthdays are stored on leads.

### Public holiday automation

```json
{
  "automation_type": "PUBLIC_HOLIDAY",
  "name": "Independence Day greeting",
  "active": true,
  "trigger_config": {
    "date": "2027-03-06"
  },
  "steps": [
    {
      "channel": "SMS",
      "message": "Happy Independence Day from Skytech."
    }
  ]
}
```

Holiday dates are entered manually in `YYYY-MM-DD` format. Invalid or missing dates return `400`. The scheduler checks them every day at 7 AM. Create the next year's configuration for holidays that should recur annually.

### Payment automation

```json
{
  "automation_type": "PAYMENT",
  "name": "Payment acknowledgement",
  "active": true,
  "trigger_config": {},
  "steps": [
    {
      "channel": "EMAIL",
      "subject": "Payment received",
      "message": "Thank you. Your payment has been recorded."
    }
  ]
}
```

Payment workflows run when a positive deal payment log is created, including through the invoice payment endpoint.

Each step requires `channel` and `message`. `channel` must be `SMS`, `EMAIL`, or `BOTH`; `subject` is optional. Messages are only sent to leads who have opted into the selected channel and have the necessary contact data. `PERSONAL` configurations can be stored, but no execution trigger is currently defined for that type; the options endpoint reports it as non-executable, so the frontend should disable or label it accordingly.
