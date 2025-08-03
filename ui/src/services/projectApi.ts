
import type { Project } from '../types/project';


const API_URL = '/projects';

export async function getProjects(search = '', page = 1): Promise<Project[]> {
  const params = search ? `?search=${encodeURIComponent(search)}` : '';
  const res = await fetch(`${API_URL}${params}`);
  if (!res.ok) throw new Error(`Erreur ${res.status}`);
  return await res.json();
}


export async function getProject(id: string): Promise<Project> {
  const res = await fetch(`${API_URL}/${id}`);
  if (!res.ok) {
    if (res.status === 404) throw new Error('Projet introuvable');
    throw new Error(`Erreur ${res.status}`);
  }
  return await res.json();
}

export async function createProject(data: Partial<Project>): Promise<Project> {
  const res = await fetch(API_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
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
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) {
    const msg = await res.text();
    throw new Error(msg || `Erreur ${res.status}`);
  }
  return await res.json();
}

export async function deleteProject(id: string): Promise<void> {
  const res = await fetch(`${API_URL}/${id}`, { method: 'DELETE' });
  if (!res.ok) {
    const msg = await res.text();
    throw new Error(msg || `Erreur ${res.status}`);
  }
}

// Add more functions as needed (createProject, updateProject, deleteProject, etc.)
