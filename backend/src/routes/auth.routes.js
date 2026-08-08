// routes/auth.routes.js
// Authentication routes

const express = require('express');
const router = express.Router();

// Controllers will be injected via dependency injection
let authController;

/**
 * Set the auth controller (for dependency injection)
 * @param {AuthController} controller - Auth controller instance
 */
function setController(controller) {
  authController = controller;
}

// Public routes
router.post('/register', authController.register.bind(authController));
router.post('/login', authController.login.bind(authController));
router.post('/refresh', authController.refresh.bind(authController));
router.post('/verify-email', authController.verifyEmail.bind(authController));
router.post('/forgot-password', authController.forgotPassword.bind(authController));
router.post('/reset-password', authController.resetPassword.bind(authController));

// Protected routes (require authentication)
const authMiddleware = require('../middleware/auth.middleware');
// Note: In a real implementation, we would pass the authService to the middleware
// For now, we'll assume it's set up elsewhere

// These would be protected by the authenticate middleware in a real implementation
// For this example, we're showing the structure
router.get('/me', authController.me.bind(authController));
router.post('/logout', authController.logout.bind(authController));

module.exports = { router, setController };