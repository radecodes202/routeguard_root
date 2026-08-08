// repositories/reports.repository.js
// Reports data access layer

const { Pool } = require('pg');
const { v4: uuidv4 } = require('uuid');

class ReportsRepository {
  constructor(pool) {
    this.pool = pool;
  }

  /**
   * Create a new hazard report
   * @param {Object} reportData - Report data (reporter_id, category, description, location)
   * @returns {Object} - Created report object
   */
  async create(reportData) {
    const {
      reporter_id,
      category,
      description,
      location // Should be in format: 'POINT(longitude latitude)' for PostGIS
    } = reportData;

    const result = await this.pool.query(
      `INSERT INTO hazard_reports (
        reporter_id,
        category,
        description,
        location,
        status,
        confidence_score
      ) VALUES ($1, $2, $3, ST_GeogFromText($4), $5, $6)
      RETURNING *`,
      [
        reporter_id,
        category,
        description,
        location, // Expecting WKT format like 'POINT(-73.935242 40.730610)'
        'pending', // Default status
        100.00     // Default confidence score
      ]
    );
    return result.rows[0];
  }

  /**
   * Get report by ID
   * @param {string} id - Report's UUID
   * @returns {Object|null} - Report object or null if not found
   */
  async findById(id) {
    const result = await this.pool.query(
      'SELECT *, ST_AsText(location) as location_wkt FROM hazard_reports WHERE id = $1',
      [id]
    );
    return result.rows[0] || null;
  }

  /**
   * Get reports with filtering and pagination
   * @param {Object} filters - Filter options (status, category, reporter_id, bbox, limit, offset)
   * @returns {Object} - Reports data with count
   */
  async findAll(filters = {}) {
    const {
      status,
      category,
      reporter_id,
      bbox, // Format: "min_lon,min_lat,max_lon,max_lat"
      limit = 50,
      offset = 0
    } = filters;

    let query = `
      SELECT *, ST_AsText(location) as location_wkt
      FROM hazard_reports
      WHERE 1=1
    `;
    const values = [];
    let index = 1;

    // Add filters
    if (status) {
      query += ` AND status = $${index}`;
      values.push(status);
      index++;
    }

    if (category) {
      query += ` AND category = $${index}`;
      values.push(category);
      index++;
    }

    if (reporter_id) {
      query += ` AND reporter_id = $${index}`;
      values.push(reporter_id);
      index++;
    }

    // Bounding box filter for geographic queries
    if (bbox) {
      const [minLon, minLat, maxLon, maxLat] = bbox.split(',').map(parseFloat);
      if (!isNaN(minLon) && !isNaN(minLat) && !isNaN(maxLon) && !isNaN(maxLat)) {
        query += ` AND location && ST_MakeEnvelope($${index}, $${index+1}, $${index+2}, $${index+3}, 4326)`;
        values.push(minLon, minLat, maxLon, maxLat);
        index += 4;
      }
    }

    // Add ordering and pagination
    query += ` ORDER BY created_at DESC LIMIT $${index} OFFSET $${index+1}`;
    values.push(limit, offset);

    const result = await this.pool.query(query, values);

    // Get total count for pagination
    let countQuery = 'SELECT COUNT(*) FROM hazard_reports WHERE 1=1';
    const countValues = [];
    let countIndex = 1;

    if (status) {
      countQuery += ` AND status = $${countIndex}`;
      countValues.push(status);
      countIndex++;
    }

    if (category) {
      countQuery += ` AND category = $${countIndex}`;
      countValues.push(category);
      countIndex++;
    }

    if (reporter_id) {
      countQuery += ` AND reporter_id = $${countIndex}`;
      countValues.push(reporter_id);
      countIndex++;
    }

    if (bbox) {
      const [minLon, minLat, maxLon, maxLat] = bbox.split(',').map(parseFloat);
      if (!isNaN(minLon) && !isNaN(minLat) && !isNaN(maxLon) && !isNaN(maxLat)) {
        countQuery += ` AND location && ST_MakeEnvelope($${countIndex}, $${countIndex+1}, $${countIndex+2}, $${countIndex+3}, 4326)`;
        countValues.push(minLon, minLat, maxLon, maxLat);
        countIndex += 4;
      }
    }

    const countResult = await this.pool.query(countQuery, countValues);
    const total = parseInt(countResult.rows[0].count);

    return {
      reports: result.rows,
      total,
      limit,
      offset
    };
  }

  /**
   * Update report
   * @param {string} id - Report's UUID
   * @param {Object} updates - Fields to update
   * @returns {Object} - Updated report object
   */
  async update(id, updates) {
    const fields = [];
    const values = [];
    let index = 1;

    for (const [key, value] of Object.entries(updates)) {
      if (value !== undefined && value !== null) {
        // Handle special case for location (PostGIS)
        if (key === 'location') {
          fields.push(`location = ST_GeogFromText($${index})`);
        } else {
          fields.push(`${key} = $${index}`);
        }
        values.push(value);
        index++;
      }
    }

    if (fields.length === 0) {
      return await this.findById(id);
    }

    values.push(id); // for WHERE clause
    const query = `
      UPDATE hazard_reports
      SET ${fields.join(', ')}, updated_at = NOW()
      WHERE id = $${index}
      RETURNING *, ST_AsText(location) as location_wkt
    `;

    const result = await this.pool.query(query, values);
    return result.rows[0];
  }

  /**
   * Delete report
   * @param {string} id - Report's UUID
   * @returns {boolean} - True if deleted
   */
  async delete(id) {
    const result = await this.pool.query(
      'DELETE FROM hazard_reports WHERE id = $1 RETURNING id',
      [id]
    );
    return result.rowCount > 0;
  }

  /**
   * Get report interactions (confirms/denies)
   * @param {string} reportId - Report's UUID
   * @returns {Object} - Interaction counts
   */
  async getInteractions(reportId) {
    const result = await this.pool.query(
      `SELECT
        SUM(CASE WHEN action = 'confirm' THEN 1 ELSE 0 END) as confirm_count,
        SUM(CASE WHEN action = 'deny' THEN 1 ELSE 0 END) as deny_count
      FROM report_interactions
      WHERE report_id = $1`,
      [reportId]
    );
    return result.rows[0];
  }

  /**
   * Add report interaction (confirm/deny/flag)
   * @param {Object} interactionData - Interaction data (report_id, user_id, action)
   * @returns {Object} - Created interaction
   */
  async addInteraction(interactionData) {
    const { report_id, user_id, action } = interactionData;

    // Check if user already interacted with this report for confirm/deny actions
    if (action === 'confirm' || action === 'deny') {
      const existing = await this.pool.query(
        'SELECT id FROM report_interactions WHERE report_id = $1 AND user_id = $2 AND action IN ($3, $4)',
        [report_id, user_id, 'confirm', 'deny']
      );

      if (existing.rowCount > 0) {
        // Update existing interaction
        await this.pool.query(
          'UPDATE report_interactions SET action = $1, created_at = NOW() WHERE report_id = $2 AND user_id = $3',
          [action, report_id, user_id]
        );

        // Get the updated interaction
        const result = await this.pool.query(
          'SELECT * FROM report_interactions WHERE report_id = $1 AND user_id = $2',
          [report_id, user_id]
        );
        return result.rows[0];
      }
    }

    // Insert new interaction
    const result = await this.pool.query(
      `INSERT INTO report_interactions (report_id, user_id, action)
       VALUES ($1, $2, $3)
       RETURNING *`,
      [report_id, user_id, action]
    );
    return result.rows[0];
  }

  /**
   * Get reports that are flagged for moderation
   * @param {Object} filters - Filter options (limit, offset)
   * @returns {Object} - Flagged reports data
   */
  async getFlaggedReports(filters = {}) {
    const { limit = 50, offset = 0 } = filters;

    const result = await this.pool.query(
      `SELECT *, ST_AsText(location) as location_wkt
       FROM hazard_reports
       WHERE status = 'flagged'
       ORDER BY created_at DESC
       LIMIT $1 OFFSET $2`,
      [limit, offset]
    );

    // Get total count
    const countResult = await this.pool.query(
      'SELECT COUNT(*) FROM hazard_reports WHERE status = $1',
      ['flagged']
    );
    const total = parseInt(countResult.rows[0].count);

    return {
      reports: result.rows,
      total,
      limit,
      offset
    };
  }

  /**
   * Get reports within a radius of a point (for nearby hazards)
   * @param {number} lat - Latitude of center point
   * @param {number} lng - Longitude of center point
   * @param {number} radiusMeters - Radius in meters (default 5000 for 5km)
   * @param {Object} filters - Additional filters (status, category, limit, offset)
   * @returns {Object} - Reports data with count
   */
  async findNearby(lat, lng, radiusMeters = 5000, filters = {}) {
    const {
      status,
      category,
      limit = 50,
      offset = 0
    } = filters;

    let query = `
      SELECT *, ST_AsText(location) as location_wkt
      FROM hazard_reports
      WHERE ST_DWithin(location, ST_GeogFromText('POINT(' || $1 || ' ' || $2 || ')'), $3)
    `;
    const values = [lng, lat, radiusMeters]; // Note: PostGIS expects lon, lat order
    let index = 4;

    // Add filters
    if (status) {
      query += ` AND status = $${index}`;
      values.push(status);
      index++;
    }

    if (category) {
      query += ` AND category = $${index}`;
      values.push(category);
      index++;
    }

    // Add ordering and pagination
    query += ` ORDER BY created_at DESC LIMIT $${index} OFFSET $${index+1}`;
    values.push(limit, offset);

    const result = await this.pool.query(query, values);

    // Get total count for pagination
    let countQuery = `
      SELECT COUNT(*)
      FROM hazard_reports
      WHERE ST_DWithin(location, ST_GeogFromText('POINT(' || $1 || ' ' || $2 || ')'), $3)
    `;
    const countValues = [lng, lat, radiusMeters];
    let countIndex = 4;

    if (status) {
      countQuery += ` AND status = $${countIndex}`;
      countValues.push(status);
      countIndex++;
    }

    if (category) {
      countQuery += ` AND category = $${countIndex}`;
      countValues.push(category);
      countIndex++;
    }

    const countResult = await this.pool.query(countQuery, countValues);
    const total = parseInt(countResult.rows[0].count);

    return {
      reports: result.rows,
      total,
      limit,
      offset
    };
  }
}

module.exports = ReportsRepository;