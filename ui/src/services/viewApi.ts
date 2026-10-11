import type { TableQueryRequest, TableQueryResponse } from '../types/table';
import type { Page } from '../types/page';
import type { View, ViewPreviewRequest, ViewUpsertRequest } from '../types/view';
import { getAuthHeaders } from './utils';

const API_URL = '/api/v2/projects';

interface Pageable {
  page?: number;
  size?: number;
  sort?: string[];
}

async function parseError(response: Response, fallback: string): Promise<Error> {
  const payload = await response.json().catch(() => ({}));
  const message = (payload as { message?: string }).message || `${fallback} (${response.status})`;
  return new Error(message);
}

export async function getViews(projectId: string, search = '', pageable: Pageable = { page: 0, size: 25 }): Promise<Page<View>> {
  const params = new URLSearchParams();
  if (search) {
    params.append('search', search);
  }
  if (pageable.page !== undefined) {
    params.append('page', pageable.page.toString());
  }
  if (pageable.size !== undefined) {
    params.append('size', pageable.size.toString());
  }
  if (pageable.sort) {
    pageable.sort.forEach((sortParam) => {
      params.append('sort', sortParam);
    });
  }

  const response = await fetch(`${API_URL}/${projectId}/views?${params.toString()}`, { headers: getAuthHeaders() });
  if (!response.ok) {
    throw await parseError(response, 'Unable to load views');
  }
  return response.json();
}

export async function getView(projectId: string, viewId: string): Promise<View> {
  const response = await fetch(`${API_URL}/${projectId}/views/${viewId}`, { headers: getAuthHeaders() });
  if (!response.ok) {
    throw await parseError(response, 'Unable to load view');
  }
  return response.json();
}

export async function createView(projectId: string, request: ViewUpsertRequest): Promise<View> {
  const response = await fetch(`${API_URL}/${projectId}/views`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw await parseError(response, 'Unable to create view');
  }
  return response.json();
}

export async function updateView(projectId: string, viewId: string, request: ViewUpsertRequest): Promise<View> {
  const response = await fetch(`${API_URL}/${projectId}/views/${viewId}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw await parseError(response, 'Unable to update view');
  }
  return response.json();
}

export async function deleteView(projectId: string, viewId: string): Promise<void> {
  const response = await fetch(`${API_URL}/${projectId}/views/${viewId}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });
  if (!response.ok) {
    throw await parseError(response, 'Unable to delete view');
  }
}

export async function queryView(projectId: string, viewId: string, data: TableQueryRequest): Promise<TableQueryResponse> {
  const response = await fetch(`${API_URL}/${projectId}/views/${viewId}/query`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    throw await parseError(response, 'Unable to query view');
  }
  return response.json();
}

export async function previewView(projectId: string, data: ViewPreviewRequest): Promise<TableQueryResponse> {
  const response = await fetch(`${API_URL}/${projectId}/views/preview`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    throw await parseError(response, 'Unable to preview view');
  }
  return response.json();
}
