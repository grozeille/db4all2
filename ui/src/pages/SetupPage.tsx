import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { initializeApplication, checkInitialization } from '../services/setupApi';
import { useApi } from '../hooks/useApi';

const SetupPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    // Local error for password confirmation, separate from API errors
    const [formError, setFormError] = useState<string | null>(null);
    const navigate = useNavigate();

    // Hook to check if the application is already set up
    const { execute: performSetupCheck } = useApi(checkInitialization);

    // Hook for the main initialization API call
    const { isLoading: isSubmitting, error: apiError, execute: performInit } = useApi(initializeApplication);

    useEffect(() => {
        performSetupCheck()
            .then((result) => {
                if (result) {
                    // If this succeeds, the app is already initialized.
                    navigate('/login');
                }
            })
            .catch(() => {
                // This is expected if the app is not set up.
                console.info("Application not initialized, displaying setup page.");
            });
    }, []);

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setFormError(null);

        if (password !== confirmPassword) {
            setFormError('Passwords do not match.');
            return;
        }

        try {
            const result = await performInit(email, password);
            if (result) {
                navigate('/login', { state: { message: 'Application initialized successfully! Please log in.' } });
            }
        } catch (err) {
            // The useApi hook handles setting the apiError state.
            // We just need to catch the promise rejection to prevent unhandled promise errors.
            console.error("Initialization failed:", err);
        }
    };

    // Combine form validation error with API error for display
    const displayError = formError || (apiError ? apiError.message : null);

    return (
        <div className="container mt-5">
            <div className="row justify-content-center">
                <div className="col-md-6">
                    <div className="card">
                        <div className="card-body">
                            <h3 className="card-title text-center">Application Setup</h3>
                            <p className="text-center text-muted">Create the first administrator account to begin.</p>
                            <form onSubmit={handleSubmit}>
                                <div className="form-group mb-3">
                                    <label htmlFor="email">Administrator Email</label>
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
                                <div className="form-group mb-3">
                                    <label htmlFor="confirmPassword">Confirm Password</label>
                                    <input
                                        type="password"
                                        className="form-control"
                                        id="confirmPassword"
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        required
                                        disabled={isSubmitting}
                                    />
                                </div>
                                {displayError && <div className="alert alert-danger">{displayError}</div>}
                                <button type="submit" className="btn btn-primary w-100" disabled={isSubmitting}>
                                    {isSubmitting ? 'Creating...' : 'Create Administrator'}
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SetupPage;
