import { useEffect, useState } from "react";
import { userApi } from "../services/userApi.ts";
import type { User } from "../types/auth.ts";
import { Alert, Button, Container, Form, Modal, Table } from "react-bootstrap";
import { useApi } from "../hooks/useApi.ts";

export function AdminPage() {
  const { data: users, error: usersError, isLoading: isLoadingUsers, execute: loadUsers } = useApi<User[]>();
  const { error: formError, isLoading: isSubmitting, execute: execForm } = useApi();

  const [showCreateUser, setShowCreateUser] = useState(false);
  const [showUpdatePassword, setShowUpdatePassword] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);

  useEffect(() => {
    loadUsers(() => userApi.getAll().then(res => {
      return res;
    }));
  }, [loadUsers]);

  const handleCreateUser = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const login = (form.elements.namedItem('login') as HTMLInputElement).value;
    const password = (form.elements.namedItem('password') as HTMLInputElement).value;
    const superAdmin = (form.elements.namedItem('superAdmin') as HTMLInputElement).checked;

    const result = await execForm(() => userApi.createUser({ login, password, superAdmin }));
    if (result) {
      setShowCreateUser(false);
      loadUsers(() => userApi.getAll().then(res => {
        return res;
      }));
    }
  };

  const handleUpdatePassword = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const password = (form.elements.namedItem('password') as HTMLInputElement).value;

    if (selectedUser) {
      const result = await execForm(() => userApi.adminUpdatePassword(selectedUser.login, { password }));
      if (result) {
        setShowUpdatePassword(false);
        setSelectedUser(null);
      }
    }
  };

  const error = usersError || formError;

  return <>
    <Container fluid>
      <div className="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3">
        <h1 className="h2">Admin</h1>
        <div className="btn-toolbar mb-2 mb-md-0">
          <Button variant="primary" onClick={() => setShowCreateUser(true)}>Create User</Button>
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      {isLoadingUsers && <p>Loading users...</p>}

      {users && users.length === 0 && <p>No users found.</p>}
      
      <Table striped bordered hover>
        <thead>
          <tr>
            <th>Login</th>
            <th>Super Admin</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
        {users && users.map(user => (
          <tr key={user.login}>
            <td>{user.email}</td>
            <td>{user.superAdmin ? 'Yes' : 'No'}</td>
            <td>
              <Button variant="secondary" size="sm" onClick={() => {
                setSelectedUser(user);
                setShowUpdatePassword(true);
              }}>Change Password</Button>
            </td>
          </tr>
        ))}
        </tbody>
      </Table>

      <Modal show={showCreateUser} onHide={() => setShowCreateUser(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Create User</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateUser}>
          <Modal.Body>
            <Form.Group className="mb-3" controlId="login">
              <Form.Label>Login</Form.Label>
              <Form.Control type="text" required disabled={isSubmitting} />
            </Form.Group>
            <Form.Group className="mb-3" controlId="password">
              <Form.Label>Password</Form.Label>
              <Form.Control type="password" required disabled={isSubmitting} />
            </Form.Group>
            <Form.Group className="mb-3" controlId="superAdmin">
              <Form.Check type="checkbox" label="Super Admin" disabled={isSubmitting} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowCreateUser(false)} disabled={isSubmitting}>Cancel</Button>
            <Button variant="primary" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Creating...' : 'Create'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={showUpdatePassword} onHide={() => {
        setShowUpdatePassword(false);
        setSelectedUser(null);
      }}>
        <Modal.Header closeButton>
          <Modal.Title>Update Password for {selectedUser?.login}</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleUpdatePassword}>
          <Modal.Body>
            <Form.Group className="mb-3" controlId="password">
              <Form.Label>New Password</Form.Label>
              <Form.Control type="password" required disabled={isSubmitting} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => {
              setShowUpdatePassword(false);
              setSelectedUser(null);
            }} disabled={isSubmitting}>Cancel</Button>
            <Button variant="primary" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Updating...' : 'Update'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

    </Container>
  </>;
}
