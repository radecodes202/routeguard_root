-- RouteGuard Seed Script
-- Inserts initial data for development/local testing

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Insert hazard categories (these are enforced by CHECK constraints in the tables)
-- No separate table needed as they're simple enum-like values

-- Insert sample users
-- Password for all users is "password123" (hashed with bcrypt)
-- In a real application, you would use a secure password and force change on first login

INSERT INTO users (id, email, phone_number, password_hash, full_name, role, reputation_score, is_active, email_verified_at)
VALUES
-- Admin user
('e414f95e-84d2-466a-836d-1e5f3a9b2c7a', 'admin@routeguard.gov', '+639123456789', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'System Administrator', 'admin', 75.00, true, NOW()),
-- MIO Staff user
('a8f2c3b1-4e6f-4b7a-9c2d-3f4b5c6d7e8f', 'mio@routeguard.gov', '+639123456790', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'MIO Officer', 'mio_staff', 70.00, true, NOW()),
-- Moderator user
('b9g3d4c2-5f7g-4c8b-0d3e-4g5h6i7j8k9l', 'moderator@routeguard.gov', '+639123456791', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'Community Moderator', 'moderator', 65.00, true, NOW()),
-- Sample commuter users
('c0h4e5d3-6g8h-4d9c-1e4f-5h6i7j8k9l0m', 'juan.deto@email.com', '+639123456792', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'Juan Dela Cruz', 'commuter', 50.00, true, NOW()),
('d1i5f6e4-7h9i-4e0d-2f5g-6i7j8k9l0m1n', 'maria.santos@email.com', '+639123456793', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'Maria Santos', 'commuter', 60.00, true, NOW()),
('e2j6g7f5-8i0j-4f1e-3g6h-7j8k9l0m1n2o', 'jose.rizal@email.com', '+639123456794', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'Jose Rizal', 'commuter', 80.00, true, NOW()),
('f3k7h8g6-9j1k-4g2f-4h7i-8k9l0m1n2o3p', 'ana.garcia@email.com', '+639123456795', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'Ana Garcia', 'commuter', 45.00, true, NOW()),
('g4l8i9h7-0k2l-4h3g-5i8j-9l0m1n2o3p4q', 'pedro.lim@email.com', '+639123456796', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'Pedro Lim', 'commuter', 55.00, true, NOW()),
('h5m9j0i8-1l3m-4i4h-6j9k-0m1n2o3p4q5r', 'lisa.tan@email.com', '+639123456797', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'Lisa Tan', 'commuter', 65.00, true, NOW()),
('i6n0k1j9-2m4n-4j5i-7k0l-1n2o3p4q5r6s', 'carlos.villa@email.com', '+639123456798', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'Carlos Villa', 'commuter', 70.00, true, NOW()),
('j7o1l2k0-3n5o-4k6j-8l1m-2o3p4q5r6s7t', 'elena.perez@email.com', '+639123456799', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.eua3.O', 'Elena Perez', 'commuter', 40.00, true, NOW());

-- Insert sample hazard reports around Tacloban City, Leyte
-- Approximate bounding box: lat 11.20-11.28, lng 124.96-125.05

INSERT INTO hazard_reports (id, reporter_id, category, description, location, status, confidence_score, confirm_count, deny_count, created_at)
VALUES
-- Flooded area near downtown Tacloban
('e414f95e-84d2-466a-836d-1e5f3a9b2c7b', 'c0h4e5d3-6g8h-4d9c-1e4f-5h6i7j8k9l0m', 'flooded', 'Ankle-deep flood on Burgos Street near City Hall', 'POINT(125.0050 11.2500)'::geography, 'confirmed', 95.00, 12, 0, NOW() - INTERVAL '2 hours'),
-- Fully blocked road due to fallen tree
('a8f2c3b1-4e6f-4b7a-9c2d-3f4b5c6d7e8g', 'd1i5f6e4-7h9i-4e0d-2f5g-6i7j8k9l0m1n', 'fully_blocked', 'Large acacia tree fell across Maharlika Highway', 'POINT(124.9800 11.2400)'::geography, 'confirmed', 90.00, 8, 1, NOW() - INTERVAL '3 hours'),
-- Debris from construction
('b9g3d4c2-5f7g-4c8b-0d3e-4g5h6i7j8k9l', 'e2j6g7f5-8i0j-4f1e-3g6h-7j8k9l0m1n2o', 'debris', 'Construction debris blocking lane on P. Paterno St', 'POINT(125.0100 11.2450)'::geography, 'pending', 75.00, 3, 2, NOW() - INTERVAL '1 hour'),
-- Accident scene
('c0h4e5d3-6g8h-4d9c-1e4f-5h6i7j8k9l0n', 'f3k7h8g6-9j1k-4g2f-4h7i-8k9l0m1n2o3p', 'accident', 'Minor collision at intersection of Real St and P. Gomez St', 'POINT(125.0000 11.2480)'::geography, 'confirmed', 85.00, 6, 0, NOW() - INTERVAL '90 minutes'),
-- Partially passable due to flooding
('d1i5f6e4-7h9i-4e0d-2f5g-6i7j8k9l0m1o', 'g4l8i9h7-0k2l-4h3g-5i8j-9l0m1n2o3p4q', 'partially_passable', 'Knee-deep flood on Abucay Passable only for motorcycles', 'POINT(124.9900 11.2350)'::geography, 'pending', 60.00, 2, 3, NOW() - INTERVAL '30 minutes'),
-- Another flooded area
('e2j6g7f5-8i0j-4f1e-3g6h-7j8k9l0m1n2p', 'h5m9j0i8-1l3m-4i4h-6j9k-0m1n2o3p4q5r', 'flooded', 'Ankle to knee deep flood on Justice Romualdez St', 'POINT(125.0150 11.2420)'::geography, 'flagged', 40.00, 1, 4, NOW() - INTERVAL '15 minutes'),
-- Another fully blocked road
('f3k7h8g6-9j1k-4g2f-4h7i-8k9l0m1n2o3q', 'i6n0k1j9-2m4n-4j5i-7k0l-1n2o3p4q5r6s', 'fully_blocked', 'Landslide blocking road to Tacloban Airport', 'POINT(125.0200 11.2200)'::geography, 'flagged', 35.00, 0, 5, NOW() - INTERVAL '10 minutes'),
-- Another debris report
('g4l8i9h7-0k2l-4h3g-5i8j-9l0m1n2o3p4r', 'j7o1l2k0-3n5o-4k6j-8l1m-2o3p4q5r6s7t', 'debris', 'Garbage and debris clogging drainage canal', 'POINT(124.9700 11.2550)'::geography, 'pending', 70.00, 4, 1, NOW() - INTERVAL '5 minutes'),
-- Another accident
('h5m9j0i8-1l3m-4i4h-6j9k-0m1n2o3p4q5s', 'c0h4e5d3-6g8h-4d9c-1e4f-5h6i7j8k9l0m', 'accident', 'Motorcycle vs tricycle collision near Tacloban City Astrodome', 'POINT(124.9950 11.2380)'::geography, 'confirmed', 92.00, 10, 0, NOW() - INTERVAL '45 minutes'),
-- Another partially passable
('i6n0k1j9-2m4n-4j5i-7k0l-1n2o3p4q5r6t', 'd1i5f6e4-7h9i-4e0d-2f5g-6i7j8k9l0m1n', 'partially_passable', 'Ankle-deep water on road to San Jose', 'POINT(124.9600 11.2600)'::geography, 'pending', 65.00, 3, 2, NOW() - INTERVAL '20 minutes');

-- Note: For a real deployment, you would want to:
-- 1. Use secure, unique passwords for each user
-- 2. Possibly add more varied sample data
-- 3. Consider adding some flagged reports for moderation queue testing
-- 4. Ensure the timestamps are realistic for your testing scenario