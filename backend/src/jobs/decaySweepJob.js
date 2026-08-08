// jobs/decaySweepJob.js
// Scheduled job for confidence decay processing

const ConfidenceDecayService = require('../services/confidenceDecayService');
const HazardChannel = require('../sockets/hazardChannel');
const ReportsRepository = require('../reports/reports.repository');
const { Pool } = require('pg');

// This would typically be called by a scheduler like node-cron or Agenda
// For this implementation, we'll create a function that can be called externally

/**
 * Run the confidence decay sweep job
 * @param {Pool} pool - Database connection pool
 * @param {SocketIO.Server} io - Socket.IO server instance
 * @returns {Promise<void>}
 */
async function runDecaySweepJob(pool, io) {
  try {
    const reportsRepository = new ReportsRepository(pool);
    const decayService = new ConfidenceDecayService(pool);
    const hazardChannel = new HazardChannel(io, reportsRepository);

    const result = await decayService.processDecayJobWithBroadcast(hazardChannel);

    if (result.escalatedReports.length > 0) {
      console.log(`[Decay Sweep] Escalated ${result.escalatedReports.length} reports to moderation queue`);
    }

    if (result.updatedReports.length > 0) {
      console.log(`[Decay Sweep] Updated confidence scores for ${result.updatedReports.length} reports`);
    }

    if (result.escalatedReports.length === 0 && result.updatedReports.length === 0) {
      console.log('[Decay Sweep] No reports required updates or escalation');
    }
  } catch (error) {
    console.error('[Decay Sweep] Error processing decay job:', error);
    throw error;
  }
}

module.exports = { runDecaySweepJob };