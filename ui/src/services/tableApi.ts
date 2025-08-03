
import type { Table } from '../types/table';

const API_URL = '/projects';

export async function getTables(projectId: string, search = '', page = 1): Promise<Table[]> {
  const params = search ? `?search=${encodeURIComponent(search)}` : '';
  const res = await fetch(`${API_URL}/${projectId}/tables${params}`);
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return await res.json();
}

export async function getTable(projectId: string, tableId: string): Promise<Table> {
  const res = await fetch(`${API_URL}/${projectId}/tables/${tableId}`);
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    if (err.message) throw new Error(err.message);
    if (res.status === 404) throw new Error('Table introuvable');
    throw new Error(`Erreur ${res.status}`);
  }
  return await res.json();
}

export async function createTable(projectId: string, data: Partial<Table>): Promise<string> {
  const res = await fetch(`${API_URL}/${projectId}/tables`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Erreur ${res.status}`);
  }
  const table = await res.json();
  return table.id;
}

export async function updateTable(projectId: string, tableId: string, data: Partial<Table>): Promise<void> {
  const res = await fetch(`${API_URL}/${projectId}/tables/${tableId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Erreur ${res.status}`);
  }
}

export async function deleteTable(projectId: string, tableId: string): Promise<void> {
  const res = await fetch(`${API_URL}/${projectId}/tables/${tableId}`, { method: 'DELETE' });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Erreur ${res.status}`);
  }
}
