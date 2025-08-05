import { BrowserRouter, Routes, Route, useParams } from 'react-router-dom';
import ErrorPage from './pages/ErrorPage';
import ProjectListPage from './pages/ProjectListPage';
import ProjectCreatePage from './pages/ProjectCreatePage';
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
        <Route path="/projects/new" element={<ProjectCreatePage />} />
        <Route path="/projects/:projectId/settings" element={<ProjectSettingsPage />} />
        <Route path="/projects/:projectId/tables" element={<TableListPage />} />
        <Route path="/projects/:projectId/tables/new" element={<TableEditPage />} />
        <Route path="/projects/:projectId/tables/:tableId/content" element={<TableContentPage />} />
        <Route path="/projects/:projectId/tables/:tableId/settings" element={<TableEditPage />} />
        <Route path="/error/:code" element={<ErrorRoute />} />
        <Route path="*" element={<ErrorPage code={404} />} />
      </Routes>
    </BrowserRouter>
  );
}
// Route wrapper pour passer le code d'erreur
function ErrorRoute() {
  const { code } = useParams();
  return <ErrorPage code={Number(code)} />;
}
export default App;
