import { meApi } from '../api/services';
import { useAuth } from '../context/AuthContext';
import useAsync from '../hooks/useAsync';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import StatCard from '../components/StatCard';
import FineLedger from '../components/FineLedger';
import { formatRupees } from '../utils/format';

export default function MyFinesPage() {
  const { user } = useAuth();
  const { data: fines, error } = useAsync(() => meApi.fines(), []);
  if (error) return <div className="alert alert-error">{error}</div>;
  if (!fines) return <Spinner />;

  const owed = fines.reduce((s, f) => s + f.balance, 0);
  const paid = fines.reduce((s, f) => s + f.paid, 0);
  const waived = fines.reduce((s, f) => s + f.waived, 0);
  const limit = user.memberType?.fineBlockThreshold ?? 0;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Fines</h1>
          <p className="page-meta">Pay at the circulation desk. Owing more than {formatRupees(limit)} pauses borrowing, renewals and holds.</p>
        </div>
      </div>

      <div className="stats">
        <StatCard label="Owed now" value={owed} format={formatRupees} tone={owed > limit ? 'late' : 'default'} hint={owed > limit ? 'over your limit' : `limit ${formatRupees(limit)}`} />
        <StatCard label="Paid" value={paid} format={formatRupees} hint="all time" />
        <StatCard label="Waived" value={waived} format={formatRupees} hint="all time" />
      </div>

      <section className="card">
        {fines.length === 0 ? (
          <EmptyState title="No fines" icon="receipt">Books returned on time never cost anything.</EmptyState>
        ) : (
          <FineLedger fines={fines} />
        )}
      </section>
    </>
  );
}
