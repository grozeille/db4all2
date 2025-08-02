import React from 'react';
import PageLayout from '../components/PageLayout';
import { useParams } from 'react-router-dom';

export default function TableEditPage() {
  const { projectId, tableId } = useParams();
  // TODO: formulaire de mapping, configuration, upload, scan, etc.
  return (
    <PageLayout>
      <h2>{tableId ? `Édition de la table ${tableId}` : "Ajout d'une nouvelle table"}</h2>
      <div className="alert alert-info">(Formulaire d'édition/ajout à implémenter ici)</div>
    </PageLayout>
  );
}
