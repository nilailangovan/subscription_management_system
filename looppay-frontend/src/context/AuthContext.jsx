import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/axios';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  // Initialize auth state from localStorage on load
  useEffect(() => {
    try {
      const savedToken = localStorage.getItem('looppay_token');
      const savedUser = localStorage.getItem('looppay_user');

      if (savedToken && savedUser) {
        setToken(savedToken);
        setUser(JSON.parse(savedUser));
      }
    } catch (err) {
      console.error('Failed to parse saved user:', err);
      localStorage.removeItem('looppay_token');
      localStorage.removeItem('looppay_user');
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Logs in the user with backend auth response data.
   * Expects data: { token, tokenType, userId, email, fullName, role }
   */
  const login = (authData) => {
    const userData = {
      userId: authData.userId,
      email: authData.email,
      fullName: authData.fullName,
      role: authData.role,
    };

    localStorage.setItem('looppay_token', authData.token);
    localStorage.setItem('looppay_user', JSON.stringify(userData));

    setToken(authData.token);
    setUser(userData);
  };

  /**
   * Clears session and logs out.
   */
  const logout = () => {
    localStorage.removeItem('looppay_token');
    localStorage.removeItem('looppay_user');
    setToken(null);
    setUser(null);
  };

  const value = {
    user,
    role: user?.role || null,
    token,
    isAuthenticated: !!token && !!user,
    loading,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

