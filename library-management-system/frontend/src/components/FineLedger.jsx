import { useState } from 'react';
import Icon from './Icon';
import { formatDate, formatDateTime, formatRupees } from '../utils/format';

const TYPE = { LATE_RETURN: 'Late return', LOST_ITEM: 'Lost item' };

/**
 * A list of fines with their payment and waiver history. Staff get Pay and Waive actions;
 * members see the same ledger read-only.
 */
export default function FineLedger({ fines, showMember = false, onPay, onWaive }) {
  const [open, setOpen] = useState(null);
  return (
    <div className="table-wrap">
      <table className="table ledger">
        <thead>
          <tr>
            <th>Date</th>
            {showMember && <th>Member</th>}
            <th>Charge</th>
            <th className="num">Amount</th>
            <th className="num">Paid</th>
            <th className="num">Waived</th>
            <th className="num">Owed</th>
            {(onPay || onWaive) && <th />}
          </tr>
        </thead>
        <tbody>
          {fines.map((f) => (
            <FineRow key={f.id} fine={f} showMember={showMember} onPay={onPay} onWaive={onWaive}
              expanded={open === f.id} onToggle={() => setOpen(open === f.id ? null : f.id)} />
          ))}
        </tbody>
      </table>
    </div>
  );
}

function FineRow({ fine: f, showMember, onPay, onWaive, expanded, onToggle }) {
  const cols = 6 + (showMember ? 1 : 0) + (onPay || onWaive ? 1 : 0);
  return (
    <>
      <tr className={f.settled ? 'is-settled' : ''}>
        <td className="cell-date">{formatDate(f.createdOn)}</td>
        {showMember && <td><span className="cell-strong">{f.memberName}</span><div className="cell-sub">{f.cardNumber}</div></td>}
        <td>
          <span>{TYPE[f.type] || f.type}</span>
          <div className="cell-sub">{f.description}</div>
          {f.transactions.length > 0 && (
            <button className="ledger-toggle" onClick={onToggle} aria-expanded={expanded}>
              <Icon name={expanded ? 'chevronLeft' : 'chevronRight'} size={12} />
              {f.transactions.length} entr{f.transactions.length === 1 ? 'y' : 'ies'}
            </button>
          )}
        </td>
        <td className="num">{formatRupees(f.amount)}</td>
        <td className="num">{f.paid ? formatRupees(f.paid) : '—'}</td>
        <td className="num">{f.waived ? formatRupees(f.waived) : '—'}</td>
        <td className={`num ${f.balance > 0 ? 'cell-owed' : ''}`}>{f.balance ? formatRupees(f.balance) : 'Settled'}</td>
        {(onPay || onWaive) && (
          <td className="actions">
            {!f.settled && (
              <>
                {onWaive && <button className="btn btn-ghost btn-sm" onClick={() => onWaive(f)}>Waive</button>}
                {onPay && <button className="btn btn-secondary btn-sm" onClick={() => onPay(f)}>Take payment</button>}
              </>
            )}
          </td>
        )}
      </tr>
      {expanded && (
        <tr className="ledger-detail">
          <td colSpan={cols}>
            <ul>
              {f.transactions.map((t) => (
                <li key={t.id}>
                  <span className="cell-date">{formatDateTime(t.createdAt)}</span>
                  <span>{t.kind === 'PAYMENT' ? 'Paid' : 'Waived'} {formatRupees(t.amount)}</span>
                  <span className="cell-sub">{t.note ? `${t.note} · ` : ''}{t.staffName}</span>
                </li>
              ))}
            </ul>
          </td>
        </tr>
      )}
    </>
  );
}
