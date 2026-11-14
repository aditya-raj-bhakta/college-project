// Status as a dot and a word. Tone carries meaning: accent = on the shelf / good,
// late = needs action, warn = waiting on someone, muted = closed.

function Badge({ tone, children }) {
  return <span className={`status tone-${tone}`}>{children}</span>;
}

const LOAN = {
  ACTIVE: ['outline', 'On loan'],
  OVERDUE: ['late', 'Overdue'],
  RETURNED: ['muted', 'Returned'],
  LOST: ['late', 'Lost'],
};

export function LoanStatus({ status, daysOverdue }) {
  const [tone, label] = LOAN[status] || ['muted', status];
  const text = status === 'OVERDUE' && daysOverdue ? `Overdue · ${daysOverdue} day${daysOverdue === 1 ? '' : 's'}` : label;
  return <Badge tone={tone}>{text}</Badge>;
}

const HOLD = {
  WAITING: ['warn', 'Waiting'],
  READY: ['accent', 'Ready for pickup'],
  FULFILLED: ['muted', 'Collected'],
  CANCELLED: ['muted', 'Cancelled'],
  EXPIRED: ['muted', 'Expired'],
};

export function HoldStatus({ status }) {
  const [tone, label] = HOLD[status] || ['muted', status];
  return <Badge tone={tone}>{label}</Badge>;
}

const COPY = {
  AVAILABLE: ['accent', 'On the shelf'],
  ON_LOAN: ['outline', 'On loan'],
  ON_HOLD_SHELF: ['warn', 'Hold shelf'],
  DAMAGED: ['late', 'Damaged'],
  LOST: ['late', 'Lost'],
  WITHDRAWN: ['muted', 'Withdrawn'],
};

export const COPY_STATUS_LABELS = Object.fromEntries(Object.entries(COPY).map(([k, [, label]]) => [k, label]));

export function CopyStatus({ status }) {
  const [tone, label] = COPY[status] || ['muted', status];
  return <Badge tone={tone}>{label}</Badge>;
}

export function AccountStatus({ status }) {
  return status === 'SUSPENDED' ? <Badge tone="late">Suspended</Badge> : <Badge tone="accent">Active</Badge>;
}
