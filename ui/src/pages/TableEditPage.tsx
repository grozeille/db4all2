
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getTable, createTable, updateTable } from '../services/tableApi';
import type { Table } from '../types/table';

// Types et fausses datasources pour la démo
const TABLE_TYPES = [
  { value: 'csv', label: 'CSV' },
  { value: 'excel', label: 'Excel' },
  { value: 'parquet', label: 'Parquet files' },
  { value: 'iceberg', label: 'Iceberg table' },
  { value: 'postgresql', label: 'PostgreSQL table' },
];
const FAKE_DATASOURCES = [
  { name: 'CIFS', type: 'cifs', readonly: false },
  { name: 'S3', type: 's3', readonly: false },
  { name: 'PostgreSQL', type: 'postgresql', readonly: false },
];

export default function TableEditPage() {
  const { projectId, tableId } = useParams();
  const navigate = useNavigate();
  const isEdit = !!tableId;

  // Form state
  const [name, setName] = useState('');
  const [type, setType] = useState(TABLE_TYPES[0].value);
  const [dataSource, setDataSource] = useState(FAKE_DATASOURCES[0].name);
  const [config, setConfig] = useState({ path: '', separator: ',', header: true });
  const [excelTab, setExcelTab] = useState('');
  const [pgTable, setPgTable] = useState('');
  const [uploading, setUploading] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState('');
  const [scanTabs, setScanTabs] = useState<string[]>([]);

  // Charge la table existante en édition
  useEffect(() => {
    if (isEdit && projectId && tableId) {
      getTable(projectId, tableId)
        .then((table: Table) => {
          setName(table.name);
          // Pour la démo, on suppose type/dataSource non modifiables
          setType('csv'); // À remplacer par table.type si dispo
          setDataSource('CIFS'); // À remplacer par table.dataSource si dispo
          setConfig({ path: table.description || '', separator: ',', header: true });
        })
        .catch(err => {
          navigate(`/error/404`, { state: { message: err.message } });
        });
    }
  }, [isEdit, projectId, tableId, navigate]);

  // Scan et upload inchangés
  const handleScan = () => {
    setTimeout(() => {
      setScanTabs(['tab1', 'tab2', 'tab3']);
    }, 1000);
  };
  const handleUpload = () => {
    if (!file) return;
    setUploading(true);
    setTimeout(() => {
      setConfig(c => ({ ...c, path: file.name }));
      setUploading(false);
      alert('Upload terminé');
    }, 1500);
  };

  // Save : création ou édition
  const handleSave = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!name) {
      setError('Le nom est requis');
      return;
    }
    setError('');
    if (isEdit && projectId && tableId) {
      // Appel API updateTable
      await updateTable(projectId, tableId, { name, description: config.path });
      navigate(`/projects/${projectId}/tables/${tableId}/content`);
    } else if (projectId) {
      // Appel API createTable
      const newTableId = await createTable(projectId, { name, description: config.path });
      navigate(`/projects/${projectId}/tables/${newTableId}/content`);
    }
  };

  // Configuration dynamique selon le type, désactive les champs en édition
  const renderConfig = () => {
    switch (type) {
      case 'csv':
        return (
          <>
            <div className="mb-3">
              <label className="form-label">Source</label>
              <select className="form-select" value={dataSource} onChange={e => setDataSource(e.target.value)} disabled={isEdit}>
                {FAKE_DATASOURCES.filter(ds => ds.type === 'cifs' || ds.type === 's3').map(ds => (
                  <option key={ds.name} value={ds.name}>{ds.name}</option>
                ))}
              </select>
            </div>
            <div className="mb-3">
              <label className="form-label">Path</label>
              <input className="form-control" value={config.path} onChange={e => setConfig(c => ({ ...c, path: e.target.value }))} placeholder="*.csv ou subfolder/file.csv" />
            </div>
            <div className="mb-3">
              <label className="form-label">Separator</label>
              <input className="form-control" value={config.separator} onChange={e => setConfig(c => ({ ...c, separator: e.target.value }))} />
            </div>
            <div className="form-check mb-3">
              <input className="form-check-input" type="checkbox" checked={config.header} onChange={e => setConfig(c => ({ ...c, header: e.target.checked }))} id="headerCheck" />
              <label className="form-check-label" htmlFor="headerCheck">First row as header</label>
            </div>
            {!isEdit && (
              <div className="mb-3">
                <label className="form-label">Upload file</label>
                <input type="file" className="form-control" onChange={e => setFile(e.target.files?.[0] || null)} disabled={uploading} />
                <button type="button" className="btn btn-secondary mt-2" onClick={handleUpload} disabled={!file || uploading}>Upload</button>
                {uploading && <span className="ms-2">Veuillez patienter...</span>}
              </div>
            )}
          </>
        );
      case 'excel':
        return (
          <>
            <div className="mb-3">
              <label className="form-label">Source</label>
              <select className="form-select" value={dataSource} onChange={e => setDataSource(e.target.value)} disabled={isEdit}>
                {FAKE_DATASOURCES.filter(ds => ds.type === 'cifs' || ds.type === 's3').map(ds => (
                  <option key={ds.name} value={ds.name}>{ds.name}</option>
                ))}
              </select>
            </div>
            <div className="mb-3">
              <label className="form-label">Path</label>
              <input className="form-control" value={config.path} onChange={e => setConfig(c => ({ ...c, path: e.target.value }))} placeholder="*.xlsx ou subfolder/file.xlsx" />
            </div>
            {!isEdit && (
              <button type="button" className="btn btn-secondary mb-3" onClick={handleScan}>Scan</button>
            )}
            {scanTabs.length > 0 && (
              <div className="mb-3">
                <label className="form-label">Select tab/table</label>
                <select className="form-select" value={excelTab} onChange={e => setExcelTab(e.target.value)}>
                  <option value="">-- Select --</option>
                  {scanTabs.map(tab => <option key={tab} value={tab}>{tab}</option>)}
                </select>
              </div>
            )}
          </>
        );
      case 'postgresql':
        return (
          <>
            <div className="mb-3">
              <label className="form-label">Source</label>
              <select className="form-select" value={dataSource} onChange={e => setDataSource(e.target.value)} disabled={isEdit}>
                {FAKE_DATASOURCES.filter(ds => ds.type === 'postgresql').map(ds => (
                  <option key={ds.name} value={ds.name}>{ds.name}</option>
                ))}
              </select>
            </div>
            {!isEdit && (
              <button type="button" className="btn btn-secondary mb-3" onClick={handleScan}>Scan</button>
            )}
            {scanTabs.length > 0 && (
              <div className="mb-3">
                <label className="form-label">Select table</label>
                <select className="form-select" value={pgTable} onChange={e => setPgTable(e.target.value)}>
                  <option value="">-- Select --</option>
                  {scanTabs.map(tab => <option key={tab} value={tab}>{tab}</option>)}
                </select>
              </div>
            )}
          </>
        );
      default:
        return (
          <div className="mb-3">
            <label className="form-label">Source</label>
            <select className="form-select" value={dataSource} onChange={e => setDataSource(e.target.value)} disabled={isEdit}>
              {FAKE_DATASOURCES.map(ds => (
                <option key={ds.name} value={ds.name}>{ds.name}</option>
              ))}
            </select>
          </div>
        );
    }
  };

  return (
    <>
      <h2>{isEdit ? `Édition de la table ${tableId}` : "Ajout d'une nouvelle table"}</h2>
      <form onSubmit={handleSave} className="mt-4" style={{ maxWidth: 600 }}>
        <div className="mb-3">
          <label className="form-label">Nom de la table</label>
          <input className="form-control" value={name} onChange={e => setName(e.target.value)} required />
        </div>
        <div className="mb-3">
          <label className="form-label">Type</label>
          <select className="form-select" value={type} onChange={e => { setType(e.target.value); setScanTabs([]); }} disabled={isEdit}>
            {TABLE_TYPES.map(t => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
        </div>
        {renderConfig()}
        {error && <div className="text-danger mb-3">{error}</div>}
        <div className="d-flex gap-2 mt-4">
          <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>Cancel</button>
          <button type="submit" className="btn btn-primary">Save</button>
        </div>
      </form>
    </>
  );
}
