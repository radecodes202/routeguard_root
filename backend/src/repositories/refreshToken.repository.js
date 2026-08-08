// repositories/refreshToken.repository.js
// Refresh token data access layer

class RefreshTokenRepository {
  constructor(pool) {
    this.pool = pool;
  }

  /**
   * Find refresh token by hashed token
   * @param {string} hashedToken - Hashed refresh token
   * @returns {Object|null} - Refresh token object or null if not found
   */
  async findByToken(hashedToken) {
    const result = await this.pool.query(
      'SELECT * FROM refresh_tokens WHERE token_hash = $1',
      [hashedToken]
    );
    return result.rows[0] || null;
  }

  /**
   * Create a new refresh token
   * @param {Object} tokenData - Token data (user_id, token_hash, device_info, expires_at)
   * @returns {Object} - Created refresh token object
   */
  async create(tokenData) {
    const { user_id, token_hash, device_info, expires_at } = tokenData;

    const result = await this.pool.query(
      `INSERT INTO refresh_tokens (
        user_id,
        token_hash,
        device_info,
        expires_at
      ) VALUES ($1, $2, $3, $4)
      RETURNING *`,
      [user_id, token_hash, device_info, expires_at]
    );
    return result.rows[0];
  }

  /**
   * Revoke a refresh token (set revoked_at)
   * @param {string} id - Refresh token UUID
   * @returns {Object} - Updated refresh token object
   */
  async revoke(id) {
    const result = await this.pool.query(
      'UPDATE refresh_tokens SET revoked_at = NOW() WHERE id = $1 RETURNING *',
      [id]
    );
    return result.rows[0];
  }

  /**
   * Revoke all refresh tokens for a user (logout everywhere)
   * @param {string} userId - User's UUID
   * @returns {Promise<void>}
   */
  async revokeAllByUser(userId) {
    await this.pool.query(
      'UPDATE refresh_tokens SET revoked_at = NOW() WHERE user_id = $1',
      [userId]
    );
  }

  /**
   * Find valid (not expired, not revoked) refresh tokens for a user
   * @param {string} userId - User's UUID
   * @returns {Array} - Array of valid refresh token objects
   */
  async findValidByUser(userId) {
    const result = await this.pool.query(
      `SELECT * FROM refresh_tokens
       WHERE user_id = $1
         AND expires_at > NOW()
         AND revoked_at IS NULL`,
      [userId]
    );
    return result.rows;
  }

  /**
   * Delete expired refresh tokens (cleanup)
   * @returns {Promise<void>}
   */
  async deleteExpired() {
    await this.pool.query(
      'DELETE FROM refresh_tokens WHERE expires_at <= NOW()'
    );
  }
}

module.exports = RefreshTokenRepository;