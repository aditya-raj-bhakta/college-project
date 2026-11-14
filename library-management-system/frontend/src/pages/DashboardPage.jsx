import { useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi, meApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import useAsync from '../hooks/useAsync';
import StatCard from '../components/StatCard';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import BookCover from '../components/BookCover';
import DueStamp from '../components/DueStamp';
import Icon from '../components/Icon';
import CountUp from '../components/CountUp';
import { HoldStatus } from '../components/Badges';
import { formatDate, formatDateTime, formatRupees, formatToday, ordinal, plural } from '../utils/format';

export default function DashboardPage() {
  const { isStaff } = useAuth();
  return isStaff ? <StaffDashboard /> : <MemberDashboard />;
}

function PageHeader({ action }) {
  return (
    <div className="page-header">
      <div>
        <h1>Dashboard</h1>
        <p className="page-meta">{formatToday()}</p>
      </div>
      {action}
    </div>
  );
}

function CardLink({ to, children }) {
  return (
    <Link to={to} className="card-link">
      {children}
      <Icon name="arrowRight" size={14} />
    </Link>
  );
}

// ====================================================================== staff

const MAP_PARTS = [
  { key: 'AVAILABLE', cls: 'shelf', label: 'On the shelf' },
  { key: 'ON_LOAN', cls: 'loan', label: 'On loan' },
  { key: 'OVERDUE', cls: 'late', label: 'Overdue' },
  { key: 'ON_HOLD_SHELF', cls: 'hold', label: 'Hold shelf' },
  { key: 'OUT', cls: 'out', label: 'Damaged or lost' },
];

/** Every copy as one cell, lit by where it is. Large collections scale down to 240 cells. */
function StackMap({ byStatus }) {
  const counts = {
    ...byStatus,
    OUT: (byStatus.DAMAGED || 0) + (byStatus.LOST || 0),
  };
  const total = MAP_PARTS.reduce((sum, p) => sum + (counts[p.key] || 0), 0);
  const perCell = Math.max(1, Math.ceil(total / 240));
  const cells = MAP_PARTS.flatMap((p) => Array(Math.round((counts[p.key] || 0) / perCell)).fill(p.cls));

  return (
    <section className="card shelf-panel">
      <div className="card-header">
        <h2>Stack map</h2>
      </div>
      <p className="shelf-figure">
        <span><CountUp value={counts.AVAILABLE || 0} duration={900} /></span> of {total} copies on the shelf
      </p>
      <div className="stack-map" role="img" aria-label={MAP_PARTS.map((p) => `${p.label}: ${counts[p.key] || 0}`).join(', ')}>
        {cells.map((cls, i) => <span key={i} className={`cell share-${cls}`} style={{ '--i': i }} />)}
      </div>
      {perCell > 1 && <p className="stack-scale">One cell is {perCell} copies</p>}
      <ul className="share-legend">
        {MAP_PARTS.map((p) => (
          <li key={p.key}>
            <span className={`legend-dot share-${p.cls}`} />
            <span>{p.label}</span>
            <span className="legend-value"><CountUp value={counts[p.key] || 0} /></span>
          </li>
        ))}
      </ul>
    </section>
  );
}

function StaffDashboard() {
  const { data, error } = useAsync(() => dashboardApi.staff(), []);
  if (error) return <div className="alert alert-error">{error}</div>;
  if (!data) return <Spinner />;

  return (
    <>
      <PageHeader action={<Link to="/desk" className="btn btn-primary"><Icon name="scan" />Open the desk</Link>} />

      <div className="stats">
        <StatCard label="Open loans" value={data.openLoans} hint={`${data.titles} titles, ${data.members} members`} />
        <StatCard label="Overdue" value={data.overdueLoans} hint={`of ${data.openLoans} open loans`} tone={data.overdueLoans > 0 ? 'late' : 'default'} />
        <StatCard label="Holds waiting" value={data.holdsWaiting} hint={`${data.holdsReady} ready for pickup`} />
        <StatCard label="Fines owed" value={data.outstandingFines} format={formatRupees} hint={`${formatRupees(data.finesCollected)} collected all time`} />
      </div>

      <div className="dash-grid">
        <section className="card">
          <div className="card-header">
            <h2>Recent activity</h2>
            <CardLink to="/loans">All loans</CardLink>
          </div>
          {data.recentActivity.length === 0 ? (
            <EmptyState title="Nothing has happened yet" icon="scroll" />
          ) : (
            <ul className="activity">
              {data.recentActivity.map((e) => (
                <li key={e.id}>
                  <span className="activity-time">{formatDateTime(e.occurredAt)}</span>
                  <span className="activity-text">
                    <span className="activity-actor">{e.actorName}</span> {e.summary}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
        <StackMap byStatus={data.copiesByStatus} />
      </div>
    </>
  );
}

// ====================================================================== member

function MemberDashboard() {
  const toast = useToast();
  const { data, error, reload } = useAsync(() => meApi.dashboard(), []);
  const [renewing, setRenewing] = useState(null);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!data) return <Spinner />;

  const { eligibility, member } = data;
  const ready = data.holds.filter((h) => h.status === 'READY');

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
      <PageHeader action={<Link to="/books" className="btn btn-primary">Find a book</Link>} />

      {ready.length > 0 && (
        <div className="notice notice-accent">
          <Icon name="bell" />
          <div>
            <strong>{ready.length === 1 ? `"${ready[0].bookTitle}" is waiting for you` : `${ready.length} holds are waiting for you`}</strong>
            <span>Pick up at the desk by {formatDate(ready[0].expiresOn)} with card {member.cardNumber}.</span>
          </div>
        </div>
      )}

      {!eligibility.canBorrow && (
        <div className="notice notice-late">
          <Icon name="alert" />
          <div>
            <strong>You can't borrow right now</strong>
            <ul>{eligibility.blocks.map((b) => <li key={b}>{b}</li>)}</ul>
          </div>
        </div>
      )}

      <div className="stats">
        <StatCard label="Books with you" value={eligibility.openLoans} hint={`of ${eligibility.maxLoans} allowed at once`} />
        <StatCard label="Holds" value={data.holds.length} hint={ready.length ? `${ready.length} ready for pickup` : 'none ready yet'} />
        <StatCard
          label="Fines owed"
          value={data.outstandingFines}
          format={formatRupees}
          hint={`${formatRupees(data.finesPaid)} paid all time`}
          tone={data.outstandingFines > eligibility.fineBlockThreshold ? 'late' : 'default'}
        />
        <StatCard label="Borrowed so far" value={data.totalBorrowed} hint="all time" />
      </div>

      <div className="dash-grid">
        <section className="card">
          <div className="card-header">
            <h2>Currently reading</h2>
            <CardLink to="/my-loans">Loan history</CardLink>
          </div>
          {data.currentLoans.length > 0 && (
            <p className="card-note">Return books at the circulation desk. A librarian checks them in and settles any fine.</p>
          )}
          {data.currentLoans.length === 0 ? (
            <EmptyState title="No books borrowed right now" icon="bookOpen">
              <Link to="/books" className="link">Browse the catalogue</Link> to find your next read.
            </EmptyState>
          ) : (
            <div className="loan-cards">
              {data.currentLoans.map((l, i) => {
                const late = l.status === 'OVERDUE';
                return (
                  <div key={l.id} className={`loan-card ${late ? 'is-overdue' : ''}`} style={{ '--i': i }}>
                    <BookCover title={l.bookTitle} author={l.bookAuthor} size="md" />
                    <div className="loan-card-body">
                      <span className="loan-title">{l.bookTitle}</span>
                      <span className="loan-author">{l.bookAuthor}</span>
                      <div className="loan-card-foot">
                        <DueStamp due={l.dueDate} late={late} />
                        {l.accruingFine > 0 && <span className="fine">Fine so far {formatRupees(l.accruingFine)}</span>}
                      </div>
                      {l.canRenew ? (
                        <button className="btn btn-secondary btn-sm loan-renew" disabled={renewing === l.id} onClick={() => renew(l)}>
                          <Icon name="renew" />
                          {renewing === l.id ? 'Renewing…' : 'Renew'}
                        </button>
                      ) : (
                        <span className="loan-blocked">{l.renewBlockedReason}</span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        <section className="card">
          <div className="card-header">
            <h2>Holds</h2>
            <CardLink to="/my-holds">All holds</CardLink>
          </div>
          {data.holds.length === 0 ? (
            <p className="card-note">When every copy of a title is out, place a hold from the catalogue and you'll be told when it's your turn.</p>
          ) : (
            <ul className="mini-list">
              {data.holds.map((h) => (
                <li key={h.id}>
                  <div>
                    <span className="mini-title">{h.bookTitle}</span>
                    <span className="mini-sub">
                      {h.status === 'READY' ? `Pick up by ${formatDate(h.expiresOn)}` : `${ordinal(h.queuePosition)} in line`}
                    </span>
                  </div>
                  <HoldStatus status={h.status} />
                </li>
              ))}
            </ul>
          )}

          <div className="card-header card-header-split">
            <h2>Messages</h2>
          </div>
          {data.notifications.length === 0 ? (
            <p className="card-note">Due-date reminders and pickup notices will appear here.</p>
          ) : (
            <ul className="mini-list">
              {data.notifications.slice(0, 4).map((n) => (
                <li key={n.id} className="mini-message">
                  <span className="mini-sub">{formatDateTime(n.sentAt)}</span>
                  <span>{n.message}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {member.memberType && (
        <p className="page-footnote">
          {member.memberType.name} members borrow up to {plural(member.memberType.maxLoans, 'book')} for {member.memberType.loanDays} days,
          renew {plural(member.memberType.maxRenewals, 'time')}, and pay {formatRupees(member.memberType.finePerDay)} a day when late.
        </p>
      )}
    </>
  );
}
