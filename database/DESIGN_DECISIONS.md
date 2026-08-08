# Database Design Decisions for RouteGuard

## Normalization Justification

The database schema follows Third Normal Form (3NF) with minimal deliberate denormalization for performance:

### tables with Denormalized Fields
1. **users table**:
   - `reports_submitted_count`, `reports_confirmed_count`, `reports_false_count` - These counters are denormalized to avoid expensive COUNT() queries on large datasets when displaying user reputation or statistics. While this introduces update complexity (requiring triggers or application-level updates), the read performance benefit significantly outweighs the cost for frequently accessed user profile data.

2. **hazard_reports table**:
   - `confirm_count`, `deny_count` - Similar to user counters, these are denormalized to avoid COUNT() queries when displaying report confidence metrics. The application maintains these counts through transactions when users interact with reports.

All other tables are strictly normalized to eliminate redundancy and ensure data integrity.

## Indexing Decisions

### Geospatial Indexes (GIST)
- Created GIST indexes on all geography columns (`hazard_reports.location`, `advisories.location`) as required for efficient geospatial queries using PostGIS functions like `ST_DWithin`. These are essential for the core 5km hazard detection feature.

### Query-Specific Composite Indexes
1. `hazard_reports(status, created_at) WHERE status = 'flagged'` - Optimizes the moderation queue query which filters by flagged status and orders by creation time to show oldest reports first.

2. `notifications(user_id, is_read, sent_at DESC)` - Supports the unread-first notification listing with efficient filtering by user and read status.

3. Unique indexes on authentication fields (`users.email`, `refresh_tokens.token_hash`, `device_tokens.fcm_token`) ensure fast lookups for login, token validation, and device management.

### Foreign Key Indexes
Automatically created indexes on all foreign key columns to support efficient JOIN operations and maintain referential integrity.

## Constraint Enforcement

### Database-Level Constraints
- All ENUM fields implemented as native PostgreSQL ENUM types to reject invalid values at the database layer
- CHECK constraints on numeric fields (`reputation_score`, `confidence_score`) to enforce 0-100 range
- CHECK constraint on `advisories` to ensure `ends_at > starts_at` when not null
- These constraints provide a critical safety net preventing invalid data even if application logic fails

### Referential Integrity
- ON DELETE CASCADE for child records that lose meaning without parent (report_media, report_interactions, etc.)
- ON DELETE SET NULL for audit trail fields to preserve history when referenced users are deleted
- This approach balances data integrity with the need to maintain historical records

## Design Trade-offs

### Performance vs. Normalization
The deliberate denormalization of counter fields represents a conscious trade-off favoring read performance over strict normalization. Given RouteGuard's read-heavy nature (users frequently viewing maps, reports, and profiles), this optimization significantly improves user experience with minimal impact on write performance (which occurs less frequently).

### Index Overhead
While indexes improve query performance, they do add storage overhead and slightly slow write operations. The selected indexes target the specific hot query paths identified in the requirements, ensuring we pay the overhead cost only where it provides significant benefit.

### UUID Primary Keys
Using UUIDs as primary keys provides benefits for distributed systems and hides internal IDs from users, but comes with slightly larger storage requirements and potentially slower sequential scans compared to integer IDs. For RouteGuard's expected scale, the benefits outweigh the costs.