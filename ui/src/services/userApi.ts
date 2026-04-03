import type { UpdatePasswordRequest, User, UserCreate } from "../types/auth.ts";
import { api } from "./utils.ts";

export const userApi = {
  getMe: async function() {
    return await api.get<User>("/api/v2/users/me");
  },
  getAll: async function() {
    return await api.get<User[]>(`/api/v2/users`);
  },
  createUser: async function(user: UserCreate): Promise<User | boolean> {
    return await api.post<User>("/api/v2/users", user);
  },
  adminUpdatePassword: async function(login: string, request: UpdatePasswordRequest): Promise<boolean> {
    return await api.put<void>(`/api/v2/users/${login}/password`, request) as boolean;
  },
  updateSuperAdminStatus: async function(login: string, superAdmin: boolean): Promise<User | boolean> {
    return await api.put<User>(`/api/v2/users/${login}/superadmin`, { superAdmin });
  },
  updateMyPassword: async function(request: UpdatePasswordRequest) {
    return await api.put<void>("/api/v2/users/password", request);
  }
}