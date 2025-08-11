import { useState } from "react";
import type { FormEvent } from "react";
import { userApi } from "../services/userApi.ts";
import { Alert, Button, Card, Form } from "react-bootstrap";

export function ProfilePage() {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [oldPasswordError, setOldPasswordError] = useState('');
  const [newPasswordError, setNewPasswordError] = useState('');
  const [confirmPasswordError, setConfirmPasswordError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setOldPasswordError('');
    setNewPasswordError('');
    setConfirmPasswordError('');
    setSuccess(false);

    if (newPassword !== confirmPassword) {
      setConfirmPasswordError("New passwords do not match.");
      return;
    }

    try {
      await userApi.updateMyPassword({ oldPassword, password: newPassword });
      setSuccess(true);
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (e: any) {
      const errorMessage = e.message;
      if (errorMessage.includes("Old password incorrect")) {
        setOldPasswordError(errorMessage);
      } else if (errorMessage.includes("Password not strong enough")) {
        setNewPasswordError(errorMessage);
      } else {
        // Fallback for other errors
        setOldPasswordError(errorMessage); // Display general errors under old password for simplicity
      }
    }
  };

  return <>
    <div className="container mt-5">
      <div className="row justify-content-center">
        <div className="col-md-6">
          <Card>
            <Card.Body>
              <Card.Title>Change Password</Card.Title>
              {success && <Alert variant="success">Password updated successfully!</Alert>}
              <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3" controlId="oldPassword">
                  <Form.Label>Old Password</Form.Label>
                  <Form.Control
                    type="password"
                    value={oldPassword}
                    onChange={e => setOldPassword(e.target.value)}
                    isInvalid={!!oldPasswordError}
                    required
                  />
                  <Form.Control.Feedback type="invalid">
                    {oldPasswordError}
                  </Form.Control.Feedback>
                </Form.Group>
                <Form.Group className="mb-3" controlId="newPassword">
                  <Form.Label>New Password</Form.Label>
                  <Form.Control
                    type="password"
                    value={newPassword}
                    onChange={e => setNewPassword(e.target.value)}
                    isInvalid={!!newPasswordError}
                    required
                  />
                  <Form.Control.Feedback type="invalid">
                    {newPasswordError}
                  </Form.Control.Feedback>
                </Form.Group>
                <Form.Group className="mb-3" controlId="confirmPassword">
                  <Form.Label>Confirm New Password</Form.Label>
                  <Form.Control
                    type="password"
                    value={confirmPassword}
                    onChange={e => setConfirmPassword(e.target.value)}
                    isInvalid={!!confirmPasswordError}
                    required
                  />
                  <Form.Control.Feedback type="invalid">
                    {confirmPasswordError}
                  </Form.Control.Feedback>
                </Form.Group>
                <Button variant="primary" type="submit">
                  Save
                </Button>
              </Form>
            </Card.Body>
          </Card>
        </div>
      </div>
    </div>
  </>;
}