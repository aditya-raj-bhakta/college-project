import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { deskApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import Icon from '../components/Icon';
import DueStamp from '../components/DueStamp';
import { AccountStatus } from '../components/Badges';
import { formatDate, formatRupees, plural } from '../utils/format';

/**
 * The circulation desk. Built for a barcode scanner: every field submits on Enter,
 * clears itself and keeps focus, so a librarian can scan card, copy, copy, copy.
 */
export default function DeskPage() {
  return (
    <>
      <div className="page-header">
        <div>
          <h1>Desk</h1>
          <p className="page-meta">Scan a library card to issue books, or scan a returned copy to check it in</p>
        </div>
      </div>
      <div className="desk-grid">
        <CheckoutPanel />
        <CheckinPanel />
      </div>
    </>
  );
}

function ScanField({ label, placeholder, onSubmit, busy, disabled, autoFocus, inputRef }) {
  const [value, setValue] = useState('');
  const submit = async (e) => {
    e.preventDefault();
    const code = value.trim().toUpperCase();
    if (!code || busy) return;
    const ok = await onSubmit(code);
    if (ok !== false) setValue('');
    inputRef?.current?.focus();
  };
  return (
    <form className="scan-field" onSubmit={submit}>
      <label className="field">
        <span>{label}</span>
        <div className="scan-input">
          <Icon name="scan" />
          <input
            ref={inputRef}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder={placeholder}
            autoFocus={autoFocus}
            disabled={disabled}
            autoComplete="off"
            spellCheck="false"
          />
          <button className="btn btn-secondary btn-sm" disabled={busy || disabled || !value.trim()}>
            {busy ? 'Working…' : 'Enter'}
          </button>
        </div>
      </label>
    </form>
  );
}

// ====================================================================== check out

function CheckoutPanel() {
  const toast = useToast();
  const [patron, setPatron] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [issued, setIssued] = useState([]);
  const copyRef = useRef(null);
  const cardRef = useRef(null);

  const lookUp = async (card) => {
    setBusy(true);
    setError('');
    try {
      const p = await deskApi.patron(card);
      setPatron(p);
      setIssued([]);
      setTimeout(() => copyRef.current?.focus(), 0);
      return true;
    } catch (err) {
      setPatron(null);
      setError(getErrorMessage(err));
      return false;
    } finally {
      setBusy(false);
    }
  };

  const issue = async (barcode) => {
    setBusy(true);
    setError('');
    try {
      const loan = await deskApi.checkout(patron.member.cardNumber, barcode);
      setIssued((list) => [loan, ...list]);
      toast(`"${loan.bookTitle}" issued, due ${formatDate(loan.dueDate)}`);
      setPatron(await deskApi.patron(patron.member.cardNumber));
      return true;
    } catch (err) {
      setError(getErrorMessage(err));
      return false;
    } finally {
      setBusy(false);
    }
  };

  const clear = () => {
    setPatron(null);
    setIssued([]);
    setError('');
    setTimeout(() => cardRef.current?.focus(), 0);
  };

  const m = patron?.member;
  const e = patron?.eligibility;
  const openLoans = patron?.loans.filter((l) => l.status === 'ACTIVE' || l.status === 'OVERDUE') || [];
  const readyHolds = patron?.holds.filter((h) => h.status === 'READY') || [];

  return (
    <section className="card desk-panel">
      <div className="card-header">
        <h2>Check out</h2>
        {patron && <button className="card-link" onClick={clear}>Next member<Icon name="arrowRight" size={14} /></button>}
      </div>

      {!patron ? (
        <ScanField label="Library card" placeholder="M000003" onSubmit={lookUp} busy={busy} autoFocus inputRef={cardRef} />
      ) : (
        <>
          <div className="patron">
            <div className="patron-head">
              <div>
                <Link to={`/members/${m.id}`} className="patron-name">{m.name}</Link>
                <span className="patron-meta">{m.cardNumber} · {m.memberType?.name || 'No type'} · {m.email}</span>
              </div>
              <AccountStatus status={m.status} />
            </div>

            {e.canBorrow ? (
              <p className="patron-ok"><Icon name="check" />Can borrow {plural(e.maxLoans - e.openLoans, 'more book')}</p>
            ) : (
              <ul className="patron-blocks">
                {e.blocks.map((b) => <li key={b}><Icon name="alert" />{b}</li>)}
              </ul>
            )}

            <dl className="patron-facts">
              <div><dt>Out</dt><dd>{e.openLoans} of {e.maxLoans}</dd></div>
              <div><dt>Fines owed</dt><dd className={e.outstandingFines > e.fineBlockThreshold ? 'is-late' : ''}>{formatRupees(e.outstandingFines)}</dd></div>
              <div><dt>Holds ready</dt><dd>{readyHolds.length}</dd></div>
            </dl>

            {readyHolds.length > 0 && (
              <div className="patron-holds">
                {readyHolds.map((h) => (
                  <p key={h.id}><Icon name="bell" />On the hold shelf: <strong>{h.bookTitle}</strong> <span className="mono">{h.barcode}</span></p>
                ))}
              </div>
            )}
          </div>

          <ScanField label="Copy barcode" placeholder="SW000012" onSubmit={issue} busy={busy} disabled={!e.canBorrow} inputRef={copyRef} />

          {(issued.length > 0 || openLoans.length > 0) && (
            <ul className="desk-list">
              {issued.map((l) => (
                <li key={`new-${l.id}`} className="is-new">
                  <span className="desk-list-main"><strong>{l.bookTitle}</strong><span className="mono">{l.barcode}</span></span>
                  <span className="desk-tag">Issued now</span>
                  <DueStamp due={l.dueDate} />
                </li>
              ))}
              {openLoans.filter((l) => !issued.some((n) => n.id === l.id)).map((l) => (
                <li key={l.id}>
                  <span className="desk-list-main"><strong>{l.bookTitle}</strong><span className="mono">{l.barcode}</span></span>
                  <DueStamp due={l.dueDate} late={l.status === 'OVERDUE'} />
                </li>
              ))}
            </ul>
          )}
        </>
      )}
      {error && <div className="alert alert-error desk-alert">{error}</div>}
    </section>
  );
}

// ====================================================================== check in

function CheckinPanel() {
  const [results, setResults] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const ref = useRef(null);

  const checkIn = async (barcode) => {
    setBusy(true);
    setError('');
    try {
      const r = await deskApi.checkin(barcode);
      setResults((list) => [{ ...r, at: Date.now() }, ...list].slice(0, 12));
      return true;
    } catch (err) {
      setError(getErrorMessage(err));
      return false;
    } finally {
      setBusy(false);
    }
  };

  const [latest, ...earlier] = results;

  return (
    <section className="card desk-panel">
      <div className="card-header">
        <h2>Check in</h2>
        {results.length > 0 && <span className="card-meta">{plural(results.length, 'return')} this session</span>}
      </div>

      <ScanField label="Returned copy" placeholder="SW000003" onSubmit={checkIn} busy={busy} inputRef={ref} />
      {error && <div className="alert alert-error desk-alert">{error}</div>}

      {latest ? (
        <div key={latest.at} className={`checkin-result ${latest.holdFor ? 'to-hold' : ''}`}>
          <div className="checkin-route">
            <span className="checkin-route-label">{latest.holdFor ? 'Hold shelf' : 'Back to shelf'}</span>
            <span className="checkin-route-target">
              {latest.holdFor ? `${latest.holdFor.memberName} · ${latest.holdFor.cardNumber}` : latest.shelfLocation || 'Open shelf'}
            </span>
            {latest.holdFor && <span className="checkin-route-sub">Held until {formatDate(latest.holdFor.expiresOn)}. They've been notified.</span>}
          </div>
          <div className="checkin-loan">
            <span><strong>{latest.loan.bookTitle}</strong> <span className="mono">{latest.loan.barcode}</span></span>
            <span className="checkin-from">from {latest.loan.memberName}</span>
            {latest.fineCharged > 0
              ? <span className="fine">{formatRupees(latest.fineCharged)} late fine added to their account</span>
              : <span className="checkin-ontime"><Icon name="check" size={14} />On time, no fine</span>}
          </div>
        </div>
      ) : (
        <p className="card-note desk-empty">Scan the barcode on the spine. If someone is waiting for the title, Shelfwise sets the copy aside for them.</p>
      )}

      {earlier.length > 0 && (
        <ul className="desk-list">
          {earlier.map((r) => (
            <li key={r.at}>
              <span className="desk-list-main"><strong>{r.loan.bookTitle}</strong><span className="mono">{r.loan.barcode}</span></span>
              <span className="desk-tag">{r.holdFor ? `Hold: ${r.holdFor.memberName}` : 'Shelf'}</span>
              {r.fineCharged > 0 && <span className="fine">{formatRupees(r.fineCharged)}</span>}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
