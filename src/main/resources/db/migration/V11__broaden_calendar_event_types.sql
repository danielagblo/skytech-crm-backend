-- Broaden calendar event types so synced records (tasks, broadcasts,
-- renewals, follow-ups, automations) can be stored without constraint violations.
ALTER TABLE calendar_events DROP CONSTRAINT IF EXISTS chk_calendar_type;
ALTER TABLE calendar_events
  ADD CONSTRAINT chk_calendar_type
  CHECK (event_type IS NULL OR event_type IN
    ('CALL_LOG_FOLLOWUP','PAYMENT_DUE','MEETING','REMINDER','TASK_DUE'));