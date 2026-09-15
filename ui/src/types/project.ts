export interface ProjectAdministrator {
  email: string;
  currentUser: boolean;
}

export type Project = {
  id: string;
  name: string;
  description: string;
  administrator: boolean;
  administrators: ProjectAdministrator[];
};
