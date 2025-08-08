const API_URL = '/api/v2/setup';

export async function checkInitialization(): Promise<boolean> {
    const response = await fetch(API_URL);
    if (response.ok) {
        return true; // 200 OK, initialized
    }
    if (response.status === 404) {
        return false; // 404 Not Found, not initialized
    }
    // For other errors, throw an exception
    throw new Error(`Failed to check initialization: ${response.statusText}`);
}

export async function initializeApplication(email: string, password: string) {
    const body = new URLSearchParams();
    body.append('email', email);
    body.append('password', password);

    const response = await fetch(API_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: body,
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Initialization failed' }));
        throw new Error(errorData.message);
    }

    return await response.json();
}
