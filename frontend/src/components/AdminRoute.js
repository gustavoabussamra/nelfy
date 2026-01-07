import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const AdminRoute = ({ children }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>Carregando...</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  const userRole = (user.role || '').toUpperCase().trim();
  console.log('AdminRoute: Verificando acesso. Role:', userRole);
  
  if (userRole !== 'ADMIN') {
    console.log('>>> AdminRoute: Usuário não é ADMIN, redirecionando para /');
    window.location.href = '/';
    return null;
  }

  console.log('AdminRoute: Acesso permitido para ADMIN');
  return children;
};

export default AdminRoute;

