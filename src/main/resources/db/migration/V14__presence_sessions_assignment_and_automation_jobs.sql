ALTER TABLE users ADD COLUMN last_seen_at TIMESTAMPTZ;

ALTER TABLE settings ADD COLUMN assignment_cursor BIGINT NOT NULL DEFAULT 0;

CREATE TABLE user_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id UUID,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  started_at TIMESTAMPTZ NOT NULL,
  last_activity_at TIMESTAMPTZ NOT NULL,
  ended_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_sessions_open ON user_sessions(user_id, ended_at, last_activity_at);
CREATE INDEX idx_user_sessions_tenant ON user_sessions(company_id, user_id, started_at);
CREATE TRIGGER user_sessions_updated_at BEFORE UPDATE ON user_sessions
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE automations DROP CONSTRAINT IF EXISTS chk_automations_type;
UPDATE automations SET automation_type = 'PAYMENT_RECEIVED' WHERE automation_type = 'PAYMENT';
ALTER TABLE automations ADD CONSTRAINT chk_automations_type CHECK (
  automation_type IS NULL OR automation_type IN (
    'BIRTHDAY','PUBLIC_HOLIDAY','PAYMENT_RECEIVED','PAYMENT_DUE','PAYMENT_OVERDUE',
    'PAYMENT_RECOVERY','PERSONAL'
  )
);

CREATE TABLE automation_execution_jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id UUID,
  automation_id UUID NOT NULL REFERENCES automations(id) ON DELETE CASCADE,
  lead_id UUID REFERENCES leads(id) ON DELETE CASCADE,
  deal_id UUID REFERENCES deals(id) ON DELETE CASCADE,
  step_index INT NOT NULL,
  scheduled_at TIMESTAMPTZ NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  attempt_count INT NOT NULL DEFAULT 0,
  last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_automation_job_status CHECK (
    status IN ('PENDING','PROCESSING','COMPLETED','FAILED','CANCELLED')
  ),
  CONSTRAINT chk_automation_job_attempts CHECK (attempt_count >= 0),
  CONSTRAINT uq_automation_execution_job UNIQUE (
    automation_id, lead_id, deal_id, step_index, scheduled_at
  )
);
CREATE INDEX idx_automation_jobs_due
  ON automation_execution_jobs(status, scheduled_at);
CREATE TRIGGER automation_execution_jobs_updated_at
  BEFORE UPDATE ON automation_execution_jobs
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
