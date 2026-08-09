import React, { useState, useEffect } from 'react';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { FiTrash2, FiEdit2, FiPlus, FiUsers, FiCheckCircle, FiXCircle } from 'react-icons/fi';

const AdminUsersPage = () => {
  const queryClient = useQueryClient();
  const [selectedUser, setSelectedUser] = useState(null);
  const [formData, setFormData] = useState({
    email: '',
    full_name: '',
    role: '',
    is_active: true,
    phone_number: '',
    password: '' // Only for new users
  });
  const [token, setToken] = useState(null);
  const [showPassword, setShowPassword] = useState(false);

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

  // Fetch users
  const { data: usersData, isLoading, error } = useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      const response = await fetch(`${API_URL}/admin/users`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!response.ok) throw new Error('Failed to fetch users');
      return response.json();
    },
  });

  // Update user role mutation
  const updateUserRoleMutation = useMutation({
    mutationFn: async ({ id, role }) => {
      const response = await fetch(`${API_URL}/admin/users/${id}/role`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ role }),
      });
      if (!response.ok) throw new Error('Failed to update user role');
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['users']);
      setSelectedUser(null);
      setFormData({
        email: '',
        full_name: '',
        role: '',
        is_active: true,
        phone_number: '',
        password: ''
      });
    },
  });

  // Update user status mutation
  const updateUserStatusMutation = useMutation({
    mutationFn: async ({ id, isActive }) => {
      const response = await fetch(`${API_URL}/admin/users/${id}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ isActive }),
      });
      if (!response.ok) throw new Error('Failed to update user status');
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['users']);
      setSelectedUser(null);
      setFormData({
        email: '',
        full_name: '',
        role: '',
        is_active: true,
        phone_number: '',
        password: ''
      });
    },
  });

  // Handle form change
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    if (type === 'checkbox') {
      setFormData(prev => ({ ...prev, [name]: checked }));
    } else if (name === 'password' && type === 'text') {
      // Handle password field visibility toggle
      setFormData(prev => ({ ...prev, [name]: value }));
    } else {
      setFormData(prev => ({ ...prev, [name]: value }));
    }
  };

  // Handle form submit (for creating new user)
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      // In a real implementation, we would have a create user endpoint
      // For now, we'll just show an alert that this feature needs backend implementation
      alert('Create user functionality would be implemented here with a POST /api/v1/admin/users endpoint');
    } catch (error) {
      console.error('Error creating user:', error);
      alert('Failed to create user. Please try again.');
    }
  };

  // Handle user selection for editing
  const handleEditUser = (user) => {
    setSelectedUser(user);
    setFormData({
      email: user.email,
      full_name: user.full_name,
      role: user.role,
      is_active: user.is_active,
      phone_number: user.phone_number || '',
      password: '' // Don't pre-fill password for security
    });
  };

  // Handle user role update
  const handleUpdateRole = async (userId, role) => {
    try {
      await updateUserRoleMutation.mutateAsync({ id: userId, role });
    } catch (error) {
      console.error('Error updating user role:', error);
      alert('Failed to update user role. Please try again.');
    }
  };

  // Handle user status update
  const handleUpdateStatus = async (userId, isActive) => {
    try {
      await updateUserStatusMutation.mutateAsync({ id: userId, isActive });
    } catch (error) {
      console.error('Error updating user status:', error);
      alert('Failed to update user status. Please try again.');
    }
  };

  if (isLoading) return <div className="flex items-center justify-center h-64">Loading users...</div>;
  if (error) return <div className="p-4 bg-red-50 border border-red-200 text-red-600 rounded">Error: {error.message}</div>;

  const users = usersData?.data || [];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-md">
        <div className="flex items-center justify-between px-6 py-4">
          <h1 className="text-2xl font-bold text-gray-800">
            <FiUsers /> User Management
          </h1>
          <div className="flex items-center space-x-4">
            <button
              onClick={() => {
                setSelectedUser(null);
                setFormData({
                  email: '',
                  full_name: '',
                  role: '',
                  is_active: true,
                  phone_number: '',
                  password: ''
                });
              }}
              className="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded transition-colors"
            >
              <FiPlus /> New User
            </button>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 py-6">
        {/* User Form */}
        <div className="bg-white rounded-lg shadow-md mb-6">
          <div className="px-6 py-4 border-b">
            <h2 className="text-xl font-semibold text-gray-800">
              {selectedUser ? 'Edit User' : 'Create New User'}
            </h2>
          </div>
          <form onSubmit={handleSubmit} className="px-6 py-4 space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Email
                </label>
                <input
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Enter email address"
                  type="email"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Full Name
                </label>
                <input
                  name="full_name"
                  value={formData.full_name}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Enter full name"
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Role
                </label>
                <select
                  name="role"
                  value={formData.role}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select Role</option>
                  <option value="commuter">Commuter</option>
                  <option value="responder">Responder</option>
                  <option value="mio_staff">MIO Staff</option>
                  <option value="moderator">Moderator</option>
                  <option value="admin">Admin</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Status
                </label>
                <select
                  name="is_active"
                  value={formData.is_active.toString()}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="true">Active</option>
                  <option value="false">Inactive</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Phone Number (Optional)
              </label>
              <input
                name="phone_number"
                value={formData.phone_number}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter phone number"
              />
            </div>

            {/* Password field - only show for new users or when explicitly requested */}
            {!selectedUser && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Password
                </label>
                <div className="flex items-center">
                  <input
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    value={formData.password}
                    onChange={handleChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Enter password"
                    required
                    minLength="6"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="ml-2 p-1 bg-gray-200 rounded-md hover:bg-gray-300"
                  >
                    {showPassword ? <FiXCircle size={16} /> : <FiCheckCircle size={16} />}
                  </button>
                </div>
                <p className="text-xs text-gray-500 mt-1">
                  Password must be at least 6 characters
                </p>
              </div>
            )}

            <div className="flex items-center justify-between">
              <button
                type="submit"
                className="bg-green-600 hover:bg-green-700 text-white px-5 py-2 rounded transition-colors"
              >
                {selectedUser ? 'Update User' : 'Create User'}
              </button>
              <button
                type="button"
                onClick={() => {
                  setSelectedUser(null);
                  setFormData({
                    email: '',
                    full_name: '',
                    role: '',
                    is_active: true,
                    phone_number: '',
                    password: ''
                  });
                }}
                className="bg-gray-500 hover:bg-gray-600 text-white px-5 py-2 rounded transition-colors ml-2"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>

        {/* Users List */}
        <div className="bg-white rounded-lg shadow-md">
          <div className="px-6 py-4 border-b">
            <h2 className="text-xl font-semibold text-gray-800">Registered Users</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Email
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Full Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Role
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Phone
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {users.length === 0 ? (
                  <tr>
                    <td className="px-6 py-4 text-center text-gray-500" colSpan="6">
                      No users found
                    </td>
                  </tr>
                ) : (
                  users.map((user) => (
                    <tr key={user.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {user.email}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {user.full_name}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${user.role === 'admin'
                            ? 'bg-blue-100 text-blue-800'
                            : user.role === 'moderator'
                            ? 'bg-purple-100 text-purple-800'
                            : user.role === 'responder'
                            ? 'bg-green-100 text-green-800'
                            : user.role === 'mio_staff'
                            ? 'bg-orange-100 text-orange-800'
                            : 'bg-gray-100 text-gray-800'}`}>
                          {user.role.charAt(0).toUpperCase() + user.role.slice(1)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${user.is_active
                            ? 'bg-green-100 text-green-800'
                            : 'bg-red-100 text-red-800'}`}>
                          {user.is_active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {user.phone_number || 'N/A'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <div className="flex space-x-2">
                          {!selectedUser || selectedUser.id !== user.id ? (
                            <button
                              onClick={() => handleEditUser(user)}
                              className="text-indigo-600 hover:text-indigo-900 px-3"
                            >
                              <FiEdit2 size={16} />
                            </button>
                          ) : (
                            <>
                              <button
                                onClick={() => handleUpdateRole(user.id, formData.role)}
                                disabled={!formData.role}
                                className="text-indigo-600 hover:text-indigo-900 px-3"
                              >
                                <FiCheckCircle size={16} />
                              </button>
                              <button
                                onClick={() => handleUpdateStatus(user.id, formData.is_active)}
                                className={formData.is_active === user.is_active ? 'text-gray-400' : 'text-green-600 hover:text-green-800 px-3'}
                              >
                                {formData.is_active === user.is_active ? <FiCheckCircle size={16} /> : <FiCheckCircle size={16} />}
                              </button>
                            </>
                          )}
                          {(user.role !== 'admin' ||
                            (users.filter(u => u.role === 'admin' && u.is_active).length > 1)) && (
                            <button
                              onClick={() => handleUpdateRole(user.id, 'commuter')}
                              className="text-red-600 hover:text-red-900 px-3"
                              title="Remove admin role"
                            >
                              <FiXCircle size={16} />
                            </button>
                          )}
                          {(user.is_active ||
                            (users.filter(u => u.is_active).length > 1)) && (
                            <button
                              onClick={() => handleUpdateStatus(user.id, false)}
                              className="text-red-600 hover:text-red-900 px-3"
                              title="Deactivate user"
                            >
                              <fi-XCircle size={16} />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminUsersPage;