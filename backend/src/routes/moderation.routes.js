// routes/moderation.routes.js
// Moderation routes

const express = require('express');
const router = express.Router();

// Controllers will be injected via dependency injection
let moderationController;

/**
 * Set the moderation controller (for dependency injection)
 * @param {ModerationController} controller - Moderation controller instance
 */
function setController(controller) {
  moderationController = controller;

  // Re-define routes when controller is set
  router.stack = []; // Clear existing routes

  // Moderator-only routes
  const { authenticate, requireRole } = require('../middleware/auth.middleware');

  // These routes require authentication and moderator/admin role
  router.use(authenticate); // Apply auth middleware to all routes below

  // Get flagged reports (moderation queue)
  router.get(
    '/queue',
    requireRole('moderator', 'admin'),
    moderationController.getModerationQueue.bind(moderationController)
  );

  // Resolve flagged report (moderator action)
  router.post(
    '/queue/:reportId/resolve',
    requireRole('moderator', 'admin'),
    moderationController.resolveReport.bind(moderationController)
  );

  // Initialize with placeholder handlers (will be replaced when setController is called)
  router.get('/queue', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Moderation controller not initialized' } });
  });

  router.post('/queue/:reportId/resolve', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Moderation controller not initialized' } });
  });
}

module.exports = { router, setController };