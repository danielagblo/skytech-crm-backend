-- Early installations stored their single tenant as NULL. New tenant-scoped tables,
-- including department_targets, require an actual tenant identifier.
DO $$
DECLARE
  legacy_company_id UUID := '00000000-0000-0000-0000-000000000001';
BEGIN
  UPDATE users SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE leads SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE deals SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE deal_logs SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE deal_log_comments SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE tasks SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE sub_tasks SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE task_comments SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE activities SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE calendar_events SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE automations SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE broadcast_messages SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE invoices SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE invoice_items SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE invoice_payments SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE ratings SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE in_app_notifications SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE user_sessions SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE automation_execution_jobs SET company_id = legacy_company_id WHERE company_id IS NULL;
  UPDATE settings SET company_id = legacy_company_id WHERE company_id IS NULL;
END $$;

ALTER TABLE users ALTER COLUMN company_id SET NOT NULL;
