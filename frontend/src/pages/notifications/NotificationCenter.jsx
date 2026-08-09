import React, { useState, useEffect } from 'react';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { FiBell, FiCheck, FiX, FiTrash2, FiEdit2, FiRefreshCw } from 'react-icons/fi';

const NotificationCenter = () => {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState({
    type: '',
    isRead: '',
    page: 1,
    limit: 20
  });
  const [token, setToken] = useState(null);
  const [showFilters, setShowFilters] = useState(false);
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [formData, setFormData] = useState({
    title: '',
    message: '',
    type: 'info',
    isRead: false,
    relatedEntityType: '',
    relatedEntityId: ''
  });

  // Get API URL from environment variable
  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000/api/v1';

  // Login to get token
  useEffect(() => {
    const loginUser = async () => {
      try {
        const response = await fetch(`${API_URL}/auth/login`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            email: 'mio@routeguard.gov',
            password: 'password123'
          })
        });
        if (!response.ok) throw new Error('Login failed');
        const data = await response.json();
        setToken(data.data.accessToken);
      } catch (error) {
        console.error('Login error:', error);
        // In a real app, we would show an error message to the user
      }
    };

    loginUser();
  }, [API_URL]);

  // Fetch notifications
  const { data: notificationsData, isLoading, error } = useQuery({
    queryKey: ['notifications', filters],
    queryFn: async () => {
      // Build query parameters
      const params = new URLSearchParams();
      Object.keys(filters).forEach(key => {
        if (filters[key] !== '' && filters[key] !== null) {
          params.append(key, filters[key]);
        }
      });

      const response = await fetch(`${API_URL}/notifications?${params.toString()}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!response.ok) throw new Error('Failed to fetch notifications');
      return response.json();
    },
  });

  // Mark notification as read mutation
  const markAsReadMutation = useMutation({
    mutationFn: async (id) => {
      const response = await fetch(`${API_URL}/notifications/${id}/read`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!response.ok) throw new Error('Failed to mark notification as read');
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['notifications']);
      setSelectedNotification(null);
    },
  });

  // Delete notification mutation
  const deleteNotificationMutation = useMutation({
    mutationFn: async (id) => {
      const response = await fetch(`${API_URL}/notifications/${id}`, {
        method: 'DELETE',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!response.ok) throw new Error('Failed to delete notification');
      return response;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['notifications']);
      setSelectedNotification(null);
    },
  });

  // Create notification mutation (for admin/testing purposes)
  const createNotificationMutation = useMutation({
    mutationFn: async (notificationData) => {
      const response = await fetch(`${API_URL}/notifications`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(notificationData),
      });
      if (!response.ok) throw new Error('Failed to create notification');
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['notifications']);
      setFormData({
        title: '',
        message: '',
        type: 'info',
        isRead: false,
        relatedEntityType: '',
        relatedEntityId: ''
      });
      setSelectedNotification(null);
    },
  });

  // Handle filter change
  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({
      ...prev,
      [name]: value,
      page: 1 // Reset to first page when filters change
    }));
  };

  // Handle page change
  const handlePageChange = (page) => {
    setFilters(prev => ({ ...prev, page }));
  };

  // Handle limit change
  const handleLimitChange = (e) => {
    const { value } = e.target;
    setFilters(prev => ({
      ...prev,
      limit: parseInt(value),
      page: 1 // Reset to first page when limit changes
    }));
  };

  // Reset filters
  const resetFilters = () => {
    setFilters({
      type: '',
      isRead: '',
      page: 1,
      limit: 20
    });
  };

  // Apply filters
  const applyFilters = () => {
    setShowFilters(false);
  };

  // Handle notification selection
  const handleSelectNotification = (notification) => {
    setSelectedNotification(notification);
    setFormData({
      title: notification.title,
      message: notification.message,
      type: notification.type,
      isRead: notification.is_read,
      relatedEntityType: notification.related_entity_type || '',
      relatedEntityId: notification.related_entity_id || ''
    });
  };

  // Handle form change
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    if (type === 'checkbox') {
      setFormData(prev => ({ ...prev, [name]: checked }));
    } else {
      setFormData(prev => ({ ...prev, [name]: value }));
    }
  };

  // Handle form submit
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (selectedNotification) {
        // Update existing notification
        await createNotificationMutation.mutateAsync({
          ...formData,
          id: selectedNotification.id
        });
      } else {
        // Create new notification
        await createNotificationMutation.mutateAsync(formData);
      }
    } catch (error) {
      console.error('Error saving notification:', error);
      alert('Failed to save notification. Please try again.');
    }
  };

  // Handle mark as read
  const handleMarkAsRead = async (id) => {
    try {
      await markAsReadMutation.mutateAsync(id);
    } catch (error) {
      console.error('Error marking notification as read:', error);
      alert('Failed to mark notification as read. Please try again.');
    }
  };

  // Handle delete notification
  const handleDeleteNotification = async (id) => {
    if (window.confirm('Are you sure you want to delete this notification?')) {
      try {
        await deleteNotificationMutation.mutateAsync(id);
      } catch (error) {
        console.error('Error deleting notification:', error);
        alert('Failed to delete notification. Please try again.');
      }
    }
  };

  if (isLoading) return <div className="flex items-center justify-center h-64">Loading notifications...</div>;
  if (error) return <div className="p-4 bg-red-50 border border-red-200 text-red-600 rounded">Error: {error.message}</div>;

  const notifications = notificationsData?.notifications || [];
  const total = notificationsData?.total || 0;
  const totalPages = notificationsData?.totalPages || 1;
  const unreadCount = notifications.filter(n => !n.is_read).length;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-md">
        <div className="flex items-center justify-between px-6 py-4">
          <div className="flex items-center space-x-3">
            <FiBell className="h-5 w-5 text-gray-500" />
            <h1 className="text-2xl font-bold text-gray-800">
              Notification Center
              {unreadCount > 0 && (
                <span className="ml-2 bg-red-500 text-white rounded-full px-2 py-1 text-xs">
                  {unreadCount}
                </span>
              )}
            </h1>
          </div>
          <div className="flex items-center space-x-3">
            <button
              onClick={() => setShowFilters(!showFilters)}
              className="bg-blue-500 hover:bg-blue-600 text-white px-3 py-2 rounded transition-colors"
            >
              <FiFilter />
            </button>
            <button
              onClick={() => {
                resetFilters();
                applyFilters();
              }}
              className="bg-gray-500 hover:bg-gray-600 text-white px-3 py-2 rounded transition-colors"
            >
              <FiRefreshCw />
            </button>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 py-6">
        {/* Filters Panel */}
        {showFilters && (
          <div className="bg-white rounded-lg shadow-md mb-6">
            <div className="px-6 py-4 border-b">
              <h2 className="text-xl font-semibold text-gray-800">
                Filter Notifications
              </h2>
            </div>
            <form className="px-6 py-4 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Type
                  </label>
                  <select
                    name="type"
                    value={filters.type}
                    onChange={handleFilterChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">All Types</option>
                    <option value="info">Info</option>
                    <option value="warning">Warning</option>
                    <option value="error">Error</option>
                    <option value="success">Success</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Read Status
                  </label>
                  <select
                    name="isRead"
                    value={filters.isRead}
                    onChange={handleFilterChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">All</option>
                    <option value="true">Read</option>
                    <option value="false">Unread</option>
                  </select>
                </div>
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <label className="text-sm font-medium text-gray-700">
                    Items per page:
                  </label>
                  <select
                    value={filters.limit.toString()}
                    onChange={handleLimitChange}
                    className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="5">5</option>
                    <option value="10">10</option>
                    <option value="20">20</option>
                    <option value="50">50</option>
                  </select>
                </div>
                <div className="flex items-center space-x-3">
                  <button
                    type="button"
                    onClick={applyFilters}
                    className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded transition-colors"
                  >
                    Apply Filters
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      resetFilters();
                      applyFilters();
                    }}
                    className="bg-gray-500 hover:bg-gray-600 text-white px-4 py-2 rounded transition-colors"
                  >
                    Reset
                  </button>
                </div>
              </div>
            </form>
          </div>
        )}

        {/* Main Content Area */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Notifications List */}
          <div className="lg:col-span-2 bg-white rounded-lg shadow-md">
            <div className="px-6 py-4 border-b">
              <h2 className="text-xl font-semibold text-gray-800">
                Notifications ({total})
              </h2>
            </div>
            <div className="space-y-4">
              {notifications.length === 0 ? (
                <div className="text-center py-8">
                  <p className="text-gray-500">No notifications found matching the criteria</p>
                </div>
              ) : (
                notifications.map((notification) => (
                  <div
                    key={notification.id}
                    className={`border-l-4 ${notification.is_read
                        ? 'border-gray-300 bg-gray-50'
                        : 'border-blue-500 bg-white'}`}
                    className="p-4 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
                    onClick={() => handleSelectNotification(notification)}
                  >
                    <div className="flex justify-between items-start">
                      <div className="flex-1">
                        <div className="flex items-center mb-2">
                          <span className={`mr-2 px-2 py-1 text-xs rounded-full ${notification.type === 'info'
                              ? 'bg-blue-100 text-blue-800'
                              : notification.type === 'warning'
                              ? 'bg-yellow-100 text-yellow-800'
                              : notification.type === 'error'
                              ? 'bg-red-100 text-red-800'
                              : 'bg-green-100 text-green-800'}`}>
                            {notification.type.charAt(0).toUpperCase() + notification.type.slice(1)}
                          </span>
                          {!notification.is_read && (
                            <span className="ml-2 px-2 py-1 text-xs bg-red-500 text-white rounded-full">
                              NEW
                            </span>
                          )}
                          <span className="ml-2 text-sm text-gray-500">
                            {new Date(notification.created_at).toLocaleString()}
                          </span>
                        </div>
                        <h3 className="font-semibold text-gray-900">
                          {notification.title}
                        </h3>
                        <p className="mt-1 text-gray-600 line-clamp-2">
                          {notification.message}
                        </p>
                        {notification.related_entity_type && notification.related_entity_id && (
                          <p className="mt-2 text-xs text-gray-500">
                            Related to: {notification.related_entity_type} #{notification.related_entity_id}
                          </p>
                        )}
                      </div>
                      <div className="ml-4 flex-shrink-0">
                        <div className="space-y-2">
                          <button
                            onClick={() => handleMarkAsRead(notification.id)}
                            className={notification.is_read
                                ? 'text-gray-400 hover:text-gray-600'
                                : 'text-blue-600 hover:text-blue-800'}
                          >
                            {notification.is_read ? <FiCheck size={16} /> : <FiCheck size={16} />}
                          </button>
                          <button
                            onClick={() => handleDeleteNotification(notification.id)}
                            className="text-red-600 hover:text-red-800"
                          >
                            <FiTrash2 size={16} />
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Pagination for notifications list */}
            {totalPages > 1 && (
              <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
                <div className="text-sm text-gray-500">
                  Page {filters.page} of {totalPages}
                </div>
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => handlePageChange(Math.max(1, filters.page - 1))}
                    disabled={filters.page <= 1}
                    className="px-3 py-1 border border-gray-300 rounded-md hover:bg-gray-50"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => handlePageChange(Math.min(totalPages, filters.page + 1))}
                    disabled={filters.page >= totalPages}
                    className="px-3 py-1 border border-gray-300 rounded-md hover:bg-gray-50"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Notification Details/Form */}
          <div className="lg:col-span-1 bg-white rounded-lg shadow-md">
            {selectedNotification ? (
              <div className="">
                <div className="px-6 py-4 border-b">
                  <h2 className="text-xl font-semibold text-gray-800">
                    Notification Details
                  </h2>
                </div>
                <form onSubmit={handleSubmit} className="px-6 py-4 space-y-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Title
                    </label>
                    <input
                      name="title"
                      value={formData.title}
                      onChange={handleChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Message
                    </label>
                    <textarea
                      name="message"
                      value={formData.message}
                      onChange={handleChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 h-24"
                      required
                    />
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Type
                      </label>
                      <select
                        name="type"
                        value={formData.type}
                        onChange={handleChange}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      >
                        <option value="info">Info</option>
                        <option value="warning">Warning</option>
                        <option value="error">Error</option>
                        <option value="success">Success</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Read Status
                      </label>
                      <select
                        name="isRead"
                        value={formData.isRead.toString()}
                        onChange={handleChange}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      >
                        <option value="true">Read</option>
                        <option value="false">Unread</option>
                      </select>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Related Entity Type (Optional)
                      </label>
                      <input
                        name="relatedEntityType"
                        value={formData.relatedEntityType}
                        onChange={handleChange}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="e.g., USER, REPORT"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Related Entity ID (Optional)
                      </label>
                      <input
                        name="relatedEntityId"
                        value={formData.relatedEntityId}
                        onChange={handleChange}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="Numeric ID"
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-between">
                    <button
                      type="submit"
                      className="bg-green-600 hover:bg-green-700 text-white px-5 py-2 rounded transition-colors"
                    >
                      {selectedNotification ? 'Update Notification' : 'Create Notification'}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setSelectedNotification(null);
                        setFormData({
                          title: '',
                          message: '',
                          type: 'info',
                          isRead: false,
                          relatedEntityType: '',
                          relatedEntityId: ''
                        });
                      }}
                      className="bg-gray-500 hover:bg-gray-600 text-white px-5 py-2 rounded transition-colors ml-2"
                    >
                      Clear
                    </button>
                  </div>
                </form>
              </div>
            ) : (
              <div className="px-6 py-6 text-center">
                <p className="text-gray-500">
                  Select a notification from the list to view details, or click "Clear" to create a new notification.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default NotificationCenter;