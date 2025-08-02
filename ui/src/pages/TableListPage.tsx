
import React, { useEffect, useState } from 'react';
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
    <div className="container py-4">
      <h2>Tables of project {projectId}</h2>
      <button className="btn btn-primary mb-3">Add table</button>
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
    </div>
  );
}
