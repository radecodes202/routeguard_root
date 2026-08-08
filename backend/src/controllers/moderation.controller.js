// controllers/moderation.controller.js
// HTTP handlers for moderation

const ReportsService = require('../services/reports.service');
const ReputationService = require('../services/reputationService');

class ModerationController {
  constructor(reportsService, reputationService) {
    this.reportsService = reportsService;
    this.reputationService = reputationService;
  }

  /**
   * Get flagged reports (moderation queue)
   * GET /api/v1/moderation/queue
   */
  async getModerationQueue(req, res) {
    try {
      // Check if user is moderator, admin, or MIO staff
      const user = req.user;
      if (user.role !== 'moderator' && user.role !== 'admin' && user.role !== 'mio_staff') {
        return res.status(403).json({
          success: false,
          error: {
            message: 'Insufficient permissions'
          }
        });
      }

      const filters = {
        limit: parseInt(req.query.limit) || 50,
        offset: parseInt(req.query.offset) || 0
      };

      const result = await this.reportsService.getFlaggedReports(filters);

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
      res.status(500).json({
        success: false,
        error: {
          message: 'Internal server error'
        }
      });
    }
  }

  /**
   * Resolve a flagged report (moderator action)
   * POST /api/v1/moderation/queue/:reportId/resolve
   */
  async resolveReport(req, res) {
    try {
      const reportId = req.params.reportId;
      const moderatorId = req.user.id; // From auth middleware

      // Check if user is moderator, admin, or MIO staff
      const user = req.user;
      if (user.role !== 'moderator' && user.role !== 'admin' && user.role !== 'mio_staff') {
        return res.status(403).json({
          success: false,
          error: {
            message: 'Insufficient permissions'
          }
        });
      }

      const { resolution, notes } = req.body;

      // Delegate to reputation service which handles the full resolution workflow
      const updatedReport = await this.reputationService.applyModerationResolution(
        reportId,
        moderatorId,
        resolution,
        notes
      );

      res.status(200).json({
        success: true,
        data: updatedReport
      });
    } catch (error) {
      // Handle validation and authorization errors
      if (error.message === 'Report not found' ||
          error.message === 'Report is not flagged for moderation' ||
          error.message === 'Moderator not found' ||
          error.message === 'Not authorized to moderate reports' ||
          error.message === 'Invalid resolution') {
        res.status(422).json({
          success: false,
          error: {
            message: error.message
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
}

module.exports = ModerationController;