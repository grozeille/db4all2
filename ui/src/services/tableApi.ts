import type { Table } from '../types/table';

export async function getTables(projectId: string, search = '', page = 1): Promise<Table[]> {
  // TODO: Replace with real API call
  return Array.from({ length: 10 }, (_, i) => ({
    id: (i + 1).toString(),
    name: `Table ${i + 1}`,
    description: `Description for table ${i + 1}`
  }));
}

export async function getTable(projectId: string, tableId: string): Promise<Table> {
  // TODO: Replace with real API call
  return { id: tableId, name: `Table ${tableId}`, description: `Description for table ${tableId}` };
}


export async function createTable(projectId: string, data: any): Promise<string> {
  // TODO: Remplacer par un vrai appel API
  // Retourne l'ID simulé de la nouvelle table
  return Promise.resolve('42');
}

export async function updateTable(projectId: string, tableId: string, data: any): Promise<void> {
  // TODO: Remplacer par un vrai appel API
  return Promise.resolve();
}
