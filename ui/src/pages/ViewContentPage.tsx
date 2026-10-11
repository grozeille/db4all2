import { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Alert, Badge, Button } from 'react-bootstrap';
import { deleteView, getView, previewView, queryView, updateView, createView } from '../services/viewApi';
import { queryTable } from '../services/tableApi';
import { DataPreviewGrid } from '../components/DataPreviewGrid';
import { FilterStepPanel } from '../components/FilterStepPanel';
import { StepsEditorLayout } from '../components/StepsEditorLayout';
import { TransformationStepsPanel } from '../components/TransformationStepsPanel';
import { SaveViewModal, type SaveViewMode } from '../components/SaveViewModal';
import type { FieldOption } from '../components/QueryBuilder';
import type { TableQueryFilterGroup, TableQueryResponse, TableQuerySort } from '../types/table';
import type { View, ViewTransformationStep } from '../types/view';
import {
  appendTransformationSteps,
  compileSteps,
  makeDropStep,
  makeFilterStep,
  makeRenameStep,
  makeReorderStep,
  makeSortStep,
  normalizeSteps,
  queryGroupFromFilterGroup,
  sameStringArray,
} from '../services/viewSteps';

export default function ViewContentPage() {
  const { projectId, viewId } = useParams();
  const navigate = useNavigate();
  const [view, setView] = useState<View | null>(null);
  const [parentView, setParentView] = useState<View | null>(null);
  const [baseColumns, setBaseColumns] = useState<string[]>([]);
  const [steps, setSteps] = useState<ViewTransformationStep[]>([]);
  const [savedSteps, setSavedSteps] = useState<ViewTransformationStep[]>([]);
  const [editingFilter, setEditingFilter] = useState<{ index: number; id: string } | null>(null);
  const [activeStepCount, setActiveStepCount] = useState(0);
  const [appliedSorts, setAppliedSorts] = useState<TableQuerySort[]>([]);
  const [queryResponse, setQueryResponse] = useState<TableQueryResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [queryError, setQueryError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [showSaveAsModal, setShowSaveAsModal] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [saving, setSaving] = useState(false);

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

  const dirty = JSON.stringify(steps) !== JSON.stringify(savedSteps);

  const linkPrefixKept = savedSteps.every((saved, index) => steps[index]?.id === saved.id);
  const linkAvailable = linkPrefixKept && steps.length > savedSteps.length;
  const linkDisabledReason = !linkPrefixKept
    ? 'Linking is only available when new steps were added without changing existing ones.'
    : 'Add at least one step to create a linked view.';

  const runPreview = async (
    stepCount: number,
    stepsOverride: ViewTransformationStep[],
    baseOverride: string[],
    viewOverride: View,
  ) => {
    if (!projectId) {
      return;
    }
    let compiled;
    try {
      compiled = compileSteps(stepsOverride.slice(0, stepCount), baseOverride, {
        distinct: viewOverride.query.distinct,
        aggregations: viewOverride.query.aggregations,
      });
    } catch (err) {
      setQueryError(err instanceof Error ? err.message : 'Unexpected error');
      return;
    }

    setLoading(true);
    setQueryError('');
    setSuccessMessage('');
    try {
      const response = viewOverride.parentViewId
        ? await previewView(projectId, {
            parentViewId: viewOverride.parentViewId,
            query: { ...compiled.query, page: 0, size: 100 },
          })
        : await queryTable(projectId, viewOverride.sourceTableId, { ...compiled.query, page: 0, size: 100 });
      setQueryResponse(response);
      setActiveStepCount(stepCount);
      setAppliedSorts(compiled.logicalSorts);
    } catch (err) {
      setQueryError(err instanceof Error ? err.message : 'Unexpected error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const load = async () => {
      if (!projectId || !viewId) {
        return;
      }
      setLoading(true);
      setQueryError('');
      try {
        const loadedView = await getView(projectId, viewId);
        const normalized = normalizeSteps(loadedView.steps || []);
        let parent: View | null = null;
        let base;
        if (loadedView.parentViewId) {
          parent = await getView(projectId, loadedView.parentViewId);
          base = await queryView(projectId, loadedView.parentViewId, { page: 0, size: 100 });
        } else {
          base = await queryTable(projectId, loadedView.sourceTableId, { page: 0, size: 100 });
        }
        const baseCols = base.columns.map((column) => column.name);
        setView(loadedView);
        setParentView(parent);
        setBaseColumns(baseCols);
        setSteps(normalized);
        setSavedSteps(normalized);
        await runPreview(normalized.length, normalized, baseCols, loadedView);
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Unexpected error';
        if (message.toLowerCase().includes('not found')) {
          navigate(`/error/404`, { state: { message } });
          return;
        }
        setQueryError(message);
        setLoading(false);
      }
    };
    load().catch((err: Error) => {
      setLoading(false);
      setQueryError(err.message);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId, viewId, navigate]);

  const appendSteps = (newSteps: ViewTransformationStep[]) => {
    if (!view) {
      return;
    }
    setEditingFilter(null);
    const updated = appendTransformationSteps(baseColumns, steps.slice(0, activeStepCount), newSteps);
    setSteps(updated);
    runPreview(updated.length, updated, baseColumns, view).catch((err: Error) => setQueryError(err.message));
  };

  const handleApplyFilters = (groups: TableQueryFilterGroup[]) => {
    if (!view) {
      return;
    }
    if (editingFilter) {
      const step = steps[editingFilter.index];
      if (step && step.id === editingFilter.id && step.type === 'FILTER' && editingFilter.index === steps.length - 1) {
        const replaced = groups.map((group) => makeFilterStep(group));
        const updated = [...steps];
        updated.splice(editingFilter.index, 1, ...replaced);
        setSteps(updated);
        // The updated filter is still the last step: keep editing it.
        setEditingFilter({ index: editingFilter.index, id: replaced[replaced.length - 1].id });
        runPreview(updated.length, updated, baseColumns, view).catch((err: Error) => setQueryError(err.message));
        return;
      }
      setEditingFilter(null);
    }
    const created = groups.map((group) => makeFilterStep(group));
    appendSteps(created);
    // The new filter is the last step: keep editing it instead of resetting the builder.
    setEditingFilter({ index: activeStepCount + created.length - 1, id: created[created.length - 1].id });
  };

  const handlePreviewStep = (count: number) => {
    if (!view) {
      return;
    }
    const target = count > 0 ? steps[count - 1] : undefined;
    if (target && target.type === 'FILTER' && count === steps.length) {
      setEditingFilter({ index: count - 1, id: target.id });
    } else {
      setEditingFilter(null);
    }
    runPreview(count, steps, baseColumns, view).catch((err: Error) => setQueryError(err.message));
  };

  const handleSortChange = (sorts: TableQuerySort[]) => {
    if (sorts.length === 0 && appliedSorts.length === 0) {
      return;
    }
    appendSteps([makeSortStep(sorts)]);
  };

  const handleTruncate = (index: number) => {
    if (!view) {
      return;
    }
    setEditingFilter(null);
    const updated = steps.slice(0, index);
    setSteps(updated);
    runPreview(index, updated, baseColumns, view).catch((err: Error) => setQueryError(err.message));
  };

  const handleReorder = (order: string[]) => {
    if (sameStringArray(order, gridColumns)) {
      return;
    }
    appendSteps([makeReorderStep(order)]);
  };

  const handleSave = async () => {
    if (!projectId || !viewId || !view) {
      return;
    }
    let compiled;
    try {
      compiled = compileSteps(steps, baseColumns, {
        distinct: view.query.distinct,
        aggregations: view.query.aggregations,
      });
    } catch (err) {
      setQueryError(err instanceof Error ? err.message : 'Unexpected error');
      return;
    }
    setSaving(true);
    setSaveError('');
    setSuccessMessage('');
    try {
      const updated = await updateView(projectId, viewId, {
        name: view.name,
        description: view.description,
        type: view.type,
        sourceTableId: view.sourceTableId,
        steps,
        query: compiled.query,
      });
      const normalized = normalizeSteps(updated.steps || []);
      setView(updated);
      setSteps(normalized);
      setSavedSteps(normalized);
      setSuccessMessage(`View saved: ${updated.name}`);
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Unexpected error');
    } finally {
      setSaving(false);
    }
  };

  const loadAncestorSteps = async (leaf: View): Promise<ViewTransformationStep[]> => {
    if (!projectId) {
      return [];
    }
    const ancestorSteps: ViewTransformationStep[] = [];
    let currentParentId = leaf.parentViewId;
    const chain: ViewTransformationStep[][] = [];
    while (currentParentId) {
      const parent = await getView(projectId, currentParentId);
      chain.unshift(normalizeSteps(parent.steps || []));
      currentParentId = parent.parentViewId;
    }
    chain.forEach((level) => ancestorSteps.push(...level));
    return ancestorSteps;
  };

  const handleSaveAsNew = async (name: string, description: string, mode: SaveViewMode) => {
    if (!projectId || !view) {
      return;
    }
    if (!name) {
      setSaveError('View name is required.');
      return;
    }
    setSaving(true);
    setSaveError('');
    try {
      if (mode === 'link') {
        if (!linkAvailable) {
          setSaveError(linkDisabledReason);
          return;
        }
        const delta = steps.slice(savedSteps.length);
        const parentOutput = compileSteps(savedSteps, baseColumns).outputColumns;
        const compiledDelta = compileSteps(delta, parentOutput);
        const created = await createView(projectId, {
          name,
          description,
          type: view.type,
          parentViewId: view.id,
          steps: delta,
          query: compiledDelta.query,
        });
        setShowSaveAsModal(false);
        navigate(`/projects/${projectId}/views/${created.id}/content`);
      } else {
        const ancestorSteps = await loadAncestorSteps(view);
        const fullSteps = [...ancestorSteps, ...steps];
        let rootBase = baseColumns;
        if (view.parentViewId) {
          const rootPreview = await queryTable(projectId, view.sourceTableId, { page: 0, size: 1 });
          rootBase = rootPreview.columns.map((column) => column.name);
        }
        const compiledFull = compileSteps(fullSteps, rootBase, {
          distinct: view.query.distinct,
          aggregations: view.query.aggregations,
        });
        const created = await createView(projectId, {
          name,
          description,
          type: view.type,
          sourceTableId: view.sourceTableId,
          steps: fullSteps,
          query: compiledFull.query,
        });
        setShowSaveAsModal(false);
        navigate(`/projects/${projectId}/views/${created.id}/content`);
      }
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Unexpected error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!projectId || !viewId || !view) {
      return;
    }
    if (!window.confirm(`Delete view "${view.name}"?`)) {
      return;
    }
    try {
      await deleteView(projectId, viewId);
      navigate(`/projects/${projectId}/tables`);
    } catch (err) {
      setQueryError(err instanceof Error ? err.message : 'Unexpected error');
    }
  };

  return (
    <>
      <div className="d-flex justify-content-between align-items-start mb-3">
        <div>
          <h2 className="mb-1">
            {view?.name || `View ${viewId}`}{' '}
            {view?.parentViewId && <Badge bg="info">Linked view</Badge>}{' '}
            {dirty && <Badge bg="warning">Unsaved changes</Badge>}
          </h2>
          <div className="text-muted">
            {view?.description || 'Previewing up to 100 rows'}
            {parentView && (
              <>
                {' '}— linked to{' '}
                <Button variant="link" size="sm" className="p-0" onClick={() => navigate(`/projects/${projectId}/views/${parentView.id}/content`)}>
                  {parentView.name}
                </Button>
              </>
            )}
          </div>
        </div>
        <div className="d-flex gap-2">
          <Button variant="outline-danger" onClick={handleDelete} disabled={loading || saving}>
            Delete
          </Button>
          <Button variant="outline-primary" onClick={() => { setSaveError(''); setShowSaveAsModal(true); }} disabled={loading || saving}>
            Save as new view
          </Button>
          <Button variant="primary" onClick={handleSave} disabled={loading || saving || !dirty}>
            {saving ? 'Saving...' : 'Save'}
          </Button>
        </div>
      </div>
      {queryError && <Alert variant="danger">{queryError}</Alert>}
      {saveError && <Alert variant="danger">{saveError}</Alert>}
      {successMessage && <Alert variant="success">{successMessage}</Alert>}
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
            baseLabel={parentView ? `Parent view: ${parentView.name}` : 'Source table'}
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
        show={showSaveAsModal}
        title="Save as a new view"
        saveLabel="Save new view"
        saving={saving}
        error={saveError}
        showMode={true}
        linkAvailable={linkAvailable}
        linkDisabledReason={linkDisabledReason}
        onClose={() => setShowSaveAsModal(false)}
        onSave={handleSaveAsNew}
      />
    </>
  );
}
