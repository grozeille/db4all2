import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProjects } from '../services/projectApi';
import type { Project } from '../types/project';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import type { Page } from '../types/page';


export default function ProjectListPage() {
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search, 2000);
  const [page, setPage] = useState(0);
  const [projectsPage, setProjectsPage] = useState<Page<Project> | null>(null);
  const pageSize = 25;
  const navigate = useNavigate(); // Already present in the original code

  const [error, setError] = useState('');
  useEffect(() => {
    setError('');
    getProjects(debouncedSearch, { page, size: pageSize })
      .then((data) => setProjectsPage(data))
      .catch((err: any) => setError(err.message || 'Unknown error'));
  }, [debouncedSearch, page, pageSize]);

  // Debounce la recherche

  const paged = projectsPage ? projectsPage.content : [];
  const totalPages = projectsPage ? projectsPage.totalPages : 0;

  const getProjectName = (project: Project) => project.name || 'Untitled project';
  const getProjectDescription = (project: Project) => project.description || 'No description';

  return (
    <>
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
      {error && <div className="text-danger mb-2">{error}</div>}
      <div className="row g-3">
        {paged.map((p: Project) => (
          <div className="col-md-4" key={p.id}>
            <div className="card h-100" style={{ cursor: 'pointer' }} onClick={() => navigate(`/projects/${p.id}/tables`, { state: { projectName: p.name } })}>
              <div className="card-body">
                <h5 className="card-title">{getProjectName(p).length > 10 ? getProjectName(p).slice(0, 10) + '...' : getProjectName(p)}</h5>
                <p className="card-text text-muted">{getProjectDescription(p).length > 120 ? getProjectDescription(p).slice(0, 120) + '...' : getProjectDescription(p)}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
      <div className="d-flex justify-content-center mt-4 gap-2">
        <button className="btn btn-outline-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
        <button className="btn btn-outline-secondary" disabled={page + 1 === totalPages} onClick={() => setPage(page + 1)}>Next</button>
      </div>
    </>
  );
}
