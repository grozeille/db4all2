import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PageLayout from '../components/PageLayout';

export default function ProjectCreatePage() {
  const [name, setName] = useState('');
  const navigate = useNavigate();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Ici, on simule la création, puis on redirige vers la liste
    // TODO: Appeler le vrai service d'API
    //navigate('/projects');
  };

  return (
    <PageLayout>
      <h2>Créer un nouveau projet</h2>
      <form onSubmit={handleSubmit} className="mt-4" style={{maxWidth: 400}}>
        <div className="mb-3">
          <label htmlFor="projectName" className="form-label">Nom du projet</label>
          <input
            type="text"
            className="form-control"
            id="projectName"
            value={name}
            onChange={e => setName(e.target.value)}
            required
          />
        </div>
        <button type="submit" className="btn btn-primary">Créer</button>
      </form>
    </PageLayout>
  );
}
