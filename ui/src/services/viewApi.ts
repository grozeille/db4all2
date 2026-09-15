import type { View, ViewUpsertRequest } from '../types/view';
import { getAuthHeaders } from './utils';

const API_URL = '/api/v2/projects';

export async function createView(projectId: string, request: ViewUpsertRequest): Promise<View> {
  const response = await fetch(`${API_URL}/${projectId}/views`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}));
    throw new Error(payload.message || `Unable to create view (${response.status})`);
  }
  return response.json();
}