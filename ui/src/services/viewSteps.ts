import type { QueryGroup } from '../components/QueryBuilder';
import type {
  TableQueryAggregations,
  TableQueryCondition,
  TableQueryFilterGroup,
  TableQueryRequest,
  TableQuerySelectedColumn,
  TableQuerySort,
} from '../types/table';
import type { ViewStepType, ViewTransformationStep } from '../types/view';

export type CompiledSteps = {
  query: TableQueryRequest;
  outputColumns: string[];
  logicalSorts: TableQuerySort[];
  effectiveColumns: EffectiveColumn[];
};

export type EffectiveColumn = {
  logical: string;
  physical: string;
};

export function createStepId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `step-${Date.now()}-${Math.floor(Math.random() * 1000000000)}`;
}

export function makeFilterStep(group: TableQueryFilterGroup): ViewTransformationStep {
  return {
    id: createStepId(),
    type: 'FILTER',
    label: 'Filter',
    filter: group,
  };
}

export function makeRenameStep(column: string, newName: string): ViewTransformationStep {
  return {
    id: createStepId(),
    type: 'RENAME',
    label: 'Rename columns',
    column,
    newName,
  };
}

export function makeDropStep(column: string): ViewTransformationStep {
  return {
    id: createStepId(),
    type: 'DROP',
    label: 'Remove columns',
    column,
  };
}

export function makeReorderStep(columns: string[]): ViewTransformationStep {
  return {
    id: createStepId(),
    type: 'REORDER',
    label: 'Reorder columns',
    columns: [...columns],
  };
}

export function makeSortStep(sorts: TableQuerySort[]): ViewTransformationStep {
  return {
    id: createStepId(),
    type: 'SORT',
    label: describeSorts(sorts),
    sorts: sorts.map((sort) => ({ ...sort })),
  };
}

export function describeSorts(sorts: TableQuerySort[]): string {
  if (sorts.length === 0) {
    return 'Clear sort';
  }
  return 'Sort';
}

export function sameSorts(left: TableQuerySort[], right: TableQuerySort[]): boolean {
  if (left.length !== right.length) {
    return false;
  }
  return left.every((sort, index) => sort.column === right[index].column && sort.direction === right[index].direction);
}

export function sameStringArray(left: string[], right: string[]): boolean {
  return left.length === right.length && left.every((value, index) => value === right[index]);
}

/**
 * Appends steps, merging a new SORT step into the previous one when they are
 * consecutive: refining a sort (asc then desc, or picking another direction
 * from the menu) updates a single step instead of stacking one step per click.
 * Clearing a sort right after setting it removes the step altogether.
 * The merged step keeps the previous id so saved views still match by prefix.
 */
export function coalesceSortSteps(
  current: ViewTransformationStep[],
  added: ViewTransformationStep[],
): ViewTransformationStep[] {
  if (
    added.length === 1
    && added[0].type === 'SORT'
    && current.length > 0
    && current[current.length - 1].type === 'SORT'
  ) {
    const sorts = added[0].sorts || [];
    if (sorts.length === 0) {
      return current.slice(0, -1);
    }
    return [...current.slice(0, -1), { ...added[0], id: current[current.length - 1].id }];
  }
  return [...current, ...added];
}

const COLUMN_STEP_TYPES: ReadonlySet<string> = new Set(['RENAME', 'DROP', 'REORDER', 'COLUMNS']);

export function isColumnStep(step: ViewTransformationStep): boolean {
  return COLUMN_STEP_TYPES.has(step.type);
}

/**
 * Appends steps, merging a new RENAME/DROP/REORDER operation into the trailing
 * run of column operations as a single COLUMNS step holding the net result.
 * A run that nets to no change disappears.
 * FILTER and SORT steps break the run and are never merged.
 */
export function coalesceColumnSteps(
  baseColumns: string[],
  current: ViewTransformationStep[],
  added: ViewTransformationStep[],
): ViewTransformationStep[] {
  if (added.length !== 1 || !isColumnStep(added[0])) {
    return [...current, ...added];
  }
  let runStart = current.length;
  while (runStart > 0 && isColumnStep(current[runStart - 1])) {
    runStart--;
  }
  if (runStart >= current.length) {
    return [...current, ...added];
  }
  try {
    const prefix = current.slice(0, runStart);
    const before = compileSteps(prefix, baseColumns).effectiveColumns;
    const work = before.map((column) => ({ ...column }));
    for (const step of [...current.slice(runStart), ...added]) {
      applyColumnStep(work, step, step.label || step.type);
    }
    const label = describeColumnChange(before, work);
    if (!label) {
      return prefix;
    }
    const merged: ViewTransformationStep = {
      id: createStepId(),
      type: 'COLUMNS',
      label,
      selections: work.map((column) => {
        const source = before.find((candidate) => candidate.physical === column.physical);
        const entry: TableQuerySelectedColumn = { column: source ? source.logical : column.physical };
        if (column.logical !== entry.column) {
          entry.alias = column.logical;
        }
        return entry;
      }),
    };
    return [...prefix, merged];
  } catch {
    return [...current, ...added];
  }
}

/**
 * Appends transformation steps with consecutive-step coalescing:
 * SORT merges with a trailing SORT, RENAME/DROP/REORDER merge with a trailing
 * column run. Anything else appends unchanged.
 */
export function appendTransformationSteps(
  baseColumns: string[],
  current: ViewTransformationStep[],
  added: ViewTransformationStep[],
): ViewTransformationStep[] {
  if (added.length === 1 && added[0].type === 'SORT') {
    return coalesceSortSteps(current, added);
  }
  if (added.length === 1 && isColumnStep(added[0])) {
    return coalesceColumnSteps(baseColumns, current, added);
  }
  return [...current, ...added];
}

function describeColumnChange(before: EffectiveColumn[], after: EffectiveColumn[]): string {
  const beforeByPhysical = new Map(before.map((column) => [column.physical, column.logical]));
  const afterPhysicals = new Set(after.map((column) => column.physical));
  const renamed = after.some((column) => {
    const previous = beforeByPhysical.get(column.physical);
    return previous !== undefined && previous !== column.logical;
  });
  const removed = before.some((column) => !afterPhysicals.has(column.physical));
  const beforeOrder = before.filter((column) => afterPhysicals.has(column.physical)).map((column) => column.physical);
  const reordered = !sameStringArray(beforeOrder, after.map((column) => column.physical));
  const kinds = [renamed, removed, reordered].filter(Boolean).length;
  if (kinds === 0) {
    return '';
  }
  if (kinds > 1) {
    return 'Edit columns';
  }
  if (renamed) {
    return 'Rename columns';
  }
  if (removed) {
    return 'Remove columns';
  }
  return 'Reorder columns';
}

/**
 * Converts legacy single-column SORT steps (column + direction) into the
 * full-list form (sorts), accumulating them so previews stay identical.
 */
export function normalizeSteps(steps: ViewTransformationStep[]): ViewTransformationStep[] {
  const normalized: ViewTransformationStep[] = [];
  let cumulative: TableQuerySort[] = [];
  for (const step of steps) {
    const type = ((step.type || '') as string).toUpperCase() as ViewStepType;
    if (type === 'SORT' && !step.sorts) {
      if (step.column) {
        const direction = (step.direction || 'ASC').toUpperCase() === 'DESC' ? 'DESC' : 'ASC';
        cumulative = [...cumulative.filter((sort) => sort.column !== step.column), { column: step.column, direction }];
      }
      normalized.push({ ...step, type, sorts: [...cumulative] });
    } else {
      if (type === 'SORT' && step.sorts) {
        cumulative = step.sorts.map((sort) => ({ ...sort }));
      }
      normalized.push({ ...step, type });
    }
  }
  return normalized;
}

/**
 * Compiles transformation steps into a single backend query.
 * Column references inside steps use the logical names visible at that step;
 * they are rewritten to physical source names for filters and sorts, while
 * renames, removals and reorders are folded into the final projection.
 */
export function compileSteps(
  steps: ViewTransformationStep[],
  baseColumns: string[],
  carryOver?: { distinct?: boolean; aggregations?: TableQueryAggregations },
): CompiledSteps {
  const effective: EffectiveColumn[] = baseColumns.map((name) => ({ logical: name, physical: name }));
  const filters: TableQueryFilterGroup[] = [];
  let sorts: TableQuerySort[] = [];
  let logicalSorts: TableQuerySort[] = [];
  let columnsTouched = false;

  steps.forEach((step, index) => {
    const stepRef = `Step ${index + 1} (${step.label || step.type})`;
    switch (step.type) {
      case 'FILTER': {
        if (!step.filter) {
          throw new Error(`${stepRef}: missing filter definition.`);
        }
        filters.push(mapFilterGroup(step.filter, (name) => resolvePhysical(effective, name, stepRef)));
        break;
      }
      case 'RENAME':
      case 'DROP':
      case 'REORDER':
      case 'COLUMNS': {
        applyColumnStep(effective, step, stepRef);
        columnsTouched = true;
        break;
      }
      case 'SORT': {
        const wanted = normalizeSortList(step, stepRef);
        sorts = wanted.map((sort) => ({ column: resolvePhysical(effective, sort.column, stepRef), direction: sort.direction }));
        logicalSorts = wanted.map((sort) => ({ ...sort }));
        break;
      }
      default:
        throw new Error(`${stepRef}: unsupported step type "${step.type}".`);
    }
  });

  let selectedColumns: TableQuerySelectedColumn[] | undefined;
  if (columnsTouched) {
    selectedColumns = effective.map((column) =>
      column.logical === column.physical ? { column: column.physical } : { column: column.physical, alias: column.logical },
    );
  }

  return {
    query: {
      selectedColumns,
      distinct: carryOver?.distinct,
      filters: filters.length > 0 ? filters : undefined,
      aggregations: carryOver?.aggregations,
      sorts: sorts.length > 0 ? sorts : undefined,
    },
    outputColumns: effective.map((column) => column.logical),
    logicalSorts,
    effectiveColumns: effective.map((column) => ({ ...column })),
  };
}

function applyColumnStep(effective: EffectiveColumn[], step: ViewTransformationStep, stepRef: string): void {
  switch (step.type) {
    case 'RENAME': {
      const target = findEffective(effective, step.column);
      if (!target) {
        throw new Error(`${stepRef}: unknown column "${step.column}".`);
      }
      const newName = (step.newName || '').trim();
      if (!newName) {
        throw new Error(`${stepRef}: new name is required.`);
      }
      if (effective.some((candidate) => candidate !== target && candidate.logical.toLowerCase() === newName.toLowerCase())) {
        throw new Error(`${stepRef}: column "${newName}" already exists.`);
      }
      target.logical = newName;
      break;
    }
    case 'DROP': {
      const position = effective.findIndex((candidate) => matchesColumn(candidate.logical, step.column));
      if (position < 0) {
        throw new Error(`${stepRef}: unknown column "${step.column}".`);
      }
      if (effective.length === 1) {
        throw new Error(`${stepRef}: cannot remove the last column.`);
      }
      effective.splice(position, 1);
      break;
    }
    case 'REORDER': {
      const wanted = step.columns || [];
      if (wanted.length !== effective.length || !wanted.every((name) => effective.some((candidate) => candidate.logical === name))) {
        throw new Error(`${stepRef}: reorder list does not match the current columns.`);
      }
      effective.sort((left, right) => wanted.indexOf(left.logical) - wanted.indexOf(right.logical));
      break;
    }
    case 'COLUMNS': {
      const selections = step.selections || [];
      if (selections.length === 0) {
        throw new Error(`${stepRef}: column selection is empty.`);
      }
      const next: EffectiveColumn[] = selections.map((selection) => {
        const target = findEffective(effective, selection.column);
        if (!target) {
          throw new Error(`${stepRef}: unknown column "${selection.column}".`);
        }
        return { logical: (selection.alias || '').trim() || target.logical, physical: target.physical };
      });
      const seen = new Set<string>();
      for (const column of next) {
        const key = column.logical.toLowerCase();
        if (seen.has(key)) {
          throw new Error(`${stepRef}: column "${column.logical}" appears twice.`);
        }
        seen.add(key);
      }
      effective.length = 0;
      effective.push(...next);
      break;
    }
    default:
      throw new Error(`${stepRef}: unsupported column step type "${step.type}".`);
  }
}

function normalizeSortList(step: ViewTransformationStep, stepRef: string): TableQuerySort[] {
  const raw = step.sorts || (step.column ? [{ column: step.column, direction: step.direction || 'ASC' }] : []);
  return raw.map((sort) => {
    const direction = (sort.direction || '').toUpperCase();
    if (direction !== 'ASC' && direction !== 'DESC') {
      throw new Error(`${stepRef}: invalid sort direction "${sort.direction}".`);
    }
    return { column: sort.column, direction };
  });
}

function matchesColumn(logical: string, wanted: string | undefined): boolean {
  return !!wanted && logical.toLowerCase() === wanted.toLowerCase();
}

function findEffective(effective: EffectiveColumn[], wanted: string | undefined): EffectiveColumn | undefined {
  return effective.find((candidate) => candidate.logical === wanted) || effective.find((candidate) => matchesColumn(candidate.logical, wanted));
}

function resolvePhysical(effective: EffectiveColumn[], wanted: string | undefined, stepRef: string): string {
  const target = findEffective(effective, wanted);
  if (!target) {
    throw new Error(`${stepRef}: unknown column "${wanted}".`);
  }
  return target.physical;
}

function mapFilterGroup(group: TableQueryFilterGroup, resolve: (name: string) => string): TableQueryFilterGroup {
  return {
    operator: group.operator,
    conditions: group.conditions?.map((condition) => ({ ...condition, column: resolve(condition.column) })),
    groups: group.groups?.map((nested) => mapFilterGroup(nested, resolve)),
  };
}

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

export function buildFilterGroups(group: QueryGroup): TableQueryFilterGroup[] | undefined {
  const normalized = normalizeGroup(group);
  if (!normalized) {
    return undefined;
  }
  return [normalized];
}

const REVERSE_OPERATOR_MAPPING: Record<string, string> = {
  'EQ': '=',
  'NEQ': '!=',
  'LT': '<',
  'LTE': '<=',
  'GT': '>',
  'GTE': '>=',
  'IS_NULL': 'IS NULL',
  'IS_NOT_NULL': 'IS NOT NULL',
  'STARTS_WITH': 'STARTS WITH',
  'NOT_STARTS_WITH': 'NOT STARTS WITH',
  'CONTAINS': 'CONTAINS',
  'NOT_CONTAINS': 'NOT CONTAINS',
  'ENDS_WITH': 'ENDS WITH',
  'NOT_ENDS_WITH': 'NOT ENDS WITH',
  'IN': 'IN',
  'NOT_IN': 'NOT IN',
};

/**
 * Converts a saved filter group back into a builder group so a FILTER step
 * can be reloaded for editing. Boolean shortcuts the builder cannot express
 * fall back to an equality check; unknown operators are skipped.
 */
export function queryGroupFromFilterGroup(group: TableQueryFilterGroup): QueryGroup {
  const rules: QueryGroup['rules'] = [];
  for (const condition of group.conditions || []) {
    const operator = (condition.operator || '').toUpperCase();
    if (operator === 'IS_TRUE' || operator === 'IS_FALSE') {
      rules.push({ field: condition.column, condition: '=', data: operator === 'IS_TRUE' ? 'true' : 'false' });
      continue;
    }
    const mapped = REVERSE_OPERATOR_MAPPING[operator];
    if (!mapped || !condition.column) {
      continue;
    }
    if (operator === 'IN' || operator === 'NOT_IN') {
      rules.push({ field: condition.column, condition: mapped, data: (condition.values || []).join(', ') });
    } else if (operator === 'IS_NULL' || operator === 'IS_NOT_NULL') {
      rules.push({ field: condition.column, condition: mapped });
    } else {
      rules.push({ field: condition.column, condition: mapped, data: condition.value === undefined ? '' : String(condition.value) });
    }
  }
  for (const nested of group.groups || []) {
    rules.push({ group: queryGroupFromFilterGroup(nested) });
  }
  return { operator: group.operator === 'OR' ? 'OR' : 'AND', rules };
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
