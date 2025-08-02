import React, { useState, useEffect } from 'react';
import PageLayout from '../components/PageLayout';
import { useNavigate } from 'react-router-dom';
import { getProjects } from '../services/projectApi';
import type { Project } from '../types/project';


export default function ProjectListPage() {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [projects, setProjects] = useState<Project[]>([]);
  const pageSize = 25;
  const navigate = useNavigate(); // Already present in the original code

  useEffect(() => {
    getProjects(search, page).then((data) => setProjects(data));
  }, [search, page]);

  const paged = projects.slice((page - 1) * pageSize, page * pageSize);
  const totalPages = Math.ceil(projects.length / pageSize);

  return (
    <PageLayout>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>Projects</h2>
        <button className="btn btn-primary" onClick={() => navigate('/projects/new')}>Create</button>
      </div>
      <input
        className="form-control mb-3"
        placeholder="Search a project..."
        value={search}
        onChange={e => setSearch(e.target.value)}
      />
      <div className="row g-3">
        {paged.map((p: Project) => (
          <div className="col-md-4" key={p.id}>
            <div className="card h-100" style={{ cursor: 'pointer' }} onClick={() => navigate(`/projects/${p.id}/tables`)}>
              <div className="card-body">
                <h5 className="card-title">{p.name.length > 10 ? p.name.slice(0, 10) + '...' : p.name}</h5>
                <p className="card-text text-muted">{p.description.length > 120 ? p.description.slice(0, 120) + '...' : p.description}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
      <div className="d-flex justify-content-center mt-4 gap-2">
        <button className="btn btn-outline-secondary" disabled={page === 1} onClick={() => setPage(page - 1)}>Previous</button>
        <button className="btn btn-outline-secondary" disabled={page === totalPages} onClick={() => setPage(page + 1)}>Next</button>
      </div>
    </PageLayout>
  );
}
