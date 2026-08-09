// controllers/responder.controller.js
// HTTP handlers for responder functionality

const ReportsService = require('../services/reports.service');

class ResponderController {
  constructor(reportsService) {
    this.reportsService = reportsService;
  }

  /**
   * Get verified active hazards for responder view
   * GET /api/v1/responder/hazards
   */
  async getResponderHazards(req, res) {
    try {
      // Check if user is responder or admin
      const user = req.user;
      if (user.role !== 'responder' && user.role !== 'admin') {
        return res.status(403).json({
          success: false,
          error: {
            message: 'Insufficient permissions - responder or admin role required'
          }
        });
      }

      // Get all confirmed reports (no radius limit for responders)
      const filters = {
        status: 'confirmed',
        limit: 1000, // Reasonable limit for all confirmed hazards
        offset: 0
      };

      const result = await this.reportsService.getReports(filters);

      res.status(200).json({
        success: true,
        data: result.reports,
        pagination: {
          total: result.total,
          limit: result.limit,
          offset: result.offset
        }
      });
    } catch (error) {
      console.error('Error in getResponderHazards:', error);
      res.status(500).json({
        success: false,
        error: {
          message: 'Internal server error'
        }
      });
    }
  }
}

module.exports = ResponderController;