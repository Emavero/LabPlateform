import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { authApi } from '../api/authApi';

const TOKEN_KEY = 'lab_platform_token';
const USER_KEY = 'lab_platform_user';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  });

  const persistSession = useCallback((authResponse) => {
    localStorage.setItem(TOKEN_KEY, authResponse.token);
    const sessionUser = { email: authResponse.email, role: authResponse.role };
    localStorage.setItem(USER_KEY, JSON.stringify(sessionUser));
    setUser(sessionUser);
  }, []);

  const login = useCallback(
    async (email, password) => {
      const response = await authApi.login({ email, password });
      persistSession(response);
      return response;
    },
    [persistSession]
  );

  const register = useCallback(
    async (email, password, confirmPassword) => {
      const response = await authApi.register({ email, password, confirmPassword });
      persistSession(response);
      return response;
    },
    [persistSession]
  );

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      login,
      register,
      logout,
    }),
    [user, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth doit être utilisé à l\'intérieur de AuthProvider');
  }
  return ctx;
}
