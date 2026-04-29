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