-- Create notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('hazard_alert', 'advisory', 'moderation_result', 'system')),
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    related_report_id UUID NULL REFERENCES hazard_reports(id) ON DELETE SET NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for notifications table
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_sent_at ON notifications(sent_at);
CREATE INDEX idx_notifications_user_id_is_read_sent_at ON notifications(user_id, is_read, sent_at DESC);
CREATE INDEX idx_notifications_related_report_id ON notifications(related_report_id);