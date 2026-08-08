// sockets/hazardChannel.js
// WebSocket channel for hazard map updates with geo-room scoping

const GeoQueryService = require('../services/geoQuery.service');

class HazardChannel {
  constructor(io, reportsRepository) {
    this.io = io;
    this.geoQueryService = new GeoQueryService(reportsRepository);
    this.userRooms = new Map(); // Track which rooms each user is in
  }

  /**
   * Initialize WebSocket connection and handlers
   * @param {Socket} socket - Socket.IO socket instance
   */
  async handleConnection(socket) {
    try {
      // Extract user from JWT token (handled by auth middleware)
      const user = socket.user;
      if (!user) {
        socket.disconnect();
        return;
      }

      console.log(`User ${user.id} connected to hazard channel`);

      // Join the geo-room based on current location
      // This would typically be called when user sends location update
      socket.on('join-geo-room', async (data) => {
        await this.handleJoinGeoRoom(socket, data);
      });

      // Handle location updates from user
      socket.on('location-update', async (data) => {
        await this.handleLocationUpdate(socket, data);
      });

      // Handle disconnection
      socket.on('disconnect', () => {
        this.handleDisconnect(socket);
      });

    } catch (error) {
      console.error('Error in hazard channel connection:', error);
      socket.disconnect();
    }
  }

  /**
   * Handle user joining a geo-room based on their location
   * @param {Socket} socket - Socket.IO socket instance
   * @param {Object} data - {lat, lng}
   */
  async handleJoinGeoRoom(socket, data) {
    try {
      const { lat, lng } = data;

      // Validate coordinates
      if (isNaN(lat) || isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
        socket.emit('error', { message: 'Invalid coordinates' });
        return;
      }

      // Leave previous rooms if any
      await this.leaveUserRooms(socket);

      // Calculate geo-room key (using a coarse grid for room scoping)
      // Using 0.01 degree precision (~1km at equator) for room granularity
      const roomLat = Math.floor(lat * 100) / 100;
      const roomLng = Math.floor(lng * 100) / 100;
      const roomKey = `geo-${roomLat}-${roomLng}`;

      // Join the geo-room
      socket.join(roomKey);

      // Track user's current room
      if (!this.userRooms.has(socket.id)) {
        this.userRooms.set(socket.id, new Set());
      }
      this.userRooms.get(socket.id).add(roomKey);

      // Send initial hazard data for this room
      await this.sendInitialHazards(socket, lat, lng, roomKey);

      socket.emit('joined-geo-room', { roomKey, lat, lng });

    } catch (error) {
      console.error('Error joining geo-room:', error);
      socket.emit('error', { message: 'Failed to join geo-room' });
    }
  }

  /**
   * Handle user location updates
   * @param {Socket} socket - Socket.IO socket instance
   * @param {Object} data - {lat, lng}
   */
  async handleLocationUpdate(socket, data) {
    try {
      const { lat, lng } = data;

      // Validate coordinates
      if (isNaN(lat) || isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
        socket.emit('error', { message: 'Invalid coordinates' });
        return;
      }

      // Leave previous rooms and join new ones based on updated location
      await this.handleJoinGeoRoom(socket, data);

      // Optionally, we could also check for proximity to hazards and trigger alerts here
      // This would be done by the notification service in a real implementation

    } catch (error) {
      console.error('Error handling location update:', error);
      socket.emit('error', { message: 'Failed to process location update' });
    }
  }

  /**
   * Leave all rooms a user is currently in
   * @param {Socket} socket - Socket.IO socket instance
   */
  async leaveUserRooms(socket) {
    const rooms = this.userRooms.get(socket.id);
    if (rooms) {
      for (const room of rooms) {
        socket.leave(room);
      }
      this.userRooms.delete(socket.id);
    }
  }

  /**
   * Handle user disconnection
   * @param {Socket} socket - Socket.IO socket instance
   */
  handleDisconnect(socket) {
    console.log(`User ${socket.user?.id} disconnected from hazard channel`);
    this.leaveUserRooms(socket);
  }

  /**
   * Send initial hazard data for a user's geo-room
   * @param {Socket} socket - Socket.IO socket instance
   * @param {number} lat - User's latitude
   * @param {number} lng - User's longitude
   * @param {string} roomKey - Geo-room key
   */
  async sendInitialHazards(socket, lat, lng, roomKey) {
    try {
      // Get hazards within 5km of user's location
      const result = await this.geoQueryService.getReportsInRadius(lat, lng, 5000, {
        status: ['pending', 'flagged', 'confirmed'],
        limit: 100 // Limit initial load
      });

      // Emit initial hazard data
      socket.emit('hazard:initial', {
        hazards: result.reports,
        roomKey,
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      console.error('Error sending initial hazards:', error);
      socket.emit('error', { message: 'Failed to load initial hazard data' });
    }
  }

  /**
   * Broadcast a new hazard to relevant geo-rooms
   * @param {Object} hazard - The hazard report object
   */
  async broadcastHazardNew(hazard) {
    try {
      // Extract coordinates from hazard
      const locationWkt = hazard.location_wkt;
      if (!locationWkt || !locationWkt.startsWith('POINT(')) {
        console.warn('Invalid hazard location for broadcasting:', hazard.id);
        return;
      }

      const coordsMatch = locationWkt.match(/POINT\((-?\d+\.?\d*)\s+(-?\d+\.?\d*)\)/);
      if (!coordsMatch) {
        console.warn('Could not parse hazard coordinates for broadcasting:', hazard.id);
        return;
      }

      const hazardLng = parseFloat(coordsMatch[1]);
      const hazardLat = parseFloat(coordsMatch[2]);

      // Determine which geo-rooms this hazard affects
      // We'll broadcast to rooms within a 5.5km radius to ensure users near boundaries get updates
      const broadcastRadius = 5500; // 5.5km to overlap adjacent rooms

      // For simplicity, we'll broadcast to the hazard's own room and adjacent rooms
      // In a more sophisticated implementation, we could calculate all overlapping rooms
      const roomLat = Math.floor(hazardLat * 100) / 100;
      const roomLng = Math.floor(hazardLng * 100) / 100;

      // Broadcast to the hazard's room and 8 surrounding rooms
      const roomsToNotify = [];
      for (let latOffset = -1; latOffset <= 1; latOffset++) {
        for (let lngOffset = -1; lngOffset <= 1; lngOffset++) {
          const roomKey = `geo-${roomLat + latOffset * 0.01}-${roomLng + lngOffset * 0.01}`;
          roomsToNotify.push(roomKey);
        }
      }

      // Emit to all clients in the relevant rooms
      roomsToNotify.forEach(roomKey => {
        this.io.to(roomKey).emit('hazard:new', {
          hazard: hazard,
          timestamp: new Date().toISOString()
        });
      });

    } catch (error) {
      console.error('Error broadcasting new hazard:', error);
    }
  }

  /**
   * Broadcast a hazard update to relevant geo-rooms
   * @param {Object} hazard - The updated hazard report object
   */
  async broadcastHazardUpdated(hazard) {
    try {
      // Similar to broadcastHazardNew but for updates
      const locationWkt = hazard.location_wkt;
      if (!locationWkt || !locationWkt.startsWith('POINT(')) {
        console.warn('Invalid hazard location for broadcasting update:', hazard.id);
        return;
      }

      const coordsMatch = locationWkt.match(/POINT\((-?\d+\.?\d*)\s+(-?\d+\.?\d*)\)/);
      if (!coordsMatch) {
        console.warn('Could not parse hazard coordinates for broadcasting update:', hazard.id);
        return;
      }

      const hazardLng = parseFloat(coordsMatch[1]);
      const hazardLat = parseFloat(coordsMatch[2]);

      // Determine rooms to notify (same logic as new hazard)
      const roomLat = Math.floor(hazardLat * 100) / 100;
      const roomLng = Math.floor(hazardLng * 100) / 100;

      const roomsToNotify = [];
      for (let latOffset = -1; latOffset <= 1; latOffset++) {
        for (let lngOffset = -1; lngOffset <= 1; lngOffset++) {
          const roomKey = `geo-${roomLat + latOffset * 0.01}-${roomLng + lngOffset * 0.01}`;
          roomsToNotify.push(roomKey);
        }
      }

      // Emit to all clients in the relevant rooms
      roomsToNotify.forEach(roomKey => {
        this.io.to(roomKey).emit('hazard:updated', {
          hazard: hazard,
          timestamp: new Date().toISOString()
        });
      });

    } catch (error) {
      console.error('Error broadcasting hazard update:', error);
    }
  }

  /**
   * Broadcast a hazard resolution to relevant geo-rooms
   * @param {Object} hazard - The resolved hazard report object
   */
  async broadcastHazardResolved(hazard) {
    try {
      // Similar to broadcastHazardNew but for resolution
      const locationWkt = hazard.location_wkt;
      if (!locationWkt || !locationWkt.startsWith('POINT(')) {
        console.warn('Invalid hazard location for broadcasting resolution:', hazard.id);
        return;
      }

      const coordsMatch = locationWkt.match(/POINT\((-?\d+\.?\d*)\s+(-?\d+\.?\d*)\)/);
      if (!coordsMatch) {
        console.warn('Could not parse hazard coordinates for broadcasting resolution:', hazard.id);
        return;
      }

      const hazardLng = parseFloat(coordsMatch[1]);
      const hazardLat = parseFloat(coordsMatch[2]);

      // Determine rooms to notify (same logic as new hazard)
      const roomLat = Math.floor(hazardLat * 100) / 100;
      const roomLng = Math.floor(hazardLng * 100) / 100;

      const roomsToNotify = [];
      for (let latOffset = -1; latOffset <= 1; latOffset++) {
        for (let lngOffset = -1; lngOffset <= 1; lngOffset++) {
          const roomKey = `geo-${roomLat + latOffset * 0.01}-${roomLng + lngOffset * 0.01}`;
          roomsToNotify.push(roomKey);
        }
      }

      // Emit to all clients in the relevant rooms
      roomsToNotify.forEach(roomKey => {
        this.io.to(roomKey).emit('hazard:resolved', {
          hazardId: hazard.id,
          resolution: hazard.status,
          resolvedBy: hazard.resolved_by,
          resolvedAt: hazard.resolved_at,
          timestamp: new Date().toISOString()
        });
      });

    } catch (error) {
      console.error('Error broadcasting hazard resolution:', error);
    }
  }
}

module.exports = HazardChannel;