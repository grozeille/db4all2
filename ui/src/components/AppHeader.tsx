import React from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { getProject } from '../services/projectApi';
import { getTable } from '../services/tableApi';
import Breadcrumb from 'react-bootstrap/Breadcrumb';
import Dropdown from 'react-bootstrap/Dropdown';

export default function AppHeader() {
  // Style global pour le breadcrumb dans le header
  React.useEffect(() => {
    const style = document.createElement('style');
    style.innerHTML = '.app-header-breadcrumb .breadcrumb { margin-bottom: 0 !important; }';
    document.head.appendChild(style);
    return () => { document.head.removeChild(style); };
  }, []);

  // Breadcrumb dynamique
  const location = useLocation();
  const params = useParams();
  const pathnames = location.pathname.split('/').filter(Boolean);
  const [projectName, setProjectName] = useState<string | null>(null);
  const [tableName, setTableName] = useState<string | null>(null);

  useEffect(() => {
    if (params.projectId) {
      getProject(params.projectId).then(p => setProjectName(p.name)).catch(() => setProjectName(null));
    } else {
      setProjectName(null);
    }
    if (params.projectId && params.tableId) {
      getTable(params.projectId, params.tableId).then(t => setTableName(t.name)).catch(() => setTableName(null));
    } else {
      setTableName(null);
    }
  }, [params.projectId, params.tableId]);

  function getBreadcrumbs() {
    const crumbs = [
      { name: 'Projects', href: '/projects' }
    ];
    if (pathnames.length > 1) {
      if (pathnames[1] === 'new') {
        crumbs.push({ name: 'New project', href: location.pathname });
      } else if (pathnames[1]) {
        // Projet
        crumbs.push({ name: projectName || '', href: `/projects/${params.projectId}/tables` });
        if (pathnames[2] === 'settings') {
          crumbs.push({ name: 'Settings', href: location.pathname });
        } else if (pathnames[2] === 'tables') {
          if (pathnames[3] === 'new') {
            crumbs.push({ name: 'New table', href: location.pathname });
          } else if (pathnames[3]) {
            // Table
            crumbs.push({ name: tableName || '', href: `/projects/${params.projectId}/tables/${params.tableId}` });
            if (pathnames[4] === 'settings') {
              crumbs.push({ name: 'Settings', href: location.pathname });
            }
          }
        }
      }
    }
    return crumbs;
  }
  const crumbs = getBreadcrumbs();
  return (
    <header className="p-3 border-bottom w-100 bg-white">
      <div className="container-fluid">
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
              <Dropdown.Item href="#">Profile</Dropdown.Item>
              <Dropdown.Item href="#">Admin.</Dropdown.Item>
              <Dropdown.Divider />
              <Dropdown.Item href="#">Sign out</Dropdown.Item>
            </Dropdown.Menu>
          </Dropdown>
        </div>
      </div>
    </header>
  );
}
