-- Create hazard_reports table
CREATE TABLE hazard_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID NOT NULL REFERENCES users(id),
    category VARCHAR(20) NOT NULL CHECK (category IN ('flooded', 'fully_blocked', 'debris', 'accident', 'partially_passable')),
    description TEXT NULL,
    location GEOGRAPHY(Point, 4326) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'flagged', 'confirmed', 'false', 'inconclusive')),
    confidence_score NUMERIC(5,2) NOT NULL DEFAULT 100.00 CHECK (confidence_score BETWEEN 0 AND 100),
    confirm_count INTEGER NOT NULL DEFAULT 0,
    deny_count INTEGER NOT NULL DEFAULT 0,
    flagged_reason VARCHAR(20) NULL CHECK (flagged_reason IN ('low_reputation', 'first_time_user', 'conflicting_report', 'user_flagged')),
    resolved_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ NULL,
    resolution_notes TEXT NULL,
    expires_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for hazard_reports table
CREATE INDEX idx_hazard_reports_reporter_id ON hazard_reports(reporter_id);
CREATE INDEX idx_hazard_reports_location ON hazard_reports USING GIST (location);
CREATE INDEX idx_hazard_reports_status ON hazard_reports(status);
CREATE INDEX idx_hazard_reports_confidence_score ON hazard_reports(confidence_score);
CREATE INDEX idx_hazard_reports_status_created_at ON hazard_reports(status, created_at) WHERE status = 'flagged';