import { AppHeader } from "../components/AppHeader";
import { useEffect, useState } from "react";
import { userApi } from "../services/userApi.ts";
import type { User } from "../types/auth.ts";
import { Alert, Button, Container, Form, Modal, Table } from "react-bootstrap";

export function AdminPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [error, setError] = useState('');
  const [showCreateUser, setShowCreateUser] = useState(false);
  const [showUpdatePassword, setShowUpdatePassword] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);

  const loadUsers = async () => {
    try {
      const allUsers = await userApi.getAll();
      setUsers(allUsers.data);
    } catch (e: any) {
      setError(e.message);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleCreateUser = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const login = (form.elements.namedItem('login') as HTMLInputElement).value;
    const password = (form.elements.namedItem('password') as HTMLInputElement).value;
    const superAdmin = (form.elements.namedItem('superAdmin') as HTMLInputElement).checked;

    try {
      await userApi.createUser({ login, password, superAdmin });
      setShowCreateUser(false);
      loadUsers();
    } catch (e: any) {
      setError(e.message);
    }
  };

  const handleUpdatePassword = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const password = (form.elements.namedItem('password') as HTMLInputElement).value;

    if (selectedUser) {
      try {
        await userApi.adminUpdatePassword(selectedUser.login, { password });
        setShowUpdatePassword(false);
        setSelectedUser(null);
      } catch (e: any) {
        setError(e.message);
      }
    }
  };

  return <>
    <AppHeader onLogout={() => {}} />
    <Container fluid>
      <div className="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
        <h1 className="h2">Admin</h1>
        <div className="btn-toolbar mb-2 mb-md-0">
          <Button variant="primary" onClick={() => setShowCreateUser(true)}>Create User</Button>
        </div>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Table striped bordered hover>
        <thead>
          <tr>
            <th>Login</th>
            <th>Super Admin</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
        {users.map(user => (
          <tr key={user.login}>
            <td>{user.login}</td>
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
              <Form.Control type="text" required />
            </Form.Group>
            <Form.Group className="mb-3" controlId="password">
              <Form.Label>Password</Form.Label>
              <Form.Control type="password" required />
            </Form.Group>
            <Form.Group className="mb-3" controlId="superAdmin">
              <Form.Check type="checkbox" label="Super Admin" />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowCreateUser(false)}>Cancel</Button>
            <Button variant="primary" type="submit">Create</Button>
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
              <Form.Control type="password" required />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => {
              setShowUpdatePassword(false);
              setSelectedUser(null);
            }}>Cancel</Button>
            <Button variant="primary" type="submit">Update</Button>
          </Modal.Footer>
        </Form>
      </Modal>

    </Container>
  </>;
}
