-- Create responder_accreditation_requests table
CREATE TABLE responder_accreditation_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    organization_name VARCHAR(200) NOT NULL,
    id_document_url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    reviewed_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ NULL,
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for responder_accreditation_requests table
CREATE INDEX idx_responder_accreditation_requests_user_id ON responder_accreditation_requests(user_id);
CREATE INDEX idx_responder_accreditation_requests_status ON responder_accreditation_requests(status);
CREATE INDEX idx_responder_accreditation_requests_reviewed_by ON responder_accreditation_requests(reviewed_by);