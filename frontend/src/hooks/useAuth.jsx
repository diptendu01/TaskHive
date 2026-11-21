import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';

const AuthContext = createContext(null);
const TOKEN_STORAGE_KEY = 'taskhive_token';

function decodeJwtPayload(jwtToken) {
  if (!jwtToken || typeof jwtToken !== 'string') {
    return null;
  }
  const parts = jwtToken.split('.');
  if (parts.length !== 3) {
    return null;
  }
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const binary = atob(padded);
    const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
    const json = new TextDecoder().decode(bytes);
    const payload = JSON.parse(json);

    if (payload.exp && Date.now() >= payload.exp * 1000) {
      return null;
    }
    return payload;
  } catch (err) {
    console.error('Failed to decode JWT token', err);
    return null;
  }
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [isReady, setIsReady] = useState(false);

  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  }, []);

  const login = useCallback(
    (jwtToken) => {
      const payload = decodeJwtPayload(jwtToken);
      if (!payload) {
        logout();
        return false;
      }
      setToken(jwtToken);
      localStorage.setItem(TOKEN_STORAGE_KEY, jwtToken);
      setUser({
        id: payload.sub,
        name: payload.name,
        email: payload.email,
        role: payload.role,
      });
      return true;
    },
    [logout]
  );

  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (storedToken) {
      const payload = decodeJwtPayload(storedToken);
      if (payload) {
        setToken(storedToken);
        setUser({
          id: payload.sub,
          name: payload.name,
          email: payload.email,
          role: payload.role,
        });
      } else {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
      }
    }
    setIsReady(true);
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, isReady, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};