
import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import './App.css';
import ProjectListPage from './pages/ProjectListPage';
import ProjectSettingsPage from './pages/ProjectSettingsPage';
import TableListPage from './pages/TableListPage';
import TableContentPage from './pages/TableContentPage';
import TableEditPage from './pages/TableEditPage';

const FAKE_DATASETS: Dataset[] = Array.from({ length: 42 }, (_, i) => ({
  id: i + 1,
  name: `Dataset ${i + 1}`,
  description: `Description du dataset numéro ${i + 1}`,
  tags: ['tag1', 'tag2'],
}));

function fakeFetchDatasets(query: string, page: number, pageSize: number): Promise<{ data: Dataset[]; total: number }> {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = FAKE_DATASETS.filter(ds => ds.name.toLowerCase().includes(query.toLowerCase()));
      const total = filtered.length;
      const start = (page - 1) * pageSize;
      const data = filtered.slice(start, start + pageSize);
      resolve({ data, total });
    }, 400);
  });
}

function Home() {
  const [datasets, setDatasets] = useState<Dataset[]>([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const pageSize = 8;
  const totalPages = Math.ceil(total / pageSize);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    fakeFetchDatasets(search, page, pageSize).then(({ data, total }) => {
      setDatasets(data);
      setTotal(total);
      setLoading(false);
    });
  }, [search, page]);

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setPage(1);
    setSearch(e.currentTarget.search.value);
  };

  return (
    <div className="container py-4">
      <h1 className="mb-4">Liste des datasets</h1>
      <form className="mb-4" onSubmit={handleSearch}>
        <div className="input-group">
          <input
            type="text"
            name="search"
            className="form-control"
            placeholder="Rechercher un dataset..."
            defaultValue={search}
          />
          <button className="btn btn-primary" type="submit">Rechercher</button>
        </div>
      </form>
      {loading ? (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Chargement...</span>
          </div>
        </div>
      ) : (
        <div className="row g-4">
          {datasets.map(ds => (
            <div className="col-md-3" key={ds.id}>
              <div className="card h-100" style={{ cursor: 'pointer' }} onClick={() => navigate(`/dataset/${ds.id}`)}>
                <div className="card-body">
                  <h5 className="card-title">{ds.name}</h5>
                  <p className="card-text">{ds.description}</p>
                </div>
              </div>
            </div>
          ))}
          {datasets.length === 0 && (
            <div className="col-12 text-center text-muted">Aucun dataset trouvé.</div>
          )}
        </div>
      )}
      <nav className="mt-4">
        <ul className="pagination justify-content-center">
          <li className={`page-item${page === 1 ? ' disabled' : ''}`}>
            <button className="page-link" onClick={() => setPage(page - 1)} disabled={page === 1}>&laquo;</button>
          </li>
          {Array.from({ length: totalPages }, (_, i) => (
            <li key={i} className={`page-item${page === i + 1 ? ' active' : ''}`}>
              <button className="page-link" onClick={() => setPage(i + 1)}>{i + 1}</button>
            </li>
          ))}
          <li className={`page-item${page === totalPages || totalPages === 0 ? ' disabled' : ''}`}>
            <button className="page-link" onClick={() => setPage(page + 1)} disabled={page === totalPages || totalPages === 0}>&raquo;</button>
          </li>
        </ul>
      </nav>
    </div>
  );
}


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
        <Route path="/projects/:projectId/tables/:tableId/configure" element={<TableEditPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
