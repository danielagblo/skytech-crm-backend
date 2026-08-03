CREATE SEQUENCE invoice_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE invoices (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, deal_id UUID NOT NULL REFERENCES deals(id),
 created_by UUID REFERENCES users(id), invoice_number VARCHAR(50) UNIQUE, status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
 issue_date DATE, due_date DATE, currency VARCHAR(3) NOT NULL DEFAULT 'GHS', recipient_name VARCHAR(255) NOT NULL,
 recipient_company VARCHAR(255), recipient_email VARCHAR(255), recipient_address TEXT,
 issuer_name VARCHAR(255) NOT NULL DEFAULT 'Skytech', issuer_email VARCHAR(255), issuer_phone VARCHAR(30),
 issuer_address TEXT, issuer_tax_id VARCHAR(100), payment_instructions TEXT,
 subtotal NUMERIC(15,2) NOT NULL DEFAULT 0, tax_rate NUMERIC(7,4) NOT NULL DEFAULT 0,
 tax_amount NUMERIC(15,2) NOT NULL DEFAULT 0, discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
 total NUMERIC(15,2) NOT NULL DEFAULT 0, amount_paid NUMERIC(15,2) NOT NULL DEFAULT 0,
 balance_due NUMERIC(15,2) NOT NULL DEFAULT 0, notes TEXT, terms TEXT, last_send_error TEXT,
 issued_at TIMESTAMPTZ, send_requested_at TIMESTAMPTZ, sent_at TIMESTAMPTZ, paid_at TIMESTAMPTZ, voided_at TIMESTAMPTZ,
 version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
 CONSTRAINT chk_invoices_status CHECK (status IN ('DRAFT','ISSUED','SENDING','SENT','SEND_FAILED','PARTIALLY_PAID','PAID','VOID')),
 CONSTRAINT chk_invoices_currency CHECK (currency ~ '^[A-Z]{3}$'),
 CONSTRAINT chk_invoices_amounts CHECK (subtotal >= 0 AND tax_rate >= 0 AND tax_rate <= 100 AND tax_amount >= 0 AND discount_amount >= 0 AND total >= 0 AND amount_paid >= 0 AND balance_due >= 0)
);

CREATE TABLE invoice_items (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
 description VARCHAR(500) NOT NULL, quantity NUMERIC(15,4) NOT NULL, unit_price NUMERIC(15,2) NOT NULL,
 amount NUMERIC(15,2) NOT NULL, position INT NOT NULL, created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
 CONSTRAINT chk_invoice_items_amounts CHECK (quantity > 0 AND unit_price >= 0 AND amount >= 0)
);

CREATE TABLE invoice_payments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
 deal_log_id UUID REFERENCES deal_logs(id), recorded_by UUID REFERENCES users(id), amount NUMERIC(15,2) NOT NULL,
 payment_mode VARCHAR(50) NOT NULL, reference VARCHAR(100), paid_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
 CONSTRAINT chk_invoice_payment_amount CHECK (amount > 0),
 CONSTRAINT chk_invoice_payment_mode CHECK (payment_mode IN ('MOMO','BANK_TRANSFER','CASH','CHEQUE'))
);

CREATE INDEX idx_invoices_deal ON invoices(deal_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_number ON invoices(invoice_number);
CREATE INDEX idx_invoice_payments_invoice ON invoice_payments(invoice_id);

CREATE TRIGGER invoices_updated_at BEFORE UPDATE ON invoices FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER invoice_items_updated_at BEFORE UPDATE ON invoice_items FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER invoice_payments_updated_at BEFORE UPDATE ON invoice_payments FOR EACH ROW EXECUTE FUNCTION set_updated_at();
