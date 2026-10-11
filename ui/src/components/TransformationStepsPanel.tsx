import { Button } from 'react-bootstrap';
import type { ViewTransformationStep } from '../types/view';

type Props = {
  steps: ViewTransformationStep[];
  activeStepCount: number;
  baseLabel: string;
  onPreview: (stepCount: number) => void;
  onTruncate: (index: number) => void;
};

function TrashIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      fill="currentColor"
      viewBox="0 0 16 16"
      aria-hidden="true"
    >
      <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0z" />
      <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4zM2.5 3h11V2h-11z" />
    </svg>
  );
}

export function TransformationStepsPanel({ steps, activeStepCount, baseLabel, onPreview, onTruncate }: Props) {
  return (
    <div className="card mb-3">
      <div className="card-body">
        <h5 className="card-title">Transformation steps</h5>
        {activeStepCount < steps.length && (
          <div className="alert alert-warning py-2 small">
            Previewing step {activeStepCount} of {steps.length}. Adding a step now will discard the later steps.
          </div>
        )}
        <ol className="list-group list-group-numbered">
          <li
            className={`list-group-item d-flex justify-content-between align-items-center ${activeStepCount === 0 ? 'active' : ''}`}
            style={{ cursor: 'pointer' }}
            onClick={() => onPreview(0)}
          >
            <span>{baseLabel}</span>
          </li>
          {steps.map((step, index) => {
            const isActive = activeStepCount === index + 1;
            const isFuture = index + 1 > activeStepCount;
            return (
              <li
                key={step.id}
                className={`list-group-item d-flex justify-content-between align-items-center ${isActive ? 'active' : ''} ${isFuture && !isActive ? 'list-group-item-light' : ''}`}
                style={{ cursor: 'pointer' }}
                onClick={() => onPreview(index + 1)}
              >
                <span className={isFuture && !isActive ? 'text-muted fst-italic' : ''}>{step.label}</span>
                <Button
                  variant="link"
                  size="sm"
                  className="p-0 text-danger"
                  title="Remove this step and the following ones"
                  aria-label={`Remove ${step.label} and the following steps`}
                  onClick={(event) => {
                    event.stopPropagation();
                    onTruncate(index);
                  }}
                >
                  <TrashIcon />
                </Button>
              </li>
            );
          })}
        </ol>
        {steps.length === 0 && <div className="text-muted small mt-2">No transformation yet. Apply a filter, sort, rename, remove or reorder columns.</div>}
      </div>
    </div>
  );
}
