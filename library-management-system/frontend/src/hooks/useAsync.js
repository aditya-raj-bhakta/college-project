import { useCallback, useEffect, useRef, useState } from 'react';
import { getErrorMessage } from '../api/client';

/**
 * Loads data for a page. Re-runs when `deps` change; `reload` runs it again on demand.
 * Responses that arrive after a newer request started are ignored.
 */
export default function useAsync(load, deps = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const latest = useRef(0);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(load, deps);

  const reload = useCallback(() => {
    const id = ++latest.current;
    setLoading(true);
    return run()
      .then((result) => {
        if (id === latest.current) {
          setData(result);
          setError('');
        }
        return result;
      })
      .catch((err) => {
        if (id === latest.current) setError(getErrorMessage(err));
      })
      .finally(() => {
        if (id === latest.current) setLoading(false);
      });
  }, [run]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, error, loading, reload, setData };
}
