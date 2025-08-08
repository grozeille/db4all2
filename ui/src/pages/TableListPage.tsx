import { useEffect, useState } from 'react';
import PageLayout from '../components/PageLayout';
import { useParams, useNavigate } from 'react-router-dom';
import { useLocation } from 'react-router-dom';
import { getTables } from '../services/tableApi';
import { getProject } from '../services/projectApi';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import type { Table } from '../types/table';
import type { Page } from '../types/page';

export default function TableListPage() {
  const { projectId } = useParams();
  const [tablesPage, setTablesPage] = useState<Page<Table> | null>(null);
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search, 2000);
  const [page, setPage] = useState(0);
  const pageSize = 25;
  const [project, setProject] = useState<any>(null);
  const navigate = useNavigate();
  const location = useLocation();
  // Récupère le nom du projet depuis l'objet ou le state
  const projectName = project?.name || location.state?.projectName || '';

  useEffect(() => {
    if (projectId) {
      getTables(projectId as string, debouncedSearch, { page, size: pageSize }).then(setTablesPage);
      getProject(projectId as string)
        .then(setProject)
        .catch(err => {
          navigate(`/error/404`, { state: { message: err.message } });
        });
    }
  }, [projectId, debouncedSearch, navigate, page, pageSize]);

  // Debounce la recherche
  const tables = tablesPage ? tablesPage.content : [];
  const totalPages = tablesPage ? tablesPage.totalPages : 0;

  return (
    <PageLayout>
      <div className="d-flex align-items-start justify-content-between mb-4">
        <h2 className="mb-0">Tables of project {projectName || projectId}</h2>
        <div>
          <button className="btn btn-primary me-2" onClick={() => navigate(`/projects/${projectId}/settings`, { state: { projectName } })}>Settings</button>
          <button
            className="btn btn-primary"
            onClick={() => navigate(`/projects/${projectId}/tables/new`, { state: { projectName } })}
          >
            Add Table
          </button>
        </div>
      </div>
      <input
        className="form-control mb-3"
        placeholder="Search a table..."
        value={search}
        onChange={e => setSearch(e.target.value)}
      />
      <div className="row g-3">
        {tables.map((t: any) => (
          <div className="col-md-4" key={t.id}>
            <div className="card h-100" style={{ cursor: 'pointer' }} onClick={() => navigate(`/projects/${projectId}/tables/${t.id}/content`, { state: { projectName, tableName: t.name } })}>
              <div className="card-body">
                <h5 className="card-title">{t.name}</h5>
                <p className="card-text text-muted">{t.description}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
      <div className="d-flex justify-content-center mt-4 gap-2">
        <button className="btn btn-outline-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
        <button className="btn btn-outline-secondary" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Next</button>
      </div>
    </PageLayout>
  );
      
}
