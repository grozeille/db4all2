import type { LoginResponse } from '../types/auth';

const API_URL = '/api/v2/auth';

export async function login(login: string, password: string):Promise<LoginResponse> {
  const response = await fetch(`${API_URL}/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ login, password }),
  });

  if (!response.ok) {
    // Try to parse the error response, but fallback to a generic error
    try {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Login failed');
    } catch (e) {
      throw new Error('Login failed with status: ' + response.status);
    }
  }

  return response.json();
}
