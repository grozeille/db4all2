import { useEffect, useMemo, useRef, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ModuleRegistry, AllCommunityModule } from 'ag-grid-community';
import type { ColumnState, GridApi } from 'ag-grid-community';
import { Button, Form, Modal } from 'react-bootstrap';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import type { TableQuerySort } from '../types/table';
import { sameSorts } from '../services/viewSteps';

ModuleRegistry.registerModules([AllCommunityModule]);

type Props = {
  columns: string[];
  rows: Array<Record<string, unknown>>;
  loading: boolean;
  sorts: TableQuerySort[];
  onSortChange: (sorts: TableQuerySort[]) => void;
  onReorder: (order: string[]) => void;
  onRename: (column: string, newName: string) => void;
  onDrop: (column: string) => void;
};

type ContextMenuState = {
  x: number;
  y: number;
  column: string;
};

export function DataPreviewGrid({ columns, rows, loading, sorts, onSortChange, onReorder, onRename, onDrop }: Props) {
  const gridApiRef = useRef<GridApi | null>(null);
  const userSortIntentRef = useRef(false);
  const [menu, setMenu] = useState<ContextMenuState | null>(null);
  const [renameColumn, setRenameColumn] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const [renameError, setRenameError] = useState('');
  const renameInputRef = useRef<HTMLInputElement>(null);

  const columnDefs = useMemo(() => {
    return columns.map((column) => ({
      headerName: column,
      field: column,
      sortable: true,
      filter: false,
      resizable: true,
    }));
  }, [columns]);

  const applySortState = (sortState: TableQuerySort[]) => {
    const api = gridApiRef.current;
    if (!api) {
      return;
    }
    const state: ColumnState[] = api.getColumnState().map((entry) => ({ colId: entry.colId, sort: null, sortIndex: null }));
    sortState.forEach((sort, index) => {
      const entry = state.find((candidate) => candidate.colId === sort.column);
      if (entry) {
        entry.sort = sort.direction === 'ASC' ? 'asc' : 'desc';
        entry.sortIndex = index;
      }
    });
    api.applyColumnState({ state });
  };

  const sortsKey = sorts.map((sort) => `${sort.column}:${sort.direction}`).join('\u0000');
  const columnsKey = columns.join('\u0000');

  useEffect(() => {
    applySortState(sorts);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sortsKey, columnsKey]);

  const readSortModel = (api: GridApi): TableQuerySort[] => {
    return api
      .getColumnState()
      .filter((entry) => entry.sort === 'asc' || entry.sort === 'desc')
      .sort((left, right) => (left.sortIndex ?? 0) - (right.sortIndex ?? 0))
      .map((entry) => ({ column: entry.colId, direction: entry.sort === 'asc' ? 'ASC' : 'DESC' }));
  };

  const handleContextMenu = (event: React.MouseEvent) => {
    const target = event.target as HTMLElement;
    const cell = target.closest('[col-id]');
    if (!cell) {
      return;
    }
    const column = cell.getAttribute('col-id');
    if (!column || !columns.includes(column)) {
      return;
    }
    event.preventDefault();
    setMenu({ x: event.clientX, y: event.clientY, column });
  };

  const openRename = (column: string) => {
    setMenu(null);
    setRenameColumn(column);
    setRenameValue(column);
    setRenameError('');
  };

  const handleRenameSave = () => {
    if (!renameColumn) {
      return;
    }
    const newName = renameValue.trim();
    if (!newName) {
      setRenameError('New name is required.');
      return;
    }
    if (columns.some((column) => column.toLowerCase() === newName.toLowerCase())) {
      setRenameError(`Column "${newName}" already exists.`);
      return;
    }
    onRename(renameColumn, newName);
    setRenameColumn(null);
  };

  return (
    <>
      <div
        className="ag-theme-alpine"
        style={{ height: 400, width: '100%' }}
        onContextMenu={handleContextMenu}
        onClickCapture={(event) => {
          if ((event.target as HTMLElement).closest('.ag-header-cell')) {
            userSortIntentRef.current = true;
          }
        }}
        onKeyDownCapture={(event) => {
          if ((event.key === 'Enter' || event.key === ' ') && (event.target as HTMLElement).closest('.ag-header-cell')) {
            userSortIntentRef.current = true;
          }
        }}
      >
        <AgGridReact
          rowData={rows}
          columnDefs={columnDefs}
          domLayout="autoHeight"
          overlayLoadingTemplate={'<span class="ag-overlay-loading-center">Loading preview...</span>'}
          loading={loading}
          onGridReady={(event) => {
            gridApiRef.current = event.api;
            applySortState(sorts);
          }}
          onSortChanged={(event) => {
            const model = readSortModel(event.api);
            if (sameSorts(model, sorts)) {
              userSortIntentRef.current = false;
              return;
            }
            if (userSortIntentRef.current) {
              userSortIntentRef.current = false;
              onSortChange(model);
            } else {
              applySortState(sorts);
            }
          }}
          onColumnMoved={(event) => {
            if (event.source !== 'uiColumnMoved' || !event.finished) {
              return;
            }
            onReorder(event.api.getColumnState().map((entry) => entry.colId));
          }}
        />
      </div>

      {menu && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 1050 }}
          onClick={() => setMenu(null)}
          onContextMenu={(event) => {
            event.preventDefault();
            setMenu(null);
          }}
        >
          <div
            className="card shadow"
            style={{ position: 'fixed', left: menu.x, top: menu.y, minWidth: 200 }}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="card-body p-2">
              <div className="px-2 py-1 text-muted small text-truncate" style={{ maxWidth: 240 }}>{menu.column}</div>
              <button
                className="dropdown-item btn btn-link text-start w-100 px-2 py-1 text-decoration-none"
                onClick={() => {
                  setMenu(null);
                  onSortChange([{ column: menu.column, direction: 'ASC' }]);
                }}
              >
                Sort ascending
              </button>
              <button
                className="dropdown-item btn btn-link text-start w-100 px-2 py-1 text-decoration-none"
                onClick={() => {
                  setMenu(null);
                  onSortChange([{ column: menu.column, direction: 'DESC' }]);
                }}
              >
                Sort descending
              </button>
              <button
                className="dropdown-item btn btn-link text-start w-100 px-2 py-1 text-decoration-none"
                disabled={sorts.length === 0}
                onClick={() => {
                  setMenu(null);
                  onSortChange([]);
                }}
              >
                Clear sort
              </button>
              <div className="dropdown-divider" />
              <button className="dropdown-item btn btn-link text-start w-100 px-2 py-1 text-decoration-none" onClick={() => openRename(menu.column)}>
                Rename column
              </button>
              <button
                className="dropdown-item btn btn-link text-start w-100 px-2 py-1 text-decoration-none text-danger"
                disabled={columns.length <= 1}
                onClick={() => {
                  setMenu(null);
                  onDrop(menu.column);
                }}
              >
                Remove column
              </button>
            </div>
          </div>
        </div>
      )}

      <Modal
        show={renameColumn !== null}
        onHide={() => setRenameColumn(null)}
        onEntered={() => {
          renameInputRef.current?.focus();
          renameInputRef.current?.select();
        }}
      >
        <Modal.Header closeButton>
          <Modal.Title>Rename column</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {renameError && <div className="alert alert-danger">{renameError}</div>}
          <Form.Group>
            <Form.Label>New name for {renameColumn}</Form.Label>
            <Form.Control
              ref={renameInputRef}
              value={renameValue}
              onChange={(event) => {
                setRenameValue(event.target.value);
                setRenameError('');
              }}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault();
                  handleRenameSave();
                }
              }}
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setRenameColumn(null)}>Cancel</Button>
          <Button variant="primary" onClick={handleRenameSave}>Rename</Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
