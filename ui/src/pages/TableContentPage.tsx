import { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { AgGridReact } from 'ag-grid-react';
import { Alert, Button, Form, Modal, Spinner } from 'react-bootstrap';
import { createView } from '../services/viewApi';
import { getTable, queryTable } from '../services/tableApi';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import { QueryBuilder } from '../components/QueryBuilder';
import type { QueryGroup, FieldOption } from '../components/QueryBuilder';
import type { Table, TableQueryCondition, TableQueryFilterGroup, TableQueryRequest, TableQueryResponse } from '../types/table';

const OPERATOR_MAPPING: Record<string, TableQueryCondition['operator']> = {
  '=': 'EQ',
  '!=': 'NEQ',
  '<': 'LT',
  '<=': 'LTE',
  '>': 'GT',
  '>=': 'GTE',
  'IS NULL': 'IS_NULL',
  'IS NOT NULL': 'IS_NOT_NULL',
  'STARTS WITH': 'STARTS_WITH',
  'NOT STARTS WITH': 'NOT_STARTS_WITH',
  'CONTAINS': 'CONTAINS',
  'NOT CONTAINS': 'NOT_CONTAINS',
  'ENDS WITH': 'ENDS_WITH',
  'NOT ENDS WITH': 'NOT_ENDS_WITH',
  'IN': 'IN',
  'NOT IN': 'NOT_IN',
};

function buildFilterGroups(group: QueryGroup): TableQueryFilterGroup[] | undefined {
  const normalized = normalizeGroup(group);
  if (!normalized) {
    return undefined;
  }
  return [normalized];
}

function normalizeGroup(group: QueryGroup): TableQueryFilterGroup | null {
  const conditions: TableQueryCondition[] = [];
  const groups: TableQueryFilterGroup[] = [];

  for (const rule of group.rules) {
    if ('group' in rule) {
      const nestedGroup = normalizeGroup(rule.group);
      if (nestedGroup) {
        groups.push(nestedGroup);
      }
      continue;
    }

    const operator = OPERATOR_MAPPING[rule.condition];
    if (!rule.field || !operator) {
      continue;
    }

    if (operator === 'IS_NULL' || operator === 'IS_NOT_NULL') {
      conditions.push({ column: rule.field, operator });
      continue;
    }

    if (operator === 'IN' || operator === 'NOT_IN') {
      const values = (rule.data || '')
        .split(',')
        .map((value) => value.trim())
        .filter(Boolean);
      if (values.length > 0) {
        conditions.push({ column: rule.field, operator, values });
      }
      continue;
    }

    if ((rule.data || '').trim()) {
      conditions.push({
        column: rule.field,
        operator,
        value: rule.data?.trim(),
      });
    }
  }

  if (conditions.length === 0 && groups.length === 0) {
    return null;
  }

  return {
    operator: group.operator,
    conditions: conditions.length > 0 ? conditions : undefined,
    groups: groups.length > 0 ? groups : undefined,
  };
}


export default function TableContentPage() {
  const { projectId, tableId } = useParams();
  const navigate = useNavigate();
  const [table, setTable] = useState<Table | null>(null);
  const [filterGroup, setFilterGroup] = useState<QueryGroup>({ operator: 'AND', rules: [] });
  const [queryResponse, setQueryResponse] = useState<TableQueryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [queryError, setQueryError] = useState('');
  const [showSaveViewModal, setShowSaveViewModal] = useState(false);
  const [viewName, setViewName] = useState('');
  const [viewDescription, setViewDescription] = useState('');
  const [saveViewError, setSaveViewError] = useState('');
  const [saveViewSuccess, setSaveViewSuccess] = useState('');
  const [savingView, setSavingView] = useState(false);

  const fields = useMemo<FieldOption[]>(() => {
    return (queryResponse?.columns || []).map((column) => ({ name: column.name }));
  }, [queryResponse]);

  const columnDefs = useMemo(() => {
    return (queryResponse?.columns || []).map((column) => ({
      headerName: column.name,
      field: column.name,
      sortable: true,
      filter: true,
      resizable: true,
    }));
  }, [queryResponse]);

  const buildQueryRequest = (): TableQueryRequest => ({
    filters: buildFilterGroups(filterGroup),
    page: 0,
    size: 100,
  });

  const loadTableAndPreview = async () => {
    if (!projectId || !tableId) {
      return;
    }

    setLoading(true);
    setQueryError('');
    try {
      const [loadedTable, preview] = await Promise.all([
        getTable(projectId, tableId),
        queryTable(projectId, tableId, buildQueryRequest()),
      ]);
      setTable(loadedTable);
      setQueryResponse(preview);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unexpected error';
      if (message.toLowerCase().includes('not found')) {
        navigate(`/error/404`, { state: { message } });
        return;
      }
      setQueryError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTableAndPreview().catch((err: Error) => {
      setLoading(false);
      setQueryError(err.message);
    });
  }, [projectId, tableId, navigate]);

  const handleApplyFilters = async () => {
    if (!projectId || !tableId) {
      return;
    }

    setLoading(true);
    setQueryError('');
    setSaveViewSuccess('');
    try {
      const response = await queryTable(projectId, tableId, buildQueryRequest());
      setQueryResponse(response);
    } catch (err) {
      setQueryError(err instanceof Error ? err.message : 'Unexpected error');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveView = async () => {
    if (!projectId || !tableId) {
      return;
    }
    if (!viewName.trim()) {
      setSaveViewError('View name is required.');
      return;
    }

    setSavingView(true);
    setSaveViewError('');
    setSaveViewSuccess('');
    try {
      const savedView = await createView(projectId, {
        name: viewName.trim(),
        description: viewDescription,
        type: 'FILTER',
        sourceTableId: tableId,
        query: buildQueryRequest(),
      });
      setSaveViewSuccess(`View created: ${savedView.name}`);
      setShowSaveViewModal(false);
      setViewName('');
      setViewDescription('');
    } catch (err) {
      setSaveViewError(err instanceof Error ? err.message : 'Unexpected error');
    } finally {
      setSavingView(false);
    }
  };

  return (
    <>
      <div className="d-flex justify-content-between align-items-start mb-3">
        <div>
          <h2 className="mb-1">{table?.name || `Table ${tableId}`}</h2>
          <div className="text-muted">
            Previewing up to 100 rows from {table?.sourceKind || 'the source'} file {table?.configuration.path || ''}
          </div>
        </div>
        <div className="d-flex gap-2">
          <Button variant="outline-secondary" onClick={() => navigate(`/projects/${projectId}/tables/${tableId}/settings`)}>
            Configure
          </Button>
          <Button variant="primary" onClick={() => setShowSaveViewModal(true)} disabled={loading}>
            Save as view
          </Button>
        </div>
      </div>
      {queryError && <Alert variant="danger">{queryError}</Alert>}
      {saveViewSuccess && <Alert variant="success">{saveViewSuccess}</Alert>}
      <div className="mb-3">
        <details>
          <summary className="mb-2"><b>Filter</b></summary>
          {fields.length === 0 ? (
            <div className="text-muted">Run the preview once to load available columns.</div>
          ) : (
            <QueryBuilder group={filterGroup} fields={fields} onChange={setFilterGroup} />
          )}
          <div className="mt-3">
            <Button variant="primary" onClick={handleApplyFilters} disabled={loading || !projectId || !tableId}>
              {loading ? 'Loading...' : 'Apply filters'}
            </Button>
          </div>
        </details>
      </div>
      <div className="ag-theme-alpine" style={{ height: 400, width: '100%' }}>
        <AgGridReact
          rowData={queryResponse?.rows || []}
          columnDefs={columnDefs}
          domLayout="autoHeight"
          overlayLoadingTemplate={'<span class="ag-overlay-loading-center">Loading preview...</span>'}
          loading={loading}
        />
      </div>

      <Modal show={showSaveViewModal} onHide={() => setShowSaveViewModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Save current query as a view</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {saveViewError && <Alert variant="danger">{saveViewError}</Alert>}
          <Form.Group className="mb-3">
            <Form.Label>Name</Form.Label>
            <Form.Control value={viewName} onChange={(event) => setViewName(event.target.value)} />
          </Form.Group>
          <Form.Group>
            <Form.Label>Description</Form.Label>
            <Form.Control as="textarea" rows={3} value={viewDescription} onChange={(event) => setViewDescription(event.target.value)} />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowSaveViewModal(false)} disabled={savingView}>Cancel</Button>
          <Button variant="primary" onClick={handleSaveView} disabled={savingView}>
            {savingView ? <Spinner size="sm" /> : 'Save view'}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
