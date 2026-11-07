export type DatasourceType = 'LOCAL_FILESYSTEM';

export type LocalFilesystemDatasourceConfiguration = {
  rootPath: string;
};

export type Datasource = {
  id: string;
  name: string;
  description: string;
  type: DatasourceType;
  readOnly: boolean;
  configuration: LocalFilesystemDatasourceConfiguration;
};

export type DatasourceUpsertRequest = {
  name: string;
  description: string;
  type: DatasourceType;
  readOnly: boolean;
  configuration: LocalFilesystemDatasourceConfiguration;
};

export type DatasourceScanType = 'EXCEL_SHEETS' | 'EXCEL_NAMED_RANGES';

export type DatasourceScanRequest = {
  filePath: string;
  scanType: DatasourceScanType;
};

export type ScanItem = {
  id: string;
  label: string;
  itemType: string;
  metadata?: Record<string, unknown>;
};

export type DatasourceScanResponse = {
  items: ScanItem[];
};