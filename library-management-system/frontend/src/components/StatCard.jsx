import CountUp from './CountUp';

// One cell of the stat strip: a label, a figure and what it is measured against.
// Numeric values count up; pass `format` to render them, e.g. as rupees.
export default function StatCard({ label, value, format, hint, tone = 'default' }) {
  return (
    <div className={`stat stat-${tone}`}>
      <span className="stat-label">{label}</span>
      <span className="stat-value">
        {typeof value === 'number' ? <CountUp value={value} format={format} /> : value}
      </span>
      {hint && <span className="stat-hint">{hint}</span>}
    </div>
  );
}
