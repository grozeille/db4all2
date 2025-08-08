export interface LoginResponse {
  access_token: string;
}

export interface User {
  login: string;
  superAdmin: boolean;
}

export interface UserCreate {
  login: string;
  password?: string;
  superAdmin: boolean;
}

export interface UpdatePasswordRequest {
  oldPassword?: string;
  password?: string;
}