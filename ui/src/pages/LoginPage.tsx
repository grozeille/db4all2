import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../services/authApi';
import { checkInitialization } from '../services/setupApi';
import { useApi } from '../hooks/useApi';

const LoginPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate();

    // Hook for the setup check
    const { isLoading: isCheckingSetup, execute: performSetupCheck } = useApi(checkInitialization);

    // Hook for the login submission
    const { isLoading: isSubmitting, error: loginError, execute: performLogin } = useApi(login);

    useEffect(() => {
        performSetupCheck()
            .then((result) => {
                if (!result) {
                    // If this succeeds, the app is already initialized.
                    navigate('/setup');
                }
            })
            .catch(() => {
                // This means the app is not initialized, redirect to setup.
                navigate('/setup');
            });
    }, []);

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        try {
            const data = await performLogin(email, password);
            if (data && data.access_token) {
                localStorage.setItem('token', data.access_token);
                navigate('/'); // Redirect to the main application
            } else {
                // This case is unlikely if the API throws on failure, but good for safety
                throw new Error('Login response did not contain a token.');
            }
        } catch (err) {
            // The useApi hook automatically sets the error state.
            // We can log the error here or perform other side-effects if needed.
            console.error("Login failed:", err);
        }
    };

    if (isCheckingSetup) {
        return <div>Loading...</div>; // Or a spinner component
    }

    return (
        <div className="container mt-5">
            <div className="row justify-content-center">
                <div className="col-md-6">
                    <div className="card">
                        <div className="card-body">
                            <h3 className="card-title text-center">Login</h3>
                            <form onSubmit={handleSubmit}>
                                <div className="form-group mb-3">
                                    <label htmlFor="email">Email</label>
                                    <input
                                        type="email"
                                        className="form-control"
                                        id="email"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        required
                                        disabled={isSubmitting}
                                    />
                                </div>
                                <div className="form-group mb-3">
                                    <label htmlFor="password">Password</label>
                                    <input
                                        type="password"
                                        className="form-control"
                                        id="password"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        required
                                        disabled={isSubmitting}
                                    />
                                </div>
                                {loginError && <div className="alert alert-danger">{loginError}</div>}
                                <button type="submit" className="btn btn-primary w-100" disabled={isSubmitting}>
                                    {isSubmitting ? 'Logging in...' : 'Login'}
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;
