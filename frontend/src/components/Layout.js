import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationBell from './NotificationBell';
import NelfyLogo from './NelfyLogo';
import './Layout.css';

const Layout = ({ children }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  // Verificar se é admin de forma mais robusta
  const userRole = (user?.role || '').toUpperCase().trim();
  const isAdmin = userRole === 'ADMIN';
  
  console.log('Layout - User role:', userRole);
  console.log('Layout - Is Admin:', isAdmin);
  console.log('Layout - User completo:', user);

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="header-top">
            <NelfyLogo size="small" />
            {!isAdmin && <NotificationBell />}
          </div>
        </div>
        <nav className="sidebar-nav">
          {isAdmin ? (
            // Menu apenas para ADMIN - apenas Admin e Perfil
            <>
              <Link to="/admin" className={`nav-item ${isActive('/admin') ? 'active' : ''}`}>
                <span className="nav-icon">⚙️</span>
                <span>Admin</span>
              </Link>
              <Link to="/admin/withdrawals" className={`nav-item ${isActive('/admin/withdrawals') ? 'active' : ''}`}>
                <span className="nav-icon">💵</span>
                <span>Saques</span>
              </Link>
              <Link to="/profile" className={`nav-item ${isActive('/profile') ? 'active' : ''}`}>
                <span className="nav-icon">👤</span>
                <span>Perfil</span>
              </Link>
            </>
          ) : (
            // Menu para usuários normais - Dashboard, Transações, Categorias, Metas, Perfil
            <>
              <Link to="/" className={`nav-item ${isActive('/') ? 'active' : ''}`}>
                <span className="nav-icon">📊</span>
                <span>Dashboard</span>
              </Link>
              <Link to="/transactions" className={`nav-item ${isActive('/transactions') ? 'active' : ''}`}>
                <span className="nav-icon">💸</span>
                <span>Transações</span>
              </Link>
              <Link to="/categories" className={`nav-item ${isActive('/categories') ? 'active' : ''}`}>
                <span className="nav-icon">📁</span>
                <span>Categorias</span>
              </Link>
              <Link to="/budgets" className={`nav-item ${isActive('/budgets') ? 'active' : ''}`}>
                <span className="nav-icon">💰</span>
                <span>Orçamentos</span>
              </Link>
              <Link to="/goals" className={`nav-item ${isActive('/goals') ? 'active' : ''}`}>
                <span className="nav-icon">🎯</span>
                <span>Metas</span>
              </Link>
              <Link to="/reports" className={`nav-item ${isActive('/reports') ? 'active' : ''}`}>
                <span className="nav-icon">📈</span>
                <span>Relatórios</span>
              </Link>
              <Link to="/installments" className={`nav-item ${isActive('/installments') ? 'active' : ''}`}>
                <span className="nav-icon">💳</span>
                <span>Parceladas</span>
              </Link>
              <Link to="/recurring" className={`nav-item ${isActive('/recurring') ? 'active' : ''}`}>
                <span className="nav-icon">🔄</span>
                <span>Recorrências</span>
              </Link>
              <Link to="/accounts" className={`nav-item ${isActive('/accounts') ? 'active' : ''}`}>
                <span className="nav-icon">💳</span>
                <span>Contas</span>
              </Link>
              <Link to="/referrals" className={`nav-item ${isActive('/referrals') ? 'active' : ''}`}>
                <span className="nav-icon">🎁</span>
                <span>Afiliados</span>
              </Link>
              <Link to="/automation" className={`nav-item ${isActive('/automation') ? 'active' : ''}`}>
                <span className="nav-icon">🤖</span>
                <span>Automação</span>
              </Link>
              <Link to="/profile" className={`nav-item ${isActive('/profile') ? 'active' : ''}`}>
                <span className="nav-icon">👤</span>
                <span>Perfil</span>
              </Link>
            </>
          )}
        </nav>
        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">{user?.name?.charAt(0).toUpperCase()}</div>
            <div className="user-details">
              <div className="user-name">{user?.name}</div>
              <div className="user-email">{user?.email}</div>
            </div>
          </div>
          <button onClick={handleLogout} className="logout-btn">
            Sair
          </button>
        </div>
      </aside>
      <main className="main-content">
        {children}
      </main>
    </div>
  );
};

export default Layout;

