WITH duplicate_links AS (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY lead_id ORDER BY created_at, id) AS row_number
  FROM deals
  WHERE lead_id IS NOT NULL
)
UPDATE deals
SET lead_id = NULL
WHERE id IN (SELECT id FROM duplicate_links WHERE row_number > 1);

CREATE UNIQUE INDEX uq_deals_lead_id ON deals(lead_id) WHERE lead_id IS NOT NULL;

UPDATE leads SET category = industry WHERE category IS NULL AND industry IS NOT NULL;

ALTER TABLE leads
  ADD CONSTRAINT chk_leads_category
  CHECK (category IS NULL OR category IN (
    'Hospitality', 'Retail & E-commerce', 'Education', 'Tourism & Logistics',
    'Real estate & construction', 'Healthcare', 'Tech', 'NGO', 'Religion', 'Other'
  )) NOT VALID;
