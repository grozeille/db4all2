import type { Project } from '../types/project';
import type { Page } from '../types/page';
import { getAuthHeaders } from "./utils";

const API_URL = '/api/v2/projects';

interface Pageable {
  page?: number;
  size?: number;
  sort?: string[];
}

export async function getProjects(search: string = '', pageable: Pageable = { page: 0, size: 10 }): Promise<Page<Project>> {
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

  const res = await fetch(`${API_URL}?${params.toString()}`, { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return await res.json();
}


export async function getProject(id: string): Promise<Project> {
  const res = await fetch(`${API_URL}/${id}`, { headers: getAuthHeaders() });
  if (!res.ok) {
    if (res.status === 404) throw new Error('Projet introuvable');
    throw new Error(`Erreur ${res.status}`);
  }
  return await res.json();
}

export async function createProject(data: Partial<Project>): Promise<Project> {
  const res = await fetch(API_URL, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data)
  });
  if (!res.ok) {
    const msg = await res.text();
    throw new Error(msg || `Erreur ${res.status}`);
  }
  return await res.json();
}

export async function updateProject(id: string, data: Partial<Project>): Promise<Project> {
  const res = await fetch(`${API_URL}/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data)
  });
  if (!res.ok) {
    const msg = await res.text();
    throw new Error(msg || `Erreur ${res.status}`);
  }
  return await res.json();
}

export async function deleteProject(id: string): Promise<void> {
  const res = await fetch(`${API_URL}/${id}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    });
  if (!res.ok) {
    const msg = await res.text();
    throw new Error(msg || `Erreur ${res.status}`);
  }
}
