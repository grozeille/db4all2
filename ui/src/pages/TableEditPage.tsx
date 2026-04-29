import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getTable, createTable, updateTable } from '../services/tableApi';
import { getDatasources } from '../services/datasourceApi';
import type { Datasource } from '../types/datasource';
import type { Table, TableSourceKind, TableUpsertRequest } from '../types/table';

const TABLE_TYPES: Array<{ value: TableSourceKind; label: string }> = [
  { value: 'CSV', label: 'CSV' },
  { value: 'PARQUET', label: 'Parquet files' },
];

export default function TableEditPage() {
  const { projectId, tableId } = useParams();
  const navigate = useNavigate();
  const isEdit = !!tableId;
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<TableSourceKind>('CSV');
  const [datasourceId, setDatasourceId] = useState('');
  const [config, setConfig] = useState({ path: '', separator: ',', firstRowAsHeader: true });
  const [datasources, setDatasources] = useState<Datasource[]>([]);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!projectId) {
      return;
    }

    getDatasources(projectId)
      .then((loadedDatasources) => {
        setDatasources(loadedDatasources);
        if (!datasourceId && loadedDatasources.length > 0) {
          setDatasourceId(loadedDatasources[0].id);
        }
      })
      .catch((err: Error) => {
        setError(err.message);
      });
  }, [isEdit, projectId, tableId, navigate]);

  useEffect(() => {
    if (!isEdit || !projectId || !tableId) {
      return;
    }

    getTable(projectId, tableId)
      .then((table: Table) => {
        setName(table.name);
        setDescription(table.description);
        setType(table.sourceKind);
        setDatasourceId(table.datasourceId);
        setConfig({
          path: table.configuration.path ?? '',
          separator: table.configuration.separator ?? ',',
          firstRowAsHeader: table.configuration.firstRowAsHeader ?? true,
        });
      })
      .catch((err: Error) => {
        navigate(`/error/404`, { state: { message: err.message } });
      });
  }, [isEdit, navigate, projectId, tableId]);

  const handleSave = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!projectId) {
      return;
    }
    if (!name.trim()) {
      setError('Name is required.');
      return;
    }
    if (!datasourceId) {
      setError('A datasource is required.');
      return;
    }
    if (!config.path.trim()) {
      setError('Path is required.');
      return;
    }

    setError('');

    const payload: TableUpsertRequest = {
      name: name.trim(),
      description,
      datasourceId,
      sourceKind: type,
      configuration: type === 'CSV'
        ? {
            path: config.path.trim(),
            separator: config.separator || ',',
            firstRowAsHeader: config.firstRowAsHeader,
          }
        : {
            path: config.path.trim(),
          },
    };

    try {
      setIsSubmitting(true);
      if (isEdit && tableId) {
        const updatedTable = await updateTable(projectId, tableId, payload);
        navigate(`/projects/${projectId}/tables/${updatedTable.id}/content`);
      } else {
        const createdTable = await createTable(projectId, payload);
        navigate(`/projects/${projectId}/tables/${createdTable.id}/content`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unexpected error.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <h2>{isEdit ? `Edit table ${tableId}` : 'Add a new table'}</h2>
      <form onSubmit={handleSave} className="mt-4" style={{ maxWidth: 600 }}>
        <div className="mb-3">
          <label className="form-label">Table name</label>
          <input className="form-control" value={name} onChange={e => setName(e.target.value)} required />
        </div>
        <div className="mb-3">
          <label className="form-label">Description</label>
          <textarea className="form-control" value={description} onChange={e => setDescription(e.target.value)} rows={3} />
        </div>
        <div className="mb-3">
          <label className="form-label">Type</label>
          <select className="form-select" value={type} onChange={e => setType(e.target.value as TableSourceKind)} disabled={isEdit}>
            {TABLE_TYPES.map(t => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
        </div>
        <div className="mb-3">
          <label className="form-label">Datasource</label>
          <select className="form-select" value={datasourceId} onChange={e => setDatasourceId(e.target.value)} disabled={isEdit || datasources.length === 0}>
            {datasources.length === 0 && <option value="">No datasource available</option>}
            {datasources.map((datasource) => (
              <option key={datasource.id} value={datasource.id}>{datasource.name}</option>
            ))}
          </select>
        </div>
        <div className="mb-3">
          <label className="form-label">Path</label>
          <input className="form-control" value={config.path} onChange={e => setConfig(c => ({ ...c, path: e.target.value }))} placeholder={type === 'CSV' ? 'data/customers.csv' : 'data/customers.parquet'} />
        </div>
        {type === 'CSV' && (
          <>
            <div className="mb-3">
              <label className="form-label">Separator</label>
              <input className="form-control" value={config.separator} onChange={e => setConfig(c => ({ ...c, separator: e.target.value }))} />
            </div>
            <div className="form-check mb-3">
              <input
                className="form-check-input"
                type="checkbox"
                checked={config.firstRowAsHeader}
                onChange={e => setConfig(c => ({ ...c, firstRowAsHeader: e.target.checked }))}
                id="headerCheck"
              />
              <label className="form-check-label" htmlFor="headerCheck">First row as header</label>
            </div>
          </>
        )}
        {datasources.length === 0 && (
          <div className="alert alert-warning">
            Create a local filesystem datasource first in the project settings page.
          </div>
        )}
        {error && <div className="text-danger mb-3">{error}</div>}
        <div className="d-flex gap-2 mt-4">
          <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={isSubmitting || datasources.length === 0}>
            {isSubmitting ? 'Saving...' : 'Save'}
          </button>
        </div>
      </form>
    </>
  );
}
