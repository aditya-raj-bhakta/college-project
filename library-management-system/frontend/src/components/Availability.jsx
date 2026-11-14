/** "2 of 3 on the shelf", or "All out · 2 waiting" when every copy is taken. */
export default function Availability({ availability }) {
  const { available, total, waitingHolds } = availability;
  const tone = total === 0 ? 'none' : available === 0 ? 'none' : available <= Math.ceil(total / 3) ? 'low' : 'ok';
  const text = total === 0
    ? 'Not in circulation'
    : available > 0
      ? `${available} of ${total} on the shelf`
      : `All out${waitingHolds ? ` · ${waitingHolds} waiting` : ''}`;
  return (
    <span className={`availability availability-${tone}`}>
      <span className="dot" />
      {text}
    </span>
  );
}
