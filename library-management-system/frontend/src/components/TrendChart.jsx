import { useEffect, useRef, useState } from 'react';

/**
 * Daily checkouts and returns as paired columns on one axis.
 * Colours validated for the dark card surface with the dataviz palette validator (all checks pass).
 * Returns are drawn outlined as well as coloured, so the two never rely on colour alone.
 */
export const SERIES = [
  { key: 'checkouts', label: 'Checkouts', color: '#2a9f81', outlined: false },
  { key: 'returns', label: 'Returns', color: '#7f8fd6', outlined: true },
];

const HEIGHT = 280;
const PAD = { top: 16, right: 8, bottom: 28, left: 36 };
const GAP = 2; // surface gap between the two columns of a day

function niceMax(v) {
  if (v <= 4) return 4;
  const step = Math.pow(10, Math.floor(Math.log10(v)));
  const n = Math.ceil(v / step) * step;
  return n % 4 === 0 ? n : Math.ceil(n / 4) * 4;
}

/** A column with a 4px rounded top, anchored square to the baseline. */
function columnPath(x, y, w, h) {
  if (h <= 0) return '';
  const r = Math.min(4, w / 2, h);
  return `M${x},${y + h} V${y + r} Q${x},${y} ${x + r},${y} H${x + w - r} Q${x + w},${y} ${x + w},${y + r} V${y + h} Z`;
}

/** Which days get a date label: evenly spaced, and the last day always, without crowding the one before it. */
function labelledDays(count, every) {
  const last = count - 1;
  const picked = [];
  for (let i = 0; i < count; i += every) picked.push(i);
  if (picked[picked.length - 1] !== last) {
    if (last - picked[picked.length - 1] < every * 0.6) picked.pop();
    picked.push(last);
  }
  return new Set(picked);
}

export default function TrendChart({ days }) {
  const wrap = useRef(null);
  const [width, setWidth] = useState(720);
  const [hover, setHover] = useState(null);

  useEffect(() => {
    const el = wrap.current;
    if (!el) return undefined;
    const ro = new ResizeObserver(([entry]) => setWidth(Math.max(320, entry.contentRect.width)));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const max = niceMax(Math.max(1, ...days.flatMap((d) => [d.checkouts, d.returns])));
  const innerW = width - PAD.left - PAD.right;
  const innerH = HEIGHT - PAD.top - PAD.bottom;
  const slot = innerW / days.length;
  const colW = Math.max(2, Math.min(14, (slot - GAP) * 0.36));
  const y = (v) => PAD.top + innerH - (v / max) * innerH;
  const ticks = [0, max / 4, max / 2, (3 * max) / 4, max];
  const labels = labelledDays(days.length, Math.max(1, Math.ceil(64 / slot)));
  const fmt = (iso) => new Date(`${iso}T00:00`).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
  const center = (i) => PAD.left + slot * i + slot / 2;

  const onMove = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const i = Math.floor((e.clientX - rect.left - PAD.left) / slot);
    setHover(i >= 0 && i < days.length ? i : null);
  };

  const h = hover != null ? days[hover] : null;

  return (
    <div className="trend" ref={wrap}>
      <svg width={width} height={HEIGHT} role="img" aria-label="Daily checkouts and returns"
        onMouseMove={onMove} onMouseLeave={() => setHover(null)}>
        {ticks.map((t) => (
          <g key={t}>
            <line x1={PAD.left} x2={width - PAD.right} y1={y(t)} y2={y(t)} className="trend-grid" />
            <text x={PAD.left - 8} y={y(t)} className="trend-axis" textAnchor="end" dominantBaseline="middle">{Math.round(t)}</text>
          </g>
        ))}

        {hover != null && (
          <rect x={PAD.left + slot * hover} y={PAD.top} width={slot} height={innerH} className="trend-hover" />
        )}

        {days.map((d, i) => {
          const left = center(i) - colW - GAP / 2;
          return (
            <g key={d.date} className="trend-day" style={{ '--i': Math.min(i, 40) }}>
              {SERIES.map((s, si) => {
                const v = d[s.key];
                const top = y(v);
                const x = left + si * (colW + GAP);
                const path = columnPath(x, top, colW, PAD.top + innerH - top);
                if (!path) return null;
                return s.outlined
                  ? <path key={s.key} d={path} fill={s.color} fillOpacity="0.28" stroke={s.color} strokeWidth="1.5" />
                  : <path key={s.key} d={path} fill={s.color} />;
              })}
            </g>
          );
        })}

        {days.map((d, i) => labels.has(i) && (
          <text key={d.date} x={center(i)} y={HEIGHT - 8} className="trend-axis" textAnchor="middle">{fmt(d.date)}</text>
        ))}
      </svg>

      {h && (
        <div className="trend-tip" style={{ left: Math.min(Math.max(center(hover) - 74, 0), width - 156), top: PAD.top }}>
          <span className="trend-tip-date">{fmt(h.date)}</span>
          {SERIES.map((s) => (
            <span key={s.key} className="trend-tip-row">
              <span className={`trend-swatch ${s.outlined ? 'is-outlined' : ''}`} style={{ '--c': s.color }} />
              {s.label}
              <span className="trend-tip-val">{h[s.key]}</span>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
