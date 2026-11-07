import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getTable, createTable, updateTable } from '../services/tableApi';
import { getDatasources, scanDatasource } from '../services/datasourceApi';
import type { Datasource, ScanItem } from '../types/datasource';
import type { Table, TableSourceKind, TableUpsertRequest } from '../types/table';

const TABLE_TYPES: Array<{ value: TableSourceKind; label: string }> = [
  { value: 'CSV', label: 'CSV' },
  { value: 'PARQUET', label: 'Parquet files' },
  { value: 'JSON', label: 'JSON file' },
  { value: 'EXCEL', label: 'Excel file' },
];

type ExcelSelectionMode = 'SHEET' | 'NAMED_RANGE' | 'CUSTOM_RANGE';

const EXCEL_MODES: Array<{ value: ExcelSelectionMode; label: string }> = [
  { value: 'SHEET', label: 'Sheet' },
  { value: 'NAMED_RANGE', label: 'Named range' },
  { value: 'CUSTOM_RANGE', label: 'Custom range' },
];

export default function TableEditPage() {
  const { projectId, tableId } = useParams();
  const navigate = useNavigate();
  const isEdit = !!tableId;
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<TableSourceKind>('CSV');
  const [datasourceId, setDatasourceId] = useState('');
  const [config, setConfig] = useState({
    path: '',
    separator: ',',
    firstRowAsHeader: true,
    jsonPath: '',
    sheetName: '',
    namedRange: '',
    explicitRange: '',
  });
  const [excelMode, setExcelMode] = useState<ExcelSelectionMode>('SHEET');
  const [sheets, setSheets] = useState<ScanItem[]>([]);
  const [namedRanges, setNamedRanges] = useState<ScanItem[]>([]);
  const [scanError, setScanError] = useState('');
  const [isScanning, setIsScanning] = useState(false);
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
          jsonPath: table.configuration.jsonPath ?? '',
          sheetName: table.configuration.sheetName ?? '',
          namedRange: table.configuration.namedRange ?? '',
          explicitRange: table.configuration.explicitRange ?? '',
        });
        if (table.configuration.namedRange) {
          setExcelMode('NAMED_RANGE');
        } else if (table.configuration.explicitRange) {
          setExcelMode('CUSTOM_RANGE');
        } else {
          setExcelMode('SHEET');
        }
      })
      .catch((err: Error) => {
        navigate(`/error/404`, { state: { message: err.message } });
      });
  }, [isEdit, navigate, projectId, tableId]);

  const handleExcelModeChange = (mode: ExcelSelectionMode) => {
    setExcelMode(mode);
    if (mode === 'SHEET') {
      setConfig(c => ({ ...c, namedRange: '', explicitRange: '' }));
    } else if (mode === 'NAMED_RANGE') {
      setConfig(c => ({ ...c, sheetName: '', explicitRange: '' }));
    } else {
      setConfig(c => ({ ...c, namedRange: '' }));
    }
  };

  const handleScan = async () => {
    if (!projectId || !datasourceId) {
      setScanError('Select a datasource first.');
      return;
    }
    if (!config.path.trim()) {
      setScanError('Enter the Excel file path first.');
      return;
    }
    setScanError('');
    setIsScanning(true);
    try {
      const [sheetResponse, rangeResponse] = await Promise.all([
        scanDatasource(projectId, datasourceId, { filePath: config.path.trim(), scanType: 'EXCEL_SHEETS' }),
        scanDatasource(projectId, datasourceId, { filePath: config.path.trim(), scanType: 'EXCEL_NAMED_RANGES' }),
      ]);
      setSheets(sheetResponse.items);
      setNamedRanges(rangeResponse.items);
      if (sheetResponse.items.length > 0 && !config.sheetName.trim()) {
        setConfig(c => ({ ...c, sheetName: sheetResponse.items[0].label }));
      }
    } catch (err) {
      setScanError(err instanceof Error ? err.message : 'Unable to scan the Excel file.');
    } finally {
      setIsScanning(false);
    }
  };

  const sheetOptions = config.sheetName.trim()
    ? [config.sheetName.trim(), ...sheets.map(s => s.label).filter(label => label !== config.sheetName.trim())]
    : sheets.map(s => s.label);
  const namedRangeOptions = config.namedRange.trim()
    ? [config.namedRange.trim(), ...namedRanges.map(r => r.label).filter(label => label !== config.namedRange.trim())]
    : namedRanges.map(r => r.label);

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
    if (type === 'EXCEL' && excelMode === 'NAMED_RANGE' && !config.namedRange.trim()) {
      setError('A named range is required.');
      return;
    }
    if (type === 'EXCEL' && excelMode === 'CUSTOM_RANGE' && !config.explicitRange.trim()) {
      setError('A custom range is required.');
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
        : type === 'JSON'
          ? {
              path: config.path.trim(),
              jsonPath: config.jsonPath.trim(),
            }
          : type === 'EXCEL'
            ? {
                path: config.path.trim(),
                sheetName: excelMode === 'SHEET' || excelMode === 'CUSTOM_RANGE' ? config.sheetName.trim() : '',
                namedRange: excelMode === 'NAMED_RANGE' ? config.namedRange.trim() : '',
                explicitRange: excelMode === 'CUSTOM_RANGE' ? config.explicitRange.trim() : '',
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
          <input className="form-control" value={config.path} onChange={e => setConfig(c => ({ ...c, path: e.target.value }))} placeholder={type === 'CSV' ? 'data/customers.csv' : type === 'JSON' ? 'data/orders.json' : type === 'EXCEL' ? 'data/workbook.xlsx' : 'data/customers.parquet'} />
        </div>
        {type === 'JSON' && (
          <div className="mb-3">
            <label className="form-label">JSON path of the object array</label>
            <input className="form-control" value={config.jsonPath} onChange={e => setConfig(c => ({ ...c, jsonPath: e.target.value }))} placeholder="$.orders" />
            <div className="form-text">Dot-separated path to the array to expose as rows, ex: $.data.items. Leave empty when the file itself is an array of objects.</div>
          </div>
        )}
        {type === 'EXCEL' && (
          <>
            <div className="mb-3">
              <button type="button" className="btn btn-outline-secondary" data-testid="excel-scan" onClick={handleScan} disabled={isScanning}>
                {isScanning ? 'Scanning...' : 'Scan file'}
              </button>
              <div className="form-text">List the sheets and named ranges of the Excel file.</div>
              {scanError && <div className="text-danger mt-2">{scanError}</div>}
            </div>
            <div className="mb-3">
              <label className="form-label">Selection</label>
              <select className="form-select" data-testid="excel-mode" value={excelMode} onChange={e => handleExcelModeChange(e.target.value as ExcelSelectionMode)}>
                {EXCEL_MODES.map(m => (
                  <option key={m.value} value={m.value}>{m.label}</option>
                ))}
              </select>
            </div>
            {excelMode === 'SHEET' && (
              <div className="mb-3">
                <label className="form-label">Sheet</label>
                <select className="form-select" data-testid="excel-sheet" value={config.sheetName} onChange={e => setConfig(c => ({ ...c, sheetName: e.target.value }))}>
                  <option value="">First sheet</option>
                  {sheetOptions.filter(option => option !== '').map(option => (
                    <option key={option} value={option}>{option}</option>
                  ))}
                </select>
              </div>
            )}
            {excelMode === 'NAMED_RANGE' && (
              <div className="mb-3">
                <label className="form-label">Named range</label>
                <select className="form-select" data-testid="excel-named-range" value={config.namedRange} onChange={e => setConfig(c => ({ ...c, namedRange: e.target.value }))}>
                  <option value="">Select a named range</option>
                  {namedRangeOptions.filter(option => option !== '').map(option => (
                    <option key={option} value={option}>{option}</option>
                  ))}
                </select>
              </div>
            )}
            {excelMode === 'CUSTOM_RANGE' && (
              <>
                <div className="mb-3">
                  <label className="form-label">Sheet</label>
                  <select className="form-select" data-testid="excel-sheet" value={config.sheetName} onChange={e => setConfig(c => ({ ...c, sheetName: e.target.value }))}>
                    <option value="">First sheet</option>
                    {sheetOptions.filter(option => option !== '').map(option => (
                      <option key={option} value={option}>{option}</option>
                    ))}
                  </select>
                  <div className="form-text">Used when the range below does not name its own sheet.</div>
                </div>
                <div className="mb-3">
                  <label className="form-label">Range</label>
                  <input className="form-control" data-testid="excel-range" value={config.explicitRange} onChange={e => setConfig(c => ({ ...c, explicitRange: e.target.value }))} placeholder="A1:D10 or Sheet2!A1:D10" />
                </div>
              </>
            )}
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
