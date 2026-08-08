-- Create device_tokens table
CREATE TABLE device_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token VARCHAR(255) UNIQUE NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'android' CHECK (platform = 'android'),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for device_tokens table
CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);
CREATE INDEX idx_device_tokens_fcm_token ON device_tokens(fcm_token);