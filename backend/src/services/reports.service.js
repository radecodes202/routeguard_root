// services/reports.service.js
// Reports business logic

const ReportsRepository = require('../reports/reports.repository');
const UserRepository = require('../reports/user.repository');
const ReputationService = require('./reputationService');

class ReportsService {
  constructor(reportsRepository, userRepository) {
    this.reportsRepository = reportsRepository;
    this.userRepository = userRepository;
    this.reputationService = new ReputationService(userRepository, reportsRepository);
    this.confidenceDecayService = new ConfidenceDecayService(reportsRepository.pool);
  }

  /**
   * Create a new hazard report
   * @param {Object} reportData - Report data (reporter_id, category, description, location)
   * @returns {Object} - Created report object (without sensitive data)
   * @throws {Error} - If validation fails or user not found
   */
  async createReport(reportData) {
    const { reporter_id, category, description, location } = reportData;

    // Validate reporter exists and is active
    const reporter = await this.userRepository.findById(reporter_id);
    if (!reporter) {
      throw new Error('Reporter not found');
    }
    if (!reporter.is_active) {
      throw new Error('Reporter is not active');
    }

    // Validate category
    const validCategories = ['flooded', 'fully_blocked', 'debris', 'accident', 'partially_passable'];
    if (!validCategories.includes(category)) {
      throw new Error('Invalid report category');
    }

    // Validate location format (basic check)
    if (!location || !location.startsWith('POINT(')) {
      throw new Error('Invalid location format');
    }

    // Create the report
    const report = await this.reportsRepository.create({
      reporter_id,
      category,
      description,
      location
    });

    // Update reporter's submitted reports count
    await this.userRepository.incrementReportCount(reporter_id, 'submitted');

    // Return report without internal fields if needed
    const {
      location: location_wkt,
      ...reportWithoutLocationWkt
    } = report;

    return reportWithoutLocationWkt;
  }

  /**
   * Get report by ID
   * @param {string} id - Report's UUID
   * @returns {Object|null} - Report object or null if not found
   */
  async getReportById(id) {
    return await this.reportsRepository.findById(id);
  }

  /**
   * Get reports with filtering and pagination
   * @param {Object} filters - Filter options (status, category, reporter_id, bbox, limit, offset)
   * @returns {Object} - Reports data with count
   */
  async getReports(filters = {}) {
    return await this.reportsRepository.findAll(filters);
  }

  /**
   * Update report
   * @param {string} id - Report's UUID
   * @param {Object} updates - Fields to update
   * @param {string} userId - User making the update (for authorization)
   * @returns {Object} - Updated report object
   * @throws {Error} - If report not found or user not authorized
   */
  async updateReport(id, updates, userId) {
    // Check if report exists
    const report = await this.reportsRepository.findById(id);
    if (!report) {
      throw new Error('Report not found');
    }

    // Check if user is authorized (reporter or admin/moderator)
    const user = await this.userRepository.findById(userId);
    if (!user) {
      throw new Error('User not found');
    }

    const isAuthorized =
      report.reporter_id === userId ||
      user.role === 'admin' ||
      user.role === 'moderator' ||
      user.role === 'mio_staff';

    if (!isAuthorized) {
      throw new Error('Not authorized to update this report');
    }

    // Validate category if being updated
    if (updates.category) {
      const validCategories = ['flooded', 'fully_blocked', 'debris', 'accident', 'partially_passable'];
      if (!validCategories.includes(updates.category)) {
        throw new Error('Invalid report category');
      }
    }

    // Update the report
    const updatedReport = await this.reportsRepository.update(id, updates);

    // Return without location_wkt if present
    const {
      location_wkt,
      ...reportWithoutLocationWkt
    } = updatedReport;

    return reportWithoutLocationWkt;
  }

  /**
   * Delete report
   * @param {string} id - Report's UUID
   * @param {string} userId - User making the deletion (for authorization)
   * @returns {boolean} - True if deleted
   * @throws {Error} - If report not found or user not authorized
   */
  async deleteReport(id, userId) {
    // Check if report exists
    const report = await this.reportsRepository.findById(id);
    if (!report) {
      throw new Error('Report not found');
    }

    // Check if user is authorized (reporter or admin/moderator)
    const user = await this.userRepository.findById(userId);
    if (!user) {
      throw new Error('User not found');
    }

    const isAuthorized =
      report.reporter_id === userId ||
      user.role === 'admin' ||
      user.role === 'moderator';

    if (!isAuthorized) {
      throw new Error('Not authorized to delete this report');
    }

    return await this.reportsRepository.delete(id);
  }

  /**
   * Add interaction to report (confirm/deny/flag)
   * @param {Object} interactionData - Interaction data (report_id, user_id, action)
   * @returns {Object} - Created/updated interaction
   * @throws {Error} - If validation fails
   */
  async addReportInteraction(interactionData) {
    const { report_id, user_id, action } = interactionData;

    // Validate report exists
    const report = await this.reportsRepository.findById(report_id);
    if (!report) {
      throw new Error('Report not found');
    }

    // Validate user exists and is active
    const user = await this.userRepository.findById(user_id);
    if (!user) {
      throw new Error('User not found');
    }
    if (!user.is_active) {
      throw new Error('User is not active');
    }

    // Validate action
    const validActions = ['confirm', 'deny', 'flag_suspicious'];
    if (!validActions.includes(action)) {
      throw new Error('Invalid action');
    }

    // For confirm/deny actions, check if user already interacted
    if (action === 'confirm' || action === 'deny') {
      const existingInteraction = await this.reportsRepository.pool.query(
        'SELECT action FROM report_interactions WHERE report_id = $1 AND user_id = $2',
        [report_id, user_id]
      );

      if (existingInteraction.rowCount > 0) {
        const existingAction = existingInteraction.rows[0].action;
        // If user is changing their vote, update counters accordingly
        if (existingAction !== action) {
          // Decrement old action count
          if (existingAction === 'confirm') {
            await this.reportsRepository.update(report_id, {
              confirm_count: report.confirm_count - 1
            });
          } else if (existingAction === 'deny') {
            await this.reportsRepository.update(report_id, {
              deny_count: report.deny_count - 1
            });
          }

          // Increment new action count
          if (action === 'confirm') {
            await this.reportsRepository.update(report_id, {
              confirm_count: report.confirm_count + 1
            });
          } else if (action === 'deny') {
            await this.reportsRepository.update(report_id, {
              deny_count: report.deny_count + 1
            });
          }
        }
        // Update the interaction
        return await this.reportsRepository.addInteraction({
          report_id,
          user_id,
          action
        });
      }
    }

    // Add new interaction
    const interaction = await this.reportsRepository.addInteraction(interactionData);

    // Update report counters based on action
    if (action === 'confirm') {
      await this.reportsRepository.update(report_id, {
        confirm_count: report.confirm_count + 1
      });

      // Update reporter's confirmed count
      await this.userRepository.incrementReportCount(report.reporter_id, 'confirmed');

      // Apply confirm bonus to confidence score
      const updatedReport = this.confidenceDecayService.applyConfirm(report, user.reputation_score);
      await this.reportsRepository.update(report_id, {
        confidence_score: updatedReport.confidence_score
      });
    } else if (action === 'deny') {
      await this.reportsRepository.update(report_id, {
        deny_count: report.deny_count + 1
      });

      // Update reporter's false count
      await this.userRepository.incrementReportCount(report.reporter_id, 'false');

      // Apply deny penalty to confidence score
      const updatedReport = this.confidenceDecayService.applyDeny(report, user.reputation_score);
      await this.reportsRepository.update(report_id, {
        confidence_score: updatedReport.confidence_score
      });
    }

    // Check if report should be auto-confirmed or auto-false based on thresholds
    await this.checkReportStatus(report_id);

    return interaction;
  }

  /**
   * Check if report status should be updated based on confirmation/denial ratios
   * @param {string} report_id - Report's UUID
   * @returns {Promise<void>}
   */
  async checkReportStatus(report_id) {
    const report = await this.reportsRepository.findById(report_id);
    if (!report) return;

    const totalInteractions = report.confirm_count + report.deny_count;

    // Only check if we have enough interactions
    if (totalInteractions >= 3) {
      const confirmationRatio = report.confirm_count / totalInteractions;
      const denialRatio = report.deny_count / totalInteractions;

      // Auto-confirm if >= 60% confirmations and at least 3 confirms
      if (confirmationRatio >= 0.6 && report.confirm_count >= 3) {
        await this.reportsRepository.update(report_id, {
          status: 'confirmed',
          resolved_at: new Date()
        });
      }
      // Auto-false if >= 60% denials and at least 3 denials
      else if (denialRatio >= 0.6 && report.deny_count >= 3) {
        await this.reportsRepository.update(report_id, {
          status: 'false',
          resolved_at: new Date()
        });
      }
    }
  }

  /**
   * Get reports that are flagged for moderation
   * @param {Object} filters - Filter options (limit, offset)
   * @returns {Object} - Flagged reports data
   */
  async getFlaggedReports(filters = {}) {
    return await this.reportsRepository.getFlaggedReports(filters);
  }

  /**
   * Get reports within a radius of a point (for nearby hazards)
   * @param {number} lat - Latitude of center point
   * @param {number} lng - Longitude of center point
   * @param {number} radiusMeters - Radius in meters (default 5000 for 5km)
   * @param {Object} filters - Additional filters (status, category, limit, offset)
   * @returns {Object} - Reports data with count
   */
  async getReportsNearby(lat, lng, radiusMeters = 5000, filters = {}) {
    return await this.reportsRepository.findNearby(lat, lng, radiusMeters, filters);
  }

  /**
   * Resolve a flagged report (moderator action)
   * @param {string} reportId - Report's UUID
   * @param {string} moderatorId - Moderator's UUID
   * @param {string} resolution - Resolution ('confirmed', 'false', 'inconclusive')
   * @param {string} notes - Moderator notes
   * @returns {Object} - Updated report with reputation info
   * @throws {Error} - If validation fails
   */
  async resolveFlaggedReport(reportId, moderatorId, resolution, notes = '') {
    // Use reputation service to handle the resolution atomically
    return await this.reputationService.applyModerationResolution(
      reportId,
      moderatorId,
      resolution,
      notes
    );
  }
}

module.exports = ReportsService;