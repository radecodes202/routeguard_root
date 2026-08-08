-- Create advisories table
CREATE TABLE advisories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mio_staff_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    location GEOGRAPHY(Point, 4326) NULL,
    road_name VARCHAR(150) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'expired', 'removed')),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (ends_at IS NULL OR ends_at > starts_at)
);

-- Create indexes for advisories table
CREATE INDEX idx_advisories_mio_staff_id ON advisories(mio_staff_id);
CREATE INDEX idx_advisories_location ON advisories USING GIST (location);
CREATE INDEX idx_advisories_status ON advisories(status);
CREATE INDEX idx_advisories_starts_at_ends_at ON advisories(starts_at, ends_at);