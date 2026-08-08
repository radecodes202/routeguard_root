import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import AdvisoriesPage from './pages/advisories/AdvisoriesPage';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <div className="App">
        <Routes>
          <Route path="/" element={<Navigate replace to="/advisories" />} />
          <Route path="/advisories" element={<AdvisoriesPage />} />
          {/* Add other routes here as they are implemented */}
          <Route path="*" element={<div className="p-8 text-center">404 - Page Not Found</div>} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;