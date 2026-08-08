-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) UNIQUE NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'commuter' CHECK (role IN ('commuter', 'responder', 'mio_staff', 'moderator', 'admin')),
    reputation_score NUMERIC(5,2) NOT NULL DEFAULT 50.00 CHECK (reputation_score BETWEEN 0 AND 100),
    reports_submitted_count INTEGER NOT NULL DEFAULT 0,
    reports_confirmed_count INTEGER NOT NULL DEFAULT 0,
    reports_false_count INTEGER NOT NULL DEFAULT 0,
    responder_status VARCHAR(20) NOT NULL DEFAULT 'not_applicable' CHECK (responder_status IN ('not_applicable', 'pending', 'approved', 'rejected')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for users table
CREATE INDEX idx_users_email ON users(email);