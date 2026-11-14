import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { membersApi } from '../api/services';
import useAsync from '../hooks/useAsync';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import Icon from '../components/Icon';
import { AccountStatus } from '../components/Badges';
import { formatDate, formatRupees } from '../utils/format';

const STATUSES = [
  { value: '', label: 'All' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'SUSPENDED', label: 'Suspended' },
];

export default function MembersPage() {
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const [status, setStatus] = useState('');
  const [typeId, setTypeId] = useState('');
  const [types, setTypes] = useState([]);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(query), 250);
    return () => clearTimeout(t);
  }, [query]);
  useEffect(() => {
    membersApi.types().then(setTypes).catch(() => {});
  }, []);

  const { data, error } = useAsync(
    () => membersApi.search({ q: debounced || undefined, status: status || undefined, typeId: typeId || undefined, size: 100 }),
    [debounced, status, typeId]
  );

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Members</h1>
          <p className="page-meta">{data ? `${data.totalElements} member${data.totalElements === 1 ? '' : 's'}` : ' '}</p>
        </div>
      </div>

      <div className="toolbar">
        <label className="search-field">
          <Icon name="search" />
          <input type="search" placeholder="Name, email or card number" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Search members" />
        </label>
        <div className="segmented" role="tablist">
          {STATUSES.map((s) => (
            <button key={s.value} role="tab" aria-selected={status === s.value} className={status === s.value ? 'active' : ''} onClick={() => setStatus(s.value)}>
              {s.label}
            </button>
          ))}
        </div>
        <select value={typeId} onChange={(e) => setTypeId(e.target.value)} aria-label="Member type">
          <option value="">All types</option>
          {types.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="card">
        {!data ? <Spinner /> : data.content.length === 0 ? (
          <EmptyState title="No members found" icon="users" />
        ) : (
          <div className="table-wrap">
            <table className="table table-links">
              <thead>
                <tr>
                  <th>Member</th><th>Card</th><th>Type</th><th>Status</th>
                  <th className="num">Out</th><th className="num">Overdue</th><th className="num">Owed</th><th className="num">Borrowed</th><th>Joined</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((m) => (
                  <tr key={m.id}>
                    <td><Link to={`/members/${m.id}`} className="cell-link cell-strong">{m.name}</Link><div className="cell-sub">{m.email}</div></td>
                    <td className="mono">{m.cardNumber}</td>
                    <td className="cell-sub-inline">{m.memberTypeName || '—'}</td>
                    <td><AccountStatus status={m.status} /></td>
                    <td className="num">{m.openLoans}</td>
                    <td className={`num ${m.overdueLoans ? 'cell-owed' : ''}`}>{m.overdueLoans || '—'}</td>
                    <td className={`num ${m.outstandingFines ? 'cell-owed' : ''}`}>{m.outstandingFines ? formatRupees(m.outstandingFines) : '—'}</td>
                    <td className="num">{m.totalLoans}</td>
                    <td className="cell-date">{formatDate(m.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
