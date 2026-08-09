import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import AdvisoriesPage from './pages/advisories/AdvisoriesPage';
import AdminUsersPage from './pages/admin/AdminUsersPage';
import AuditLogsPage from './pages/auditlogs/AuditLogsPage';
import NotificationCenter from './pages/notifications/NotificationCenter';
import ModerationQueuePage from './pages/moderation/ModerationQueuePage';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <div className="App">
        <Routes>
          <Route path="/" element={<Navigate replace to="/advisories" />} />
          <Route path="/advisories" element={<AdvisoriesPage />} />
          <Route path="/admin/users" element={<AdminUsersPage />} />
          <Route path="/admin/auditlogs" element={<AuditLogsPage />} />
          <Route path="/notifications" element={<NotificationCenter />} />
          <Route path="/moderation" element={<ModerationQueuePage />} />
          {/* Add other routes here as they are implemented */}
          <Route path="*" element={<div className="p-8 text-center">404 - Page Not Found</div>} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;