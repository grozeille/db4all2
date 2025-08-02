import React from 'react';

export type FieldOption = { name: string; groupName?: string };

const OPERATORS = [
  { name: 'AND' },
  { name: 'OR' }
];

const CONDITIONS = [
  { name: '=', noArgs: false },
  { name: '!=', noArgs: false },
  { name: '<', noArgs: false },
  { name: '<=', noArgs: false },
  { name: '>', noArgs: false },
  { name: '>=', noArgs: false },
  { name: 'IS NULL', noArgs: true },
  { name: 'IS NOT NULL', noArgs: true },
  { name: 'STARTS WITH', noArgs: false },
  { name: 'NOT STARTS WITH', noArgs: false },
  { name: 'CONTAINS', noArgs: false },
  { name: 'NOT CONTAINS', noArgs: false },
  { name: 'ENDS WITH', noArgs: false },
  { name: 'NOT ENDS WITH', noArgs: false },
  { name: 'IN', noArgs: false, help: 'use comma as separator' },
  { name: 'NOT IN', noArgs: false, help: 'use comma as separator' }
];

export type QueryCondition = {
  field: string;
  condition: string;
  data?: string;
};

export type QueryGroup = {
  operator: 'AND' | 'OR';
  rules: Array<QueryCondition | { group: QueryGroup }>;
};

type Props = {
  group: QueryGroup;
  fields: FieldOption[];
  removeVisible?: boolean;
  onChange: (group: QueryGroup) => void;
  onRemoveGroup?: () => void;
};

export const QueryBuilder: React.FC<Props> = ({ group, fields, removeVisible, onChange, onRemoveGroup }) => {
  const addCondition = () => {
    const first = fields.length > 0 ? fields[0].name : '';
    onChange({
      ...group,
      rules: [...group.rules, { field: first, condition: '=', data: '' }]
    });
  };

  const removeCondition = (idx: number) => {
    onChange({
      ...group,
      rules: group.rules.filter((_, i) => i !== idx)
    });
  };

  const addGroup = () => {
    onChange({
      ...group,
      rules: [...group.rules, { group: { operator: 'AND', rules: [] } }]
    });
  };

  const removeGroup = () => {
    if (onRemoveGroup) onRemoveGroup();
  };

  const updateRule = (idx: number, rule: QueryCondition) => {
    onChange({
      ...group,
      rules: group.rules.map((r, i) => (i === idx ? rule : r))
    });
  };

  const updateSubGroup = (idx: number, subGroup: QueryGroup) => {
    onChange({
      ...group,
      rules: group.rules.map((r, i) => (i === idx ? { group: subGroup } : r))
    });
  };

  return (
    <div className="query-group border p-2 mb-2">
      <div className="d-flex align-items-center mb-2 gap-2">
        <select
          className="form-select form-select-sm w-auto"
          value={group.operator}
          onChange={e => onChange({ ...group, operator: e.target.value as 'AND' | 'OR' })}
        >
          {OPERATORS.map(o => (
            <option key={o.name} value={o.name}>{o.name}</option>
          ))}
        </select>
        <button className="btn btn-success btn-sm" onClick={addCondition}>+ Condition</button>
        <button className="btn btn-success btn-sm" onClick={addGroup}>+ Group</button>
        {removeVisible && (
          <button className="btn btn-danger btn-sm" onClick={removeGroup}>Remove group</button>
        )}
      </div>
      <div>
        {group.rules.map((rule, idx) =>
          'group' in rule ? (
            <QueryBuilder
              key={idx}
              group={rule.group}
              fields={fields}
              removeVisible={true}
              onChange={sub => updateSubGroup(idx, sub)}
              onRemoveGroup={() => removeCondition(idx)}
            />
          ) : (
            <div className="d-flex align-items-center mb-2 gap-2" key={idx}>
              <select
                className="form-select form-select-sm w-auto"
                value={rule.field}
                onChange={e => updateRule(idx, { ...rule, field: e.target.value })}
              >
                {fields.map(f => (
                  <option key={f.name} value={f.name}>{f.name}</option>
                ))}
              </select>
              <select
                className="form-select form-select-sm w-auto"
                value={rule.condition}
                onChange={e => updateRule(idx, { ...rule, condition: e.target.value })}
              >
                {CONDITIONS.map(c => (
                  <option key={c.name} value={c.name}>{c.name}</option>
                ))}
              </select>
              {!CONDITIONS.find(c => c.name === rule.condition)?.noArgs && (
                <input
                  className="form-control form-control-sm w-auto"
                  value={rule.data || ''}
                  onChange={e => updateRule(idx, { ...rule, data: e.target.value })}
                  placeholder={CONDITIONS.find(c => c.name === rule.condition)?.help || ''}
                />
              )}
              <button className="btn btn-danger btn-sm" onClick={() => removeCondition(idx)}>Remove</button>
            </div>
          )
        )}
      </div>
    </div>
  );
};
