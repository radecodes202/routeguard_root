import React, { useState, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { FiFilter, FiRefreshCw, FiSearch, FiClock } from 'react-icons/fi';

const AuditLogsPage = () => {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState({
    action: '',
    entityType: '',
    actorId: '',
    startDate: '',
    endDate: '',
    page: 1,
    limit: 50
  });
  const [token, setToken] = useState(null);
  const [showFilters, setShowFilters] = useState(false);

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

  // Fetch audit logs
  const { data: auditLogsData, isLoading, error } = useQuery({
    queryKey: ['auditlogs', filters],
    queryFn: async () => {
      // Build query parameters
      const params = new URLSearchParams();
      Object.keys(filters).forEach(key => {
        if (filters[key] !== '' && filters[key] !== null) {
          params.append(key, filters[key]);
        }
      });

      const response = await fetch(`${API_URL}/admin/auditlogs?${params.toString()}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!response.ok) throw new Error('Failed to fetch audit logs');
      return response.json();
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

  // Handle date change
  const handleDateChange = (e) => {
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
      action: '',
      entityType: '',
      actorId: '',
      startDate: '',
      endDate: '',
      page: 1,
      limit: 50
    });
  };

  // Apply filters
  const applyFilters = () => {
    setShowFilters(false);
  };

  if (isLoading) return <div className="flex items-center justify-center h-64">Loading audit logs...</div>;
  if (error) return <div className="p-4 bg-red-50 border border-red-200 text-red-600 rounded">Error: {error.message}</div>;

  const auditLogs = auditLogsData?.data || [];
  const total = auditLogsData?.total || 0;
  const totalPages = auditLogsData?.totalPages || 1;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-md">
        <div className="flex items-center justify-between px-6 py-4">
          <div className="flex items-center space-x-3">
            <FiClock className="h-5 w-5 text-gray-500" />
            <h1 className="text-2xl font-bold text-gray-800">Audit Logs</h1>
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
                Filter Audit Logs
              </h2>
            </div>
            <form className="px-6 py-4 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Action
                  </label>
                  <input
                    name="action"
                    value={filters.action}
                    onChange={handleFilterChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="e.g., UPDATE_USER_ROLE"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Entity Type
                  </label>
                  <input
                    name="entityType"
                    value={filters.entityType}
                    onChange={handleFilterChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="e.g., USER, REPORT"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Actor ID
                  </label>
                  <input
                    name="actorId"
                    value={filters.actorId}
                    onChange={handleFilterChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="User ID"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Start Date
                  </label>
                  <input
                    name="startDate"
                    type="date"
                    value={filters.startDate}
                    onChange={handleDateChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    End Date
                  </label>
                  <input
                    name="endDate"
                    type="date"
                    value={filters.endDate}
                    onChange={handleDateChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
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
                    <option value="10">10</option>
                    <option value="25">25</option>
                    <option value="50">50</option>
                    <option value="100">100</option>
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

        {/* Audit Logs Table */}
        <div className="bg-white rounded-lg shadow-md">
          <div className="px-6 py-4 border-b">
            <h2 className="text-xl font-semibold text-gray-800">
              System Audit Trail ({total} entries)
            </h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Timestamp
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Action
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Entity Type
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Entity ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actor (User ID)
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    IP Address
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Details
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {auditLogs.length === 0 ? (
                  <tr>
                    <td className="px-6 py-4 text-center text-gray-500" colSpan="8">
                      No audit logs found matching the criteria
                    </td>
                  </tr>
                ) : (
                  auditLogs.map((log) => (
                    <tr key={log.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {new Date(log.created_at).toLocaleString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${log.action.includes('_FAIL') || log.action.includes('_ERROR')
                            ? 'bg-red-100 text-red-800'
                            : log.action.includes('_SUCCESS') || log.action.includes('_CREATE') || log.action.includes('_UPDATE')
                            ? 'bg-green-100 text-green-800'
                            : 'bg-blue-100 text-blue-800'}`}>
                          {log.action.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 capitalize">
                        {log.entity_type || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {log.entity_id || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {log.actor_id || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {log.ip_address || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {log.metadata ? (
                          <div className="max-w-xs">
                            <p className="text-xs break-all">{JSON.stringify(log.metadata, null, 2)}</p>
                          </div>
                        ) : (
                          '-'
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
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
      </div>
    </div>
  );
};

export default AuditLogsPage;