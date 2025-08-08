const API_URL = '/api/v2/users';

export async function login(credentials: { username?: string, password?: string }): Promise<string> {
  const res = await fetch(`${API_URL}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials)
  });

  if (!res.ok) {
    throw new Error('Login failed'); // Or handle different statuses
  }

  const data = await res.json();
  return data.token;
}
