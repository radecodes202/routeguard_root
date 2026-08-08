// server.js
// Entry point for the backend application

const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');
require('dotenv').config();

// Initialize Express app
const app = express();

// Configure CORS
const allowedOrigins = process.env.ALLOWED_ORIGINS
  ? process.env.ALLOWED_ORIGINS.split(',').map(origin => origin.trim())
  : ["http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001"];

app.use(cors({
  origin: allowedOrigins,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  credentials: true
}));

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
const AdvisoryService = require('./src/services/advisory.service');
const RouteService = require('./src/services/routeService');
const ReputationService = require('./src/services/reputationService');
const authService = new AuthService(userRepository, refreshTokenRepository, pool);
const reportsService = new ReportsService(reportsRepository, userRepository);
const advisoryService = new AdvisoryService(
  new (require('./src/repositories/advisory.repository'))(pool),
  userRepository
);
const routeService = new RouteService();

// Initialize controllers
const AuthController = require('./src/controllers/auth.controller');
const ReportsController = require('./src/controllers/reports.controller');
const AdvisoryController = require('./src/controllers/advisory.controller');
const RouteController = require('./src/controllers/routeController');
const ModerationController = require('./src/controllers/moderation.controller');
const authController = new AuthController(authService);
const reportsController = new ReportsController(reportsService);
const advisoryController = new AdvisoryController(advisoryService);
const routeController = new RouteController();
const moderationController = new ModerationController(reportsService, new ReputationService(userRepository, reportsRepository));

// Initialize route sets
const { router: authRouter, setController: setAuthController } = require('./src/routes/auth.routes');
const { router: reportsRouter, setController: setReportsController } = require('./src/routes/reports.routes');
const { router: routeRouter, setController: setRouteController } = require('./src/routes/route.routes');
const { router: advisoryRouter, setController: setAdvisoryController, setAuthMiddleware: setAdvisoryAuthMiddleware } = require('./src/routes/advisory.routes');
const { router: moderationRouter, setController: setModerationController } = require('./src/routes/moderation.routes');
setAuthController(authController);
setReportsController(reportsController);
setRouteController(routeController);
setAdvisoryController(advisoryController);
setModerationController(moderationController);

// Middleware for authentication
const { authenticate } = require('./src/middleware/auth.middleware');
const authMiddleware = authenticate(authService); // Create the middleware instance
setAdvisoryAuthMiddleware(authMiddleware); // Set it in the advisory routes

// Use routes
app.use('/api/v1/auth', authRouter);
app.use('/api/v1/reports', reportsRouter);
app.use('/api/v1/route', routeRouter);
app.use('/api/v1/advisories', advisoryRouter);
app.use('/api/v1/moderation', moderationRouter);

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
const server = app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});

// Initialize Socket.IO with secure CORS configuration
const { Server } = require('socket.io');

const io = new Server(server, {
  cors: {
    origin: allowedOrigins,
    methods: ["GET", "POST"],
    credentials: true
  }
});

// Make io accessible to our modules
app.set('io', io);

// Set up decay sweep job to run every 5 minutes
const { runDecaySweepJob } = require('./src/jobs/decaySweepJob');

// Run decay sweep job every 5 minutes
setInterval(async () => {
  try {
    await runDecaySweepJob(pool, io);
  } catch (error) {
    console.error('[Decay Sweep] Error in scheduled job:', error);
  }
}, 5 * 60 * 1000); // 5 minutes in milliseconds

// Also run it once on startup
runDecaySweepJob(pool, io).catch(console.error);

// Make io available to hazard channel
const HazardChannel = require('./src/sockets/hazardChannel');
const hazardChannel = new HazardChannel(io, reportsRepository, new (require('./src/repositories/advisory.repository'))(pool));

// Handle socket connections
io.on('connection', (socket) => {
  console.log('User connected to socket.io');

  // Handle hazard channel connection
  hazardChannel.handleConnection(socket);

  socket.on('disconnect', () => {
    console.log('User disconnected from socket.io');
  });
});

module.exports = app;