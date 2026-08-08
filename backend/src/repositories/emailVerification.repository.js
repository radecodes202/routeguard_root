// repositories/emailVerification.repository.js
// Email verification token data access layer

const { Pool } = require('pg');

class EmailVerificationRepository {
  constructor(pool) {
    this.pool = pool;
  }

  /**
   * Create a new email verification token
   * @param {Object} tokenData - Token data (user_id, token_hash, expires_at)
   * @returns {Object} - Created token object
   */
  async create(tokenData) {
    const { user_id, token_hash, expires_at } = tokenData;

    const result = await this.pool.query(
      `INSERT INTO email_verification_tokens (
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
   * Find verification token by hash
   * @param {string} hashedToken - Hashed verification token
   * @returns {Object|null} - Token object or null if not found/expired
   */
  async findByToken(hashedToken) {
    const result = await this.pool.query(
      `SELECT * FROM email_verification_tokens
       WHERE token_hash = $1 AND expires_at > NOW()`,
      [hashedToken]
    );
    return result.rows[0] || null;
  }

  /**
   * Delete used verification token
   * @param {string} id - Token UUID
   * @returns {Promise<void>}
   */
  async consume(id) {
    await this.pool.query(
      'DELETE FROM email_verification_tokens WHERE id = $1',
      [id]
    );
  }

  /**
   * Delete expired verification tokens (cleanup)
   * @returns {Promise<void>}
   */
  async deleteExpired() {
    await this.pool.query(
      'DELETE FROM email_verification_tokens WHERE expires_at <= NOW()'
    );
  }
}

module.exports = EmailVerificationRepository;