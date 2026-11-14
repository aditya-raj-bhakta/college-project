import { formatShortDate } from '../utils/format';

// The signature detail: a due date read out like a countdown on an instrument.
// T−12d is twelve days left, T+6d is six days overdue. Used for due dates only.
export default function DueStamp({ due, late = false, showDate = true }) {
  const [y, m, d] = due.slice(0, 10).split('-').map(Number);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const days = Math.round((new Date(y, m - 1, d) - today) / 86400000);
  const t = days >= 0 ? `T−${days}d` : `T+${-days}d`;
  const isLate = late || days < 0;
  return (
    <span className={`readout ${isLate ? 'readout-late' : ''}`} title={`Due ${formatShortDate(due)}`}>
      <span className="readout-t">{t}</span>
      {showDate && <span className="readout-date">{formatShortDate(due)}</span>}
    </span>
  );
}
