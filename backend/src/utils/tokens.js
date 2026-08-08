// utils/tokens.js
// Hash/compare helpers for authentication

const bcrypt = require('bcryptjs');
const crypto = require('crypto');

/**
 * Hash a password using bcrypt
 * @param {string} password - Plain text password
 * @returns {string} - Hashed password
 */
async function hashPassword(password) {
  const saltRounds = 12; // As specified in the prompt
  return await bcrypt.hash(password, saltRounds);
}

/**
 * Compare a plain text password with a hashed password
 * @param {string} password - Plain text password
 * @param {string} hashedPassword - Hashed password from database
 * @returns {boolean} - True if passwords match
 */
async function comparePassword(password, hashedPassword) {
  return await bcrypt.compare(password, hashedPassword);
}

/**
 * Generate a random token for refresh tokens
 * @returns {string} - Random token
 */
function generateToken() {
  return crypto.randomBytes(32).toString('hex');
}

/**
 * Hash a token for storage (as specified: stored hashed in refresh_tokens table)
 * @param {string} token - Plain token
 * @returns {string} - Hashed token
 */
function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

module.exports = {
  hashPassword,
  comparePassword,
  generateToken,
  hashToken
};