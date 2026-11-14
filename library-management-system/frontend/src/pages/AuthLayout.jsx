import Logo from '../components/Logo';

// Real titles from the catalogue, drawn as a line elevation of one bay in the stacks.
// Width and height in px. One slot is being retrieved, one is empty: the book is out on loan.
const SPINES = [
  ['Clean Code', 32, 168],
  ['Effective Java', 28, 156],
  ['Sapiens', 36, 176],
  ['Design Patterns', 28, 148],
  ['Introduction to Algorithms', 40, 184, 'retrieving'],
  ['The Pragmatic Programmer', 32, 160],
  [null, 28, 152, 'empty'],
  ['Atomic Habits', 28, 152],
  ['Spring in Action', 32, 164],
  ['The Alchemist', 24, 136],
  ['Operating System Concepts', 36, 180],
  ['Wings of Fire', 28, 144],
];

// A faint row further back in the bay, for depth. Heights only, no titles.
const BACK_ROW = [120, 132, 112, 140, 124, 116, 136, 128, 108, 132, 120, 140, 116, 128, 124, 136, 112, 132];

function Stacks() {
  return (
    <div className="stacks" aria-hidden="true">
      <div className="stacks-back">
        {BACK_ROW.map((h, i) => <div key={i} className="spine-back" style={{ height: h, '--i': i }} />)}
      </div>
      <div className="stacks-books">
        {SPINES.map(([title, w, h, state], i) => (
          <div key={i} className={`spine ${state ? `spine-${state}` : ''}`} style={{ width: w, height: h, '--i': i }}>
            {title && <span>{title}</span>}
          </div>
        ))}
      </div>
      <div className="stacks-rail" />
    </div>
  );
}

export default function AuthLayout({ title, subtitle, children }) {
  return (
    <div className="auth-page">
      <aside className="auth-hero">
        <div className="auth-hero-brand">
          <Logo size={20} />
          <span>Shelfwise</span>
        </div>
        <div className="auth-hero-copy">
          <h1>Every book in the library, one search away</h1>
          <p>Browse the catalogue, borrow in one click and see when each book is due back</p>
          <ul className="auth-hero-points">
            <li>Live availability for every title</li>
            <li>Due dates and late fines worked out for you</li>
            <li>Librarian tools for the whole collection</li>
          </ul>
        </div>
        <Stacks />
      </aside>
      <section className="auth-panel">
        <div className="auth-card">
          <h2>{title}</h2>
          <p className="auth-subtitle">{subtitle}</p>
          {children}
        </div>
      </section>
    </div>
  );
}
