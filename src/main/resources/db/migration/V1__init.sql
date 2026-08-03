CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$ LANGUAGE plpgsql;

CREATE TABLE users (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL,
 email VARCHAR(255) UNIQUE NOT NULL, password_hash VARCHAR(255) NOT NULL, phone VARCHAR(30), username VARCHAR(100) UNIQUE,
 role VARCHAR(50) NOT NULL, plan_tier VARCHAR(50) NOT NULL DEFAULT 'FREE', profile_photo_url TEXT, is_active BOOLEAN DEFAULT TRUE,
 last_login TIMESTAMPTZ, otp_code VARCHAR(10), otp_expires_at TIMESTAMPTZ, refresh_token_hash VARCHAR(255), deleted_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE leads (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, assigned_to UUID[], created_by UUID REFERENCES users(id),
 first_name VARCHAR(100), last_name VARCHAR(100), email VARCHAR(255), phone_1 VARCHAR(30), phone_2 VARCHAR(30), whatsapp VARCHAR(30),
 company_name VARCHAR(255), role VARCHAR(100), address TEXT, industry VARCHAR(100), category VARCHAR(100), lead_source VARCHAR(100),
 priority VARCHAR(20), status VARCHAR(50), launch_timeline VARCHAR(50), has_public_office BOOLEAN, meeting_arranged BOOLEAN, birthday DATE,
 sms_opt_in BOOLEAN DEFAULT FALSE, email_opt_in BOOLEAN DEFAULT FALSE, newsletter_opt_in BOOLEAN DEFAULT FALSE, description TEXT,
 conversion_score INT DEFAULT 0 CHECK (conversion_score BETWEEN 0 AND 100), deleted_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE deals (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, lead_id UUID REFERENCES leads(id), created_by UUID REFERENCES users(id), assigned_to UUID REFERENCES users(id),
 title VARCHAR(255) NOT NULL, stage VARCHAR(50) NOT NULL, priority VARCHAR(20), contract_value NUMERIC(15,2), total_paid NUMERIC(15,2) DEFAULT 0,
 arrears NUMERIC(15,2) DEFAULT 0, is_paid_in_full BOOLEAN DEFAULT FALSE, hosting_expiry DATE, domain_expiry DATE, maintenance_expiry DATE,
 hosting_cost NUMERIC(15,2), domain_cost NUMERIC(15,2), maintenance_cost NUMERIC(15,2), notes TEXT, version BIGINT NOT NULL DEFAULT 0,
 deleted_at TIMESTAMPTZ, created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE deal_logs (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, deal_id UUID NOT NULL REFERENCES deals(id) ON DELETE CASCADE, created_by UUID REFERENCES users(id),
 log_type VARCHAR(50), contact_mode VARCHAR(50), response_type VARCHAR(50), follow_up_at TIMESTAMPTZ, settlement_value NUMERIC(15,2),
 settlement_follow_up TIMESTAMPTZ, special_conditions TEXT, amount_paid NUMERIC(15,2), payment_mode VARCHAR(50), invoice_number VARCHAR(100),
 receipt_number VARCHAR(100), invoice_issued BOOLEAN, service_type VARCHAR(50), expiry_date DATE, retention_amount NUMERIC(15,2),
 retention_invoice VARCHAR(100), retention_receipt VARCHAR(100), auto_review_score INT CHECK (auto_review_score BETWEEN 1 AND 5), body TEXT,
 created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE TABLE deal_log_comments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, deal_log_id UUID NOT NULL REFERENCES deal_logs(id) ON DELETE CASCADE,
 parent_comment_id UUID REFERENCES deal_log_comments(id) ON DELETE CASCADE, author_id UUID REFERENCES users(id), body TEXT NOT NULL,
 created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE tasks (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, title VARCHAR(255) NOT NULL, description TEXT, status VARCHAR(50), priority VARCHAR(20),
 created_by UUID REFERENCES users(id), allow_reminder BOOLEAN DEFAULT TRUE, linked_lead_id UUID REFERENCES leads(id), linked_deal_id UUID REFERENCES deals(id),
 due_date TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE TABLE task_assignees (task_id UUID REFERENCES tasks(id) ON DELETE CASCADE, user_id UUID REFERENCES users(id), PRIMARY KEY(task_id,user_id));
CREATE TABLE sub_tasks (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE, title VARCHAR(255) NOT NULL,
 description TEXT, priority VARCHAR(20), is_complete BOOLEAN DEFAULT FALSE, created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE TABLE task_comments (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
 parent_comment_id UUID REFERENCES task_comments(id) ON DELETE CASCADE, author_id UUID REFERENCES users(id), body TEXT NOT NULL,
 created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE activities (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, actor_id UUID REFERENCES users(id), event_type VARCHAR(100), entity_type VARCHAR(50),
 entity_id UUID, description TEXT, metadata JSONB, created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE TABLE calendar_events (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, title VARCHAR(255), description TEXT, owner_id UUID REFERENCES users(id),
 linked_lead_id UUID REFERENCES leads(id), linked_deal_id UUID REFERENCES deals(id), start_time TIMESTAMPTZ, end_time TIMESTAMPTZ,
 event_type VARCHAR(50), assignees UUID[], created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE TABLE automations (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, automation_type VARCHAR(50), name VARCHAR(255), is_active BOOLEAN DEFAULT TRUE,
 trigger_config JSONB, steps JSONB, created_by UUID REFERENCES users(id), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE TABLE broadcast_messages (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID, name VARCHAR(255), message_content TEXT NOT NULL, channel VARCHAR(20), status VARCHAR(20),
 recipient_count INT DEFAULT 0, segment_filter JSONB, created_by UUID REFERENCES users(id), scheduled_at TIMESTAMPTZ, sent_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE TABLE settings (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID UNIQUE, auto_assign_enabled BOOLEAN DEFAULT FALSE, lead_assignment_config JSONB,
 general_config JSONB, created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
);

DO $$ DECLARE t text; BEGIN FOREACH t IN ARRAY ARRAY['users','leads','deals','deal_logs','deal_log_comments','tasks','sub_tasks','task_comments','activities','calendar_events','automations','broadcast_messages','settings'] LOOP EXECUTE format('CREATE TRIGGER %I_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION set_updated_at()',t,t); END LOOP; END $$;

CREATE INDEX idx_leads_status ON leads(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_leads_assigned_to ON leads USING GIN(assigned_to) WHERE deleted_at IS NULL;
CREATE INDEX idx_deals_stage ON deals(stage) WHERE deleted_at IS NULL;
CREATE INDEX idx_tasks_due ON tasks(due_date,status);
CREATE INDEX idx_activities_created ON activities(created_at DESC);
CREATE INDEX idx_calendar_range ON calendar_events(start_time,end_time);
