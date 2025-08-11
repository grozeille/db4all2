export interface LoginResponse {
  access_token: string;
}

export interface User {
  email: string;
  superAdmin: boolean;
}

export interface UserCreate {
  email: string;
  password?: string;
  superAdmin: boolean;
}

export interface UpdatePasswordRequest {
  oldPassword?: string;
  password?: string;
}