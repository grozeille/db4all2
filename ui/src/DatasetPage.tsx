import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import type { Dataset } from './types';
import { Tabs, Tab } from 'react-bootstrap';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import { ModuleRegistry, AllCommunityModule } from 'ag-grid-community';
    
ModuleRegistry.registerModules([ AllCommunityModule ]);

// Fake API
const FAKE_DATASETS: Dataset[] = Array.from({ length: 42 }, (_, i) => ({
  id: i + 1,
  name: `Dataset ${i + 1}`,
  description: `Description du dataset numéro ${i + 1}`,
  tags: ['tag1', 'tag2'],
}));

const FAKE_DATA = Array.from({ length: 20 }, (_, i) => ({
  col1: `Valeur ${i + 1}-1`,
  col2: `Valeur ${i + 1}-2`,
  col3: `Valeur ${i + 1}-3`,
}));

export default function DatasetPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [dataset, setDataset] = useState<Dataset | null>(null);
  const [tab, setTab] = useState('data');
  const [form, setForm] = useState({ name: '', description: '', tags: '' });

  useEffect(() => {
    const found = FAKE_DATASETS.find(ds => ds.id === Number(id));
    if (!found) {
      navigate('/');
      return;
    }
    setDataset(found);
    setForm({
      name: found.name,
      description: found.description,
      tags: found.tags.join(', '),
    });
  }, [id, navigate]);

  if (!dataset) return null;

  return (
    <div className="container py-4">
      <Link to="/" className="btn btn-link mb-3">&larr; Retour</Link>
      <h2 className="mb-4">{dataset.name}</h2>
      <Tabs activeKey={tab} onSelect={k => setTab(k || 'data')} className="mb-3">
        <Tab eventKey="data" title="Données">
          <div className="ag-theme-alpine" style={{ height: 400, width: '100%' }}>
            <AgGridReact
              rowData={FAKE_DATA}
              columnDefs={[
                { headerName: 'Colonne 1', field: 'col1' },
                { headerName: 'Colonne 2', field: 'col2' },
                { headerName: 'Colonne 3', field: 'col3' },
              ]}
              domLayout="autoHeight"
            />
          </div>
        </Tab>
        <Tab eventKey="info" title="Informations">
          <form className="mt-4" onSubmit={e => { e.preventDefault(); alert('Enregistré !'); }}>
            <div className="mb-3">
              <label className="form-label">Nom</label>
              <input className="form-control" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
            </div>
            <div className="mb-3">
              <label className="form-label">Description</label>
              <textarea className="form-control" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
            </div>
            <div className="mb-3">
              <label className="form-label">Tags (séparés par des virgules)</label>
              <input className="form-control" value={form.tags} onChange={e => setForm(f => ({ ...f, tags: e.target.value }))} />
            </div>
            <button className="btn btn-success" type="submit">Save</button>
          </form>
        </Tab>
      </Tabs>
    </div>
  );
}
