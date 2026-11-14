import { useState } from 'react';
import { Link } from 'react-router-dom';
import { meApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import useAsync from '../hooks/useAsync';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import BookCover from '../components/BookCover';
import ConfirmDialog from '../components/ConfirmDialog';
import { HoldStatus } from '../components/Badges';
import { formatDate, formatDateTime, ordinal } from '../utils/format';

export default function MyHoldsPage() {
  const { user } = useAuth();
  const toast = useToast();
  const { data: holds, error, reload } = useAsync(() => meApi.holds(), []);
  const [cancelling, setCancelling] = useState(null);
  const [busy, setBusy] = useState(false);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!holds) return <Spinner />;
  const active = holds.filter((h) => h.status === 'WAITING' || h.status === 'READY');
  const past = holds.filter((h) => h.status !== 'WAITING' && h.status !== 'READY');

  const cancel = async () => {
    setBusy(true);
    try {
      await meApi.cancelHold(cancelling.id);
      toast(`Hold on "${cancelling.bookTitle}" cancelled`);
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
          <p className="page-meta">When a copy comes back it's set aside for the first person in line</p>
        </div>
        <Link to="/books" className="btn btn-secondary">Find a book</Link>
      </div>

      {active.length === 0 ? (
        <section className="card">
          <EmptyState title="No active holds" icon="clock">
            When every copy of a title is out, <Link to="/books" className="link">place a hold</Link> from the catalogue.
          </EmptyState>
        </section>
      ) : (
        <div className="hold-cards">
          {active.map((h, i) => (
            <article key={h.id} className={`hold-card ${h.status === 'READY' ? 'is-ready' : ''}`} style={{ '--i': i }}>
              <BookCover title={h.bookTitle} author={h.bookAuthor} size="md" />
              <div className="hold-card-body">
                <span className="loan-title">{h.bookTitle}</span>
                <span className="loan-author">{h.bookAuthor}</span>
                <HoldStatus status={h.status} />
                {h.status === 'READY' ? (
                  <p className="hold-card-line">At the desk until <strong>{formatDate(h.expiresOn)}</strong>. Bring card {user.cardNumber}.</p>
                ) : (
                  <p className="hold-card-line"><span className="hold-pos">{ordinal(h.queuePosition)}</span> in line · placed {formatDateTime(h.placedAt)}</p>
                )}
                <button className="btn btn-ghost btn-sm" onClick={() => setCancelling(h)}>Cancel hold</button>
              </div>
            </article>
          ))}
        </div>
      )}

      {past.length > 0 && (
        <section className="card">
          <div className="card-header"><h2>Past holds</h2></div>
          <ul className="mini-list">
            {past.map((h) => (
              <li key={h.id}>
                <div>
                  <span className="mini-title">{h.bookTitle}</span>
                  <span className="mini-sub">Placed {formatDateTime(h.placedAt)}</span>
                </div>
                <HoldStatus status={h.status} />
              </li>
            ))}
          </ul>
        </section>
      )}

      {cancelling && (
        <ConfirmDialog
          title="Cancel this hold?"
          message={`You'll lose your place in line for "${cancelling.bookTitle}".`}
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
