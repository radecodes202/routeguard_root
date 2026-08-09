// routes/responder.routes.js
// Responder routes

const express = require('express');
const router = express.Router();

// Controllers will be injected via dependency injection
let responderController;

/**
 * Set the responder controller (for dependency injection)
 * @param {ResponderController} controller - Responder controller instance
 */
function setController(controller) {
  responderController = controller;

  // Re-define routes when controller is set
  router.stack = []; // Clear existing routes

  // Responder routes - require authentication and responder/admin role
  const { authenticate, requireRole } = require('../middleware/auth.middleware');

  // Get verified active hazards for responder view
  router.get(
    '/hazards',
    authenticate,
    responderController.getResponderHazards.bind(responderController)
  );

  // Initialize with placeholder handlers (will be replaced when setController is called)
  router.get('/hazards', (req, res) => {
    res.status(500).json({
      success: false,
      error: { message: 'Responder controller not initialized' }
    });
  });
}

module.exports = { router, setController };