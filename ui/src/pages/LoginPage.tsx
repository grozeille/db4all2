import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../services/userApi';
import { checkInitialization } from '../services/setupApi';

const LoginPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true); // To show a loading state during check
    const navigate = useNavigate();

    useEffect(() => {
        const performSetupCheck = async () => {
            try {
                const isInitialized = await checkInitialization();
                if (!isInitialized) {
                    navigate('/setup');
                } else {
                    setIsLoading(false); // Show login form
                }
            } catch (err) {
                setError('Could not connect to the server.');
                console.error(err);
                setIsLoading(false);
            }
        };

        performSetupCheck();
    }, [navigate]);

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setError(null);
        try {
            const data = await login(email, password);
            if (data && data.access_token) {
                localStorage.setItem('token', data.access_token);
                navigate('/'); // Redirect to the main application
            } else {
                throw new Error('Login response did not contain a token.');
            }
        } catch (err) {
            setError('Invalid email or password');
            console.error(err);
        }
    };

    if (isLoading) {
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
                                    />
                                </div>
                                {error && <div className="alert alert-danger">{error}</div>}
                                <button type="submit" className="btn btn-primary w-100">
                                    Login
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
