import type { UpdatePasswordRequest, User, UserCreate } from "../types/auth.ts";
import { api } from "./utils.ts";
import type { ApiListPage } from "./utils.ts";

export const userApi = {
  getMe: async function() {
    return await api.get<User>("/api/users/me");
  },
  getAll: async function() {
    return await api.get<ApiListPage<User>>(`/api/users`);
  },
  createUser: async function(user: UserCreate) {
    return await api.post<User>("/api/users", user);
  },
  adminUpdatePassword: async function(login: string, request: UpdatePasswordRequest) {
    return await api.put<void>(`/api/users/${login}/password`, request);
  },
  updateMyPassword: async function(request: UpdatePasswordRequest) {
    return await api.put<void>("/api/users/me/password", request);
  }
}