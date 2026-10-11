import { useState } from 'react';
import type { ReactNode } from 'react';
import { Button } from 'react-bootstrap';

type Props = {
  stepsPanel: ReactNode;
  children: ReactNode;
};

export function StepsEditorLayout({ stepsPanel, children }: Props) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="row">
      {collapsed ? (
        <div className="col-auto">
          <Button variant="outline-secondary" size="sm" onClick={() => setCollapsed(false)} title="Show transformation steps">
            Steps »
          </Button>
        </div>
      ) : (
        <div className="col-md-2">
          <div className="d-flex justify-content-end mb-2">
            <Button variant="outline-secondary" size="sm" onClick={() => setCollapsed(true)} title="Hide transformation steps">
              « Hide
            </Button>
          </div>
          {stepsPanel}
        </div>
      )}
      <div className={collapsed ? 'col' : 'col-md-10'}>{children}</div>
    </div>
  );
}
