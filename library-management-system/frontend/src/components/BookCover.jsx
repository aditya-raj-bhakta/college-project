// A generated cloth-bound cover, so the catalogue looks good without image uploads.
// Each pair is a bookcloth colour and the foil stamped on it.
export const CLOTHS = [
  ['#24483a', '#d9c08a'],
  ['#6b2a2a', '#ead7bd'],
  ['#263a5c', '#c7d3e6'],
  ['#4b3550', '#e3cfe0'],
  ['#7a5420', '#f2dfb6'],
  ['#2c5257', '#cfe2dd'],
  ['#33312d', '#e0b65a'],
];

export function clothFor(title) {
  let h = 0;
  for (let i = 0; i < title.length; i++) h = (h * 31 + title.charCodeAt(i)) >>> 0;
  return CLOTHS[h % CLOTHS.length];
}

/** size: "lg" for the catalogue and details, "md" for loan cards */
export default function BookCover({ title, author, size = 'lg' }) {
  const [cloth, foil] = clothFor(title);
  return (
    <div className={`cover cover-${size}`} style={{ '--cloth': cloth, '--foil': foil }}>
      <div className="cover-text">
        <span className="cover-title">{title}</span>
        {size === 'lg' && <span className="cover-author">{author}</span>}
      </div>
    </div>
  );
}
