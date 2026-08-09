import React, { useState } from 'react';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { FiTrash2, FiEdit2, FiCheck, FiX, FiAlertTriangle } from 'react-icons/fi';

const ModerationQueuePage = () => {
  const queryClient = useQueryClient();
  const [selectedReport, setSelectedReport] = useState(null);
  const [formData, setFormData] = useState({
    resolution: '', // 'confirmed', 'false', 'inconclusive'
    notes: ''
  });
  const [token, setToken] = useState(null);

  // Get API URL from environment variable
  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000/api/v1';

  // Login to get token (in a real app, this would be handled by auth context)
  // For simplicity, we're doing a basic login here. In production, use proper auth state.
  // Note: This is a temporary solution for demo purposes.
  // In a full implementation, you would use an auth context or state management.

  // Fetch moderation queue (flagged reports)
  const { data: queueData, isLoading, error } = useQuery({
    queryKey: ['moderationQueue'],
    queryFn: async () => {
      const response = await fetch(`${API_URL}/moderation/queue`, {
        headers: {
          // In a real app, you would get the token from auth state or context
          // For now, we'll try to get it from localStorage or a login step
          // This is a placeholder - you should implement proper auth
          'Authorization': token ? `Bearer ${token}` : ''
        }
      });
      if (!response.ok) throw new Error('Failed to fetch moderation queue');
      return response.json();
    },
  });

  // Mutation for resolving a report
  const resolveReportMutation = useMutation({
    mutationFn: async ({ reportId, resolution, notes }) => {
      const response = await fetch(`${API_URL}/moderation/queue/${reportId}/resolve`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ resolution, notes }),
      });
      if (!response.ok) throw new Error('Failed to resolve report');
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['moderationQueue']);
      setSelectedReport(null);
      setFormData({ resolution: '', notes: '' });
    },
  });

  // Handle form change
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // Handle form submit
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (selectedReport) {
        await resolveReportMutation.mutateAsync({
          reportId: selectedReport.id,
          resolution: formData.resolution,
          notes: formData.notes
        });
      }
    } catch (error) {
      console.error('Error resolving report:', error);
      alert('Failed to resolve report. Please try again.');
    }
  };

  // Handle report selection for resolving
  const handleResolveReport = (report) => {
    setSelectedReport(report);
    // Reset form when selecting a new report
    setFormData({ resolution: '', notes: '' });
  };

  if (isLoading) return <div className="flex items-center justify-center h-64">Loading moderation queue...</div>;
  if (error) return <div className="p-4 bg-red-50 border border-red-200 text-red-600 rounded">Error: {error.message}</div>;

  const reports = queueData?.reports || [];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-md">
        <div className="flex items-center justify-between px-6 py-4">
          <h1 className="text-2xl font-bold text-gray-800">
            <FiAlertTriangle /> Moderation Queue
          </h1>
          {/* In a real app, you would have user info here */}
          <div className="flex items-center space-x-3">
            <span className="text-gray-500">Logged in as: Moderator</span>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 py-6">
        {/* Report Resolution Form */}
        {selectedReport && (
          <div className="bg-white rounded-lg shadow-md mb-6">
            <div className="px-6 py-4 border-b">
              <h2 className="text-xl font-semibold text-gray-800">
                Resolve Report #{selectedReport.id}
              </h2>
            </div>
            <form onSubmit={handleSubmit} className="px-6 py-4 space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Reporter
                </label>
                <p className="pl-4 text-gray-600">{selectedReport.email || 'Unknown'}</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Hazard Category
                </label>
                <p className="pl-4 text-gray-600 capitalize">{selectedReport.category || 'Unknown'}</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Reported At
                </label>
                <p className="pl-4 text-gray-600">
                  {new Date(selectedReport.created_at).toLocaleString()}
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Location
                </label>
                <p className="pl-4 text-gray-600 break-all">
                  {selectedReport.location_wkt || 'No location'}
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description
                </label>
                <p className="pl-4 text-gray-600">{selectedReport.description || 'No description'}</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Resolution
                  </label>
                  <select
                    name="resolution"
                    value={formData.resolution}
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  >
                    <option value="">Select Resolution</option>
                    <option value="confirmed">Confirmed (Hazard is real)</option>
                    <option value="false">False (Hazard is not real)</option>
                    <option value="inconclusive">Inconclusive (Not enough evidence)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Moderator Notes (Optional)
                  </label>
                  <textarea
                    name="notes"
                    value={formData.notes}
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 h-24"
                    placeholder="Enter any additional notes about your decision..."
                  />
                </div>
              </div>

              <div className="flex items-center justify-between">
                <button
                  type="submit"
                  className="bg-green-600 hover:bg-green-700 text-white px-5 py-2 rounded transition-colors"
                >
                  Resolve Report
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setSelectedReport(null);
                    setFormData({ resolution: '', notes: '' });
                  }}
                  className="bg-gray-500 hover:bg-gray-600 text-white px-5 py-2 rounded transition-colors ml-2"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Reports List */}
        <div className="bg-white rounded-lg shadow-md">
          <div className="px-6 py-4 border-b">
            <h2 className="text-xl font-semibold text-gray-800">
              Flagged Reports ({reports.length})
            </h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Reporter
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Category
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Reported At
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {reports.length === 0 ? (
                  <tr>
                    <td className="px-6 py-4 text-center text-gray-500" colSpan="5">
                      No flagged reports in the queue
                    </td>
                  </tr>
                ) : (
                  reports.map((report) => (
                    <tr key={report.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {report.id}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {report.email || 'Unknown'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 capitalize">
                        {report.category || 'Unknown'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {new Date(report.created_at).toLocaleString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <button
                          onClick={() => handleResolveReport(report)}
                          className="text-blue-600 hover:text-blue-900 px-3"
                        >
                          <FiEdit2 size={16} />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {queueData && queueData.total > queueData.limit && (
            <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
              <div className="text-sm text-gray-500">
                Showing {queueData.offset + 1}-{Math.min(queueData.offset + queueData.limit, queueData.total)} of {queueData.total} reports
              </div>
              {/* In a real app, you would implement proper pagination */}
              <div className="text-sm text-gray-500">
                Page {Math.floor(queueData.offset / queueData.limit) + 1} of {Math.ceil(queueData.total / queueData.limit)}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ModerationQueuePage;