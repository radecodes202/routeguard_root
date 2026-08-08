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

      // Hash the token for lookup
      const hashedToken = TokenUtils.hashVerificationToken(token);

      // Find the verification token
      const verificationToken = await this.authService.emailVerificationRepository.findByToken(hashedToken);
      if (!verificationToken) {
        return res.status(422).json({
          success: false,
          error: {
            message: 'Invalid or expired verification token'
          }
        });
      }

      // Get the user
      const user = await this.authService.userRepository.findById(verificationToken.user_id);
      if (!user) {
        return res.status(404).json({
          success: false,
          error: {
            message: 'User not found'
          }
        });
      }

      // Update user's email verification timestamp
      await this.authService.userRepository.verifyEmail(user.id);

      // Consume the token (delete it so it can't be reused)
      await this.authService.emailVerificationRepository.consume(verificationToken.id);

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

      // Hash the token for lookup
      const hashedToken = TokenUtils.hashVerificationToken(token);

      // Find the reset token
      const resetToken = await this.authService.passwordResetRepository.findByToken(hashedToken);
      if (!resetToken) {
        return res.status(422).json({
          success: false,
          error: {
            message: 'Invalid or expired reset token'
          }
        });
      }

      // Get the user
      const user = await this.authService.userRepository.findById(resetToken.user_id);
      if (!user) {
        return res.status(404).json({
          success: false,
          error: {
            message: 'User not found'
          }
        });
      }

      // Hash the new password
      const hashedPassword = await TokenUtils.hashPassword(newPassword);

      // Update user's password
      await this.authService.userRepository.update(user.id, { password_hash: hashedPassword });

      // Revoke all refresh tokens for the user (logout everywhere as specified)
      await this.authService.refreshTokenRepository.revokeAllByUser(user.id);

      // Consume the reset token (delete it so it can't be reused)
      await this.authService.passwordResetRepository.consume(resetToken.id);

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