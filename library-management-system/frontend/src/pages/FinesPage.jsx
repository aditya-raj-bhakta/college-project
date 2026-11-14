import { useMemo, useState } from 'react';
import { finesApi } from '../api/services';
import { useToast } from '../context/ToastContext';
import useAsync from '../hooks/useAsync';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import StatCard from '../components/StatCard';
import Icon from '../components/Icon';
import FineLedger from '../components/FineLedger';
import { PayDialog, WaiveDialog } from '../components/FineDialogs';
import { formatRupees } from '../utils/format';

const FILTERS = [
  { value: 'open', label: 'Owed' },
  { value: 'settled', label: 'Settled' },
  { value: 'all', label: 'All' },
];

export default function FinesPage() {
  const toast = useToast();
  const [status, setStatus] = useState('open');
  const [search, setSearch] = useState('');
  const { data: fines, error, reload } = useAsync(() => finesApi.list(status), [status]);
  const [paying, setPaying] = useState(null);
  const [waiving, setWaiving] = useState(null);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q || !fines) return fines || [];
    return fines.filter((f) => [f.memberName, f.cardNumber, f.description].some((v) => v?.toLowerCase().includes(q)));
  }, [fines, search]);

  const owed = (fines || []).reduce((s, f) => s + f.balance, 0);
  const members = new Set((fines || []).filter((f) => f.balance > 0).map((f) => f.memberId)).size;

  const done = (verb) => (fine) => {
    toast(`${verb}. ${fine.settled ? 'Fine settled.' : `${formatRupees(fine.balance)} still owed.`}`);
    setPaying(null);
    setWaiving(null);
    reload();
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Fines</h1>
          <p className="page-meta">Late returns and lost items. Every payment and waiver is kept with who recorded it.</p>
        </div>
      </div>

      {status === 'open' && fines && (
        <div className="stats">
          <StatCard label="Owed" value={owed} format={formatRupees} hint={`across ${members} member${members === 1 ? '' : 's'}`} />
          <StatCard label="Open charges" value={fines.length} />
        </div>
      )}

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
          <input type="search" placeholder="Member, card or book" value={search} onChange={(e) => setSearch(e.target.value)} aria-label="Filter fines" />
        </label>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="card">
        {!fines ? <Spinner /> : filtered.length === 0 ? (
          <EmptyState title={status === 'open' ? 'Nobody owes anything' : 'No fines to show'} icon="receipt" />
        ) : (
          <FineLedger fines={filtered} showMember onPay={setPaying} onWaive={setWaiving} />
        )}
      </section>

      {paying && <PayDialog fine={paying} onClose={() => setPaying(null)} onDone={done('Payment recorded')} />}
      {waiving && <WaiveDialog fine={waiving} onClose={() => setWaiving(null)} onDone={done('Waiver recorded')} />}
    </>
  );
}
