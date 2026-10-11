import { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useLocation } from 'react-router-dom';
import { Alert, Button, ButtonGroup, Spinner } from 'react-bootstrap';
import { getTables } from '../services/tableApi';
import { deleteView, getViews } from '../services/viewApi';
import { getProject } from '../services/projectApi';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import type { Table } from '../types/table';
import type { View } from '../types/view';
import type { Project } from '../types/project';

type KindFilter = 'ALL' | 'TABLE' | 'VIEW';

type ListItem =
  | { kind: 'table'; id: string; name: string; description: string }
  | { kind: 'view'; id: string; name: string; description: string; view: View };

const FETCH_SIZE = 100;

export default function TableListPage() {
  const { projectId } = useParams();
  const [tables, setTables] = useState<Table[]>([]);
  const [views, setViews] = useState<View[]>([]);
  const [tablesTruncated, setTablesTruncated] = useState(false);
  const [viewsTruncated, setViewsTruncated] = useState(false);
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search, 400);
  const [kindFilter, setKindFilter] = useState<KindFilter>('ALL');
  const [page, setPage] = useState(0);
  const pageSize = 12;
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [project, setProject] = useState<Project | null>(null);
  const navigate = useNavigate();
  const location = useLocation();
  // Récupère le nom du projet depuis l'objet ou le state
  const projectName = project?.name || location.state?.projectName || '';

  useEffect(() => {
    if (projectId) {
      getProject(projectId as string)
        .then(setProject)
        .catch(err => {
          navigate(`/error/404`, { state: { message: err.message } });
        });
    }
  }, [projectId, navigate]);

  useEffect(() => {
    if (!projectId) {
      return;
    }
    setLoading(true);
    setError('');
    Promise.all([
      getTables(projectId as string, debouncedSearch, { page: 0, size: FETCH_SIZE }),
      getViews(projectId as string, debouncedSearch, { page: 0, size: FETCH_SIZE }),
    ])
      .then(([tablesPage, viewsPage]) => {
        setTables(tablesPage.content);
        setViews(viewsPage.content);
        setTablesTruncated(tablesPage.totalElements > tablesPage.content.length);
        setViewsTruncated(viewsPage.totalElements > viewsPage.content.length);
      })
      .catch(err => setError(err instanceof Error ? err.message : 'Unable to load tables and views'))
      .finally(() => setLoading(false));
  }, [projectId, debouncedSearch]);

  const items = useMemo<ListItem[]>(() => {
    const merged: ListItem[] = [];
    if (kindFilter !== 'VIEW') {
      tables.forEach((table) => merged.push({ kind: 'table', id: table.id, name: table.name, description: table.description }));
    }
    if (kindFilter !== 'TABLE') {
      views.forEach((view) => merged.push({ kind: 'view', id: view.id, name: view.name, description: view.description, view }));
    }
    merged.sort((left, right) => left.name.localeCompare(right.name));
    return merged;
  }, [tables, views, kindFilter]);

  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const safePage = Math.min(page, totalPages - 1);
  const pageItems = items.slice(safePage * pageSize, safePage * pageSize + pageSize);
  const showTruncatedHint = (kindFilter !== 'VIEW' && tablesTruncated) || (kindFilter !== 'TABLE' && viewsTruncated);

  const handleSearchChange = (value: string) => {
    setSearch(value);
    setPage(0);
  };

  const handleKindFilterChange = (filter: KindFilter) => {
    setKindFilter(filter);
    setPage(0);
  };

  const handleDeleteView = async (event: React.MouseEvent, view: View) => {
    event.stopPropagation();
    if (!projectId) {
      return;
    }
    if (!window.confirm(`Delete view "${view.name}"?`)) {
      return;
    }
    try {
      await deleteView(projectId, view.id);
      setViews((current) => current.filter((candidate) => candidate.id !== view.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to delete view');
    }
  };

  return (
    <>
      <div className="d-flex align-items-start justify-content-between mb-4">
        <h2 className="mb-0">Tables and views of project {projectName || projectId}</h2>
        <div>
          {project?.administrator && (
            <button className="btn btn-primary me-2" onClick={() => navigate(`/projects/${projectId}/settings`, { state: { projectName } })}>Settings</button>
          )}
          <button
            className="btn btn-primary"
            onClick={() => navigate(`/projects/${projectId}/tables/new`, { state: { projectName } })}
          >
            Add Table
          </button>
        </div>
      </div>
      {error && <Alert variant="danger">{error}</Alert>}
      <div className="d-flex gap-3 mb-3 align-items-center flex-wrap">
        <input
          className="form-control"
          style={{ maxWidth: 400 }}
          placeholder="Search a table or a view..."
          value={search}
          onChange={e => handleSearchChange(e.target.value)}
        />
        <ButtonGroup>
          <Button variant={kindFilter === 'ALL' ? 'primary' : 'outline-primary'} onClick={() => handleKindFilterChange('ALL')}>
            All
          </Button>
          <Button variant={kindFilter === 'TABLE' ? 'primary' : 'outline-primary'} onClick={() => handleKindFilterChange('TABLE')}>
            Tables
          </Button>
          <Button variant={kindFilter === 'VIEW' ? 'primary' : 'outline-primary'} onClick={() => handleKindFilterChange('VIEW')}>
            Views
          </Button>
        </ButtonGroup>
        {loading && <Spinner size="sm" />}
      </div>
      {showTruncatedHint && (
        <Alert variant="info">More than {FETCH_SIZE} results, refine your search to see everything.</Alert>
      )}
      <div className="row g-3">
        {pageItems.map((item) => (
          <div className="col-md-4" key={`${item.kind}-${item.id}`}>
            <div
              className="card h-100"
              style={{ cursor: 'pointer' }}
              onClick={() => item.kind === 'table'
                ? navigate(`/projects/${projectId}/tables/${item.id}/content`, { state: { projectName, tableName: item.name } })
                : navigate(`/projects/${projectId}/views/${item.id}/content`, { state: { projectName, viewName: item.name } })}
            >
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start gap-2">
                  <h5 className="card-title mb-0">{item.name}</h5>
                  {item.kind === 'view' && (
                    <button className="btn btn-sm btn-outline-danger" onClick={(event) => handleDeleteView(event, item.view)}>
                      Delete
                    </button>
                  )}
                </div>
                <p className="card-text text-muted">{item.description}</p>
                <div className="d-flex gap-1">
                  {item.kind === 'table'
                    ? <span className="badge bg-secondary">Table</span>
                    : <span className="badge bg-primary">View</span>}
                  {item.kind === 'view' && item.view.parentViewId && <span className="badge bg-info">Linked</span>}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
      {!loading && pageItems.length === 0 && (
        <div className="text-muted mt-2">
          {debouncedSearch || kindFilter !== 'ALL'
            ? 'No tables or views match.'
            : 'No tables yet. Add a table, then open it and use "Save as view" to create views.'}
        </div>
      )}
      <div className="d-flex justify-content-center mt-4 gap-2 align-items-center">
        <button className="btn btn-outline-secondary" disabled={safePage === 0} onClick={() => setPage(safePage - 1)}>Previous</button>
        <span className="text-muted small">Page {safePage + 1} of {totalPages}</span>
        <button className="btn btn-outline-secondary" disabled={safePage + 1 >= totalPages} onClick={() => setPage(safePage + 1)}>Next</button>
      </div>
    </>
  );

}
