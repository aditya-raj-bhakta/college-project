// Three spines on a shelf, one of them leaning. Flat, one colour.
export default function Logo({ size = 20, color = 'var(--text-1)' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <g fill={color}>
        <rect x="6" y="6" width="5" height="20" rx="1" />
        <rect x="13" y="9" width="5" height="17" rx="1" />
        <rect x="21" y="6" width="5" height="20" rx="1" transform="rotate(10 23.5 26)" />
      </g>
    </svg>
  );
}
