import type { Datasource, DatasourceUpsertRequest } from '../types/datasource';
import { getAuthHeaders } from './utils';

const API_URL = '/api/v2/projects';

async function readError(response: Response, fallbackMessage: string): Promise<Error> {
  const payload = await response.json().catch(() => null);
  const message = typeof payload?.message === 'string' ? payload.message : fallbackMessage;
  return new Error(message);
}

export async function getDatasources(projectId: string): Promise<Datasource[]> {
  const response = await fetch(`${API_URL}/${projectId}/datasources`, {
    headers: getAuthHeaders(),
  });
  if (!response.ok) {
    throw await readError(response, `Unable to load datasources (${response.status})`);
  }
  return response.json();
}

export async function createDatasource(projectId: string, request: DatasourceUpsertRequest): Promise<Datasource> {
  const response = await fetch(`${API_URL}/${projectId}/datasources`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw await readError(response, `Unable to create datasource (${response.status})`);
  }
  return response.json();
}

export async function updateDatasource(projectId: string, datasourceId: string, request: DatasourceUpsertRequest): Promise<Datasource> {
  const response = await fetch(`${API_URL}/${projectId}/datasources/${datasourceId}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw await readError(response, `Unable to update datasource (${response.status})`);
  }
  return response.json();
}

export async function deleteDatasource(projectId: string, datasourceId: string): Promise<void> {
  const response = await fetch(`${API_URL}/${projectId}/datasources/${datasourceId}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });
  if (!response.ok) {
    throw await readError(response, `Unable to delete datasource (${response.status})`);
  }
}

export async function testDatasourceConnection(projectId: string, datasourceId: string): Promise<string> {
  const response = await fetch(`${API_URL}/${projectId}/datasources/${datasourceId}/test-connection`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });
  if (!response.ok) {
    throw await readError(response, `Unable to test datasource (${response.status})`);
  }
  const payload = await response.json();
  return typeof payload?.message === 'string' ? payload.message : 'Connection successful.';
}