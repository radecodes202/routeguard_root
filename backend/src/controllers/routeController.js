// controllers/routeController.js
// HTTP handlers for routing

const RouteService = require('../services/routeService');

class RouteController {
  constructor() {
    this.routeService = new RouteService();
  }

  /**
   * Get route between two points
   * GET /api/v1/route?start=lng,lat&end=lng,lat
   */
  async getRoute(req, res) {
    try {
      const { start, end, alternatives } = req.query;

      // Parse start and end coordinates (expected format: "lng,lat")
      const startCoords = start.split(',').map(parseFloat);
      const endCoords = end.split(',').map(parseFloat);

      if (startCoords.length !== 2 || endCoords.length !== 2 ||
          isNaN(startCoords[0]) || isNaN(startCoords[1]) ||
          isNaN(endCoords[0]) || isNaN(endCoords[1])) {
        return res.status(422).json({
          success: false,
          error: {
            message: 'Invalid start or end coordinates. Format: lng,lat'
          }
        });
      }

      const start = { lng: startCoords[0], lat: startCoords[1] };
      const end = { lng: endCoords[0], lat: endCoords[1] };

      const route = await this.routeService.getRoute(start, end, {
        alternatives: alternatives === 'true'
      });

      res.status(200).json({
        success: true,
        data: route
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
   * Get route avoiding hazards
   * GET /api/v1/route/avoid?start=lng,lat&end=lng,lat&hazards=lng1,lat1;lng2,lat2;...
   */
  async getRouteAvoidingHazards(req, res) {
    try {
      const { start, end, hazards } = req.query;

      // Parse start and end coordinates
      const startCoords = start.split(',').map(parseFloat);
      const endCoords = end.split(',').map(parseFloat);

      if (startCoords.length !== 2 || endCoords.length !== 2 ||
          isNaN(startCoords[0]) || isNaN(startCoords[1]) ||
          isNaN(endCoords[0]) || isNaN(endCoords[1])) {
        return res.status(422).json({
          success: false,
          error: {
            message: 'Invalid start or end coordinates. Format: lng,lat'
          }
        });
      }

      const start = { lng: startCoords[0], lat: startCoords[1] };
      const end = { lng: endCoords[0], lat: endCoords[1] };

      // Parse hazards (expected format: "lng1,lat1;lng2,lat2;...")
      const hazardPoints = [];
      if (hazards) {
        const hazardPairs = hazards.split(';');
        for (const pair of hazardPairs) {
          const coords = pair.split(',').map(parseFloat);
          if (coords.length === 2 && !isNaN(coords[0]) && !isNaN(coords[1])) {
            hazardPoints.push({ lng: coords[0], lat: coords[1] });
          }
        }
      }

      const route = await this.routeService.getRouteAvoidingHazards(start, end, hazardPoints, {
        alternatives: false // We want the best route avoiding hazards
      });

      res.status(200).json({
        success: true,
        data: route
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
   * Get distance matrix between points
   * GET /api/v1/route/table?points=lng1,lat1;lng2,lat2;...
   */
  async getTable(req, res) {
    try {
      const { points } = req.query;

      if (!points) {
        return res.status(422).json({
          success: false,
          error: {
            message: 'Points parameter is required. Format: lng1,lat1;lng2,lat2;...'
          }
        });
      }

      // Parse points
      const pointPairs = points.split(';');
      const parsedPoints = [];
      for (const pair of pointPairs) {
        const coords = pair.split(',').map(parseFloat);
        if (coords.length === 2 && !isNaN(coords[0]) && !isNaN(coords[1])) {
          parsedPoints.push({ lng: coords[0], lat: coords[1] });
        }
      }

      if (parsedPoints.length < 2) {
        return res.status(422).json({
          success: false,
          error: {
            message: 'At least two points are required'
          }
        });
      }

      const table = await this.routeService.getTable(parsedPoints);

      res.status(200).json({
        success: true,
        data: table
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
}

module.exports = RouteController;