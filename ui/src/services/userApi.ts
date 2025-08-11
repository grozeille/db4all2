import type { UpdatePasswordRequest, User, UserCreate } from "../types/auth.ts";
import { api } from "./utils.ts";

export const userApi = {
  getMe: async function() {
    return await api.get<User>("/api/v2/users/me");
  },
  getAll: async function() {
    return await api.get<User[]>(`/api/v2/users`);
  },
  createUser: async function(user: UserCreate) {
    return await api.post<User>("/api/v2/users", user);
  },
  adminUpdatePassword: async function(login: string, request: UpdatePasswordRequest) {
    return await api.put<void>(`/api/v2/users/${login}/password`, request);
  },
  updateMyPassword: async function(request: UpdatePasswordRequest) {
    return await api.put<void>("/api/v2/users/password", request);
  }
}