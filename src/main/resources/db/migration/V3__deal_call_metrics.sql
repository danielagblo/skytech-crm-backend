ALTER TABLE deal_logs
    ADD COLUMN call_direction VARCHAR(20),
    ADD COLUMN call_duration_seconds INT CHECK (call_duration_seconds IS NULL OR call_duration_seconds >= 0),
    ADD COLUMN call_outcome VARCHAR(50),
    ADD CONSTRAINT chk_call_direction CHECK (call_direction IS NULL OR call_direction IN ('OUTGOING','INCOMING')),
    ADD CONSTRAINT chk_call_outcome CHECK (call_outcome IS NULL OR call_outcome IN ('COMPLETED','NETWORK_INTERRUPTION','CUSTOMER_HUNG_UP','NO_RESPONSE'));

CREATE INDEX idx_deal_logs_follow_up ON deal_logs(follow_up_at) WHERE follow_up_at IS NOT NULL;
CREATE INDEX idx_deal_logs_call_metrics ON deal_logs(call_direction, contact_mode) WHERE contact_mode = 'PHONE_CALL';
CREATE INDEX idx_broadcasts_scheduled ON broadcast_messages(status, scheduled_at) WHERE status = 'WAITING';
