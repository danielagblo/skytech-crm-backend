ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN','MANAGER','AGENT')),
    ADD CONSTRAINT chk_users_plan CHECK (plan_tier IN ('FREE','PRO'));

ALTER TABLE leads
    ADD CONSTRAINT chk_leads_priority CHECK (priority IS NULL OR priority IN ('LOW','MEDIUM','HIGH')),
    ADD CONSTRAINT chk_leads_status CHECK (status IS NULL OR status IN ('NEW','CONTACTED','QUALIFIED','LOST','CONVERTED')),
    ADD CONSTRAINT chk_leads_source CHECK (lead_source IS NULL OR lead_source IN ('SMS','EMAIL','FACEBOOK','GOOGLE','BANNER','META_ADS')),
    ADD CONSTRAINT chk_leads_timeline CHECK (launch_timeline IS NULL OR launch_timeline IN ('IN_1_WEEK','ONE_TO_TWO_MONTHS','THREE_PLUS_MONTHS'));

ALTER TABLE deals
    ADD CONSTRAINT chk_deals_stage CHECK (stage IN ('PROSPECTING','NEGOTIATION','SETTLEMENT','PAYMENT','CLIENT_RETENTION')),
    ADD CONSTRAINT chk_deals_priority CHECK (priority IS NULL OR priority IN ('LOW','MEDIUM','HIGH'));

ALTER TABLE deal_logs
    ADD CONSTRAINT chk_deal_logs_type CHECK (log_type IS NULL OR log_type IN ('PROSPECTING','NEGOTIATION','SETTLEMENT','PAYMENT','CLIENT_RETENTION')),
    ADD CONSTRAINT chk_deal_logs_contact CHECK (contact_mode IS NULL OR contact_mode IN ('PHONE_CALL','EMAIL','IN_PERSON','WHATSAPP')),
    ADD CONSTRAINT chk_deal_logs_response CHECK (response_type IS NULL OR response_type IN ('POSITIVE','NEGATIVE','NEUTRAL','NO_RESPONSE')),
    ADD CONSTRAINT chk_deal_logs_payment CHECK (payment_mode IS NULL OR payment_mode IN ('MOMO','BANK_TRANSFER','CASH','CHEQUE')),
    ADD CONSTRAINT chk_deal_logs_service CHECK (service_type IS NULL OR service_type IN ('HOSTING','DOMAIN','MAINTENANCE'));

ALTER TABLE tasks
    ADD CONSTRAINT chk_tasks_status CHECK (status IS NULL OR status IN ('TODO','DOING','DONE','OVERDUE')),
    ADD CONSTRAINT chk_tasks_priority CHECK (priority IS NULL OR priority IN ('LOW','MEDIUM','HIGH'));

ALTER TABLE sub_tasks ADD CONSTRAINT chk_subtasks_priority CHECK (priority IS NULL OR priority IN ('LOW','MEDIUM','HIGH'));
ALTER TABLE automations ADD CONSTRAINT chk_automations_type CHECK (automation_type IS NULL OR automation_type IN ('BIRTHDAY','PUBLIC_HOLIDAY','PAYMENT','PERSONAL'));
ALTER TABLE broadcast_messages
    ADD CONSTRAINT chk_broadcast_status CHECK (status IS NULL OR status IN ('DRAFT','SENT','WAITING','FAILED')),
    ADD CONSTRAINT chk_broadcast_channel CHECK (channel IS NULL OR channel IN ('SMS','EMAIL'));
ALTER TABLE calendar_events ADD CONSTRAINT chk_calendar_type CHECK (event_type IS NULL OR event_type IN ('CALL_LOG_FOLLOWUP','PAYMENT_DUE','MEETING','REMINDER'));
ALTER TABLE activities ADD CONSTRAINT chk_activity_type CHECK (event_type IS NULL OR event_type IN (
    'UNAUTHORIZED_LOGIN','LEAD_STATUS_CHANGED','LEAD_LOG_CALL','LEAD_STAGE_CHANGED','TASK_STATUS_CHANGED',
    'COMMENT_RECEIVED_TASK','COMMENT_RECEIVED_LEAD','SUBTASK_CREATED','TASK_APPROVED',
    'HOSTING_EXPIRY_NOTICE','DOMAIN_EXPIRY_NOTICE','MAINTENANCE_EXPIRY_NOTICE'
));
