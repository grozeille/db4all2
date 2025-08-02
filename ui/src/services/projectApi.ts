import type { Project } from '../types/project';

export async function getProjects(search = '', page = 1): Promise<Project[]> {
  // TODO: Replace with real API call
  return Array.from({ length: 30 }, (_, i) => ({
    id: (i + 1).toString(),
    name: `Project ${i + 1}`,
    description: `Description for project ${i + 1}`
  }));
}

export async function getProject(id: string): Promise<Project> {
  // TODO: Replace with real API call
  return { id, name: `Project ${id}`, description: `Description for project ${id}`, administrator: true };
}

// Add more functions as needed (createProject, updateProject, deleteProject, etc.)
