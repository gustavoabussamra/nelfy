import React, { createContext, useState, useEffect, useContext } from 'react';
import api from '../services/api';

const AuthContext = createContext({});

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const userData = localStorage.getItem('user');
    
    if (token && userData) {
      try {
        api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        const parsedUser = JSON.parse(userData);
        
        // Garantir que a role está em uppercase
        if (parsedUser && parsedUser.role) {
          parsedUser.role = parsedUser.role.toUpperCase().trim();
        }
        
        // ADMIN não deve ter subscription - remover se existir
        const userRole = (parsedUser?.role || '').toUpperCase().trim();
        if (userRole === 'ADMIN') {
          console.log('AuthContext INIT: Removendo subscription do ADMIN');
          delete parsedUser.subscription;
          // Salvar novamente sem subscription
          localStorage.setItem('user', JSON.stringify(parsedUser));
        }
        
        console.log('=== AUTHCONTEXT INIT ===');
        console.log('User carregado do localStorage:', parsedUser);
        console.log('Role normalizada:', parsedUser?.role);
        console.log('Subscription:', parsedUser?.subscription);
        
        setUser(parsedUser);
      } catch (error) {
        console.error('Erro ao carregar usuário do localStorage:', error);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    }
    
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    const { token, user } = response.data;
    
    console.log('AuthContext login: Response completa:', response.data);
    console.log('AuthContext login: User recebido do backend:', user);
    console.log('AuthContext login: Role recebida:', user?.role);
    console.log('AuthContext login: Role em uppercase:', user?.role?.toUpperCase());
    
    // Garantir que a role está em uppercase
    if (user && user.role) {
      user.role = user.role.toUpperCase().trim();
    }
    
    // ADMIN não deve ter subscription - remover se existir
    const userRole = (user?.role || '').toUpperCase().trim();
    if (userRole === 'ADMIN') {
      console.log('AuthContext: Removendo subscription do ADMIN');
      delete user.subscription;
    }
    
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    
    setUser(user);
    return { ...response.data, user }; // Garantir que user está no retorno
  };

  const register = async (name, email, password, referralCode = null) => {
    const requestData = { name, email, password };
    if (referralCode) {
      requestData.referralCode = referralCode;
    }
    
    const response = await api.post('/auth/register', requestData);
    const { token, user } = response.data;
    
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    
    setUser(user);
    return response.data;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    delete api.defaults.headers.common['Authorization'];
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, setUser }}>
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

