// repositories/user.repository.js
// User data access layer

const { Pool } = require('pg');
const { v4: uuidv4 } = require('uuid');

class UserRepository {
  constructor(pool) {
    this.pool = pool;
  }

  /**
   * Find user by email
   * @param {string} email - User's email
   * @returns {Object|null} - User object or null if not found
   */
  async findByEmail(email) {
    const result = await this.pool.query(
      'SELECT * FROM users WHERE email = $1',
      [email]
    );
    return result.rows[0] || null;
  }

  /**
   * Find user by ID
   * @param {string} id - User's UUID
   * @returns {Object|null} - User object or null if not found
   */
  async findById(id) {
    const result = await this.pool.query(
      'SELECT * FROM users WHERE id = $1',
      [id]
    );
    return result.rows[0] || null;
  }

  /**
   * Create a new user
   * @param {Object} userData - User data (email, phone_number, password_hash, full_name, role)
   * @returns {Object} - Created user object
   */
  async create(userData) {
    const {
      email,
      phone_number,
      password_hash,
      full_name,
      role = 'commuter'
    } = userData;

    const result = await this.pool.query(
      `INSERT INTO users (
        email,
        phone_number,
        password_hash,
        full_name,
        role
      ) VALUES ($1, $2, $3, $4, $5)
      RETURNING *`,
      [email, phone_number, password_hash, full_name, role]
    );
    return result.rows[0];
  }

  /**
   * Update user profile
   * @param {string} id - User's UUID
   * @param {Object} updates - Fields to update
   * @returns {Object} - Updated user object
   */
  async update(id, updates) {
    const fields = [];
    const values = [];
    let index = 1;

    for (const [key, value] of Object.entries(updates)) {
      if (value !== undefined && value !== null) {
        fields.push(`${key} = $${index}`);
        values.push(value);
        index++;
      }
    }

    if (fields.length === 0) {
      return await this.findById(id);
    }

    values.push(id); // for WHERE clause
    const query = `
      UPDATE users
      SET ${fields.join(', ')}, updated_at = NOW()
      WHERE id = $${index}
      RETURNING *
    `;

    const result = await this.pool.query(query, values);
    return result.rows[0];
  }

  /**
   * Update user's email verification timestamp
   * @param {string} id - User's UUID
   * @returns {Object} - Updated user object
   */
  async verifyEmail(id) {
    return await this.update(id, { email_verified_at: new Date() });
  }

  /**
   * Update user's role
   * @param {string} id - User's UUID
   * @param {string} role - New role
   * @returns {Object} - Updated user object
   */
  async updateRole(id, role) {
    return await this.update(id, { role });
  }

  /**
   * Update user's active status
   * @param {string} id - User's UUID
   * @param {boolean} isActive - Active status
   * @returns {Object} - Updated user object
   */
  async updateStatus(id, isActive) {
    return await this.update(id, { is_active: isActive });
  }

  /**
   * Increment report counters
   * @param {string} userId - User's UUID
   * @param {string} type - Type of report ('submitted', 'confirmed', 'false')
   * @returns {Object} - Updated user object
   */
  async incrementReportCount(userId, type) {
    let field = '';
    switch (type) {
      case 'submitted':
        field = 'reports_submitted_count';
        break;
      case 'confirmed':
        field = 'reports_confirmed_count';
        break;
      case 'false':
        field = 'reports_false_count';
        break;
      default:
        throw new Error('Invalid report type');
    }

    const result = await this.pool.query(
      `UPDATE users
       SET ${field} = ${field} + 1, updated_at = NOW()
       WHERE id = $1
       RETURNING *`,
      [userId]
    );
    return result.rows[0];
  }
}

module.exports = UserRepository;