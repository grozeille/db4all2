import type { Table, TableQueryRequest, TableQueryResponse, TableUpsertRequest } from '../types/table';
import type { Page } from '../types/page';
import { getAuthHeaders } from "./utils";

const API_URL = '/api/v2/projects';

interface Pageable {
  page?: number;
  size?: number;
  sort?: string[];
}

export async function getTables(projectId: string, search: string = '', pageable: Pageable = { page: 0, size: 10 }): Promise<Page<Table>> {
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
    pageable.sort.forEach(sortParam => {
        params.append('sort', sortParam);
    });
  }

  const res = await fetch(`${API_URL}/${projectId}/tables?${params.toString()}`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Unable to load tables (${res.status})`);
  return await res.json();
}

export async function getTable(projectId: string, tableId: string): Promise<Table> {
  const res = await fetch(`${API_URL}/${projectId}/tables/${tableId}`, { headers: getAuthHeaders() });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    if (err.message) throw new Error(err.message);
    if (res.status === 404) throw new Error('Table not found');
    throw new Error(`Unable to load table (${res.status})`);
  }
  return await res.json();
}

export async function createTable(projectId: string, data: TableUpsertRequest): Promise<Table> {
  const res = await fetch(`${API_URL}/${projectId}/tables`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data)
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Unable to create table (${res.status})`);
  }
  return await res.json();
}

export async function updateTable(projectId: string, tableId: string, data: TableUpsertRequest): Promise<Table> {
  const res = await fetch(`${API_URL}/${projectId}/tables/${tableId}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data)
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Unable to update table (${res.status})`);
  }
  return await res.json();
}

export async function deleteTable(projectId: string, tableId: string): Promise<void> {
  const res = await fetch(`${API_URL}/${projectId}/tables/${tableId}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Unable to delete table (${res.status})`);
  }
}

export async function queryTable(projectId: string, tableId: string, data: TableQueryRequest): Promise<TableQueryResponse> {
  const res = await fetch(`${API_URL}/${projectId}/tables/${tableId}/query`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Unable to query table (${res.status})`);
  }
  return await res.json();
}
