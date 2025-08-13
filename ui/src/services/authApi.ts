import type { LoginResponse } from '../types/auth';
import { ApiError } from './utils';

const API_URL = '/api/v2/auth';

export async function login(username: string, password: string):Promise<LoginResponse> {
  const params = new URLSearchParams();
  params.append('username', username);
  params.append('password', password);

  const response = await fetch(`${API_URL}/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params.toString(),
  });

  if (!response.ok) {
    let errorMessage: string;
    switch (response.status) {
      case 404:
        errorMessage = "This user doesn't exist";
        break;
      case 401:
        errorMessage = "Invalid credentials";
        break;
      default:
        errorMessage = 'Login failed with status: ' + response.status;
    }
    throw new ApiError(errorMessage, response.status);
  }

  return response.json();
}
