import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ProjectListPage from './pages/ProjectListPage';
import ProjectSettingsPage from './pages/ProjectSettingsPage';
import TableListPage from './pages/TableListPage';
import TableContentPage from './pages/TableContentPage';
import TableEditPage from './pages/TableEditPage';




function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ProjectListPage />} />
        <Route path="/projects" element={<ProjectListPage />} />
        <Route path="/projects/:projectId/settings" element={<ProjectSettingsPage />} />
        <Route path="/projects/:projectId/tables" element={<TableListPage />} />
        <Route path="/projects/:projectId/tables/new" element={<TableEditPage />} />
        <Route path="/projects/:projectId/tables/:tableId/content" element={<TableContentPage />} />
        <Route path="/projects/:projectId/tables/:tableId/settings" element={<TableEditPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
