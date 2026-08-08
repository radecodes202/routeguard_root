// middleware/auth.middleware.js
// JWT verification middleware

const AuthService = require('../services/auth.service');

/**
 * Middleware to verify JWT access token
 * @param {AuthService} authService - Auth service instance
 * @returns {Function} - Express middleware function
 */
function authenticate(authService) {
  return async function(req, res, next) {
    try {
      // Get token from Authorization header
      const authHeader = req.headers.authorization;
      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({
          success: false,
          error: {
            message: 'Access token required'
          }
        });
      }

      const token = authHeader.substring(7); // Remove 'Bearer ' prefix

      // Verify token
      const decoded = authService.verifyAccessToken(token);
      if (!decoded) {
        return res.status(401).json({
          success: false,
          error: {
            message: 'Invalid or expired access token'
          }
        });
      }

      // Get user from database
      const user = await authService.userRepository.findById(decoded.sub);
      if (!user || !user.is_active) {
        return res.status(401).json({
          success: false,
          error: {
            message: 'User not found or inactive'
          }
        });
      }

      // Attach user to request object
      req.user = {
        id: user.id,
        email: user.email,
        role: user.role,
        is_active: user.is_active
      };

      next();
    } catch (error) {
      res.status(500).json({
        success: false,
        error: {
          message: 'Internal server error'
        }
      });
    }
  };
}

/**
 * Middleware to check user roles
 * @param {...string} allowedRoles - Roles allowed to access the route
 * @returns {Function} - Express middleware function
 */
function requireRole(...allowedRoles) {
  return function(req, res, next) {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        error: {
          message: 'Not authenticated'
        }
      });
    }

    if (!allowedRoles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        error: {
          message: 'Insufficient permissions'
        }
      });
    }

    next();
  };
}

/**
 * Middleware for owner-or-admin checks
 * @param {Function} getResourceOwnerId - Function that extracts owner ID from request
 * @returns {Function} - Express middleware function
 */
function requireOwnerOrAdmin(getResourceOwnerId) {
  return function(req, res, next) {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        error: {
          message: 'Not authenticated'
        }
      });
    }

    // Admin can access everything
    if (req.user.role === 'admin') {
      return next();
    }

    // Check if user owns the resource
    const ownerId = getResourceOwnerId(req);
    if (ownerId && ownerId.toString() === req.user.id.toString()) {
      return next();
    }

    // Neither admin nor owner
    return res.status(403).json({
      success: false,
      error: {
        message: 'Insufficient permissions'
      }
    });
  };
}

module.exports = {
  authenticate,
  requireRole,
  requireOwnerOrAdmin
};