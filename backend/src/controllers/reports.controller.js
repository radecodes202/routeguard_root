// controllers/reports.controller.js
// HTTP handlers for reports

const ReportsService = require('../services/reports.service');

class ReportsController {
  constructor(reportsService) {
    this.reportsService = reportsService;
  }

  /**
   * Create a new hazard report
   * POST /api/v1/reports
   */
  async createReport(req, res) {
    try {
      // User ID should come from auth middleware
      const reporterId = req.user.id;

      const reportData = {
        reporter_id: reporterId,
        category: req.body.category,
        description: req.body.description,
        location: req.body.location // Expected format: 'POINT(longitude latitude)'
      };

      const report = await this.reportsService.createReport(reportData);

      res.status(201).json({
        success: true,
        data: report
      });
    } catch (error) {
      // Handle validation errors (422)
      if (error.message.includes('not found') ||
          error.message.includes('not active') ||
          error.message.includes('Invalid') ||
          error.message.includes('location')) {
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

  /**
   * Get report by ID
   * GET /api/v1/reports/:id
   */
  async getReportById(req, res) {
    try {
      const report = await this.reportsService.getReportById(req.params.id);

      if (!report) {
        return res.status(404).json({
          success: false,
          error: {
            message: 'Report not found'
          }
        });
      }

      res.status(200).json({
        success: true,
        data: report
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
   * Get reports with filtering and pagination
   * GET /api/v1/reports
   */
  async getReports(req, res) {
    try {
      const filters = {
        status: req.query.status,
        category: req.query.category,
        reporter_id: req.query.reporter_id,
        bbox: req.query.bbox, // Format: min_lon,min_lat,max_lon,max_lat
        limit: parseInt(req.query.limit) || 50,
        offset: parseInt(req.query.offset) || 0
      };

      // Remove undefined filters
      Object.keys(filters).forEach(key => {
        if (filters[key] === undefined || filters[key] === '') {
          delete filters[key];
        }
      });

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
      res.status(500).json({
        success: false,
        error: {
          message: 'Internal server error'
        }
      });
    }
  }

  /**
   * Get nearby reports (within radius)
   * GET /api/v1/reports/nearby?lat=&lng=&radius=
   */
  async getNearbyReports(req, res) {
    try {
      const lat = parseFloat(req.query.lat);
      const lng = parseFloat(req.query.lng);
      const radius = parseFloat(req.query.radius) || 5000; // Default 5km
      const filters = {
        status: req.query.status,
        category: req.query.category,
        limit: parseInt(req.query.limit) || 50,
        offset: parseInt(req.query.offset) || 0
      };

      // Validate coordinates
      if (isNaN(lat) || isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
        return res.status(422).json({
          success: false,
          error: {
            message: 'Invalid latitude or longitude'
          }
        });
      }

      // Remove undefined filters
      Object.keys(filters).forEach(key => {
        if (filters[key] === undefined || filters[key] === '') {
          delete filters[key];
        }
      });

      const result = await this.reportsService.getReportsNearby(lat, lng, radius, filters);

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
   * Update report
   * PUT /api/v1/reports/:id
   */
  async updateReport(req, res) {
    try {
      const reportId = req.params.id;
      const userId = req.user.id; // From auth middleware

      const updates = {};
      // Only allow updating certain fields
      if (req.body.category !== undefined) updates.category = req.body.category;
      if (req.body.description !== undefined) updates.description = req.body.description;
      if (req.body.location !== undefined) updates.location = req.body.location;

      const updatedReport = await this.reportsService.updateReport(
        reportId,
        updates,
        userId
      );

      res.status(200).json({
        success: true,
        data: updatedReport
      });
    } catch (error) {
      // Handle authorization and validation errors
      if (error.message === 'Not authorized to update this report' ||
          error.message === 'Report not found' ||
          error.message === 'User not found') {
        res.status(403).json({
          success: false,
          error: {
            message: error.message
          }
        });
      } else if (error.message.includes('Invalid')) {
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

  /**
   * Delete report
   * DELETE /api/v1/reports/:id
   */
  async deleteReport(req, res) {
    try {
      const reportId = req.params.id;
      const userId = req.user.id; // From auth middleware

      const deleted = await this.reportsService.deleteReport(reportId, userId);

      if (deleted) {
        res.status(200).json({
          success: true,
          data: {
            message: 'Report deleted successfully'
          }
        });
      } else {
        res.status(404).json({
          success: false,
          error: {
            message: 'Report not found'
          }
        });
      }
    } catch (error) {
      // Handle authorization errors
      if (error.message === 'Not authorized to delete this report' ||
          error.message === 'Report not found' ||
          error.message === 'User not found') {
        res.status(403).json({
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

  /**
   * Get nearby reports (within radius)
   * GET /api/v1/reports/nearby?lat=&lng=&radius=
   */
  async getNearbyReports(req, res) {
    try {
      const lat = parseFloat(req.query.lat);
      const lng = parseFloat(req.query.lng);
      const radius = parseFloat(req.query.radius) || 5000; // Default 5km
      const filters = {
        status: req.query.status,
        category: req.query.category,
        limit: parseInt(req.query.limit) || 50,
        offset: parseInt(req.query.offset) || 0
      };

      // Validate coordinates
      if (isNaN(lat) || isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
        return res.status(422).json({
          success: false,
          error: {
            message: 'Invalid latitude or longitude'
          }
        });
      }

      // Remove undefined filters
      Object.keys(filters).forEach(key => {
        if (filters[key] === undefined || filters[key] === '') {
          delete filters[key];
        }
      });

      const result = await this.reportsService.getReportsNearby(lat, lng, radius, filters);

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
   * Add interaction to report (confirm/deny/flag)
   * POST /api/v1/reports/:id/interactions
   */
  async addInteraction(req, res) {
    try {
      const reportId = req.params.id;
      const userId = req.user.id; // From auth middleware

      const interactionData = {
        report_id: reportId,
        user_id: userId,
        action: req.body.action // 'confirm', 'deny', or 'flag_suspicious'
      };

      const interaction = await this.reportsService.addReportInteraction(interactionData);

      res.status(201).json({
        success: true,
        data: interaction
      });
    } catch (error) {
      // Handle validation errors
      if (error.message === 'Report not found' ||
          error.message === 'User not found' ||
          error.message === 'User is not active' ||
          error.message.includes('Invalid')) {
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

  /**
   * Get flagged reports (for moderation queue)
   * GET /api/v1/reports/flagged
   */
  async getFlaggedReports(req, res) {
    try {
      // Check if user is moderator or admin
      const user = req.user;
      if (user.role !== 'moderator' && user.role !== 'admin') {
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
   * POST /api/v1/reports/:id/resolve
   */
  async resolveFlaggedReport(req, res) {
    try {
      const reportId = req.params.id;
      const moderatorId = req.user.id; // From auth middleware

      // Check if user is moderator or admin
      const user = req.user;
      if (user.role !== 'moderator' && user.role !== 'admin') {
        return res.status(403).json({
          success: false,
          error: {
            message: 'Insufficient permissions'
          }
        });
      }

      const { resolution, notes } = req.body;

      const updatedReport = await this.reportsService.resolveFlaggedReport(
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

  /**
   * Confirm a report
   * POST /api/v1/reports/:id/confirm
   */
  async confirmReport(req, res) {
    try {
      const reportId = req.params.id;
      const userId = req.user.id; // From auth middleware

      // Get the report to confirm
      const report = await this.reportsService.getReportById(reportId);

      if (!report) {
        return res.status(404).json({
          success: false,
          error: {
            message: 'Report not found'
          }
        });
      }

      // Get the user to check reputation
      const user = await this.userRepository.findById(userId);
      if (!user) {
        return res.status(404).json({
          success: false,
          error: {
            message: 'User not found'
          }
        });
      }

      if (!user.is_active) {
        return res.status(400).json({
          success: false,
          error: {
            message: 'User is not active'
          }
        });
      }

      // Add the confirmation interaction
      const interactionData = {
        report_id: reportId,
        user_id: userId,
        action: 'confirm'
      };

      const interaction = await this.reportsService.addReportInteraction(interactionData);

      // Get updated report
      const updatedReport = await this.reportsService.getReportById(reportId);

      res.status(200).json({
        success: true,
        data: updatedReport
      });
    } catch (error) {
      // Handle validation errors
      if (error.message === 'Report not found' ||
          error.message === 'User not found' ||
          error.message === 'User is not active' ||
          error.message.includes('Invalid')) {
        return res.status(422).json({
          success: false,
          error: {
            message: error.message
          }
        });
      } else {
        return res.status(500).json({
          success: false,
          error: {
            message: 'Internal server error'
          }
        });
      }
    }
  }

  /**
   * Deny a report
   * POST /api/v1/reports/:id/deny
   */
  async denyReport(req, res) {
    try {
      const reportId = req.params.id;
      const userId = req.user.id; // From auth middleware

      // Get the report to deny
      const report = await this.reportsService.getReportById(reportId);

      if (!report) {
        return res.status(404).json({
          success: false,
          error: {
            message: 'Report not found'
          }
        });
      }

      // Get the user to check reputation
      const user = await this.userRepository.findById(userId);
      if (!user) {
        return res.status(404).json({
          success: false,
          error: {
            message: 'User not found'
          }
        });
      }

      if (!user.is_active) {
        return res.status(400).json({
          success: false,
          error: {
            message: 'User is not active'
          }
        });
      }

      // Add the denial interaction
      const interactionData = {
        report_id: reportId,
        user_id: userId,
        action: 'deny'
      };

      const interaction = await this.reportsService.addReportInteraction(interactionData);

      // Get updated report
      const updatedReport = await this.reportsService.getReportById(reportId);

      res.status(200).json({
        success: true,
        data: updatedReport
      });
    } catch (error) {
      // Handle validation errors
      if (error.message === 'Report not found' ||
          error.message === 'User not found' ||
          error.message === 'User is not active' ||
          error.message.includes('Invalid')) {
        return res.status(422).json({
          success: false,
          error: {
            message: error.message
          }
        });
      } else {
        return res.status(500).json({
          success: false,
          error: {
            message: 'Internal server error'
          }
        });
      }
    }
  }
}

module.exports = ReportsController;