import { useEffect, useState } from 'react';
import { Alert, Button, Form, Modal, Spinner } from 'react-bootstrap';

export type SaveViewMode = 'clone' | 'link';

type Props = {
  show: boolean;
  title: string;
  saveLabel: string;
  saving: boolean;
  error: string;
  showMode: boolean;
  linkAvailable: boolean;
  linkDisabledReason: string;
  onClose: () => void;
  onSave: (name: string, description: string, mode: SaveViewMode) => void;
};

export function SaveViewModal({ show, title, saveLabel, saving, error, showMode, linkAvailable, linkDisabledReason, onClose, onSave }: Props) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [mode, setMode] = useState<SaveViewMode>('clone');

  useEffect(() => {
    if (show) {
      setName('');
      setDescription('');
      setMode('clone');
    }
  }, [show]);

  const handleSave = () => {
    onSave(name.trim(), description, mode);
  };

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {error && <Alert variant="danger">{error}</Alert>}
        <Form.Group className="mb-3">
          <Form.Label>Name</Form.Label>
          <Form.Control value={name} onChange={(event) => setName(event.target.value)} />
        </Form.Group>
        <Form.Group className={showMode ? 'mb-3' : ''}>
          <Form.Label>Description</Form.Label>
          <Form.Control as="textarea" rows={3} value={description} onChange={(event) => setDescription(event.target.value)} />
        </Form.Group>
        {showMode && (
          <Form.Group>
            <Form.Label>How should steps be saved?</Form.Label>
            <Form.Check
              type="radio"
              id="save-view-mode-clone"
              label="Clone all steps into the new view"
              checked={mode === 'clone'}
              onChange={() => setMode('clone')}
            />
            <Form.Check
              type="radio"
              id="save-view-mode-link"
              label="Link to this view and keep only the new steps in the new view"
              checked={mode === 'link'}
              disabled={!linkAvailable}
              onChange={() => setMode('link')}
            />
            {!linkAvailable && linkDisabledReason && <div className="text-muted small">{linkDisabledReason}</div>}
          </Form.Group>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onClose} disabled={saving}>Cancel</Button>
        <Button variant="primary" onClick={handleSave} disabled={saving}>
          {saving ? <Spinner size="sm" /> : saveLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
