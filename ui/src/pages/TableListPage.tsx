
import React, { useEffect, useState } from 'react';
import AppHeader from '../components/AppHeader';
import PageLayout from '../components/PageLayout';
import { useParams, useNavigate } from 'react-router-dom';
import { getTables } from '../services/tableApi';


export default function TableListPage() {
  const { projectId } = useParams();
  const [tables, setTables] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    if (projectId) getTables(projectId as string).then(setTables);
  }, [projectId]);

  return (
    <PageLayout>
      <div className="d-flex align-items-start justify-content-between mb-4">
        <h2 className="mb-0">Tables of project {projectId}</h2>
        <div>
          <button className="btn btn-primary me-2" onClick={() => navigate(`/projects/${projectId}/settings`)}>Settings</button>
          <button className="btn btn-primary">Add Table</button>
        </div>
      </div>
      <div className="row g-3">
        {tables.map((t: any) => (
          <div className="col-md-4" key={t.id}>
            <div className="card h-100" style={{ cursor: 'pointer' }} onClick={() => navigate(`/projects/${projectId}/tables/${t.id}/content`)}>
              <div className="card-body">
                <h5 className="card-title">{t.name}</h5>
                <p className="card-text text-muted">{t.description}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </PageLayout>
  );
      
}
