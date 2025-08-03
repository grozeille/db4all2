import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageLayout from '../components/PageLayout';

export default function ProjectCreatePage() {
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    try {
      // TODO: Appeler le vrai service d'API
      // await createProject({ name });
      navigate('/projects');
    } catch (err: any) {
      setError(err.message || 'Unknown error');
    }
  };

  return (
    <PageLayout>
      <h2>Create a new project</h2>
      <form onSubmit={handleSubmit} className="mt-4" style={{maxWidth: 400}}>
        <div className="mb-3">
          <label htmlFor="projectName" className="form-label">Project name</label>
          <input
            type="text"
            className="form-control"
            id="projectName"
            value={name}
            onChange={e => setName(e.target.value)}
            required
          />
        </div>
        {error && <div className="text-danger mb-2">{error}</div>}
        <button type="submit" className="btn btn-primary">Create</button>
      </form>
    </PageLayout>
  );
}
