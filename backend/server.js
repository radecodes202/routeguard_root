// server.js
// Entry point for the backend application

const express = require('express');
const { Pool } = require('pg');
require('dotenv').config();

// Initialize Express app
const app = express();
app.use(express.json());

// Database connection pool
const pool = new Pool({
  connectionString: process.env.DATABASE_URL || 'postgresql://localhost/routeguard'
});

// Initialize repositories
const UserRepository = require('./src/repositories/user.repository');
const RefreshTokenRepository = require('./src/repositories/refreshToken.repository');
const ReportsRepository = require('./src/repositories/reports.repository');
const userRepository = new UserRepository(pool);
const refreshTokenRepository = new RefreshTokenRepository(pool);
const reportsRepository = new ReportsRepository(pool);

// Initialize services
const AuthService = require('./src/services/auth.service');
const ReportsService = require('./src/services/reports.service');
const RouteService = require('./src/services/routeService');
const authService = new AuthService(userRepository, refreshTokenRepository);
const reportsService = new ReportsService(reportsRepository, userRepository);
const routeService = new RouteService();

// Initialize controllers
const AuthController = require('./src/controllers/auth.controller');
const ReportsController = require('./src/controllers/reports.controller');
const RouteController = require('./src/controllers/routeController');
const authController = new AuthController(authService);
const reportsController = new ReportsController(reportsService);
const routeController = new RouteController();

// Initialize route sets
const { router: authRouter, setController: setAuthController } = require('./src/routes/auth.routes');
const { router: reportsRouter, setController: setReportsController } = require('./src/routes/reports.routes');
const { router: routeRouter, setController: setRouteController } = require('./src/routes/route.routes');
setAuthController(authController);
setReportsController(reportsController);
setRouteController(routeController);

// Middleware for authentication
const { authenticate } = require('./src/middleware/auth.middleware');

// Use routes
app.use('/api/v1/auth', authRouter);
app.use('/api/v1/reports', reportsRouter);
app.use('/api/v1/route', routeRouter);

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'OK', timestamp: new Date().toISOString() });
});

// Protected route example (to demonstrate middleware)
app.get('/api/v1/protected', authenticate(authService), (req, res) => {
  res.status(200).json({
    success: true,
    data: {
      message: 'This is a protected route',
      user: req.user
    }
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({
    success: false,
    error: {
      message: 'Something went wrong!'
    }
  });
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});

module.exports = app;