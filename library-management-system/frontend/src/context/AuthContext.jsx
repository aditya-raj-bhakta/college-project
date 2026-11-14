import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/services';
import { TOKEN_KEY, USER_KEY } from '../api/client';

const AuthContext = createContext(null);

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);
  const [checking, setChecking] = useState(() => !!localStorage.getItem(TOKEN_KEY));

  // On first load, confirm the saved token is still valid
  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) return;
    authApi
      .me()
      .then((me) => {
        setUser(me);
        localStorage.setItem(USER_KEY, JSON.stringify(me));
      })
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        setUser(null);
      })
      .finally(() => setChecking(false));
  }, []);

  const saveSession = useCallback(({ token, user: u }) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(u));
    setUser(u);
  }, []);

  const login = useCallback(async (email, password) => {
    const res = await authApi.login({ email, password });
    saveSession(res);
    return res.user;
  }, [saveSession]);

  const register = useCallback(async (name, email, password) => {
    const res = await authApi.register({ name, email, password });
    saveSession(res);
    return res.user;
  }, [saveSession]);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  /** Re-reads the account, e.g. after a suspension or member type change. */
  const refresh = useCallback(async () => {
    const me = await authApi.me();
    localStorage.setItem(USER_KEY, JSON.stringify(me));
    setUser(me);
    return me;
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAdmin: user?.role === 'ADMIN',
      isStaff: user?.role === 'ADMIN' || user?.role === 'LIBRARIAN',
      checking,
      login,
      register,
      logout,
      refresh,
    }),
    [user, checking, login, register, logout, refresh]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
