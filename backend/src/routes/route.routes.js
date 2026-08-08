// routes/route.routes.js
// Route definitions for routing

const express = require('express');
const router = express.Router();

// Controller will be set via dependency injection
let routeController;

/**
 * Set the route controller (for dependency injection)
 * @param {Object} controller - The route controller instance
 */
function setController(controller) {
  routeController = controller;
}

// Define routes
router.get('/route', routeController.getRoute.bind(routeController));
router.get('/route/avoid', routeController.getRouteAvoidingHazards.bind(routeController));
router.get('/route/table', routeController.getTable.bind(routeController));

module.exports = { router, setController };