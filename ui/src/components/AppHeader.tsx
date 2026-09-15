import React from 'react';
import { useLocation, useMatch } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { getProject } from '../services/projectApi';
import { getTable } from '../services/tableApi';
import { userApi } from "../services/userApi.ts";
import type { User } from "../types/auth.ts";
import Breadcrumb from 'react-bootstrap/Breadcrumb';
import Dropdown from 'react-bootstrap/Dropdown';

export function AppHeader({ onLogout }: { onLogout: () => void }) {
  // Style global pour le breadcrumb dans le header
  React.useEffect(() => {
    const style = document.createElement('style');
    style.innerHTML = '.app-header-breadcrumb .breadcrumb { margin-bottom: 0 !important; }';
    document.head.appendChild(style);
    return () => { document.head.removeChild(style); };
  }, []);

  // Breadcrumb dynamique
  const location = useLocation();
  const projectListMatch = useMatch('/projects');
  const projectNewMatch = useMatch('/projects/new');
  const projectSettingsMatch = useMatch('/projects/:projectId/settings');
  const projectTablesMatch = useMatch('/projects/:projectId/tables');
  const tableNewMatch = useMatch('/projects/:projectId/tables/new');
  const tableContentMatch = useMatch('/projects/:projectId/tables/:tableId/content');
  const tableSettingsMatch = useMatch('/projects/:projectId/tables/:tableId/settings');
  const routeProjectId = projectSettingsMatch?.params.projectId
    || projectTablesMatch?.params.projectId
    || tableNewMatch?.params.projectId
    || tableContentMatch?.params.projectId
    || tableSettingsMatch?.params.projectId
    || null;
  const routeTableId = tableContentMatch?.params.tableId
    || tableSettingsMatch?.params.tableId
    || null;
  const navigationState = location.state as { projectName?: string; tableName?: string } | null;
  const [projectName, setProjectName] = useState<string | null>(null);
  const [tableName, setTableName] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    userApi.getMe().then(setUser).catch((error) => {
      console.log(error);
    });
  }, []);

  useEffect(() => {
    if (navigationState?.projectName) {
      setProjectName(navigationState.projectName);
    }

    if (routeProjectId) {
      getProject(routeProjectId).then(p => setProjectName(p.name)).catch(() => setProjectName(navigationState?.projectName ?? null));
    } else {
      setProjectName(null);
    }

    if (navigationState?.tableName) {
      setTableName(navigationState.tableName);
    }

    if (routeProjectId && routeTableId) {
      getTable(routeProjectId, routeTableId).then(t => setTableName(t.name)).catch(() => setTableName(navigationState?.tableName ?? null));
    } else {
      setTableName(null);
    }
  }, [navigationState?.projectName, navigationState?.tableName, routeProjectId, routeTableId]);

  function getBreadcrumbs() {
    const crumbs = [
      { name: 'Projects', href: '/projects' }
    ];
    if (projectListMatch) {
      return crumbs;
    }

    if (projectNewMatch) {
      crumbs.push({ name: 'New project', href: location.pathname });
      return crumbs;
    }

    if (routeProjectId) {
      crumbs.push({ name: projectName || routeProjectId, href: `/projects/${routeProjectId}/tables` });
    }

    if (projectSettingsMatch) {
      crumbs.push({ name: 'Settings', href: location.pathname });
      return crumbs;
    }

    if (projectTablesMatch) {
      crumbs.push({ name: 'Tables', href: location.pathname });
      return crumbs;
    }

    if (tableNewMatch) {
      crumbs.push({ name: 'Tables', href: `/projects/${routeProjectId}/tables` });
      crumbs.push({ name: 'New table', href: location.pathname });
      return crumbs;
    }

    if (routeProjectId && routeTableId) {
      crumbs.push({ name: 'Tables', href: `/projects/${routeProjectId}/tables` });
      crumbs.push({ name: tableName || routeTableId, href: `/projects/${routeProjectId}/tables/${routeTableId}/content` });
    }

    if (tableSettingsMatch) {
      crumbs.push({ name: 'Settings', href: location.pathname });
    }

    return crumbs;
  }
  const crumbs = getBreadcrumbs();

  return (
    <header className="p-3 border-bottom w-100 bg-white">
      <div className="w-100">
        <div className="d-flex flex-wrap align-items-center justify-content-between">
          <div className="d-flex align-items-center gap-3 app-header-breadcrumb">
            <Breadcrumb className="mb-0 align-items-center d-flex mb-0">
              {crumbs.map((crumb, idx) => (
                idx < crumbs.length - 1 ? (
                  <Breadcrumb.Item key={idx} href={crumb.href}>{crumb.name}</Breadcrumb.Item>
                ) : (
                  <Breadcrumb.Item key={idx} active>{crumb.name}</Breadcrumb.Item>
                )
              ))}
            </Breadcrumb>
          </div>
          <Dropdown align="end">
            <Dropdown.Toggle variant="link" className="d-block link-body-emphasis text-decoration-none p-0 border-0" id="dropdown-user">
              <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" className="bi bi-person-circle" viewBox="0 0 16 16">
                <path d="M13.468 12.37C12.758 11.226 11.383 10.5 8 10.5c-3.383 0-4.758.726-5.468 1.87A6.987 6.987 0 0 0 8 15a6.987 6.987 0 0 0 5.468-2.63z"/>
                <path fillRule="evenodd" d="M8 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm2-3a2 2 0 1 1-4 0 2 2 0 0 1 4 0z"/>
                <path fillRule="evenodd" d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1zm0 13A6 6 0 1 1 8 2a6 6 0 0 1 0 12z"/>
              </svg>
            </Dropdown.Toggle>
            <Dropdown.Menu className="text-small">
              <Dropdown.Item href="/profile">Profile</Dropdown.Item>
              {user?.superAdmin && <Dropdown.Item href="/admin">Admin.</Dropdown.Item>}
              <Dropdown.Divider />
              <Dropdown.Item onClick={onLogout}>Sign out</Dropdown.Item>
            </Dropdown.Menu>
          </Dropdown>
        </div>
      </div>
    </header>
  );
}
