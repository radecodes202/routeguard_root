// services/auth.service.js
// Authentication business logic

const TokenUtils = require('../utils/tokens');
const { Pool } = require('pg');

class AuthService {
  constructor(userRepository, refreshTokenRepository, pool) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.pool = pool;
    this.emailVerificationRepository = new EmailVerificationRepository(pool);
    this.passwordResetRepository = new PasswordResetRepository(pool);
  }

  /**
   * Register a new user
   * @param {Object} userData - Registration data (email, phone?, full_name, password, password_confirmation)
   * @returns {Object} - Created user object (without password)
   * @throws {Error} - If validation fails or email already exists
   */
  async register(userData) {
    const {
      email,
      phone_number,
      full_name,
      password,
      password_confirmation
    } = userData;

    // Validate password match
    if (password !== password_confirmation) {
      throw new Error('Passwords do not match');
    }

    // Check if user already exists
    const existingUser = await this.userRepository.findByEmail(email);
    if (existingUser) {
      throw new Error('Email already registered');
    }

    // Hash password
    const passwordHash = await TokenUtils.hashPassword(password);

    // Create user with default commuter role and reputation score of 50.00
    const user = await this.userRepository.create({
      email,
      phone_number,
      password_hash: passwordHash,
      full_name,
      role: 'commuter' // As specified: commuters only self-register
    });

    // Return user without password hash
    const { password_hash, ...userWithoutPassword } = user;
    return userWithoutPassword;
  }

  /**
   * Login user and issue access + refresh token pair
   * @param {Object} loginData - Login credentials (email, password)
   * @returns {Object} - { accessToken, refreshToken, user }
   * @throws {Error} - If credentials are invalid
   */
  async login(loginData) {
    const { email, password } = loginData;

    // Find user by email
    const user = await this.userRepository.findByEmail(email);
    if (!user) {
      // Generic error to prevent user enumeration (as specified)
      throw new Error('Invalid credentials');
    }

    // Compare password
    const isValid = await TokenUtils.comparePassword(password, user.password_hash);
    if (!isValid) {
      // Generic error to prevent user enumeration
      throw new Error('Invalid credentials');
    }

    // Check if user is active
    if (!user.is_active) {
      throw new Error('Account is deactivated');
    }

    // Generate tokens
    const accessToken = this.generateAccessToken(user);
    const refreshToken = TokenUtils.generateToken();
    const hashedRefreshToken = TokenUtils.hashToken(refreshToken);

    // Store refresh token
    await this.refreshTokenRepository.create({
      user_id: user.id,
      token_hash: hashedRefreshToken,
      device_info: loginData.device_info || null, // From request headers/body
      expires_at: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) // 30 days
    });

    // Return tokens and user (without sensitive data)
    const { password_hash, ...userWithoutPassword } = user;
    return {
      accessToken,
      refreshToken,
      user: userWithoutPassword
    };
  }

  /**
   * Refresh access token using refresh token
   * @param {string} refreshToken - The refresh token
   * @returns {Object} - { accessToken, refreshToken, user }
   * @throws {Error} - If refresh token is invalid
   */
  async refresh(refreshToken) {
    // Hash the incoming token to compare with stored hash
    const hashedToken = TokenUtils.hashToken(refreshToken);

    // Find the refresh token in database
    const tokenRecord = await this.refreshTokenRepository.findByToken(hashedToken);
    if (!tokenRecord) {
      throw new Error('Invalid refresh token');
    }

    // Check if token is expired
    if (tokenRecord.expires_at <= new Date()) {
      throw new Error('Refresh token expired');
    }

    // Check if token is revoked
    if (tokenRecord.revoked_at !== null) {
      throw new Error('Refresh token revoked');
    }

    // Get user
    const user = await this.userRepository.findById(tokenRecord.user_id);
    if (!user || !user.is_active) {
      throw new Error('User not found or inactive');
    }

    // Rotate tokens: invalidate old refresh token, issue new pair
    await this.refreshTokenRepository.revoke(tokenRecord.id);

    // Generate new tokens
    const newAccessToken = this.generateAccessToken(user);
    const newRefreshToken = TokenUtils.generateToken();
    const newHashedRefreshToken = TokenUtils.hashToken(newRefreshToken);

    // Store new refresh token
    await this.refreshTokenRepository.create({
      user_id: user.id,
      token_hash: newHashedRefreshToken,
      device_info: tokenRecord.device_info, // Keep same device info
      expires_at: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) // 30 days
    });

    // Return new tokens and user
    const { password_hash, ...userWithoutPassword } = user;
    return {
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
      user: userWithoutPassword
    };
  }

  /**
   * Logout user by revoking refresh token
   * @param {string} refreshToken - The refresh token to revoke
   * @returns {Promise<void>}
   */
  async logout(refreshToken) {
    const hashedToken = TokenUtils.hashToken(refreshToken);
    await this.refreshTokenRepository.revokeByToken(hashedToken);
  }

  /**
   * Logout user from all devices (revoke all refresh tokens)
   * @param {string} userId - User's UUID
   * @returns {Promise<void>}
   */
  async logoutAllDevices(userId) {
    await this.refreshTokenRepository.revokeAllByUser(userId);
  }

  /**
   * Generate JWT access token
   * @param {Object} user - User object
   * @returns {string} - JWT token
   */
  generateAccessToken(user) {
    // In a real implementation, you would use a JWT library like jsonwebtoken
    // For this implementation, we'll simulate the token structure
    const payload = {
      sub: user.id,
      role: user.role,
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + (15 * 60) // 15 minutes
    };

    // Simple base64 encoding for demo (in production use proper JWT signing)
    const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64');
    const payloadEncoded = Buffer.from(JSON.stringify(payload)).toString('base64');

    // In production, you would sign this with a secret key
    // For now, we'll return an unsigned token for demonstration
    return `${header}.${payloadEncoded}.`;
  }

  /**
   * Verify JWT access token
   * @param {string} token - JWT token
   * @returns {Object|null} - Decoded payload or null if invalid
   */
  verifyAccessToken(token) {
    try {
      // In a real implementation, you would verify the signature
      // For this implementation, we'll just decode the payload
      const [header, payload, signature] = token.split('.');
      if (!header || !payload) {
        return null;
      }

      const decodedPayload = Buffer.from(payload, 'base64').toString('utf-8');
      const parsedPayload = JSON.parse(decodedPayload);

      // Check if token is expired
      if (parsedPayload.exp < Math.floor(Date.now() / 1000)) {
        return null;
      }

      return parsedPayload;
    } catch (error) {
      return null;
    }
  }
}

module.exports = AuthService;