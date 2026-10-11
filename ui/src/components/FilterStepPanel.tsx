import { useEffect, useRef, useState } from 'react';
import { Alert, Button } from 'react-bootstrap';
import { QueryBuilder, type FieldOption, type QueryGroup } from './QueryBuilder';
import type { TableQueryFilterGroup } from '../types/table';
import { buildFilterGroups } from '../services/viewSteps';

type Props = {
  fields: FieldOption[];
  disabled: boolean;
  initialGroup?: QueryGroup;
  editingStepNumber?: number | null;
  applyLabel?: string;
  onCancelEdit?: () => void;
  onApply: (groups: TableQueryFilterGroup[]) => void;
};

const EMPTY_GROUP: QueryGroup = { operator: 'AND', rules: [] };

export function FilterStepPanel({ fields, disabled, initialGroup, editingStepNumber, applyLabel, onCancelEdit, onApply }: Props) {
  const [group, setGroup] = useState<QueryGroup>(initialGroup ?? EMPTY_GROUP);
  const [error, setError] = useState('');
  const fieldsKey = fields.map((field) => field.name).join('\u0000');
  const firstRender = useRef(true);

  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    if (editingStepNumber == null) {
      setGroup(EMPTY_GROUP);
    }
  }, [fieldsKey, editingStepNumber]);

  const handleApply = () => {
    const groups = buildFilterGroups(group);
    if (!groups) {
      setError('Add at least one condition before applying.');
      return;
    }
    setError('');
    onApply(groups);
    setGroup(EMPTY_GROUP);
  };

  return (
    <div className="mb-3">
      <details open>
        <summary className="mb-2"><b>Filter</b></summary>
        {error && <Alert variant="danger">{error}</Alert>}
        {editingStepNumber != null && (
          <div className="d-flex align-items-center gap-2 mb-2">
            <span className="text-muted small">Editing the filter of step {editingStepNumber}.</span>
            {onCancelEdit && (
              <Button variant="outline-secondary" size="sm" onClick={onCancelEdit}>
                Cancel
              </Button>
            )}
          </div>
        )}
        {fields.length === 0 ? (
          <div className="text-muted">Run the preview once to load available columns.</div>
        ) : (
          <QueryBuilder group={group} fields={fields} onChange={setGroup} />
        )}
        <div className="mt-3">
          <Button variant="primary" onClick={handleApply} disabled={disabled || fields.length === 0}>
            {applyLabel ?? 'Apply'}
          </Button>
        </div>
      </details>
    </div>
  );
}
