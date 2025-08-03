import React, { useState } from 'react';
import PageLayout from '../components/PageLayout';
import { useNavigate } from 'react-router-dom';

export default function InitializationPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    try {
      const res = await fetch('/auth/initialize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, passwordConfirm })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        setError(err.message || 'Initialization failed');
        return;
      }
      // Après création, redirige vers login
      navigate('/login');
    } catch (err: any) {
      setError(err.message || 'Network error');
    }
  };

  return (
    <PageLayout>
      <h2>Initialize the application</h2>
      <form onSubmit={handleSubmit} style={{ maxWidth: 400 }} className="mt-4">
        <div className="mb-3">
          <label className="form-label">Email</label>
          <input type="email" className="form-control" value={email} onChange={e => setEmail(e.target.value)} required />
        </div>
        <div className="mb-3">
          <label className="form-label">Password</label>
          <input type="password" className="form-control" value={password} onChange={e => setPassword(e.target.value)} required />
        </div>
        <div className="mb-3">
          <label className="form-label">Confirm password</label>
          <input type="password" className="form-control" value={passwordConfirm} onChange={e => setPasswordConfirm(e.target.value)} required />
        </div>
        {error && <div className="text-danger mb-2">{error}</div>}
        <button type="submit" className="btn btn-primary">Initialize</button>
      </form>
    </PageLayout>
  );
}
