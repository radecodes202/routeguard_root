-- Create report_interactions table
CREATE TABLE report_interactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES hazard_reports(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL CHECK (action IN ('confirm', 'deny', 'flag_suspicious')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for report_interactions table
CREATE INDEX idx_report_interactions_report_id ON report_interactions(report_id);
CREATE INDEX idx_report_interactions_user_id ON report_interactions(user_id);
CREATE INDEX idx_report_interactions_action ON report_interactions(action);

-- Create partial unique index for confirm/deny actions (one per user per report)
CREATE UNIQUE INDEX idx_report_interactions_user_report_confirm_deny
ON report_interactions(report_id, user_id)
WHERE action IN ('confirm', 'deny');