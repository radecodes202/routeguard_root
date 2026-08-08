-- Create moderation_actions table
CREATE TABLE moderation_actions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES hazard_reports(id),
    moderator_id UUID NOT NULL REFERENCES users(id),
    resolution VARCHAR(20) NOT NULL CHECK (resolution IN ('confirmed', 'false', 'inconclusive')),
    reputation_delta NUMERIC(5,2) NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for moderation_actions table
CREATE INDEX idx_moderation_actions_report_id ON moderation_actions(report_id);
CREATE INDEX idx_moderation_actions_moderator_id ON moderation_actions(moderator_id);
CREATE INDEX idx_moderation_actions_resolution ON moderation_actions(resolution);
CREATE INDEX idx_moderation_actions_created_at ON moderation_actions(created_at);