import type { TableQueryFilterGroup, TableQueryRequest, TableQuerySelectedColumn, TableQuerySort } from './table';

export type ViewType = 'FILTER';

export type ViewStepType = 'FILTER' | 'RENAME' | 'DROP' | 'REORDER' | 'SORT' | 'COLUMNS';

export type ViewTransformationStep = {
  id: string;
  type: ViewStepType;
  label: string;
  filter?: TableQueryFilterGroup;
  column?: string;
  newName?: string;
  columns?: string[];
  direction?: 'ASC' | 'DESC';
  sorts?: TableQuerySort[];
  selections?: TableQuerySelectedColumn[];
};

export type View = {
  id: string;
  name: string;
  description: string;
  type: ViewType;
  sourceTableId: string;
  parentViewId?: string | null;
  query: TableQueryRequest;
  steps: ViewTransformationStep[];
};

export type ViewUpsertRequest = {
  name: string;
  description: string;
  type: ViewType;
  sourceTableId?: string;
  parentViewId?: string | null;
  steps: ViewTransformationStep[];
  query: TableQueryRequest;
};

export type ViewPreviewRequest = {
  sourceTableId?: string;
  parentViewId?: string;
  query: TableQueryRequest;
};
