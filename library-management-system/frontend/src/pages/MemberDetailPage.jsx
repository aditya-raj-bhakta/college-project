import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { holdsApi, loansApi, membersApi } from '../api/services';
import { getErrorMessage, getFieldErrors } from '../api/client';
import { useToast } from '../context/ToastContext';
import useAsync from '../hooks/useAsync';
import Spinner from '../components/Spinner';
import Modal from '../components/Modal';
import Icon from '../components/Icon';
import DueStamp from '../components/DueStamp';
import FineLedger from '../components/FineLedger';
import { PayDialog, WaiveDialog } from '../components/FineDialogs';
import { AccountStatus, HoldStatus, LoanStatus } from '../components/Badges';
import { formatDate, formatDateTime, formatRupees, ordinal } from '../utils/format';

export default function MemberDetailPage() {
  const { id } = useParams();
  const toast = useToast();
  const { data, error, reload, setData } = useAsync(() => membersApi.get(id), [id]);
  const [types, setTypes] = useState([]);
  const [suspending, setSuspending] = useState(false);
  const [paying, setPaying] = useState(null);
  const [waiving, setWaiving] = useState(null);
  const [busy, setBusy] = useState(null);

  useEffect(() => {
    membersApi.types().then(setTypes).catch(() => {});
  }, []);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!data) return <Spinner />;
  const { member: m, eligibility: e, loans, holds, fines, notifications } = data;
  const openLoans = loans.filter((l) => l.status === 'ACTIVE' || l.status === 'OVERDUE');
  const pastLoans = loans.filter((l) => !(l.status === 'ACTIVE' || l.status === 'OVERDUE'));
  const activeHolds = holds.filter((h) => h.status === 'WAITING' || h.status === 'READY');

  const act = async (key, fn, message) => {
    setBusy(key);
    try {
      const result = await fn();
      if (result?.member) setData(result); else reload();
      if (message) toast(typeof message === 'function' ? message(result) : message);
    } catch (err) {
      toast(getErrorMessage(err), 'error');
    } finally {
      setBusy(null);
    }
  };

  return (
    <>
      <Link to="/members" className="back-link"><Icon name="arrowLeft" size={14} />Members</Link>
      <div className="page-header">
        <div>
          <h1>{m.name}</h1>
          <p className="page-meta"><span className="mono">{m.cardNumber}</span> · {m.email} · joined {formatDate(m.createdAt)}</p>
        </div>
        <div className="header-actions">
          {m.status === 'SUSPENDED' ? (
            <button className="btn btn-secondary" disabled={busy === 'status'} onClick={() => act('status', () => membersApi.reactivate(m.id), `${m.name} can borrow again`)}>Reactivate</button>
          ) : (
            <button className="btn btn-ghost" onClick={() => setSuspending(true)}>Suspend</button>
          )}
        </div>
      </div>

      <div className="member-top">
        <section className="card member-standing">
          <div className="card-header">
            <h2>Standing</h2>
            <AccountStatus status={m.status} />
          </div>
          {e.canBorrow ? (
            <p className="patron-ok"><Icon name="check" />Can borrow {e.maxLoans - e.openLoans} more</p>
          ) : (
            <ul className="patron-blocks">{e.blocks.map((b) => <li key={b}><Icon name="alert" />{b}</li>)}</ul>
          )}
          <dl className="patron-facts">
            <div><dt>Out</dt><dd>{e.openLoans} of {e.maxLoans}</dd></div>
            <div><dt>Fines owed</dt><dd className={e.outstandingFines > e.fineBlockThreshold ? 'is-late' : ''}>{formatRupees(e.outstandingFines)}</dd></div>
            <div><dt>Holds</dt><dd>{activeHolds.length}</dd></div>
          </dl>
        </section>

        <section className="card">
          <div className="card-header"><h2>Member type</h2></div>
          <label className="field">
            <span>Borrowing rules</span>
            <select
              value={m.memberType?.id || ''}
              disabled={busy === 'type'}
              onChange={(ev) => act('type', () => membersApi.changeType(m.id, Number(ev.target.value)), (r) => `${m.name} is now ${r.member.memberType.name.toLowerCase()}`)}
            >
              {!m.memberType && <option value="">Choose a type</option>}
              {types.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </label>
          {m.memberType && (
            <p className="card-note type-rules">
              {m.memberType.maxLoans} books for {m.memberType.loanDays} days · {m.memberType.maxRenewals} renewals ·
              {' '}{formatRupees(m.memberType.finePerDay)}/day late · blocked over {formatRupees(m.memberType.fineBlockThreshold)}
            </p>
          )}
          {m.status === 'SUSPENDED' && m.suspendedReason && <p className="notice-inline">Suspended: {m.suspendedReason}</p>}
        </section>
      </div>

      <section className="card">
        <div className="card-header"><h2>Loans</h2><span className="card-meta">{openLoans.length} out · {pastLoans.length} past</span></div>
        {loans.length === 0 ? <p className="card-note">No loans yet.</p> : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Book</th><th>Borrowed</th><th>Due</th><th>Returned</th><th>Status</th><th /></tr></thead>
              <tbody>
                {[...openLoans, ...pastLoans].map((l) => {
                  const open = l.status === 'ACTIVE' || l.status === 'OVERDUE';
                  return (
                    <tr key={l.id}>
                      <td><span className="cell-strong">{l.bookTitle}</span><div className="cell-sub mono">{l.barcode}</div></td>
                      <td className="cell-date">{formatDate(l.borrowDate)}</td>
                      <td>{open ? <DueStamp due={l.dueDate} late={l.status === 'OVERDUE'} /> : <span className="cell-date">{formatDate(l.dueDate)}</span>}</td>
                      <td className="cell-date">{formatDate(l.returnDate)}</td>
                      <td><LoanStatus status={l.status} daysOverdue={l.daysOverdue} /></td>
                      <td className="actions">
                        {open && (
                          <>
                            <button className="btn btn-ghost btn-sm" disabled={busy === l.id} onClick={() => act(l.id, () => loansApi.renew(l.id), 'Renewed')}>Renew</button>
                            <button className="btn btn-secondary btn-sm" disabled={busy === l.id} onClick={() => act(l.id, () => loansApi.checkin(l.id),
                              (r) => (r.holdFor ? `Checked in. Hold shelf: ${r.holdFor.memberName}` : 'Checked in'))}>Check in</button>
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

      <div className="member-bottom">
        <section className="card member-fines">
          <div className="card-header"><h2>Fines</h2><span className="card-meta">{formatRupees(e.outstandingFines)} owed</span></div>
          {fines.length === 0 ? <p className="card-note">No fines.</p> : (
            <FineLedger fines={fines} onPay={setPaying} onWaive={setWaiving} />
          )}
        </section>

        <section className="card">
          <div className="card-header"><h2>Holds</h2></div>
          {activeHolds.length === 0 ? <p className="card-note">No active holds.</p> : (
            <ul className="mini-list">
              {activeHolds.map((h) => (
                <li key={h.id}>
                  <div>
                    <span className="mini-title">{h.bookTitle}</span>
                    <span className="mini-sub">{h.status === 'READY' ? `${h.barcode} until ${formatDate(h.expiresOn)}` : `${ordinal(h.queuePosition)} in line`}</span>
                  </div>
                  <HoldStatus status={h.status} />
                  <button className="btn btn-ghost btn-sm" disabled={busy === `h${h.id}`} onClick={() => act(`h${h.id}`, () => holdsApi.cancel(h.id), 'Hold cancelled')}>Cancel</button>
                </li>
              ))}
            </ul>
          )}
          <div className="card-header card-header-split"><h2>Messages sent</h2></div>
          {notifications.length === 0 ? <p className="card-note">No reminders sent yet.</p> : (
            <ul className="mini-list">
              {notifications.slice(0, 6).map((n) => (
                <li key={n.id} className="mini-message">
                  <span className="mini-sub">{formatDateTime(n.sentAt)}</span>
                  <span>{n.message}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {suspending && <SuspendDialog member={m} onClose={() => setSuspending(false)} onDone={(detail) => {
        setData(detail);
        setSuspending(false);
        toast(`${m.name} is suspended`);
      }} />}
      {paying && <PayDialog fine={paying} onClose={() => setPaying(null)} onDone={() => { setPaying(null); toast('Payment recorded'); reload(); }} />}
      {waiving && <WaiveDialog fine={waiving} onClose={() => setWaiving(null)} onDone={() => { setWaiving(null); toast('Waiver recorded'); reload(); }} />}
    </>
  );
}

function SuspendDialog({ member, onClose, onDone }) {
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      onDone(await membersApi.suspend(member.id, reason));
    } catch (err) {
      setError(getFieldErrors(err).reason || getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };
  return (
    <Modal title={`Suspend ${member.name}?`} onClose={onClose} width={440} footer={
      <>
        <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button type="submit" form="suspend-form" className="btn btn-danger" disabled={busy}>{busy ? 'Suspending…' : 'Suspend'}</button>
      </>
    }>
      <form id="suspend-form" className="form-stack" onSubmit={submit}>
        <p className="muted">They can still log in and see their account, but can't borrow, renew or place holds until reactivated.</p>
        <label className="field">
          <span>Reason</span>
          <input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Repeated late returns" autoFocus />
          {error && <small className="field-error">{error}</small>}
        </label>
      </form>
    </Modal>
  );
}
