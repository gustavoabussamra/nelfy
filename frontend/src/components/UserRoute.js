import React, { useEffect } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Rota que bloqueia acesso de usuários ADMIN (apenas para usuários normais)
const UserRoute = ({ children }) => {
  const { user, loading } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!loading && user) {
      const userRole = (user.role || '').toUpperCase().trim();
      if (userRole === 'ADMIN') {
        console.log('>>> UserRoute: ADMIN detectado, REDIRECIONANDO para /admin');
        navigate('/admin', { replace: true });
      }
    }
  }, [user, loading, navigate]);

  if (loading) {
    return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>Carregando...</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Se for ADMIN, redireciona para /admin
  const userRole = (user.role || '').toUpperCase().trim();
  if (userRole === 'ADMIN') {
    return null; // Aguardar redirecionamento do useEffect
  }

  return children;
};

export default UserRoute;

