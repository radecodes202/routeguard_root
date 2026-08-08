// services/confidenceDecayService.js
// Confidence decay business logic

const ReportsRepository = require('../reports/reports.repository');
const { Pool } = require('pg');

class ConfidenceDecayService {
  constructor(pool) {
    this.pool = pool;
    this.reportsRepository = new ReportsRepository(pool);

    // Base hourly decay rate per category (per Section 4 assumptions)
    // Format: category => decay rate per hour
    this.DECAY_RATES_PER_HOUR = {
      flooded: 4.0,      // 4% per hour
      fully_blocked: 3.0, // 3% per hour
      accident: 6.0,      // 6% per hour (accidents clear fastest)
      debris: 2.0,        // 2% per hour
      partially_passable: 3.0 // 3% per hour
    };

    // Confidence floor for escalation to moderation queue
    this.CONFIDENCE_FLOOR = 20.0; // Below this, escalate if no consensus

    // Confirm bonus (capped at 100)
    this.CONFIRM_BONUS = 15.0;

    // Deny penalty (immediate)
    this.DENY_PENALTY = 25.0;

    // Confirm extends decay pause for 30 minutes
    this.CONFIRM_PAUSE_MINUTES = 30;
  }

  /**
   * Calculate decay for a single report based on time elapsed
   * @param {Object} report - Report object from database
   * @param {number} hoursElapsed - Hours since last update/creation
   * @returns {number} - New confidence score after decay
   */
  calculateDecay(report, hoursElapsed) {
    if (!this.DECAY_RATES_PER_HOUR[report.category]) {
      return report.confidence_score;
    }

    const decayRate = this.DECAY_RATES_PER_HOUR[report.category];
    const decayAmount = decayRate * hoursElapsed;

    // Apply decay
    let newScore = report.confidence_score - decayAmount;

    // Ensure we don't go below 0
    return Math.max(0, newScore);
  }

  /**
   * Apply confirm bonus to a report
   * @param {Object} report - Report object
   * @param {number} userReputation - Reputation of user confirming (0-100)
   * @returns {Object} - Updated report with new confidence score and confirm timestamp
   */
  applyConfirm(report, userReputation) {
    // Weight the confirm bonus by user reputation (0-1)
    const weight = userReputation / 100;
    const bonus = this.CONFIRM_BONUS * weight;

    // Apply bonus, capped at 100
    let newScore = Math.min(100, report.confidence_score + bonus);

    // In a real implementation, we would also store a timestamp for the pause
    // For now, we'll just return the updated score
    return {
      ...report,
      confidence_score: newScore
    };
  }

  /**
   * Apply deny penalty to a report
   * @param {Object} report - Report object
   * @param {number} userReputation - Reputation of user denying (0-100)
   * @returns {Object} - Updated report with new confidence score
   */
  applyDeny(report, userReputation) {
    // Weight the deny penalty by user reputation (0-1)
    const weight = userReputation / 100;
    const penalty = this.DENY_PENALTY * weight;

    // Apply penalty, floored at 0
    let newScore = Math.max(0, report.confidence_score - penalty);

    return {
      ...report,
      confidence_score: newScore
    };
  }

  /**
   * Check if report should be escalated to moderation queue based on confidence floor and consensus
   * @param {Object} report - Report object
   * @returns {boolean} - True if should be escalated
   */
  shouldEscalateToModeration(report) {
    // Only escalate reports that are pending, flagged, or confirmed (actively managed)
    if (!['pending', 'flagged', 'confirmed'].includes(report.status)) {
      return false;
    }

    // Check if below confidence floor
    if (report.confidence_score >= this.CONFIDENCE_FLOOR) {
      return false;
    }

    // Check for clear consensus: confirm_count > deny_count
    // If confirm_count <= deny_count, there's no clear consensus (including equal counts)
    return report.confirm_count <= report.deny_count;
  }

  /**
   * Determine the flagged reason for escalation
   * @param {Object} report - Report object
   * @returns {string} - Flagged reason
   */
  getFlaggedReasonForEscalation(report) {
    // If we're escalating due to confidence floor without consensus
    if (this.shouldEscalateToModeration(report)) {
      return 'confidence_floor';
    }
    return null;
  }

  /**
   * Process confidence decay for all active reports with broadcasting
   * This would be called by the scheduled job every 5 minutes
   * @param {HazardChannel} hazardChannel - Hazard channel for broadcasting updates
   * @returns {Promise<Object>} - Object containing escalated and updated reports
   */
  async processDecayJobWithBroadcast(hazardChannel) {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');

      // Get all reports that are pending, flagged, or confirmed
      const reportsResult = await client.query(
        `SELECT * FROM hazard_reports
         WHERE status IN ('pending', 'flagged', 'confirmed')`
      );

      const escalatedReports = [];
      const updatedReports = [];

      // Process each report
      for (const reportRow of reportsResult.rows) {
        const report = reportRow;

        // Calculate hours since last update
        const hoursElapsed = (new Date() - new Date(report.updated_at)) / (1000 * 60 * 60);

        // Apply decay
        let newScore = this.calculateDecay(report, hoursElapsed);

        // Check if we need to escalate
        const tempReport = { ...report, confidence_score: newScore };
        if (this.shouldEscalateToModeration(tempReport)) {
          // Determine flagged reason
          const flaggedReason = this.getFlaggedReasonForEscalation(tempReport);

          // Update report in database
          const updateResult = await client.query(
            `UPDATE hazard_reports
             SET confidence_score = $1, status = $2, flagged_reason = $3, updated_at = NOW()
             WHERE id = $4
             RETURNING *`,
            [newScore, 'flagged', flaggedReason, report.id]
          );

          const updatedReport = updateResult.rows[0];
          escalatedReports.push(updatedReport);

          // Broadcast the update
          await hazardChannel.broadcastHazardUpdated(updatedReport);
        } else if (newScore !== report.confidence_score) {
          // Just update the confidence score if not escalating
          await client.query(
            `UPDATE hazard_reports
             SET confidence_score = $1, updated_at = NOW()
             WHERE id = $2`,
            [newScore, report.id]
          );

          // Fetch the updated report to broadcast
          const updatedReport = await this.reportsRepository.findById(report.id);
          if (updatedReport) {
            updatedReports.push(updatedReport);

            // Broadcast the update
            await hazardChannel.broadcastHazardUpdated(updatedReport);
          }
        }
      }

      await client.query('COMMIT');
      return { escalatedReports, updatedReports };
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  /**
   * Process confidence decay for all active reports
   * This would be called by the scheduled job every 5 minutes
   * @returns {Promise<Array>} - Array of reports that were escalated to moderation
   */
  async processDecayJob() {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');

      // Get all reports that are pending, flagged, or confirmed
      const reportsResult = await client.query(
        `SELECT * FROM hazard_reports
         WHERE status IN ('pending', 'flagged', 'confirmed')`
      );

      const escalatedReports = [];

      // Process each report
      for (const reportRow of reportsResult.rows) {
        const report = reportRow;

        // Calculate hours since last update
        const hoursElapsed = (new Date() - new Date(report.updated_at)) / (1000 * 60 * 60);

        // Apply decay
        let newScore = this.calculateDecay(report, hoursElapsed);

        // Check if we need to escalate
        const tempReport = { ...report, confidence_score: newScore };
        if (this.shouldEscalateToModeration(tempReport)) {
          // Determine flagged reason
          const flaggedReason = this.getFlaggedReasonForEscalation(tempReport);

          // Update report in database
          const updateResult = await client.query(
            `UPDATE hazard_reports
             SET confidence_score = $1, status = $2, flagged_reason = $3, updated_at = NOW()
             WHERE id = $4
             RETURNING *`,
            [newScore, 'flagged', flaggedReason, report.id]
          );

          escalatedReports.push(updateResult.rows[0]);
        } else if (newScore !== report.confidence_score) {
          // Just update the confidence score if not escalating
          await client.query(
            `UPDATE hazard_reports
             SET confidence_score = $1, updated_at = NOW()
             WHERE id = $2`,
            [newScore, report.id]
          );
        }
      }

      await client.query('COMMIT');
      return escalatedReports;
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }
}

module.exports = ConfidenceDecayService;