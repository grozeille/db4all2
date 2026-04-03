import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Form, Modal, Table } from 'react-bootstrap';
import { addProjectAdministrator, deleteProject, getAvailableProjectAdministrators, getProject, removeProjectAdministrator, updateProject } from '../services/projectApi';
import type { Project } from '../types/project';

export default function ProjectSettingsPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState<Project | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [generalError, setGeneralError] = useState('');
  const [generalSuccess, setGeneralSuccess] = useState('');
  const [administratorsError, setAdministratorsError] = useState('');
  const [availableUsers, setAvailableUsers] = useState<string[]>([]);
  const [selectedUserToAdd, setSelectedUserToAdd] = useState('');
  const [showAddAdminModal, setShowAddAdminModal] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState('');

  const loadProject = useCallback(async () => {
    if (!projectId) {
      return;
    }

    const loadedProject = await getProject(projectId);
    setProject(loadedProject);
    setName(loadedProject.name);
    setDescription(loadedProject.description);
  }, [projectId]);

  useEffect(() => {
    if (!projectId) {
      return;
    }

    loadProject().catch((err: Error) => {
      navigate(`/error/404`, { state: { message: err.message } });
    });
  }, [loadProject, navigate, projectId]);

  const handleSave = async () => {
    if (!projectId) {
      return;
    }
    if (!name.trim()) {
      setGeneralError('Name is required');
      setGeneralSuccess('');
      return;
    }

    setGeneralError('');
    setGeneralSuccess('');

    try {
      const updatedProject = await updateProject(projectId, { name: name.trim(), description });
      setProject(updatedProject);
      setName(updatedProject.name);
      setDescription(updatedProject.description);
      setDeleteConfirm('');
      setGeneralSuccess('Project settings saved.');
    } catch (err) {
      setGeneralError(err instanceof Error ? err.message : 'Unexpected error');
    }
  };

  const handleDelete = async () => {
    if (!projectId) {
      return;
    }

    try {
      await deleteProject(projectId);
      navigate('/projects');
    } catch (err) {
      setGeneralError(err instanceof Error ? err.message : 'Unexpected error');
    }
  };

  const openAddAdministratorModal = async () => {
    if (!projectId) {
      return;
    }

    setAdministratorsError('');
    try {
      const users = await getAvailableProjectAdministrators(projectId);
      setAvailableUsers(users);
      setSelectedUserToAdd(users[0] ?? '');
      setShowAddAdminModal(true);
    } catch (err) {
      setAdministratorsError(err instanceof Error ? err.message : 'Unexpected error');
    }
  };

  const handleAddAdministrator = async () => {
    if (!projectId) {
      return;
    }
    if (!selectedUserToAdd) {
      setAdministratorsError('Please select a user.');
      return;
    }

    try {
      await addProjectAdministrator(projectId, selectedUserToAdd);
      setShowAddAdminModal(false);
      setSelectedUserToAdd('');
      await loadProject();
    } catch (err) {
      setAdministratorsError(err instanceof Error ? err.message : 'Unexpected error');
    }
  };

  const handleRemoveAdministrator = async (email: string) => {
    if (!projectId) {
      return;
    }

    setAdministratorsError('');
    try {
      await removeProjectAdministrator(projectId, email);
      await loadProject();
    } catch (err) {
      setAdministratorsError(err instanceof Error ? err.message : 'Unexpected error');
    }
  };

  if (!project) {
    return <div>Loading project settings...</div>;
  }

  if (!project.administrator) {
    return <Alert variant="danger">You do not have permission to manage this project.</Alert>;
  }

  return (
    <>
      <div>
        <h2 className="mb-4 text-start">Project settings: {project.name}</h2>
        <div className="mb-4 text-start">
          <h4 className="mb-3">General</h4>
          {generalError && <Alert variant="danger">{generalError}</Alert>}
          {generalSuccess && <Alert variant="success">{generalSuccess}</Alert>}
          <div className="mb-3">
            <label className="form-label">Name</label>
            <input className="form-control" value={name} onChange={event => setName(event.target.value)} />
          </div>
          <div className="mb-3">
            <label className="form-label">Description</label>
            <textarea className="form-control" value={description} onChange={event => setDescription(event.target.value)} />
          </div>
          <Button variant="success" onClick={handleSave}>Save</Button>
        </div>

        <div className="mb-4 text-start">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h4 className="mb-0">Administrators</h4>
            <Button variant="primary" onClick={openAddAdministratorModal}>Add administrator</Button>
          </div>
          {administratorsError && <Alert variant="danger">{administratorsError}</Alert>}
          <Table>
            <thead>
              <tr>
                <th>Login</th>
                <th>Current user</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {project.administrators.map((administrator) => (
                <tr key={administrator.email}>
                  <td>{administrator.email}</td>
                  <td>{administrator.currentUser ? <span className="badge bg-primary">You</span> : 'No'}</td>
                  <td className="text-end">
                    <Button
                      variant="danger"
                      size="sm"
                      disabled={administrator.currentUser || project.administrators.length === 1}
                      onClick={() => handleRemoveAdministrator(administrator.email)}
                    >
                      Remove
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>

        <div className="mb-4 text-start">
          <h4 className="mb-3">Data sources</h4>
          <Alert variant="secondary">Data source management is not implemented yet.</Alert>
        </div>

        <div className="border rounded p-3 text-start" style={{ borderColor: 'red' }}>
          <h4 className="text-danger mb-3">Danger zone</h4>
          <div className="mt-2">
            <label className="form-label">Type the project name to confirm:</label>
            <input className="form-control" value={deleteConfirm} onChange={event => setDeleteConfirm(event.target.value)} />
            <Button variant="danger" className="mt-2" disabled={deleteConfirm !== name} onClick={handleDelete}>Confirm delete</Button>
          </div>
        </div>
      </div>

      <Modal show={showAddAdminModal} onHide={() => setShowAddAdminModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Add project administrator</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {administratorsError && <Alert variant="danger">{administratorsError}</Alert>}
          <Form.Group>
            <Form.Label>User</Form.Label>
            <Form.Select value={selectedUserToAdd} onChange={(event) => setSelectedUserToAdd(event.target.value)}>
              {availableUsers.length === 0 && <option value="">No available user</option>}
              {availableUsers.map((userEmail) => (
                <option key={userEmail} value={userEmail}>{userEmail}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowAddAdminModal(false)}>Cancel</Button>
          <Button variant="primary" onClick={handleAddAdministrator} disabled={availableUsers.length === 0}>Add</Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}