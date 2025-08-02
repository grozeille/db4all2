import React from 'react';
import Dropdown from 'react-bootstrap/Dropdown';

export default function AppHeader() {
  return (
    <header className="p-3 border-bottom w-100 bg-white" style={{ marginBottom: '10px' }}>
      <div className="container-fluid">
        <div className="d-flex flex-wrap align-items-center justify-content-center justify-content-lg-start">
          <ul className="nav col-12 col-lg-auto me-lg-auto mb-2 justify-content-center mb-md-0">
            <li><a href="/projects" className="nav-link px-2 link-body-emphasis">Projects</a></li>
          </ul>
          <Dropdown align="end">
            <Dropdown.Toggle variant="link" className="d-block link-body-emphasis text-decoration-none p-0 border-0" id="dropdown-user">
              <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" className="bi bi-person-circle" viewBox="0 0 16 16">
                <path d="M13.468 12.37C12.758 11.226 11.383 10.5 8 10.5c-3.383 0-4.758.726-5.468 1.87A6.987 6.987 0 0 0 8 15a6.987 6.987 0 0 0 5.468-2.63z"/>
                <path fillRule="evenodd" d="M8 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm2-3a2 2 0 1 1-4 0 2 2 0 0 1 4 0z"/>
                <path fillRule="evenodd" d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1zm0 13A6 6 0 1 1 8 2a6 6 0 0 1 0 12z"/>
              </svg>
            </Dropdown.Toggle>
            <Dropdown.Menu className="text-small">
              <Dropdown.Item href="#">New project...</Dropdown.Item>
              <Dropdown.Item href="#">Settings</Dropdown.Item>
              <Dropdown.Item href="#">Profile</Dropdown.Item>
              <Dropdown.Divider />
              <Dropdown.Item href="#">Sign out</Dropdown.Item>
            </Dropdown.Menu>
          </Dropdown>
        </div>
      </div>
    </header>
  );
}
