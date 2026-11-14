import { useState } from 'react';
import { reportsApi } from '../api/services';
import { getErrorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import useAsync from '../hooks/useAsync';
import Spinner from '../components/Spinner';
import StatCard from '../components/StatCard';
import Icon from '../components/Icon';
import TrendChart, { SERIES } from '../components/TrendChart';
import { formatDate, formatRupees, isoDate } from '../utils/format';

function preset(name) {
  const today = new Date();
  const from = new Date(today);
  if (name === '7') from.setDate(today.getDate() - 6);
  else if (name === '30') from.setDate(today.getDate() - 29);
  else if (name === '90') from.setDate(today.getDate() - 89);
  else if (name === 'month') from.setDate(1);
  return { from: isoDate(from), to: isoDate(today) };
}

const PRESETS = [
  { value: '7', label: '7 days' },
  { value: '30', label: '30 days' },
  { value: '90', label: '90 days' },
  { value: 'month', label: 'This month' },
];

export default function ReportsPage() {
  const toast = useToast();
  const [range, setRange] = useState(() => ({ ...preset('30'), preset: '30' }));
  const { data, error, loading } = useAsync(() => reportsApi.summary(range.from, range.to), [range.from, range.to]);
  const [downloading, setDownloading] = useState(null);
  const [showTable, setShowTable] = useState(false);

  const download = async (kind) => {
    setDownloading(kind);
    try {
      if (kind === 'loans') await reportsApi.download('loans.csv', { from: range.from, to: range.to }, `loans-${range.from}-to-${range.to}.csv`);
      if (kind === 'fines') await reportsApi.download('fines.csv', { from: range.from, to: range.to }, `fines-${range.from}-to-${range.to}.csv`);
      if (kind === 'overdue') await reportsApi.download('overdue.csv', {}, `overdue-${range.to}.csv`);
    } catch (err) {
      toast(getErrorMessage(err, 'Could not download the report'), 'error');
    } finally {
      setDownloading(null);
    }
  };

  const topMax = data ? Math.max(1, ...data.topTitles.map((t) => t.checkouts)) : 1;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Reports</h1>
          <p className="page-meta">{formatDate(range.from)} to {formatDate(range.to)}</p>
        </div>
      </div>

      <div className="toolbar">
        <div className="segmented" role="tablist">
          {PRESETS.map((p) => (
            <button key={p.value} role="tab" aria-selected={range.preset === p.value} className={range.preset === p.value ? 'active' : ''}
              onClick={() => setRange({ ...preset(p.value), preset: p.value })}>
              {p.label}
            </button>
          ))}
        </div>
        <div className="date-range">
          <input type="date" value={range.from} max={range.to} onChange={(e) => setRange((r) => ({ ...r, from: e.target.value, preset: null }))} aria-label="From" />
          <span className="cell-sub">to</span>
          <input type="date" value={range.to} min={range.from} onChange={(e) => setRange((r) => ({ ...r, to: e.target.value, preset: null }))} aria-label="To" />
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {!data ? <Spinner /> : (
        <div className={loading ? 'is-loading' : ''}>
          <div className="stats">
            <StatCard label="Checkouts" value={data.checkouts} hint={`${data.returns} returns`} />
            <StatCard label="New members" value={data.newMembers} />
            <StatCard label="Fines collected" value={data.finesCollected} format={formatRupees}
              hint={`${formatRupees(data.finesAssessed)} charged, ${formatRupees(data.finesWaived)} waived`} />
            <StatCard label="Overdue now" value={data.overdueNow} tone={data.overdueNow ? 'late' : 'default'} hint="as of today" />
          </div>

          <div className="report-grid">
            <section className="card">
              <div className="card-header">
                <h2>Circulation per day</h2>
                <div className="legend-inline">
                  {SERIES.map((s) => (
                    <span key={s.key}><span className={`trend-swatch ${s.outlined ? 'is-outlined' : ''}`} style={{ '--c': s.color }} />{s.label}</span>
                  ))}
                  <button className="card-link" onClick={() => setShowTable((v) => !v)}>{showTable ? 'Chart' : 'Table'}</button>
                </div>
              </div>
              {showTable ? (
                <div className="table-wrap table-scroll">
                  <table className="table">
                    <thead><tr><th>Date</th><th className="num">Checkouts</th><th className="num">Returns</th></tr></thead>
                    <tbody>
                      {data.daily.map((d) => (
                        <tr key={d.date}><td className="cell-date">{formatDate(d.date)}</td><td className="num">{d.checkouts}</td><td className="num">{d.returns}</td></tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <TrendChart days={data.daily} />
              )}
            </section>

            <section className="card">
              <div className="card-header"><h2>Most borrowed</h2></div>
              {data.topTitles.length === 0 ? <p className="card-note">Nothing was borrowed in this period.</p> : (
                <ol className="ranked">
                  {data.topTitles.map((t, i) => (
                    <li key={t.bookId}>
                      <div className="ranked-head">
                        <span className="ranked-title">{t.title}</span>
                        <span className="ranked-value">{t.checkouts}</span>
                      </div>
                      <div className="ranked-bar"><span className={i === 0 ? 'is-lead' : ''} style={{ width: `${(t.checkouts / topMax) * 100}%` }} /></div>
                    </li>
                  ))}
                </ol>
              )}
            </section>
          </div>

          <section className="card">
            <div className="card-header"><h2>Download</h2><span className="card-meta">CSV, opens in any spreadsheet</span></div>
            <div className="downloads">
              <button className="btn btn-secondary" disabled={!!downloading} onClick={() => download('loans')}><Icon name="download" />{downloading === 'loans' ? 'Preparing…' : 'Loans in this period'}</button>
              <button className="btn btn-secondary" disabled={!!downloading} onClick={() => download('fines')}><Icon name="download" />{downloading === 'fines' ? 'Preparing…' : 'Fines in this period'}</button>
              <button className="btn btn-secondary" disabled={!!downloading} onClick={() => download('overdue')}><Icon name="download" />{downloading === 'overdue' ? 'Preparing…' : 'Overdue right now'}</button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}
