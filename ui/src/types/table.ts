export type TableSourceKind = 'CSV' | 'PARQUET';

export type TableConfiguration = {
  path: string;
  separator?: string;
  firstRowAsHeader?: boolean;
};

export type QueryConditionOperator =
  | 'EQ'
  | 'NEQ'
  | 'GT'
  | 'GTE'
  | 'LT'
  | 'LTE'
  | 'IN'
  | 'NOT_IN'
  | 'CONTAINS'
  | 'NOT_CONTAINS'
  | 'STARTS_WITH'
  | 'NOT_STARTS_WITH'
  | 'ENDS_WITH'
  | 'NOT_ENDS_WITH'
  | 'IS_NULL'
  | 'IS_NOT_NULL';

export type TableQueryCondition = {
  column: string;
  operator: QueryConditionOperator;
  value?: string | number | boolean;
  values?: Array<string | number | boolean>;
};

export type TableQueryFilterGroup = {
  operator: 'AND' | 'OR';
  conditions?: TableQueryCondition[];
  groups?: TableQueryFilterGroup[];
};

export type TableQuerySelectedColumn = {
  column: string;
  alias?: string;
};

export type TableQuerySort = {
  column: string;
  direction: 'ASC' | 'DESC';
};

export type TableQueryAggregationFunction = {
  column: string;
  function: string;
  alias?: string;
};

export type TableQueryAggregations = {
  groupedBy?: string[];
  functions?: TableQueryAggregationFunction[];
};

export type TableQueryRequest = {
  selectedColumns?: TableQuerySelectedColumn[];
  distinct?: boolean;
  filters?: TableQueryFilterGroup[];
  aggregations?: TableQueryAggregations;
  sorts?: TableQuerySort[];
  page?: number;
  size?: number;
};

export type TableQueryColumnDefinition = {
  name: string;
  dataType: string;
};

export type TableQueryResponse = {
  columns: TableQueryColumnDefinition[];
  rows: Array<Record<string, unknown>>;
};

export type Table = {
  id: string;
  name: string;
  description: string;
  datasourceId: string;
  sourceKind: TableSourceKind;
  configuration: TableConfiguration;
};

export type TableUpsertRequest = {
  name: string;
  description: string;
  datasourceId: string;
  sourceKind: TableSourceKind;
  configuration: TableConfiguration;
};
