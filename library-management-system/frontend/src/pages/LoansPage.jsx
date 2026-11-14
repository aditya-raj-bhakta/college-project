import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { loansApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import useAsync from '../hooks/useAsync';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import ConfirmDialog from '../components/ConfirmDialog';
import DueStamp from '../components/DueStamp';
import Icon from '../components/Icon';
import { LoanStatus } from '../components/Badges';
import { formatDate, formatRupees } from '../utils/format';

const FILTERS = [
  { value: 'active', label: 'Out now' },
  { value: 'overdue', label: 'Overdue' },
  { value: 'returned', label: 'Returned' },
  { value: 'lost', label: 'Lost' },
  { value: 'all', label: 'All' },
];

export default function LoansPage() {
  const toast = useToast();
  const [status, setStatus] = useState('active');
  const [search, setSearch] = useState('');
  const { data: loans, error, loading, reload } = useAsync(() => loansApi.list(status), [status]);
  const [busyId, setBusyId] = useState(null);
  const [losing, setLosing] = useState(null);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q || !loans) return loans || [];
    return loans.filter((l) => [l.bookTitle, l.memberName, l.memberEmail, l.barcode, l.cardNumber].some((v) => v?.toLowerCase().includes(q)));
  }, [loans, search]);

  const run = async (loan, action) => {
    setBusyId(loan.id);
    try {
      if (action === 'checkin') {
        const r = await loansApi.checkin(loan.id);
        toast(r.holdFor
          ? `Checked in. Put it on the hold shelf for ${r.holdFor.memberName}.`
          : `Checked in${r.fineCharged ? `, ${formatRupees(r.fineCharged)} late fine added` : ''}`);
      } else if (action === 'renew') {
        const l = await loansApi.renew(loan.id);
        toast(`Renewed until ${formatDate(l.dueDate)}`);
      } else if (action === 'lost') {
        await loansApi.markLost(loan.id);
        toast(`Marked lost. The member was charged the lost item fee.`);
        setLosing(null);
      }
      reload();
    } catch (err) {
      toast(getErrorMessage(err), 'error');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Loans</h1>
          <p className="page-meta">Everything that is out, and everything that came back</p>
        </div>
        <Link to="/desk" className="btn btn-secondary"><Icon name="scan" />Desk</Link>
      </div>

      <div className="toolbar">
        <div className="segmented" role="tablist">
          {FILTERS.map((f) => (
            <button key={f.value} role="tab" aria-selected={status === f.value} className={status === f.value ? 'active' : ''} onClick={() => setStatus(f.value)}>
              {f.label}
            </button>
          ))}
        </div>
        <label className="search-field">
          <Icon name="search" />
          <input type="search" placeholder="Book, member, card or barcode" value={search} onChange={(e) => setSearch(e.target.value)} aria-label="Filter loans" />
        </label>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="card">
        {!loans || (loading && !loans) ? (
          <Spinner />
        ) : filtered.length === 0 ? (
          <EmptyState title="No loans to show" icon="loans" />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Book</th><th>Member</th><th>Borrowed</th><th>Due</th><th>Status</th><th className="num">Fine</th><th /></tr>
              </thead>
              <tbody>
                {filtered.map((l) => {
                  const open = l.status === 'ACTIVE' || l.status === 'OVERDUE';
                  return (
                    <tr key={l.id}>
                      <td>
                        <span className="cell-strong">{l.bookTitle}</span>
                        <div className="cell-sub mono">{l.barcode} · {l.channel === 'DESK' ? 'desk' : 'online'}{l.renewalCount ? ` · renewed ${l.renewalCount}×` : ''}</div>
                      </td>
                      <td>
                        <Link to={`/members/${l.memberId}`} className="cell-link">{l.memberName}</Link>
                        <div className="cell-sub">{l.cardNumber}</div>
                      </td>
                      <td className="cell-date">{formatDate(l.borrowDate)}</td>
                      <td>{open ? <DueStamp due={l.dueDate} late={l.status === 'OVERDUE'} /> : <span className="cell-date">{formatDate(l.dueDate)}</span>}</td>
                      <td><LoanStatus status={l.status} daysOverdue={l.daysOverdue} /></td>
                      <td className="num">{l.accruingFine > 0 ? formatRupees(l.accruingFine) : '—'}</td>
                      <td className="actions">
                        {open && (
                          <>
                            <button className="btn btn-ghost btn-sm" disabled={busyId === l.id} onClick={() => setLosing(l)}>Lost</button>
                            <button className="btn btn-ghost btn-sm" disabled={busyId === l.id} onClick={() => run(l, 'renew')}>Renew</button>
                            <button className="btn btn-secondary btn-sm" disabled={busyId === l.id} onClick={() => run(l, 'checkin')}>Check in</button>
                          </>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {losing && (
        <ConfirmDialog
          title="Mark this copy lost?"
          message={`Copy ${losing.barcode} of "${losing.bookTitle}" leaves the collection, and ${losing.memberName} is charged the lost item fee plus any late fine. A librarian can waive part of it later.`}
          confirmLabel="Mark lost"
          danger
          busy={busyId === losing.id}
          onConfirm={() => run(losing, 'lost')}
          onCancel={() => setLosing(null)}
        />
      )}
    </>
  );
}
