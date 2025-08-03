
import React, { useState, useEffect } from 'react';
import PageLayout from '../components/PageLayout';
import { useParams, useNavigate } from 'react-router-dom';
import { AgGridReact } from 'ag-grid-react';
import { getTable } from '../services/tableApi';
import 'ag-grid-community/styles/ag-theme-alpine.css';
import { QueryBuilder } from '../components/QueryBuilder';
import type { QueryGroup, FieldOption } from '../components/QueryBuilder';


const FAKE_DATA = Array.from({ length: 20 }, (_, i) => ({
  col1: `Value ${i + 1}-1`,
  col2: `Value ${i + 1}-2`,
  col3: `Value ${i + 1}-3`,
}));

const FIELDS: FieldOption[] = [
  { name: 'col1' },
  { name: 'col2' },
  { name: 'col3' }
];


export default function TableContentPage() {
  const { projectId, tableId } = useParams();
  const navigate = useNavigate();
  const [filterGroup, setFilterGroup] = useState<QueryGroup>({ operator: 'AND', rules: [] });

  useEffect(() => {
    if (projectId && tableId) {
      getTable(projectId, tableId)
        .catch(err => {
          navigate(`/error/404`, { state: { message: err.message } });
        });
    }
  }, [projectId, tableId, navigate]);

  return (
    <PageLayout>
      <h2>Table content {tableId} (project {projectId})</h2>
      <div className="mb-3">
        <details>
          <summary className="mb-2"><b>Filter</b></summary>
          <QueryBuilder group={filterGroup} fields={FIELDS} onChange={setFilterGroup} />
        </details>
      </div>
      <div className="ag-theme-alpine" style={{ height: 400, width: '100%' }}>
        <AgGridReact
          rowData={FAKE_DATA}
          columnDefs={[
            { headerName: 'Column 1', field: 'col1' },
            { headerName: 'Column 2', field: 'col2' },
            { headerName: 'Column 3', field: 'col3' },
          ]}
          domLayout="autoHeight"
        />
      </div>
    </PageLayout>
  );
}
