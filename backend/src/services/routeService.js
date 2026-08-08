// services/routeService.js
// Routing service using OSRM

const axios = require('axios');

// OSRM server URL - should be configurable via environment variables
const OSRM_URL = process.env.OSRM_URL || 'http://localhost:5000';

class RouteService {
  constructor() {
    // No dependencies needed for now, but we could inject repositories if needed for hazard data
  }

  /**
   * Get route between two points using OSRM
   * @param {Object} start - { lng, lat }
   * @param {Object} end - { lng, lat }
   * @param {Object} options - OSRM service options (e.g., alternatives, steps, etc.)
   * @returns {Promise<Object>} - OSRM route response
   */
  async getRoute(start, end, options = {}) {
    try {
      // OSRM expects coordinates in lng,lat format
      const startCoord = `${start.lng},${start.lat}`;
      const endCoord = `${end.lng},${end.lat}`;
      const coords = [startCoord, endCoord].join(';');

      // Build OSRM query parameters
      const params = {
        coordinates: coords,
        alternatives: options.alternatives || false,
        steps: options.steps || true,
        geometries: options.geometries || 'polyline',
        overview: options.overview || 'simplified',
        ...options
      };

      // Remove any params that are not in the OSRM API (like start, end)
      // We'll use the OSRM table/route service for route
      const response = await axios.get(`${OSRM_URL}/route/v1/driving/${coords}`, {
        params: params
      });

      return response.data;
    } catch (error) {
      console.error('Error calling OSRM route service:', error);
      throw new Error('Failed to get route from OSRM');
    }
  }

  /**
   * Get route avoiding hazards (using OSRM barriers)
   * @param {Object} start - { lng, lat }
   * @param {Object} end - { lng, lat }
   * @param {Array} hazards - Array of hazard objects with { lng, lat } (or radius)
   * @param {Object} options - Additional OSRM options
   * @returns {Promise<Object>} - OSRM route response avoiding hazards
   */
  async getRouteAvoidingHazards(start, end, hazards = [], options = {}) {
    try {
      // OSRM barriers can be used to avoid certain points or rectangles
      // We'll convert each hazard to a barrier point (or a small circle)
      // For simplicity, we'll use point barriers at the hazard location
      // Note: OSRM barriers are specified as: coord1;coord2;...;coordn
      // We'll create a barrier string from the hazards
      const barrierString = hazards
        .map(h => `${h.lng},${h.lat}`)
        .join(';');

      const startCoord = `${start.lng},${start.lat}`;
      const endCoord = `${end.lng},${end.lat}`;
      const coords = [startCoord, endCoord].join(';');

      // Build OSRM query parameters
      const params = {
        coordinates: coords,
        alternatives: options.alternatives || false,
        steps: options.steps || true,
        geometries: options.geometries || 'polyline',
        overview: options.overview || 'simplified',
        // Add barriers if we have any
        ...(barrierString && { barriers: barrierString }),
        ...options
      };

      const response = await axios.get(`${OSRM_URL}/route/v1/driving/${coords}`, {
        params: params
      });

      return response.data;
    } catch (error) {
      console.error('Error calling OSRM route service with barriers:', error);
      throw new Error('Failed to get route from OSRM with barriers');
    }
  }

  /**
   * Get table (distance matrix) between multiple points
   * Useful for finding the nearest hazard or for rerouting decisions
   * @param {Array} points - Array of { lng, lat }
   * @returns {Promise<Object>} - OSRM table response
   */
  async getTable(points) {
    try {
      const coords = points.map(p => `${p.lng},${p.lat}`).join(';');

      const response = await axios.get(`${OSRM_URL}/table/v1/driving/${coords}`, {
        params: {
          annotations: 'duration,distance'
        }
      });

      return response.data;
    } catch (error) {
      console.error('Error calling OSRM table service:', error);
      throw new Error('Failed to get table from OSRM');
    }
  }
}

module.exports = RouteService;