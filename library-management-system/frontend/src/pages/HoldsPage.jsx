import { useState } from 'react';
import { Link } from 'react-router-dom';
import { holdsApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import useAsync from '../hooks/useAsync';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import ConfirmDialog from '../components/ConfirmDialog';
import { HoldStatus } from '../components/Badges';
import { formatDate, formatDateTime, ordinal } from '../utils/format';

const FILTERS = [
  { value: 'ready', label: 'Hold shelf' },
  { value: 'waiting', label: 'Waiting' },
  { value: 'active', label: 'All active' },
];

export default function HoldsPage() {
  const toast = useToast();
  const [status, setStatus] = useState('ready');
  const { data: holds, error, reload } = useAsync(() => holdsApi.list(status), [status]);
  const [cancelling, setCancelling] = useState(null);
  const [busy, setBusy] = useState(false);

  const cancel = async () => {
    setBusy(true);
    try {
      await holdsApi.cancel(cancelling.id);
      toast(cancelling.status === 'READY'
        ? `Hold cancelled. Copy ${cancelling.barcode} moves to the next member in line, or back to the shelf.`
        : 'Hold cancelled');
      setCancelling(null);
      reload();
    } catch (err) {
      toast(getErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Holds</h1>
          <p className="page-meta">Copies on the hold shelf wait for their member until the pickup date, then go to the next in line</p>
        </div>
      </div>

      <div className="toolbar">
        <div className="segmented" role="tablist">
          {FILTERS.map((f) => (
            <button key={f.value} role="tab" aria-selected={status === f.value} className={status === f.value ? 'active' : ''} onClick={() => setStatus(f.value)}>
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="card">
        {!holds ? (
          <Spinner />
        ) : holds.length === 0 ? (
          <EmptyState title={status === 'ready' ? 'The hold shelf is empty' : 'Nobody is waiting'} icon="clock" />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Book</th><th>Member</th><th>Placed</th><th>Status</th><th>Queue or pickup</th><th /></tr>
              </thead>
              <tbody>
                {holds.map((h) => (
                  <tr key={h.id}>
                    <td><Link to={`/books/${h.bookId}`} className="cell-link cell-strong">{h.bookTitle}</Link><div className="cell-sub">{h.bookAuthor}</div></td>
                    <td><Link to={`/members/${h.memberId}`} className="cell-link">{h.memberName}</Link><div className="cell-sub">{h.cardNumber}</div></td>
                    <td className="cell-date">{formatDateTime(h.placedAt)}</td>
                    <td><HoldStatus status={h.status} /></td>
                    <td>
                      {h.status === 'READY'
                        ? <span className="cell-pair"><span className="mono">{h.barcode}</span><span className="cell-sub">until {formatDate(h.expiresOn)}</span></span>
                        : <span className="cell-sub-inline">{ordinal(h.queuePosition)} in line</span>}
                    </td>
                    <td className="actions"><button className="btn btn-ghost btn-sm" onClick={() => setCancelling(h)}>Cancel</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {cancelling && (
        <ConfirmDialog
          title="Cancel this hold?"
          message={`${cancelling.memberName} loses their place for "${cancelling.bookTitle}".`}
          confirmLabel="Cancel hold"
          danger
          busy={busy}
          onConfirm={cancel}
          onCancel={() => setCancelling(null)}
        />
      )}
    </>
  );
}
