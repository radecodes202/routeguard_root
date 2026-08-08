-- Create report_media table
CREATE TABLE report_media (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES hazard_reports(id) ON DELETE CASCADE,
    media_url TEXT NOT NULL,
    media_type VARCHAR(20) NOT NULL DEFAULT 'image' CHECK (media_type = 'image'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for report_media table
CREATE INDEX idx_report_media_report_id ON report_media(report_id);