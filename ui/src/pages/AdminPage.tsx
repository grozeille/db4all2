import { useEffect, useState } from "react";
import { userApi } from "../services/userApi.ts";
import type { User } from "../types/auth.ts";
import { Alert, Button, Container, Form, Modal, Table } from "react-bootstrap";
import { useApi } from "../hooks/useApi.ts";

export function AdminPage() {
  const { data: users, error: usersError, isLoading: isLoadingUsers, execute: loadUsers } = useApi<User[]>(userApi.getAll);

  // Hooks for the create user form
  const { error: createUserHookError, isLoading: isCreatingUser, execute: execCreateUser } = useApi();
  const [createUserApiError, setCreateUserApiError] = useState<string | null>(null);
  const [createLoginApiError, setCreateLoginApiError] = useState<string | null>(null);
  const [createUserGeneralApiError, setCreateUserGeneralApiError] = useState<string | null>(null);

  // Hooks for the update password form
  const { error: updatePasswordHookError, isLoading: isUpdatingPassword, execute: execUpdatePassword } = useApi();
  const [updatePasswordApiError, setUpdatePasswordApiError] = useState<string | null>(null);
  const [updatePasswordGeneralApiError, setUpdatePasswordGeneralApiError] = useState<string | null>(null);

  const [showCreateUser, setShowCreateUser] = useState(false);
  const [showUpdatePassword, setShowUpdatePassword] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);

  // State for the create user form
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [superAdmin, setSuperAdmin] = useState(false);
  const [passwordMismatch, setPasswordMismatch] = useState(false);

  // State for the update password form
  const [newPassword, setNewPassword] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] = useState("");
  const [newPasswordMismatch, setNewPasswordMismatch] = useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

  // Show API error for create user form
  useEffect(() => {
    if (createUserHookError?.message) {
      try {
        const errorData = JSON.parse(createUserHookError.message);
        if (errorData.errorType === "USER_ALREADY_EXISTS") {
          setCreateLoginApiError("This login already exists");
          setCreateUserApiError(null); // Clear password field error
          setCreateUserGeneralApiError(null); // Clear general alert
        } else if (errorData.errorType === "PASSWORD_TOO_WEAK") {
          setCreateUserApiError("The password is not strong enough.");
          setCreateLoginApiError(null); // Clear login field error
          setCreateUserGeneralApiError(null); // No general alert for password too weak
        } else {
          setCreateUserApiError(null); // Clear field-specific error
          setCreateLoginApiError(null); // Clear login field error
          setCreateUserGeneralApiError("Unexpected error"); // Set general alert for other errors
        }
      } catch (e) {
        setCreateUserApiError(null); // Clear field-specific error
        setCreateLoginApiError(null); // Clear login field error
        setCreateUserGeneralApiError("Unexpected error"); // Set general alert for unexpected parsing errors
      }
    } else {
      setCreateUserApiError(null);
      setCreateLoginApiError(null);
      setCreateUserGeneralApiError(null);
    }
  }, [createUserHookError]);

  // Show API error for update password form
  useEffect(() => {
    if (updatePasswordHookError?.message) {
      try {
        const errorData = JSON.parse(updatePasswordHookError.message);
        if (errorData.errorType === "PASSWORD_TOO_WEAK") {
          setUpdatePasswordApiError("The password is not strong enough.");
          setUpdatePasswordGeneralApiError(null); // No general alert for password too weak
        } else {
          setUpdatePasswordApiError(null); // Clear field-specific error
          setUpdatePasswordGeneralApiError("Unexpected error"); // Set general alert for other errors
        }
      } catch (e) {
        setUpdatePasswordApiError(null); // Clear field-specific error
        setUpdatePasswordGeneralApiError("Unexpected error"); // Set general alert for unexpected parsing errors
      }
    } else {
      setUpdatePasswordApiError(null);
      setUpdatePasswordGeneralApiError(null);
    }
  }, [updatePasswordHookError]);


  // Validation for create user form
  useEffect(() => {
    if (password && confirmPassword) {
      setPasswordMismatch(password !== confirmPassword);
    } else {
      setPasswordMismatch(false);
    }
  }, [password, confirmPassword]);

  // Validation for update password form
  useEffect(() => {
    if (newPassword && confirmNewPassword) {
      setNewPasswordMismatch(newPassword !== confirmNewPassword);
    } else {
      setNewPasswordMismatch(false);
    }
  }, [newPassword, confirmNewPassword]);

  const handleCreateUser = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (password !== confirmPassword) {
      setPasswordMismatch(true);
      return;
    }
    setCreateUserApiError(null);
    const result = await execCreateUser(() => userApi.createUser({ email, password, superAdmin }));
    if (result) {
      setShowCreateUser(false);
      resetCreateForm();
      loadUsers();
    }
  };

  const resetCreateForm = () => {
    setEmail("");
    setPassword("");
    setConfirmPassword("");
    setSuperAdmin(false);
    setPasswordMismatch(false);
    setCreateUserApiError(null);
    setCreateLoginApiError(null); // Clear login error
  }

  const handleUpdatePassword = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (newPassword !== confirmNewPassword) {
      setNewPasswordMismatch(true);
      return;
    }

    if (selectedUser) {
      setUpdatePasswordApiError(null);
      const result = await execUpdatePassword(() => userApi.adminUpdatePassword(selectedUser.email, { password: newPassword }));
      if (result !== undefined) {
        setShowUpdatePassword(false);
        resetUpdatePasswordForm();
      }
    }
  };

  const resetUpdatePasswordForm = () => {
    setSelectedUser(null);
    setNewPassword("");
    setConfirmNewPassword("");
    setNewPasswordMismatch(false);
    setUpdatePasswordApiError(null);
  }

  const error = usersError?.message;

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
        {users && users.map((user: User) => (
          <tr key={user.email}>
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

      <Modal show={showCreateUser} onHide={() => {
        setShowCreateUser(false);
        resetCreateForm();
      }}>
        <Modal.Header closeButton>
          <Modal.Title>Create User</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateUser}>
          <Modal.Body>
            {createUserGeneralApiError && <Alert variant="danger">{createUserGeneralApiError}</Alert>}
            <Form.Group className="mb-3" controlId="email">
              <Form.Label>Login</Form.Label>
              <Form.Control type="email" required disabled={isCreatingUser} value={email} onChange={e => setEmail(e.target.value)} isInvalid={!!createLoginApiError} />
              <Form.Control.Feedback type="invalid">
                {createLoginApiError}
              </Form.Control.Feedback>
            </Form.Group>
            <Form.Group className="mb-3" controlId="password">
              <Form.Label>Password</Form.Label>
              <Form.Control type="password" required disabled={isCreatingUser} value={password} onChange={e => {
                setPassword(e.target.value);
                setCreateUserApiError(null);
              }} isInvalid={!!createUserApiError} />
              <Form.Control.Feedback type="invalid">
                {createUserApiError}
              </Form.Control.Feedback>
            </Form.Group>
            <Form.Group className="mb-3" controlId="confirmPassword">
              <Form.Label>Confirm Password</Form.Label>
              <Form.Control type="password" required disabled={isCreatingUser} value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} isInvalid={passwordMismatch} />
              <Form.Control.Feedback type="invalid">
                Passwords do not match
              </Form.Control.Feedback>
            </Form.Group>
            <Form.Group className="mb-3" controlId="superAdmin">
              <Form.Check type="checkbox" label="Super Admin" disabled={isCreatingUser} checked={superAdmin} onChange={e => setSuperAdmin(e.target.checked)} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => {
              setShowCreateUser(false);
              resetCreateForm();
            }} disabled={isCreatingUser}>Cancel</Button>
            <Button variant="primary" type="submit" disabled={isCreatingUser || passwordMismatch}>
              {isCreatingUser ? 'Creating...' : 'Create'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={showUpdatePassword} onHide={() => {
        setShowUpdatePassword(false);
        resetUpdatePasswordForm();
      }}>
        <Modal.Header closeButton>
          <Modal.Title>Update Password for {selectedUser?.email}</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleUpdatePassword}>
          <Modal.Body>
            {updatePasswordGeneralApiError && <Alert variant="danger">{updatePasswordGeneralApiError}</Alert>}
            <Form.Group className="mb-3" controlId="newPassword">
              <Form.Label>New Password</Form.Label>
              <Form.Control type="password" required disabled={isUpdatingPassword} value={newPassword} onChange={e => {
                setNewPassword(e.target.value);
                setUpdatePasswordApiError(null);
              }} isInvalid={!!updatePasswordApiError} />
              <Form.Control.Feedback type="invalid">
                {updatePasswordApiError}
              </Form.Control.Feedback>
            </Form.Group>
            <Form.Group className="mb-3" controlId="confirmNewPassword">
              <Form.Label>Confirm New Password</Form.Label>
              <Form.Control type="password" required disabled={isUpdatingPassword} value={confirmNewPassword} onChange={e => setConfirmNewPassword(e.target.value)} isInvalid={newPasswordMismatch} />
              <Form.Control.Feedback type="invalid">
                Passwords do not match
              </Form.Control.Feedback>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => {
              setShowUpdatePassword(false);
              resetUpdatePasswordForm();
            }} disabled={isUpdatingPassword}>Cancel</Button>
            <Button variant="primary" type="submit" disabled={isUpdatingPassword || newPasswordMismatch}>
              {isUpdatingPassword ? 'Updating...' : 'Update'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

    </Container>
  </>;
}

