ALTER TABLE invoices
  ADD COLUMN reception_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN reception_confirmed_at TIMESTAMPTZ,
  ADD COLUMN reception_confirmed_by UUID REFERENCES users(id);

CREATE TABLE in_app_notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id UUID,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type VARCHAR(50) NOT NULL,
  title VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  href TEXT,
  deduplication_key VARCHAR(255),
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_in_app_notification_dedup UNIQUE (user_id, deduplication_key)
);

CREATE INDEX idx_in_app_notifications_user_created
  ON in_app_notifications(user_id, created_at DESC);
CREATE INDEX idx_in_app_notifications_user_unread
  ON in_app_notifications(user_id, read_at) WHERE read_at IS NULL;

CREATE TRIGGER in_app_notifications_updated_at
  BEFORE UPDATE ON in_app_notifications
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
