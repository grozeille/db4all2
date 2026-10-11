import { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Alert, Button } from 'react-bootstrap';
import { createView } from '../services/viewApi';
import { getTable, queryTable } from '../services/tableApi';
import { DataPreviewGrid } from '../components/DataPreviewGrid';
import { FilterStepPanel } from '../components/FilterStepPanel';
import { StepsEditorLayout } from '../components/StepsEditorLayout';
import { TransformationStepsPanel } from '../components/TransformationStepsPanel';
import { SaveViewModal } from '../components/SaveViewModal';
import type { FieldOption } from '../components/QueryBuilder';
import type {
  Table,
  TableQueryFilterGroup,
  TableQueryResponse,
  TableQuerySort,
} from '../types/table';
import type { ViewTransformationStep } from '../types/view';
import {
  appendTransformationSteps,
  compileSteps,
  makeDropStep,
  makeFilterStep,
  makeRenameStep,
  makeReorderStep,
  makeSortStep,
  queryGroupFromFilterGroup,
  sameStringArray,
} from '../services/viewSteps';

export default function TableContentPage() {
  const { projectId, tableId } = useParams();
  const navigate = useNavigate();
  const [table, setTable] = useState<Table | null>(null);
  const [baseColumns, setBaseColumns] = useState<string[]>([]);
  const [steps, setSteps] = useState<ViewTransformationStep[]>([]);
  const [activeStepCount, setActiveStepCount] = useState(0);
  const [editingFilter, setEditingFilter] = useState<{ index: number; id: string } | null>(null);
  const [appliedSorts, setAppliedSorts] = useState<TableQuerySort[]>([]);
  const [queryResponse, setQueryResponse] = useState<TableQueryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [queryError, setQueryError] = useState('');
  const [showSaveViewModal, setShowSaveViewModal] = useState(false);
  const [saveViewError, setSaveViewError] = useState('');
  const [savingView, setSavingView] = useState(false);

  const fields = useMemo<FieldOption[]>(() => {
    return (queryResponse?.columns || []).map((column) => ({ name: column.name }));
  }, [queryResponse]);

  const gridColumns = useMemo(() => (queryResponse?.columns || []).map((column) => column.name), [queryResponse]);

  const filterInitialGroup = useMemo(() => {
    if (!editingFilter) {
      return undefined;
    }
    const step = steps[editingFilter.index];
    if (!step || step.id !== editingFilter.id || step.type !== 'FILTER' || !step.filter) {
      return undefined;
    }
    return queryGroupFromFilterGroup(step.filter);
  }, [editingFilter, steps]);

  const runPreview = async (stepCount: number, stepsOverride: ViewTransformationStep[] = steps, baseOverride: string[] = baseColumns) => {
    if (!projectId || !tableId) {
      return;
    }
    let compiled;
    try {
      compiled = compileSteps(stepsOverride.slice(0, stepCount), baseOverride);
    } catch (err) {
      setQueryError(err instanceof Error ? err.message : 'Unexpected error');
      return;
    }

    setLoading(true);
    setQueryError('');
    try {
      const response = await queryTable(projectId, tableId, { ...compiled.query, page: 0, size: 100 });
      setQueryResponse(response);
      setActiveStepCount(stepCount);
      setAppliedSorts(compiled.logicalSorts);
    } catch (err) {
      setQueryError(err instanceof Error ? err.message : 'Unexpected error');
    } finally {
      setLoading(false);
    }
  };

  const loadTableAndPreview = async () => {
    if (!projectId || !tableId) {
      return;
    }

    setLoading(true);
    setQueryError('');
    try {
      const [loadedTable, preview] = await Promise.all([
        getTable(projectId, tableId),
        queryTable(projectId, tableId, { page: 0, size: 100 }),
      ]);
      setTable(loadedTable);
      setQueryResponse(preview);
      setBaseColumns(preview.columns.map((column) => column.name));
      setSteps([]);
      setActiveStepCount(0);
      setAppliedSorts([]);
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId, tableId, navigate]);

  const appendSteps = (newSteps: ViewTransformationStep[]) => {
    setEditingFilter(null);
    const updated = appendTransformationSteps(baseColumns, steps.slice(0, activeStepCount), newSteps);
    setSteps(updated);
    runPreview(updated.length, updated).catch((err: Error) => setQueryError(err.message));
  };

  const handlePreviewStep = (count: number) => {
    const target = count > 0 ? steps[count - 1] : undefined;
    if (target && target.type === 'FILTER' && count === steps.length) {
      setEditingFilter({ index: count - 1, id: target.id });
    } else {
      setEditingFilter(null);
    }
    runPreview(count).catch((err: Error) => setQueryError(err.message));
  };

  const handleSortChange = (sorts: TableQuerySort[]) => {
    if (sorts.length === 0 && appliedSorts.length === 0) {
      return;
    }
    appendSteps([makeSortStep(sorts)]);
  };

  const handleApplyFilters = (groups: TableQueryFilterGroup[]) => {
    if (editingFilter) {
      const step = steps[editingFilter.index];
      if (step && step.id === editingFilter.id && step.type === 'FILTER' && editingFilter.index === steps.length - 1) {
        const replaced = groups.map((group) => makeFilterStep(group));
        const updated = [...steps];
        updated.splice(editingFilter.index, 1, ...replaced);
        setSteps(updated);
        // The updated filter is still the last step: keep editing it.
        setEditingFilter({ index: editingFilter.index, id: replaced[replaced.length - 1].id });
        runPreview(updated.length, updated).catch((err: Error) => setQueryError(err.message));
        return;
      }
      setEditingFilter(null);
    }
    const created = groups.map((group) => makeFilterStep(group));
    appendSteps(created);
    // The new filter is the last step: keep editing it instead of resetting the builder.
    setEditingFilter({ index: activeStepCount + created.length - 1, id: created[created.length - 1].id });
  };

  const handleTruncate = (index: number) => {
    setEditingFilter(null);
    const updated = steps.slice(0, index);
    setSteps(updated);
    runPreview(index, updated).catch((err: Error) => setQueryError(err.message));
  };

  const handleReorder = (order: string[]) => {
    if (sameStringArray(order, gridColumns)) {
      return;
    }
    appendSteps([makeReorderStep(order)]);
  };

  const handleSaveView = async (name: string, description: string) => {
    if (!projectId || !tableId) {
      return;
    }
    if (!name) {
      setSaveViewError('View name is required.');
      return;
    }
    let compiled;
    try {
      compiled = compileSteps(steps, baseColumns);
    } catch (err) {
      setSaveViewError(err instanceof Error ? err.message : 'Unexpected error');
      return;
    }

    setSavingView(true);
    setSaveViewError('');
    try {
      const savedView = await createView(projectId, {
        name,
        description,
        type: 'FILTER',
        sourceTableId: tableId,
        steps,
        query: compiled.query,
      });
      setShowSaveViewModal(false);
      navigate(`/projects/${projectId}/views/${savedView.id}/content`, { state: { viewName: savedView.name } });
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
          <Button variant="primary" onClick={() => { setSaveViewError(''); setShowSaveViewModal(true); }} disabled={loading}>
            Save as view
          </Button>
        </div>
      </div>
      {queryError && <Alert variant="danger">{queryError}</Alert>}
      <FilterStepPanel
        key={editingFilter ? `edit-${editingFilter.id}` : 'new-filter'}
        fields={fields}
        disabled={loading}
        initialGroup={filterInitialGroup}
        editingStepNumber={editingFilter ? editingFilter.index + 1 : null}
        applyLabel={editingFilter ? 'Update filter' : 'Apply'}
        onCancelEdit={editingFilter ? () => setEditingFilter(null) : undefined}
        onApply={handleApplyFilters}
      />
      <StepsEditorLayout
        stepsPanel={
          <TransformationStepsPanel
            steps={steps}
            activeStepCount={activeStepCount}
            baseLabel={`Source table${table ? `: ${table.name}` : ''}`}
            onPreview={handlePreviewStep}
            onTruncate={handleTruncate}
          />
        }
      >
        <DataPreviewGrid
          columns={gridColumns}
          rows={queryResponse?.rows || []}
          loading={loading}
          sorts={appliedSorts}
          onSortChange={handleSortChange}
          onReorder={handleReorder}
          onRename={(column, newName) => appendSteps([makeRenameStep(column, newName)])}
          onDrop={(column) => appendSteps([makeDropStep(column)])}
        />
        <div className="text-muted small mt-2">
          Right-click a column to sort, rename or remove it. Drag columns to reorder them. Click a header to sort.
        </div>
      </StepsEditorLayout>

      <SaveViewModal
        show={showSaveViewModal}
        title="Save current query as a view"
        saveLabel="Save view"
        saving={savingView}
        error={saveViewError}
        showMode={false}
        linkAvailable={false}
        linkDisabledReason=""
        onClose={() => setShowSaveViewModal(false)}
        onSave={(name, description) => handleSaveView(name, description)}
      />
    </>
  );
}
