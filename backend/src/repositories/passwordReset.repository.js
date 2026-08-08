// repositories/passwordReset.repository.js
// Password reset token data access layer

const { Pool } = require('pg');

class PasswordResetRepository {
  constructor(pool) {
    this.pool = pool;
  }

  /**
   * Create a new password reset token
   * @param {Object} tokenData - Token data (user_id, token_hash, expires_at)
   * @returns {Object} - Created token object
   */
  async create(tokenData) {
    const { user_id, token_hash, expires_at } = tokenData;

    const result = await this.pool.query(
      `INSERT INTO password_reset_tokens (
        user_id,
        token_hash,
        expires_at
      ) VALUES ($1, $2, $3)
      RETURNING *`,
      [user_id, token_hash, expires_at]
    );
    return result.rows[0];
  }

  /**
   * Find reset token by hash
   * @param {string} hashedToken - Hashed reset token
   * @returns {Object|null} - Token object or null if not found/expired
   */
  async findByToken(hashedToken) {
    const result = await this.pool.query(
      `SELECT * FROM password_reset_tokens
       WHERE token_hash = $1 AND expires_at > NOW()`,
      [hashedToken]
    );
    return result.rows[0] || null;
  }

  /**
   * Delete used reset token
   * @param {string} id - Token UUID
   * @returns {Promise<void>}
   */
  async consume(id) {
    await this.pool.query(
      'DELETE FROM password_reset_tokens WHERE id = $1',
      [id]
    );
  }

  /**
   * Delete expired reset tokens (cleanup)
   * @returns {Promise<void>}
   */
  async deleteExpired() {
    await this.pool.query(
      'DELETE FROM password_reset_tokens WHERE expires_at <= NOW()'
    );
  }
}

module.exports = PasswordResetRepository;