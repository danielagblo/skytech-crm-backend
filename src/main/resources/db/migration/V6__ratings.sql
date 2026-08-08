CREATE TABLE ratings (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), company_id UUID,
 agent_id UUID REFERENCES users(id), deal_id UUID REFERENCES deals(id),
 contact_channel VARCHAR(50), client_email VARCHAR(255), client_name VARCHAR(255),
 token VARCHAR(100) UNIQUE, rating INT, feedback TEXT,
 rated BOOLEAN NOT NULL DEFAULT FALSE, email_sent_at TIMESTAMPTZ, rated_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(),
 CONSTRAINT chk_ratings_rating CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5))
);

CREATE INDEX idx_ratings_agent ON ratings(agent_id);
CREATE INDEX idx_ratings_deal ON ratings(deal_id);
CREATE INDEX idx_ratings_token ON ratings(token);

CREATE TRIGGER ratings_updated_at BEFORE UPDATE ON ratings FOR EACH ROW EXECUTE FUNCTION set_updated_at();
