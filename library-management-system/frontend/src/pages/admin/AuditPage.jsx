import { useEffect, useState } from 'react';
import { adminApi } from '../../api/services';
import useAsync from '../../hooks/useAsync';
import Spinner from '../../components/Spinner';
import EmptyState from '../../components/EmptyState';
import Icon from '../../components/Icon';
import { formatDateTime } from '../../utils/format';

const ACTIONS = [
  'CHECKOUT', 'CHECKIN', 'RENEW', 'MARK_LOST', 'HOLD_PLACED', 'HOLD_CANCELLED', 'HOLD_READY', 'HOLD_EXPIRED',
  'FINE_ASSESSED', 'FINE_PAID', 'FINE_WAIVED', 'BOOK_CREATED', 'BOOK_UPDATED', 'BOOK_DELETED', 'COPY_ADDED',
  'COPY_UPDATED', 'MEMBER_REGISTERED', 'MEMBER_TYPE_CHANGED', 'MEMBER_SUSPENDED', 'MEMBER_REACTIVATED',
  'STAFF_CREATED', 'RULES_CHANGED', 'SETTINGS_CHANGED', 'JOB_RUN',
];

const label = (a) => a.charAt(0) + a.slice(1).toLowerCase().replace(/_/g, ' ');

export default function AuditPage() {
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const [action, setAction] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    const t = setTimeout(() => { setDebounced(query); setPage(0); }, 250);
    return () => clearTimeout(t);
  }, [query]);

  const { data, error } = useAsync(
    () => adminApi.audit({ q: debounced || undefined, action: action || undefined, page, size: 50 }),
    [debounced, action, page]
  );

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Audit log</h1>
          <p className="page-meta">Every circulation, fine, catalogue and rules change, with who made it. Entries can't be edited or deleted.</p>
        </div>
      </div>

      <div className="toolbar">
        <label className="search-field">
          <Icon name="search" />
          <input type="search" placeholder="Person, title or detail" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Search the audit log" />
        </label>
        <select value={action} onChange={(e) => { setAction(e.target.value); setPage(0); }} aria-label="Action">
          <option value="">All actions</option>
          {ACTIONS.map((a) => <option key={a} value={a}>{label(a)}</option>)}
        </select>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="card">
        {!data ? <Spinner /> : data.content.length === 0 ? <EmptyState title="No matching entries" icon="scroll" /> : (
          <div className="table-wrap">
            <table className="table audit-table">
              <thead><tr><th>When</th><th>Who</th><th>Action</th><th>What happened</th></tr></thead>
              <tbody>
                {data.content.map((e) => (
                  <tr key={e.id}>
                    <td className="cell-date">{formatDateTime(e.occurredAt)}</td>
                    <td className="cell-strong">{e.actorName}</td>
                    <td><span className="audit-action">{label(e.action)}</span></td>
                    <td className="audit-summary">{e.summary}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {data && data.totalPages > 1 && (
        <div className="pagination">
          <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}><Icon name="chevronLeft" />Newer</button>
          <span>Page {page + 1} of {data.totalPages}</span>
          <button className="btn btn-ghost btn-sm" disabled={data.last} onClick={() => setPage((p) => p + 1)}>Older<Icon name="chevronRight" /></button>
        </div>
      )}
    </>
  );
}
