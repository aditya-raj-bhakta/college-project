import { useEffect, useRef, useState } from 'react';

const reduceMotion = () =>
  typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

/** A figure that tweens from its previous value to the new one, like a readout settling. */
export default function CountUp({ value, format = (n) => n.toLocaleString('en-IN'), duration = 700 }) {
  const target = Number(value) || 0;
  const [shown, setShown] = useState(() => (reduceMotion() ? target : 0));
  const from = useRef(reduceMotion() ? target : 0);

  useEffect(() => {
    if (reduceMotion()) {
      setShown(target);
      return undefined;
    }
    const start = performance.now();
    const begin = from.current;
    let frame;
    const tick = (now) => {
      const t = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - t, 4); // ease-out, settles without overshoot
      setShown(Math.round(begin + (target - begin) * eased));
      if (t < 1) frame = requestAnimationFrame(tick);
      else from.current = target;
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [target, duration]);

  return <>{format(shown)}</>;
}
