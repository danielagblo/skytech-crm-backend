-- Create table to persist per-user lead seen state
CREATE TABLE IF NOT EXISTS lead_views (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id uuid,
  lead_id uuid NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  seen_at timestamptz NOT NULL,
  created_at timestamptz DEFAULT NOW(),
  updated_at timestamptz DEFAULT NOW(),
  CONSTRAINT ux_lead_user UNIQUE (lead_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_lead_views_user ON lead_views(user_id);
CREATE INDEX IF NOT EXISTS idx_lead_views_lead ON lead_views(lead_id);
