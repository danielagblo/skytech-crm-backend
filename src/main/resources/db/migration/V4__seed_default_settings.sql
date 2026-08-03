INSERT INTO settings (company_id, auto_assign_enabled, lead_assignment_config, general_config)
SELECT NULL, FALSE, '{}'::jsonb, '{}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM settings);
