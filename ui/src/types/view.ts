import type { TableQueryRequest } from './table';

export type ViewType = 'FILTER';

export type View = {
  id: string;
  name: string;
  description: string;
  type: ViewType;
  sourceTableId: string;
  query: TableQueryRequest;
};

export type ViewUpsertRequest = {
  name: string;
  description: string;
  type: ViewType;
  sourceTableId: string;
  query: TableQueryRequest;
};