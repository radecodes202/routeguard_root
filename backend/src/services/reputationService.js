// services/reputationService.js
// Reputation scoring business logic

const UserRepository = require('../repositories/user.repository');
const ReportsRepository = require('../reports/reports.repository');

class ReputationService {
  constructor(userRepository, reportsRepository) {
    this.userRepository = userRepository;
    this.reportsRepository = reportsRepository;
  }

  /**
   * Compute reputation delta based on moderation resolution
   * @param {number} previousScore - User's current reputation score
   * @param {string} resolution - Resolution ('confirmed', 'false', 'inconclusive')
   * @returns {number} - Reputation delta to apply
   */
  static computeReputationDeltaFromResolution(previousScore, resolution) {
    if (resolution === 'confirmed') {
      return Math.min(2, 100 - previousScore); // +2, capped at 100
    } else if (resolution === 'false') {
      return Math.max(-10, -previousScore); // -10, floored at 0
    } else {
      // inconclusive
      return 0; // no change
    }
  }

  /**
   * Compute reputation delta for confirm/deny voting accuracy
   * @param {string} userAction - User's action ('confirm' or 'deny')
   * @param {string} reportResolution - Final report resolution ('confirmed', 'false', 'inconclusive')
   * @param {number} userReputation - User's current reputation score (for weighting)
   * @returns {number} - Reputation delta for voting accuracy
   */
  static computeVotingAccuracyDelta(userAction, reportResolution, userReputation) {
    const weight = userReputation / 100; // Weight by user's reputation (0-1)

    if (userAction === 'confirm') {
      if (reportResolution === 'confirmed') {
        // Correct confirmation
        return Math.min(0.5, (100 - userReputation) * weight); // +0.5, capped
      } else if (reportResolution === 'false') {
        // Incorrect confirmation
        return Math.max(-0.5, -userReputation * weight); // -0.5, floored at 0
      }
    } else if (userAction === 'deny') {
      if (reportResolution === 'false') {
        // Correct denial
        return Math.min(0.5, (100 - userReputation) * weight); // +0.5, capped
      } else if (reportResolution === 'confirmed') {
        // Incorrect denial
        return Math.max(-0.5, -userReputation * weight); // -0.5, floored at 0
      }
    }

    // inconclusive or other cases
    return 0;
  }

  /**
   * Apply reputation change from moderation resolution
   * @param {string} reportId - Report's UUID
   * @param {string} moderatorId - Moderator's UUID
   * @param {string} resolution - Resolution ('confirmed', 'false', 'inconclusive')
   * @param {string} notes - Moderator notes
   * @returns {Promise<Object>} - Updated report with reputation info
   * @throws {Error} - If validation fails
   */
  async applyModerationResolution(reportId, moderatorId, resolution, notes = '') {
    // Start transaction
    const client = await this.reportsRepository.pool.connect();
    try {
      await client.query('BEGIN');

      // Get the report
      const reportResult = await client.query(
        'SELECT * FROM hazard_reports WHERE id = $1',
        [reportId]
      );

      if (reportResult.rows.length === 0) {
        throw new Error('Report not found');
      }
      const report = reportResult.rows[0];

      if (report.status !== 'flagged') {
        throw new Error('Report is not flagged for moderation');
      }

      // Get the reporter (user who submitted the report)
      const reporterResult = await client.query(
        'SELECT * FROM users WHERE id = $1',
        [report.reporter_id]
      );

      if (reporterResult.rows.length === 0) {
        throw new Error('Reporter not found');
      }
      const reporter = reporterResult.rows[0];

      // Validate moderator exists and is moderator/admin
      const moderatorResult = await client.query(
        'SELECT * FROM users WHERE id = $1',
        [moderatorId]
      );

      if (moderatorResult.rows.length === 0) {
        throw new Error('Moderator not found');
      }
      const moderator = moderatorResult.rows[0];

      if (moderator.role !== 'moderator' && moderator.role !== 'admin') {
        throw new Error('Not authorized to moderate reports');
      }

      // Validate resolution
      const validResolutions = ['confirmed', 'false', 'inconclusive'];
      if (!validResolutions.includes(resolution)) {
        throw new Error('Invalid resolution');
      }

      // Calculate reputation delta for the reporter
      const reputationDelta = ReputationService.computeReputationDeltaFromResolution(
        reporter.reputation_score,
        resolution
      );

      // Update report status
      const reportUpdateResult = await client.query(
        `UPDATE hazard_reports
         SET status = $1, resolved_by = $2, resolved_at = NOW(), resolution_notes = $3
         WHERE id = $4
         RETURNING *`,
        [resolution, moderatorId, notes, reportId]
      );

      const updatedReport = reportUpdateResult.rows[0];

      // Insert moderation action record with reputation delta
      await client.query(
        `INSERT INTO moderation_actions (
           id, report_id, moderator_id, resolution, reputation_delta, notes
         ) VALUES (
           gen_random_uuid(), $1, $2, $3, $4, $5
         )`,
        [reportId, moderatorId, resolution, reputationDelta, notes]
      );

      // Update reporter's reputation score
      const newReputationScore = Math.max(0, Math.min(100, reporter.reputation_score + reputationDelta));

      await client.query(
        `UPDATE users
         SET reputation_score = $1, updated_at = NOW()
         WHERE id = $2`,
        [newReputationScore, report.reporter_id]
      );

      // Update reporter's stats based on resolution (confirmed/false counts)
      if (resolution === 'confirmed') {
        await client.query(
          `UPDATE users
           SET reports_confirmed_count = reports_confirmed_count + 1, updated_at = NOW()
           WHERE id = $1`,
          [report.reporter_id]
        );
      } else if (resolution === 'false') {
        await client.query(
          `UPDATE users
           SET reports_false_count = reports_false_count + 1, updated_at = NOW()
           WHERE id = $1`,
          [report.reporter_id]
        );
      }

      // Handle voting accuracy adjustments for users who interacted with this report
      await this.applyVotingAccuracyAdjustments(client, reportId, resolution);

      await client.query('COMMIT');

      // Return updated report with reputation info
      const { password_hash, ...userWithoutPassword } = reporter;
      return {
        ...updatedReport,
        reporter: {
          ...userWithoutPassword,
          reputation_score: newReputationScore
        }
      };
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  /**
   * Apply voting accuracy adjustments for users who confirmed/denied a report
   * @param {object} client - Database client (for transaction)
   * @param {string} reportId - Report's UUID
   * @param {string} resolution - Final report resolution
   */
  async applyVotingAccuracyAdjustments(client, reportId, resolution) {
    // Get all confirm/deny interactions for this report
    const interactionsResult = await client.query(
      `SELECT ri.user_id, ri.action, u.reputation_score
       FROM report_interactions ri
       JOIN users u ON ri.user_id = u.id
       WHERE ri.report_id = $1 AND ri.action IN ('confirm', 'deny')`,
      [reportId]
    );

    // Process each interaction for voting accuracy
    for (const interaction of interactionsResult.rows) {
      const { user_id, action, reputation_score } = interaction;

      // Calculate voting accuracy delta
      const votingDelta = ReputationService.computeVotingAccuracyDelta(
        action,
        resolution,
        reputation_score
      );

      if (votingDelta !== 0) {
        // Update user's reputation score for voting accuracy
        const newReputationScore = Math.max(0, Math.min(100, reputation_score + votingDelta));

        await client.query(
          `UPDATE users
           SET reputation_score = $1, updated_at = NOW()
           WHERE id = $2`,
          [newReputationScore, user_id]
        );
      }
    }
  }
}

module.exports = ReputationService;