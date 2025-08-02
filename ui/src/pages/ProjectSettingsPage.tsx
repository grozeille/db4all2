import React from 'react';
import { useParams } from 'react-router-dom';

export default function ProjectSettingsPage() {
  const { projectId } = useParams();
  // TODO: fake data, gestion du nom, description, membres, data-sources, suppression
  return (
    <div className="container py-4">
      <h2>Configuration du projet {projectId}</h2>
      <div className="alert alert-info">(Formulaires de configuration à implémenter ici)</div>
    </div>
  );
}
