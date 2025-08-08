import type { LoginResponse } from '../types/auth';

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
    if (response.status === 401) {
      throw new Error('Invalid credentials');
    }
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
