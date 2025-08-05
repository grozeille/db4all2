import { useState, useEffect } from 'react';
import PageLayout from '../components/PageLayout';
import { useParams, useNavigate } from 'react-router-dom';
import { getProject } from '../services/projectApi';
export default function ProjectSettingsPage() {
  // Fake data
  const FAKE_MEMBERS = [
    { id: '1', name: 'Alice', role: 'Administrator', self: true },
    { id: '2', name: 'Bob', role: 'Writer' },
    { id: '3', name: 'Charlie', role: 'Reader' }
  ];
  const FAKE_DATASOURCES = [
    { id: '1', name: 'S3 bucket', type: 'S3', readonly: true },
    { id: '2', name: 'PostgreSQL', type: 'PostgreSQL', readonly: false }
  ];

  const { projectId } = useParams();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [members, _setMembers] = useState<any[]>(FAKE_MEMBERS);
  const [datasources, _setDatasources] = useState<any[]>(FAKE_DATASOURCES);
  const [deleteConfirm, setDeleteConfirm] = useState('');

  useEffect(() => {
    if (projectId) {
      getProject(projectId)
        .then(project => {
          setName(project.name);
          setDescription(project.description);
          // TODO: charger membres et datasources via API
        })
        .catch(err => {
          navigate(`/error/404`, { state: { message: err.message } });
        });
    }
  }, [projectId, navigate]);

  const handleSave = () => {
    if (!name) setError('Name is required');
    else setError('');
    // TODO: API call
  };
  const handleDelete = () => {
    // TODO: API call
    alert('Project deleted');
  };

  return (
    <PageLayout>
      <div>
        <h2 className="mb-4 text-start">Project settings: {projectId}</h2>
        <div className="mb-4 text-start">
          <h4 className="mb-3">General</h4>
          <div className="mb-3">
            <label className="form-label">Name</label>
            <input className="form-control" value={name} onChange={e => setName(e.target.value)} />
            {error && <div className="text-danger mt-1">{error}</div>}
          </div>
          <div className="mb-3">
            <label className="form-label">Description</label>
            <textarea className="form-control" value={description} onChange={e => setDescription(e.target.value)} />
          </div>
          <button className="btn btn-success" onClick={handleSave}>Save</button>
        </div>
        {/* Permissions section */}
        <div className="mb-4 text-start">
          <h4 className="mb-3">Permissions</h4>
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Role</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {members.map((m: any) => (
                <tr key={m.id}>
                  <td>{m.name}{m.self && <span className="ms-2 badge bg-primary">You</span>}</td>
                  <td>{m.role}</td>
                  <td>
                    <button className="btn btn-secondary btn-sm me-2">Edit</button>
                    <button className="btn btn-danger btn-sm">Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <button className="btn btn-primary">Add user</button>
        </div>
        {/* Data sources section */}
        <div className="mb-4 text-start">
          <h4 className="mb-3">Data sources</h4>
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Read-only</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {datasources.map((ds: any) => (
                <tr key={ds.id ?? ds.name}>
                  <td>{ds.name}</td>
                  <td>{ds.type}</td>
                  <td>{ds.readonly ? 'Yes' : 'No'}</td>
                  <td>
                    <button className="btn btn-secondary btn-sm me-2">Configure</button>
                    <button className="btn btn-danger btn-sm">Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <button className="btn btn-primary">Add data source</button>
        </div>
        {/* Danger zone section */}
        <div className="border rounded p-3 text-start" style={{ borderColor: 'red' }}>
          <h4 className="text-danger mb-3">Danger zone</h4>
          <button className="btn btn-danger mb-2" data-bs-toggle="modal" data-bs-target="#deleteModal">Delete project</button>
          <div className="mt-2">
            <label className="form-label">Type the project name to confirm:</label>
            <input className="form-control" value={deleteConfirm} onChange={e => setDeleteConfirm(e.target.value)} />
            <button className="btn btn-danger mt-2" disabled={deleteConfirm !== name} onClick={handleDelete}>Confirm delete</button>
          </div>
        </div>
      </div>
    </PageLayout>
  );
}