ALTER TABLE automations
  ADD COLUMN contact_ids UUID[],
  ADD COLUMN execution_state VARCHAR(30) NOT NULL DEFAULT 'WAITING',
  ADD COLUMN next_run_at TIMESTAMPTZ,
  ADD COLUMN last_executed_at TIMESTAMPTZ,
  ADD COLUMN failure_reason TEXT,
  ADD COLUMN recipient_count INT NOT NULL DEFAULT 0;

ALTER TABLE broadcast_messages
  ADD COLUMN contact_ids UUID[],
  ADD COLUMN failure_details TEXT;

CREATE INDEX idx_automations_next_run ON automations(is_active, next_run_at);
CREATE INDEX idx_broadcast_waiting ON broadcast_messages(status, scheduled_at);
