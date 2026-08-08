# Entity-Relationship Diagram for RouteGuard

## Entities and Attributes

### users
- id (UUID, PK)
- email (VARCHAR, UNIQUE, NOT NULL)
- phone_number (VARCHAR, UNIQUE, NULL)
- password_hash (VARCHAR, NOT NULL)
- full_name (VARCHAR, NOT NULL)
- role (ENUM: commuter, responder, mio_staff, moderator, admin, DEFAULT commuter)
- reputation_score (NUMERIC(5,2), DEFAULT 50.00, CHECK 0-100)
- reports_submitted_count (INT, DEFAULT 0)
- reports_confirmed_count (INT, DEFAULT 0)
- reports_false_count (INT, DEFAULT 0)
- responder_status (ENUM: not_applicable, pending, approved, rejected, DEFAULT not_applicable)
- is_active (BOOLEAN, DEFAULT TRUE)
- email_verified_at (TIMESTAMPTZ, NULL)
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())
- updated_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())

### refresh_tokens
- id (UUID, PK)
- user_id (UUID, FK to users.id, CASCADE DELETE)
- token_hash (VARCHAR, UNIQUE, NOT NULL)
- device_info (TEXT, NULL)
- expires_at (TIMESTAMPTZ, NOT NULL)
- revoked_at (TIMESTAMPTZ, NULL)
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())

### device_tokens
- id (UUID, PK)
- user_id (UUID, FK to users.id, CASCADE DELETE)
- fcm_token (VARCHAR, UNIQUE, NOT NULL)
- platform (ENUM: android, DEFAULT android)
- last_seen_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())

### hazard_reports
- id (UUID, PK)
- reporter_id (UUID, FK to users.id)
- category (ENUM: flooded, fully_blocked, debris, accident, partially_passable, NOT NULL)
- description (TEXT, NULL, max 500 chars)
- location (GEOGRAPHY(Point, 4326), NOT NULL)
- status (ENUM: pending, flagged, confirmed, false, inconclusive, DEFAULT pending)
- confidence_score (NUMERIC(5,2), DEFAULT 100.00, CHECK 0-100)
- confirm_count (INT, DEFAULT 0)
- deny_count (INT, DEFAULT 0)
- flagged_reason (ENUM: low_reputation, first_time_user, conflicting_report, user_flagged, NULL)
- resolved_by (UUID, FK to users.id, NULL, SET NULL on DELETE)
- resolved_at (TIMESTAMPTZ, NULL)
- resolution_notes (TEXT, NULL)
- expires_at (TIMESTAMPTZ, NULL)
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())
- updated_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())

### report_media
- id (UUID, PK)
- report_id (UUID, FK to hazard_reports.id, CASCADE DELETE)
- media_url (TEXT, NOT NULL)
- media_type (ENUM: image, DEFAULT image)
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())

### report_interactions
- id (UUID, PK)
- report_id (UUID, FK to hazard_reports.id, CASCADE DELETE)
- user_id (UUID, FK to users.id, CASCADE DELETE)
- action (ENUM: confirm, deny, flag_suspicious, NOT NULL)
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())
- UNIQUE constraint on (report_id, user_id) WHERE action IN ('confirm', 'deny')

### advisories
- id (UUID, PK)
- mio_staff_id (UUID, FK to users.id)
- title (VARCHAR(120), NOT NULL)
- description (TEXT, NOT NULL)
- location (GEOGRAPHY(Point, 4326), NULL)
- road_name (VARCHAR(150), NULL)
- status (ENUM: active, expired, removed, DEFAULT active)
- starts_at (TIMESTAMPTZ, NOT NULL)
- ends_at (TIMESTAMPTZ, NULL)
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())
- updated_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())
- CHECK (ends_at IS NULL OR ends_at > starts_at)

### moderation_actions
- id (UUID, PK)
- report_id (UUID, FK to hazard_reports.id)
- moderator_id (UUID, FK to users.id)
- resolution (ENUM: confirmed, false, inconclusive, NOT NULL)
- reputation_delta (NUMERIC(5,2), NOT NULL)
- notes (TEXT, NULL)
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())

### responder_accreditation_requests
- id (UUID, PK)
- user_id (UUID, FK to users.id)
- organization_name (VARCHAR(200), NOT NULL)
- id_document_url (TEXT, NOT NULL)
- status (ENUM: pending, approved, rejected, DEFAULT pending)
- reviewed_by (UUID, FK to users.id, NULL, SET NULL on DELETE)
- reviewed_at (TIMESTAMPTZ, NULL)
- notes (TEXT, NULL)
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())

### notifications
- id (UUID, PK)
- user_id (UUID, FK to users.id, CASCADE DELETE)
- type (ENUM: hazard_alert, advisory, moderation_result, system, NOT NULL)
- title (VARCHAR(150), NOT NULL)
- body (TEXT, NOT NULL)
- related_report_id (UUID, FK to hazard_reports.id, NULL, SET NULL on DELETE)
- is_read (BOOLEAN, DEFAULT FALSE)
- sent_at (TIMESTAMPTZ, NULL)
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())

### audit_logs
- id (UUID, PK)
- actor_id (UUID, FK to users.id, NULL, SET NULL on DELETE)
- action (VARCHAR(100), NOT NULL)
- entity_type (VARCHAR(50), NOT NULL)
- entity_id (UUID, NULL)
- metadata (JSONB, NULL)
- ip_address (INET, NULL)
- created_at (TIMESTAMPTZ, NOT NULL, DEFAULT NOW())

## Relationships

1. users (1) — (many) hazard_reports [reporter_id]
2. users (1) — (many) report_interactions [user_id]
3. hazard_reports (1) — (many) report_interactions [report_id]
4. hazard_reports (1) — (many) report_media [report_id]
5. hazard_reports (1) — (0..many) moderation_actions [report_id] (a report can be re-flagged and re-resolved over time)
6. users (1, as moderator) — (many) moderation_actions [moderator_id]
7. users (1, as mio_staff) — (many) advisories [mio_staff_id]
8. users (1) — (0..1) responder_accreditation_requests [user_id] (active at a time)
9. users (1) — (many) notifications [user_id]
10. users (1) — (many) refresh_tokens [user_id]
11. users (1) — (many) device_tokens [user_id]

## Mermaid ER Diagram

```erDiagram
    users ||--o{ hazard_reports : "reports"
    users ||--o{ refresh_tokens : "has"
    users ||--o{ device_tokens : "has"
    users ||--o{ report_interactions : "makes"
    users ||--o{ advisories : "posts"
    users ||--o{ responder_accreditation_requests : "applies"
    users ||--o{ notifications : "receives"
    users ||--o{ audit_logs : "creates"
    users }o--o{ moderation_actions : "moderates"
    
    hazard_reports ||--o{ report_media : "contains"
    hazard_reports ||--o{ report_interactions : "receives"
    hazard_reports }o--o{ moderation_actions : "triggers"
    
    advisories ||..|_ users : "posts by"
    
    moderation_actions ||..|_ users : "performed by"
    
    responder_accreditation_requests ||..|_ users : "applied by"
    
    notifications }o--|{ hazard_reports : "references"
```

## Indexes

### Performance Indexes
- GIST index on hazard_reports.location for geospatial queries
- GIST index on advisories.location for geospatial queries
- Index on hazard_reports(status, created_at) WHERE status = 'flagged' for moderation queue
- Index on notifications(user_id, is_read, sent_at DESC) for unread-first notification listing
- Unique index on users(email) for login lookup
- Index on refresh_tokens.user_id for token validation
- Index on refresh_tokens.token_hash for token lookup
- Index on device_tokens.user_id for device management
- Index on device_tokens.fcm_token for push notifications
- Index on report_interactions.report_id for report interactions
- Index on report_interactions.user_id for user interactions
- Index on advisories.mio_staff_id for MIO staff advisories
- Index on advisories.location for advisory map queries
- Index on moderation_actions.report_id for report moderation history
- Index on moderation_actions.moderator_id for moderator action history
- Index on responder_accreditation_requests.user_id for user applications
- Index on responder_accreditation_requests.status for application queue
- Index on audit_logs.actor_id for user activity tracking
- Index on audit_logs.action for action filtering
- Index on audit_logs.entity_type for entity filtering
- Index on audit_logs.entity_id for specific entity tracking
- Index on audit_logs.created_at for chronological queries
- Index on audit_logs.ip_address for security monitoring