// services/geoQuery.service.js
// Geospatial query service for hazard reports

const ReportsRepository = require('../reports/reports.repository');

class GeoQueryService {
  constructor(reportsRepository) {
    this.reportsRepository = reportsRepository;
  }

  /**
   * Get hazard reports within a specified radius of a point
   * @param {number} lat - Latitude of center point
   * @param {number} lng - Longitude of center point
   * @param {number} radiusMeters - Radius in meters (default 5000 for 5km)
   * @param {Object} filters - Additional filters (status, category, limit, offset)
   * @returns {Promise<Object>} - Reports data with count
   */
  async getReportsInRadius(lat, lng, radiusMeters = 5000, filters = {}) {
    // Validate input parameters
    if (isNaN(lat) || isNaN(lng)) {
      throw new Error('Invalid latitude or longitude');
    }

    if (lat < -90 || lat > 90) {
      throw new Error('Latitude must be between -90 and 90 degrees');
    }

    if (lng < -180 || lng > 180) {
      throw new Error('Longitude must be between -180 and 180 degrees');
    }

    if (radiusMeters <= 0) {
      throw new Error('Radius must be positive');
    }

    // Cap maximum radius to prevent abuse
    const maxRadius = 50000; // 50km max
    if (radiusMeters > maxRadius) {
      throw new Error(`Radius cannot exceed ${maxRadius} meters`);
    }

    // Delegate to repository method
    return await this.reportsRepository.findNearby(lat, lng, radiusMeters, filters);
  }

  /**
   * Check if a point is within a hazard's vicinity (for alerting)
   * @param {number} userLat - User's latitude
   * @param {number} userLng - User's longitude
   * @param {string} reportId - Hazard report ID
   * @param {number} distanceMeters - Distance threshold in meters
   * @returns {Promise<boolean>} - True if within distance
   */
  async isPointNearReport(userLat, userLng, reportId, distanceMeters = 500) {
    // Get the specific report
    const report = await this.reportsRepository.findById(reportId);
    if (!report) {
      throw new Error('Report not found');
    }

    // Extract coordinates from PostGIS geography
    // ST_AsText returns 'POINT(lng lat)' format
    const locationWkt = report.location_wkt;
    if (!locationWkt || !locationWkt.startsWith('POINT(')) {
      throw new Error('Invalid report location');
    }

    // Parse POINT(lng lat) format
    const coordsMatch = locationWkt.match(/POINT\((-?\d+\.?\d*)\s+(-?\d+\.?\d*)\)/);
    if (!coordsMatch) {
      throw new Error('Could not parse report coordinates');
    }

    const reportLng = parseFloat(coordsMatch[1]);
    const reportLat = parseFloat(coordsMatch[2]);

    // Calculate distance using Haversine formula (good enough for short distances)
    const distance = this.calculateDistance(
      userLat, userLng,
      reportLat, reportLng
    );

    return distance <= distanceMeters;
  }

  /**
   * Calculate distance between two points using Haversine formula
   * @param {number} lat1 - First point latitude
   * @param {number} lng1 - First point longitude
   * @param {number} lat2 - Second point latitude
   * @param {number} lng2 - Second point longitude
   * @returns {number} - Distance in meters
   */
  calculateDistance(lat1, lng1, lat2, lng2) {
    const R = 6371000; // Earth's radius in meters
    const φ1 = lat1 * Math.PI / 180; // φ, λ in radians
    const φ2 = lat2 * Math.PI / 180;
    const Δφ = (lat2 - lat1) * Math.PI / 180;
    const Δλ = (lng2 - lng1) * Math.PI / 180;

    const a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
              Math.cos(φ1) * Math.cos(φ2) *
              Math.sin(Δλ/2) * Math.sin(Δλ/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

    return R * c; // Distance in meters
  }

  /**
   * Get reports that are active (pending, flagged, confirmed) within radius
   * @param {number} lat - Latitude of center point
   * @param {number} lng - Longitude of center point
   * @param {number} radiusMeters - Radius in meters
   * @returns {Promise<Array>} - Array of active reports
   */
  async getActiveReportsInRadius(lat, lng, radiusMeters = 5000) {
    return await this.getReportsInRadius(lat, lng, radiusMeters, {
      status: ['pending', 'flagged', 'confirmed']
    });
  }
}

module.exports = GeoQueryService;