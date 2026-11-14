import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { meApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import useAsync from '../hooks/useAsync';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import DueStamp from '../components/DueStamp';
import Icon from '../components/Icon';
import { LoanStatus } from '../components/Badges';
import { formatDate, formatRupees } from '../utils/format';

export default function MyLoansPage() {
  const toast = useToast();
  const { data: loans, error, reload } = useAsync(() => meApi.loans(), []);
  const [tab, setTab] = useState('current');
  const [renewing, setRenewing] = useState(null);

  const { current, history } = useMemo(() => ({
    current: (loans || []).filter((l) => l.status === 'ACTIVE' || l.status === 'OVERDUE'),
    history: (loans || []).filter((l) => l.status === 'RETURNED' || l.status === 'LOST'),
  }), [loans]);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!loans) return <Spinner />;
  const shown = tab === 'current' ? current : history;

  const renew = async (loan) => {
    setRenewing(loan.id);
    try {
      const updated = await meApi.renew(loan.id);
      toast(`"${updated.bookTitle}" renewed until ${formatDate(updated.dueDate)}`);
      reload();
    } catch (err) {
      toast(getErrorMessage(err, 'Could not renew'), 'error');
    } finally {
      setRenewing(null);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>My loans</h1>
          <p className="page-meta">Renew online if nobody is waiting. Return books at the circulation desk.</p>
        </div>
      </div>

      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === 'current'} className={tab === 'current' ? 'active' : ''} onClick={() => setTab('current')}>
          Borrowed now <span className="tab-count">{current.length}</span>
        </button>
        <button role="tab" aria-selected={tab === 'history'} className={tab === 'history' ? 'active' : ''} onClick={() => setTab('history')}>
          History <span className="tab-count">{history.length}</span>
        </button>
      </div>

      <section className="card">
        {shown.length === 0 ? (
          <EmptyState title={tab === 'current' ? 'Nothing borrowed right now' : 'No past loans yet'} icon="bookOpen">
            {tab === 'current' && <><Link to="/books" className="link">Browse books</Link> to borrow one.</>}
          </EmptyState>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Book</th>
                  <th>Borrowed</th>
                  <th>Due</th>
                  {tab === 'history' && <th>Returned</th>}
                  <th>Status</th>
                  {tab === 'current' && <th className="num">Fine so far</th>}
                  {tab === 'current' && <th />}
                </tr>
              </thead>
              <tbody>
                {shown.map((l) => (
                  <tr key={l.id}>
                    <td>
                      <span className="cell-strong">{l.bookTitle}</span>
                      <div className="cell-sub">{l.bookAuthor} · <span className="mono">{l.barcode}</span></div>
                    </td>
                    <td className="cell-date">{formatDate(l.borrowDate)}</td>
                    <td>{tab === 'current' ? <DueStamp due={l.dueDate} late={l.status === 'OVERDUE'} /> : <span className="cell-date">{formatDate(l.dueDate)}</span>}</td>
                    {tab === 'history' && <td className="cell-date">{formatDate(l.returnDate)}</td>}
                    <td><LoanStatus status={l.status} daysOverdue={l.daysOverdue} /></td>
                    {tab === 'current' && <td className="num">{l.accruingFine > 0 ? formatRupees(l.accruingFine) : '—'}</td>}
                    {tab === 'current' && (
                      <td className="actions">
                        {l.canRenew ? (
                          <button className="btn btn-secondary btn-sm" disabled={renewing === l.id} onClick={() => renew(l)}>
                            <Icon name="renew" />{renewing === l.id ? 'Renewing…' : 'Renew'}
                          </button>
                        ) : (
                          <span className="cell-sub renew-reason">{l.renewBlockedReason}</span>
                        )}
                      </td>
                    )}
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
