import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, useParams, useNavigate } from 'react-router-dom';
import ErrorPage from './pages/ErrorPage';
import ProjectListPage from './pages/ProjectListPage';
import ProjectCreatePage from './pages/ProjectCreatePage';
import ProjectSettingsPage from './pages/ProjectSettingsPage';
import TableListPage from './pages/TableListPage';
import TableContentPage from './pages/TableContentPage';
import TableEditPage from './pages/TableEditPage';
import LoginPage from './pages/LoginPage';
import SetupPage from './pages/SetupPage'; // Import the new setup page
import { ProfilePage } from "./pages/ProfilePage.tsx";
import { AdminPage } from "./pages/AdminPage.tsx";
import { AppHeader } from './components/AppHeader';

// A wrapper for all routes that require authentication
const ProtectedRoutes = () => {
    const navigate = useNavigate();
    const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('token'));

    const handleLogout = () => {
        localStorage.removeItem('token');
        setIsAuthenticated(false);
    };

    useEffect(() => {
        if (!isAuthenticated) {
            navigate('/login');
        }
    }, [isAuthenticated, navigate]);

    // Listen for storage changes to log out from other tabs
    useEffect(() => {
        const handleStorageChange = () => {
            if (!localStorage.getItem('token')) {
                setIsAuthenticated(false);
            }
        };
        window.addEventListener('storage', handleStorageChange);
        return () => window.removeEventListener('storage', handleStorageChange);
    }, []);

    if (!isAuthenticated) {
        // This will render briefly before the navigate effect kicks in
        return null;
    }

    return (
        <>
            <AppHeader onLogout={handleLogout} />
            <main className="container-fluid py-4" style={{ minHeight: '100vh' }}>
                <Routes>
                    <Route path="/" element={<ProjectListPage />} />
                    <Route path="/projects" element={<ProjectListPage />} />
                    <Route path="/projects/new" element={<ProjectCreatePage />} />
                    <Route path="/projects/:projectId/settings" element={<ProjectSettingsPage />} />
                    <Route path="/projects/:projectId/tables" element={<TableListPage />} />
                    <Route path="/projects/:projectId/tables/new" element={<TableEditPage />} />
                    <Route path="/projects/:projectId/tables/:tableId/content" element={<TableContentPage />} />
                    <Route path="/projects/:projectId/tables/:tableId/settings" element={<TableEditPage />} />
                    <Route path="/profile" element={<ProfilePage />} />
                    <Route path="/admin" element={<AdminPage />} />
                    <Route path="/error/:code" element={<ErrorRoute />} />
                    <Route path="*" element={<ErrorPage code={404} />} />
                </Routes>
            </main>
        </>
    );
};

// The main App component now acts as the router
function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/setup" element={<SetupPage />} />
                <Route path="/*" element={<ProtectedRoutes />} />
            </Routes>
        </BrowserRouter>
    );
}

// Helper to extract route params for the error page
function ErrorRoute() {
    const { code } = useParams();
    return <ErrorPage code={Number(code)} />;
}

export default App;
