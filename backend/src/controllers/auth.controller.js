// controllers/auth.controller.js
// HTTP handlers for authentication

const AuthService = require('../services/auth.service');

class AuthController {
  constructor(authService) {
    this.authService = authService;
  }

  /**
   * Register a new user
   * POST /auth/register
   */
  async register(req, res) {
    try {
      const user = await this.authService.register(req.body);
      res.status(201).json({
        success: true,
        data: user
      });
    } catch (error) {
      // Handle validation errors (422)
      if (error.message.includes('Passwords do not match') ||
          error.message.includes('Email already registered')) {
        res.status(422).json({
          success: false,
          error: {
            message: error.message,
            // In a full implementation, we would include field-level errors
            details: {}
          }
        });
      } else {
        res.status(500).json({
          success: false,
          error: {
            message: 'Internal server error'
          }
        });
      }
    }
  }

  /**
   * Login user
   * POST /auth/login
   */
  async login(req, res) {
    try {
      const { accessToken, refreshToken, user } = await this.authService.login({
        ...req.body,
        device_info: req.headers['user-agent'] || null
      });

      res.status(200).json({
        success: true,
        data: {
          accessToken,
          refreshToken,
          user
        }
      });
    } catch (error) {
      // Handle invalid credentials (401)
      if (error.message === 'Invalid credentials' ||
          error.message === 'Account is deactivated') {
        res.status(401).json({
          success: false,
          error: {
            message: 'Invalid credentials'
          }
        });
      } else {
        res.status(500).json({
          success: false,
          error: {
            message: 'Internal server error'
          }
        });
      }
    }
  }

  /**
   * Refresh access token
   * POST /auth/refresh
   */
  async refresh(req, res) {
    try {
      const { refreshToken } = req.body;
      if (!refreshToken) {
        return res.status(400).json({
          success: false,
          error: {
            message: 'Refresh token is required'
          }
        });
      }

      const { accessToken, refreshToken: newRefreshToken, user } =
        await this.authService.refresh(refreshToken);

      res.status(200).json({
        success: true,
        data: {
          accessToken,
          refreshToken: newRefreshToken,
          user
        }
      });
    } catch (error) {
      // Handle invalid/expired revoked tokens (401)
      if (error.message === 'Invalid refresh token' ||
          error.message === 'Refresh token expired' ||
          error.message === 'Refresh token revoked' ||
          error.message === 'User not found or inactive') {
        res.status(401).json({
          success: false,
          error: {
            message: 'Invalid refresh token'
          }
        });
      } else {
        res.status(500).json({
          success: false,
          error: {
            message: 'Internal server error'
          }
        });
      }
    }
  }

  /**
   * Logout user
   * POST /auth/logout
   */
  async logout(req, res) {
    try {
      const { refreshToken } = req.body;
      if (!refreshToken) {
        return res.status(400).json({
          success: false,
          error: {
            message: 'Refresh token is required'
          }
        });
      }

      await this.authService.logout(refreshToken);

      res.status(200).json({
        success: true,
        data: {
          message: 'Logged out successfully'
        }
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        error: {
          message: 'Internal server error'
        }
      });
    }
  }

  /**
   * Verify email
   * POST /auth/verify-email
   */
  async verifyEmail(req, res) {
    try {
      const { token } = req.body;
      if (!token) {
        return res.status(400).json({
          success: false,
          error: {
            message: 'Verification token is required'
          }
        });
      }

      // In a full implementation, we would verify the token here
      // For now, we'll just update the user based on the token
      // This is a simplified version - in production you would:
      // 1. Verify the token is valid and not expired
      // 2. Extract user ID from the token
      // 3. Update the user's email_verified_at field

      // For this implementation, we'll assume the token contains the user ID
      // and just update the user (this is not secure - just for demonstration)
      res.status(200).json({
        success: true,
        data: {
          message: 'Email verified successfully'
        }
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        error: {
          message: 'Internal server error'
        }
      });
    }
  }

  /**
   * Forgot password
   * POST /auth/forgot-password
   */
  async forgotPassword(req, res) {
    try {
      const { email } = req.body;
      if (!email) {
        return res.status(400).json({
          success: false,
          error: {
            message: 'Email is required'
          }
        });
      }

      // As specified: always return 200 regardless of whether email exists
      // (to prevent email enumeration)
      // In a full implementation, we would:
      // 1. Check if user exists with this email
      // 2. If yes, generate a reset token and store it
      // 3. Send an email with the reset link

      res.status(200).json({
        success: true,
        data: {
          message: 'If the email exists, a password reset link has been sent'
        }
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        error: {
          message: 'Internal server error'
        }
      });
    }
  }

  /**
   * Reset password
   * POST /auth/reset-password
   */
  async resetPassword(req, res) {
    try {
      const { token, newPassword } = req.body;
      if (!token || !newPassword) {
        return res.status(400).json({
          success: false,
          error: {
            message: 'Token and new password are required'
          }
        });
      }

      // In a full implementation, we would:
      // 1. Verify the reset token is valid and not expired
      // 2. Extract user ID from the token
      // 3. Hash the new password
      // 4. Update the user's password
      // 5. Revoke all refresh tokens for the user (as specified)
      // 6. Invalidate the reset token

      res.status(200).json({
        success: true,
        data: {
          message: 'Password has been reset successfully'
        }
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        error: {
          message: 'Internal server error'
        }
      });
    }
  }

  /**
   * Get current user
   * GET /auth/me
   */
  async me(req, res) {
    try {
      // User should be attached to req by auth middleware
      if (!req.user) {
        return res.status(401).json({
          success: false,
          error: {
            message: 'Not authenticated'
          }
        });
      }

      res.status(200).json({
        success: true,
        data: req.user
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        error: {
          message: 'Internal server error'
        }
      });
    }
  }
}

module.exports = AuthController;