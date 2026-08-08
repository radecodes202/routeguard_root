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
  
  // Re-define routes when controller is set
  router.stack = []; // Clear existing routes
  
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
  const { authenticate, requireRole } = require('../middleware/auth.middleware');

  // These routes require authentication
  router.use(authenticate); // Apply auth middleware to all routes below

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

  
  // Initialize with placeholder handlers (will be replaced when setController is called)
  router.get('/', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
  router.get('/nearby', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
  router.get('/:id', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
  router.post('/', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
  router.put('/:id', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
  router.delete('/:id', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
  router.post('/:id/interactions', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
  router.post('/:id/confirm', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
  router.post('/:id/deny', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
  router.get('/flagged', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
  router.post('/:id/resolve', (req, res) => {
    res.status(500).json({ success: false, error: { message: 'Reports controller not initialized' } });
  });
}

module.exports = { router, setController };
