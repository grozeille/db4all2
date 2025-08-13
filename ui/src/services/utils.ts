export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export const getAuthHeaders = () => {
  const token = localStorage.getItem('token');
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
};

export const api = {
  get: async function<T>(url: string): Promise<T> {
    const response = await fetch(url, {
      method: 'GET',
      headers: getAuthHeaders()
    });
    if (response.status == 401) {
      window.location.pathname = "/login";
    }
    if (!response.ok) {
      throw new Error(await response.text());
    }
    return response.json();
  },
  post: async function<T>(url: string, data: any): Promise<T> {
    const response = await fetch(url, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    if (!response.ok) {
      throw new Error(await response.text());
    }
    return response.json();
  },
  put: async function<T>(url: string, data: any): Promise<T> {
    const response = await fetch(url, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    if (!response.ok) {
      throw new Error(await response.text());
    }
    return response.json();
  }
};