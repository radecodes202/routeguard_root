// routes/reports.routes.js
// Reports routes

const express = require('express');
const router = express.Router();

// Controllers will be injected via dependency injection
let reportsController;

/**
 * Set the reports controller (for dependency injection)
 * @param {ReportsController} controller - Reports controller instance
 */
function setController(controller) {
  reportsController = controller;
}

// Public routes (no authentication required for getting reports)
// Note: In a real implementation, some of these might require authentication
// depending on the sensitivity of the data

// Get reports with filtering and pagination
router.get('/', reportsController.getReports.bind(reportsController));

// Get nearby reports (within radius)
router.get('/nearby', reportsController.getNearbyReports.bind(reportsController));

// Get report by ID
router.get('/:id', reportsController.getReportById.bind(reportsController));

// Protected routes (require authentication)
// Note: We'll apply the auth middleware in server.js or here

const authMiddleware = require('../middleware/auth.middleware');

// These routes require authentication
router.use(authMiddleware); // Apply auth middleware to all routes below

// Create a new report
router.post('/', reportsController.createReport.bind(reportsController));

// Update report
router.put('/:id', reportsController.updateReport.bind(reportsController));

// Delete report
router.delete('/:id', reportsController.deleteReport.bind(reportsController));

// Add interaction to report
router.post('/:id/interactions', reportsController.addInteraction.bind(reportsController));

// Confirm a report (authenticated users)
router.post(
  '/:id/confirm',
  reportsController.confirmReport.bind(reportsController)
);

// Deny a report (authenticated users)
router.post(
  '/:id/deny',
  reportsController.denyReport.bind(reportsController)
);

// Moderator-only routes
const { requireRole } = require('../middleware/auth.middleware');

// Get flagged reports (moderator/admin only)
router.get(
  '/flagged',
  requireRole('moderator', 'admin'),
  reportsController.getFlaggedReports.bind(reportsController)
);

// Resolve flagged report (moderator/admin only)
router.post(
  '/:id/resolve',
  requireRole('moderator', 'admin'),
  reportsController.resolveFlaggedReport.bind(reportsController)
);

module.exports = { router, setController };