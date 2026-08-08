import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { FiTrash2, FiEdit2, FiPlus } from 'react-icons/fi';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';

const AdvisoriesPage = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [selectedAdvisory, setSelectedAdvisory] = useState(null);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    road_name: '',
    starts_at: new Date().toISOString().slice(0, 16),
    ends_at: '',
    location: '', // WKT string: 'POINT(longitude latitude)'
  });
  const [token, setToken] = useState(null);
  const [map, setMap] = useState(null);
  const mapRef = useRef(null);

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

  // Fetch advisories
  const { data: advisoriesData, isLoading, error } = useQuery({
    queryKey: ['advisories'],
    queryFn: async () => {
      const response = await fetch(`${API_URL}/advisories`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!response.ok) throw new Error('Failed to fetch advisories');
      return response.json();
    },
  });

  // Create advisory mutation
  const createAdvisoryMutation = useMutation({
    mutationFn: async (advisoryData) => {
      const response = await fetch(`${API_URL}/advisories`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(advisoryData),
      });
      if (!response.ok) throw new Error('Failed to create advisory');
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['advisories']);
      setFormData({
        title: '',
        description: '',
        road_name: '',
        starts_at: new Date().toISOString().slice(0, 16),
        ends_at: '',
        location: '',
      });
      setSelectedAdvisory(null);
      // Reset map view to default
      if (mapRef.current) {
        mapRef.current.flyTo({
          center: [125.0050, 11.2500], // Default to Tacloban City
          zoom: 13,
          essential: true,
        });
      }
    },
  });

  // Update advisory mutation
  const updateAdvisoryMutation = useMutation({
    mutationFn: async ({ id, data }) => {
      const response = await fetch(`${API_URL}/advisories/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(data),
      });
      if (!response.ok) throw new Error('Failed to update advisory');
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['advisories']);
      setFormData({
        title: '',
        description: '',
        road_name: '',
        starts_at: new Date().toISOString().slice(0, 16),
        ends_at: '',
        location: '',
      });
      setSelectedAdvisory(null);
      // Reset map view to default
      if (mapRef.current) {
        mapRef.current.flyTo({
          center: [125.0050, 11.2500],
          zoom: 13,
          essential: true,
        });
      }
    },
  });

  // Delete advisory mutation
  const deleteAdvisoryMutation = useMutation({
    mutationFn: async (id) => {
      const response = await fetch(`${API_URL}/advisories/${id}`, {
        method: 'DELETE',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!response.ok) throw new Error('Failed to delete advisory');
      return response;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['advisories']);
    },
  });

  // Handle form change (excluding location which is set via map)
  const handleChange = (e) => {
    const { name, value, type } = e.target;
    if (type === 'checkbox') {
      setFormData(prev => ({ ...prev, [name]: value }));
    } else {
      setFormData(prev => ({ ...prev, [name]: value }));
    }
  };

  // Handle form submit
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (selectedAdvisory) {
        // Update existing advisory
        await updateAdvisoryMutation.mutateAsync({
          id: selectedAdvisory.id,
          data: {
            ...formData,
            ends_at: formData.ends_at || null,
          },
        });
      } else {
        // Create new advisory
        await createAdvisoryMutation.mutateAsync({
          ...formData,
          ends_at: formData.ends_at || null,
        });
      }
    } catch (error) {
      console.error('Error submitting form:', error);
      alert('Failed to save advisory. Please try again.');
    }
  };

  // Handle advisory selection for editing
  const handleEditAdvisory = (advisory) => {
    setSelectedAdvisory(advisory);
    // Parse location from WKT to set map view
    const locationCoords = parseLocationWKT(advisory.location);
    setFormData({
      title: advisory.title,
      description: advisory.description,
      road_name: advisory.road_name || '',
      starts_at: advisory.starts_at.slice(0, 16),
      ends_at: advisory.ends_at ? advisory.ends_at.slice(0, 16) : '',
      location: advisory.location || '', // Keep the WKT string
    });
    // Update map view and marker
    if (mapRef.current && locationCoords) {
      mapRef.current.flyTo({
        center: [locationCoords.lng, locationCoords.lat],
          zoom: 15,
          essential: true,
      });
      // Remove existing marker and add new one
      if (mapRef.current.getSource('marker')) {
        mapRef.current.getSource('marker').setData({
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [locationCoords.lng, locationCoords.lat],
          },
        });
      } else {
        // Add marker source and layer if not exists
        if (!mapRef.current.getSource('marker')) {
          mapRef.current.addSource('marker', {
            type: 'geojson',
            data: {
              type: 'Feature',
              geometry: {
                type: 'Point',
                coordinates: [locationCoords.lng, locationCoords.lat],
              },
            },
          });
          mapRef.current.addLayer({
            id: 'marker',
            type: 'circle',
            source: 'marker',
            paint: {
              'circle-radius': 10,
              'circle-color': '#007bff',
              'circle-stroke-width': 2,
              'circle-stroke-color': '#fff',
            },
          });
        }
      }
    }
  };

  // Handle advisory deletion
  const handleDeleteAdvisory = async (id) => {
    if (window.confirm('Are you sure you want to delete this advisory?')) {
      try {
        await deleteAdvisoryMutation.mutateAsync(id);
      } catch (error) {
        console.error('Error deleting advisory:', error);
        alert('Failed to delete advisory. Please try again.');
      }
    }
  };

  // Parse WKT point string to extract lng, lat
  const parseLocationWKT = (wkt) => {
    if (!wkt) return null;
    const match = wkt.match(/POINT\s*\(\s*([^,\s]+)\s+([^,\s]+)\s*\)/i);
    if (match) {
      return {
        lng: parseFloat(match[1]),
        lat: parseFloat(match[2]),
      };
    }
    return null;
  };

  // Handle map click to set location
  const handleMapClick = (e) => {
    const lngLat = e.lngLat;
    const lng = lngLat.lng;
    const lat = lngLat.lat;
    // Update formData location
    setFormData(prev => ({
      ...prev,
      location: `POINT(${lng} ${lat})`,
    }));
    // Update marker on map
    if (mapRef.current) {
      if (mapRef.current.getSource('marker')) {
        mapRef.current.getSource('marker').setData({
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [lng, lat],
          },
        });
      } else {
        // Add marker source and layer
        mapRef.current.addSource('marker', {
          type: 'geojson',
          data: {
            type: 'Feature',
            geometry: {
              type: 'Point',
              coordinates: [lng, lat],
            },
          },
        });
        mapRef.current.addLayer({
          id: 'marker',
          type: 'circle',
          source: 'marker',
          paint: {
            'circle-radius': 10,
            'circle-color': '#007bff',
            'circle-stroke-width': 2,
            'circle-stroke-color': '#fff',
          },
        });
      }
    }
  };

  // Initialize map
  useEffect(() => {
    if (mapRef.current) {
      const mapInstance = new maplibregl.Map({
        container: mapRef.current,
        style: 'https://demotiles.maplibre.org/style.json',
        center: [125.0050, 11.2500], // Default to Tacloban City
        zoom: 13,
      });

      // Add navigation controls
      mapInstance.addControl(new maplibregl.NavigationControl());

      // Set the map instance
      setMap(mapInstance);
      mapRef.current = mapInstance;

      // Add click event to set location
      mapInstance.on('click', handleMapClick);

      // Cleanup on unmount
      return () => {
        mapInstance.off('click', handleMapClick);
        mapInstance.remove();
      };
    }
  }, []);

  // Update map when selectedAdvisory changes (for editing)
  useEffect(() => {
    if (selectedAdvisory && mapRef.current) {
      const locationCoords = parseLocationWKT(selectedAdvisory.location);
      if (locationCoords) {
        mapRef.current.flyTo({
          center: [locationCoords.lng, locationCoords.lat],
          zoom: 15,
          essential: true,
        });
        // Update marker
        if (mapRef.current.getSource('marker')) {
          mapRef.current.getSource('marker').setData({
            type: 'Feature',
            geometry: {
              type: 'Point',
              coordinates: [locationCoords.lng, locationCoords.lat],
            },
          });
        } else {
          mapRef.current.addSource('marker', {
            type: 'geojson',
            data: {
              type: 'Feature',
              geometry: {
                type: 'Point',
                coordinates: [locationCoords.lng, locationCoords.lat],
              },
            },
          });
          mapRef.current.addLayer({
            id: 'marker',
            type: 'circle',
            source: 'marker',
            paint: {
              'circle-radius': 10,
              'circle-color': '#007bff',
              'circle-stroke-width': 2,
              'circle-stroke-color': '#fff',
            },
          });
        }
      }
    }
  }, [selectedAdvisory]);

  if (isLoading) return <div className="flex items-center justify-center h-64">Loading advisories...</div>;
  if (error) return <div className="p-4 bg-red-50 border border-red-200 text-red-600 rounded">Error: {error.message}</div>;

  const advisories = advisoriesData?.advisories || [];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-md">
        <div className="flex items-center justify-between px-6 py-4">
          <h1 className="text-2xl font-bold text-gray-800">Advisory Management</h1>
          <button
            onClick={() => {
              setSelectedAdvisory(null);
              setFormData({
                title: '',
                description: '',
                road_name: '',
                starts_at: new Date().toISOString().slice(0, 16),
                ends_at: '',
                location: '',
              });
              // Reset map view to default
              if (mapRef.current) {
                mapRef.current.flyTo({
                  center: [125.0050, 11.2500],
                  zoom: 13,
                  essential: true,
                });
              }
            }}
            className="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded transition-colors"
          >
            <FiPlus /> New Advisory
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 py-6">
        {/* Advisory Form */}
        <div className="bg-white rounded-lg shadow-md mb-6">
          <div className="px-6 py-4 border-b">
            <h2 className="text-xl font-semibold text-gray-800">
              {selectedAdvisory ? 'Edit Advisory' : 'Create New Advisory'}
            </h2>
          </div>
          <form onSubmit={handleSubmit} className="px-6 py-4 space-y-6">
            {/* Map Container */}
            <div className="rounded-lg shadow-md h-96 mb-4">
              <div ref={mapRef} className="w-full h-full" />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Title
                </label>
                <input
                  name="title"
                  value={formData.title}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Enter advisory title"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Road Name (Optional)
                </label>
                <input
                  name="road_name"
                  value={formData.road_name}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Enter road name"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 h-24"
                placeholder="Enter advisory description"
                required
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Start Time
                </label>
                <input
                  name="starts_at"
                  type="datetime-local"
                  value={formData.starts_at}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  End Time (Optional)
                </label>
                <input
                  name="ends_at"
                  type="datetime-local"
                  value={formData.ends_at}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div className="flex items-center justify-between">
              <button
                type="submit"
                className="bg-green-600 hover:bg-green-700 text-white px-5 py-2 rounded transition-colors"
              >
                {selectedAdvisory ? 'Update Advisory' : 'Create Advisory'}
              </button>
              <button
                type="button"
                onClick={() => {
                  setSelectedAdvisory(null);
                  setFormData({
                    title: '',
                    description: '',
                    road_name: '',
                    starts_at: new Date().toISOString().slice(0, 16),
                    ends_at: '',
                    location: '',
                  });
                  // Reset map view to default
                  if (mapRef.current) {
                    mapRef.current.flyTo({
                      center: [125.0050, 11.2500],
                      zoom: 13,
                      essential: true,
                    });
                  }
                }}
                className="bg-gray-500 hover:bg-gray-600 text-white px-5 py-2 rounded transition-colors ml-2"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>

        {/* Advisories List */}
        <div className="bg-white rounded-lg shadow-md">
          <div className="px-6 py-4 border-b">
            <h2 className="text-xl font-semibold text-gray-800">Active Advisories</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Title
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Road
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Created By
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {advisories.length === 0 ? (
                  <tr>
                    <td className="px-6 py-4 text-center text-gray-500" colSpan="5">
                      No advisories found
                    </td>
                  </tr>
                ) : (
                  advisories.map((advisory) => (
                    <tr key={advisory.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {advisory.title}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {advisory.road_name || 'N/A'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${advisory.status === 'active'
                            ? 'bg-green-100 text-green-800'
                            : advisory.status === 'removed'
                            ? 'bg-gray-100 text-gray-800'
                            : 'bg-yellow-100 text-yellow-800'}`}>
                          {advisory.status.charAt(0).toUpperCase() + advisory.status.slice(1)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {advisory.created_by || 'System'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <button
                          onClick={() => handleEditAdvisory(advisory)}
                          className="text-indigo-600 hover:text-indigo-900 px-3"
                        >
                          <FiEdit2 size={16} />
                        </button>
                        <button
                          onClick={() => handleDeleteAdvisory(advisory.id)}
                          className="text-red-600 hover:text-red-900 px-3 ml-2"
                        >
                          <FiTrash2 size={16} />
                        </button>
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

export default AdvisoriesPage;