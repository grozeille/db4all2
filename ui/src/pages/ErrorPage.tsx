interface ErrorPageProps {
  code?: number;
  message?: string;
}

export default function ErrorPage({ code = 500, message }: ErrorPageProps) {
  let title = 'Erreur';
  let defaultMsg = "Une erreur est survenue.";
  if (code === 404) {
    title = 'Page introuvable';
    defaultMsg = "La ressource demandée n'existe pas.";
  } else if (code === 500) {
    title = 'Erreur serveur';
    defaultMsg = "Une erreur interne est survenue.";
  }
  return (
    <>
      <div className="d-flex flex-column align-items-center justify-content-center" style={{ minHeight: '60vh' }}>
        <h1 className="display-4 mb-3">{title} {code}</h1>
        <p className="lead text-danger">{message || defaultMsg}</p>
        <a href="/" className="btn btn-primary mt-4">Retour à l'accueil</a>
      </div>
    </>
  );
}
