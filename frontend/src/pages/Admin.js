import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './Admin.css';

const Admin = () => {
  const [users, setUsers] = useState([]);
  const [stats, setStats] = useState({ totalUsers: 0, activeSubscriptions: 0 });
  const [loading, setLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState(null);
  const [showUserModal, setShowUserModal] = useState(false);
  const [showCreateAdminModal, setShowCreateAdminModal] = useState(false);
  const [page, setPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [newAdminData, setNewAdminData] = useState({
    name: '',
    email: '',
    password: '',
  });

  useEffect(() => {
    loadData();
  }, [page]);

  const loadData = async () => {
    try {
      const [usersRes, statsRes] = await Promise.all([
        api.get(`/admin/users?page=${page}&size=20`),
        api.get('/admin/stats'),
      ]);
      setUsers(usersRes.data);
      setStats(statsRes.data);
    } catch (error) {
      toast.error('Erro ao carregar dados');
    } finally {
      setLoading(false);
    }
  };

  const handleViewUser = (user) => {
    setSelectedUser(user);
    setShowUserModal(true);
  };

  const handleUpdateSubscription = async (userId, plan) => {
    try {
      await api.put(`/admin/users/${userId}/subscription?plan=${plan}`);
      toast.success('Assinatura atualizada com sucesso!');
      loadData();
      setShowUserModal(false);
    } catch (error) {
      toast.error('Erro ao atualizar assinatura');
    }
  };

  const handleDeactivateSubscription = async (userId) => {
    if (window.confirm('Tem certeza que deseja desativar a assinatura deste usuário?')) {
      try {
        await api.put(`/admin/users/${userId}/subscription/deactivate`);
        toast.success('Assinatura desativada com sucesso!');
        loadData();
        setShowUserModal(false);
      } catch (error) {
        toast.error('Erro ao desativar assinatura');
      }
    }
  };

  const handleExtendSubscription = async (userId, days) => {
    const daysInput = prompt(`Quantos dias deseja adicionar? (atualmente: ${days || 0})`);
    if (daysInput) {
      try {
        await api.put(`/admin/users/${userId}/subscription/extend?days=${parseInt(daysInput)}`);
        toast.success('Assinatura estendida com sucesso!');
        loadData();
        setShowUserModal(false);
      } catch (error) {
        toast.error('Erro ao estender assinatura');
      }
    }
  };

  const handleDeleteUser = async (userId) => {
    if (window.confirm('Tem certeza que deseja excluir este usuário? Esta ação não pode ser desfeita!')) {
      try {
        await api.delete(`/admin/users/${userId}`);
        toast.success('Usuário excluído com sucesso!');
        loadData();
        setShowUserModal(false);
      } catch (error) {
        toast.error('Erro ao excluir usuário');
      }
    }
  };

  const handleCreateAdmin = async (e) => {
    e.preventDefault();
    try {
      await api.post('/admin/create-admin', newAdminData);
      toast.success('Administrador criado com sucesso!');
      setShowCreateAdminModal(false);
      setNewAdminData({ name: '', email: '', password: '' });
      loadData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao criar administrador');
    }
  };

  const filteredUsers = users.filter(user =>
    user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const plans = ['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'];

  if (loading) {
    return <div className="loading">Carregando...</div>;
  }

  return (
    <div className="admin-page">
      <div className="admin-header">
        <div>
          <h1>Painel Administrativo</h1>
          <p>Gerencie usuários e assinaturas do sistema</p>
        </div>
        <button className="btn-primary" onClick={() => setShowCreateAdminModal(true)}>
          + Novo Administrador
        </button>
      </div>

      <div className="admin-stats">
        <div className="stat-card">
          <div className="stat-icon">👥</div>
          <div className="stat-content">
            <p className="stat-label">Total de Usuários</p>
            <h2 className="stat-value">{stats.totalUsers}</h2>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">✅</div>
          <div className="stat-content">
            <p className="stat-label">Assinaturas Ativas</p>
            <h2 className="stat-value">{stats.activeSubscriptions}</h2>
          </div>
        </div>
      </div>

      <div className="admin-content">
        <div className="search-bar">
          <input
            type="text"
            placeholder="Buscar usuário por nome ou email..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        <div className="users-table-container">
          <table className="users-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nome</th>
                <th>Email</th>
                <th>Plano</th>
                <th>Status</th>
                <th>Válido até</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan="8" className="empty-state">
                    Nenhum usuário encontrado
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{user.name}</td>
                    <td>{user.email}</td>
                    <td>
                      <span className={`role-badge ${user.role === 'ADMIN' ? 'admin' : 'user'}`}>
                        {user.role || 'USER'}
                      </span>
                    </td>
                    <td>
                      {user.role === 'ADMIN' ? (
                        <span className="plan-badge" style={{ color: '#999' }}>-</span>
                      ) : (
                        <span className="plan-badge">{user.subscription?.plan || 'N/A'}</span>
                      )}
                    </td>
                    <td>
                      {user.role === 'ADMIN' ? (
                        <span className="status-badge" style={{ color: '#999' }}>-</span>
                      ) : (
                        <span className={`status-badge ${user.subscription?.isActive ? 'active' : 'inactive'}`}>
                          {user.subscription?.isActive ? 'Ativa' : 'Inativa'}
                        </span>
                      )}
                    </td>
                    <td>
                      {user.role === 'ADMIN' ? (
                        <span style={{ color: '#999' }}>-</span>
                      ) : (
                        user.subscription?.endDate
                          ? format(new Date(user.subscription.endDate), 'dd/MM/yyyy', { locale: ptBR })
                          : 'N/A'
                      )}
                    </td>
                    <td>
                      <button onClick={() => handleViewUser(user)} className="btn-view">
                        👁️ Ver
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showUserModal && selectedUser && (
        <div className="modal-overlay" onClick={() => setShowUserModal(false)}>
          <div className="modal-content admin-modal" onClick={(e) => e.stopPropagation()}>
            <h2>Gerenciar Usuário</h2>
            <div className="modal-scrollable-content">
              <div className="user-details">
                <div className="detail-item">
                  <label>Nome:</label>
                  <p>{selectedUser.name}</p>
                </div>
                <div className="detail-item">
                  <label>Email:</label>
                  <p>{selectedUser.email}</p>
                </div>
                <div className="detail-item">
                  <label>Plano Atual:</label>
                  <p>
                    {selectedUser.role === 'ADMIN' ? (
                      <span style={{ color: '#999', fontStyle: 'italic' }}>-</span>
                    ) : (
                      selectedUser.subscription?.plan || 'N/A'
                    )}
                  </p>
                </div>
                <div className="detail-item">
                  <label>Status:</label>
                  <p>
                    {selectedUser.role === 'ADMIN' ? (
                      <span style={{ color: '#999', fontStyle: 'italic' }}>-</span>
                    ) : (
                      <span className={`status-badge ${selectedUser.subscription?.isActive ? 'active' : 'inactive'}`}>
                        {selectedUser.subscription?.isActive ? 'Ativa' : 'Inativa'}
                      </span>
                    )}
                  </p>
                </div>
                <div className="detail-item">
                  <label>Válido até:</label>
                  <p>
                    {selectedUser.role === 'ADMIN' ? (
                      <span style={{ color: '#999', fontStyle: 'italic' }}>-</span>
                    ) : (
                      selectedUser.subscription?.endDate
                        ? format(new Date(selectedUser.subscription.endDate), 'dd/MM/yyyy HH:mm', { locale: ptBR })
                        : 'N/A'
                    )}
                  </p>
                </div>
              </div>

              {selectedUser.role !== 'ADMIN' && (
                <div className="admin-actions">
                  <h3>Alterar Plano:</h3>
                  <div className="plan-buttons">
                    {plans.map((plan) => (
                      <button
                        key={plan}
                        onClick={() => handleUpdateSubscription(selectedUser.id, plan)}
                        className="btn-plan"
                        disabled={selectedUser.subscription?.plan === plan}
                      >
                        {plan}
                      </button>
                    ))}
                  </div>

                  <h3>Outras Ações:</h3>
                  <div className="action-buttons">
                    <button
                      onClick={() => handleExtendSubscription(selectedUser.id, 30)}
                      className="btn-extend"
                    >
                      ➕ Estender 30 dias
                    </button>
                    {selectedUser.subscription?.isActive && (
                      <button
                        onClick={() => handleDeactivateSubscription(selectedUser.id)}
                        className="btn-deactivate"
                      >
                        ⏸️ Desativar Assinatura
                      </button>
                    )}
                    <button
                      onClick={() => handleDeleteUser(selectedUser.id)}
                      className="btn-delete"
                    >
                      🗑️ Excluir Usuário
                    </button>
                  </div>
                </div>
              )}
              
              {selectedUser.role === 'ADMIN' && (
                <div className="admin-actions">
                  <div className="action-buttons">
                    <button
                      onClick={() => handleDeleteUser(selectedUser.id)}
                      className="btn-delete"
                    >
                      🗑️ Excluir Usuário
                    </button>
                  </div>
                  <p style={{ color: '#666', marginTop: '16px', fontStyle: 'italic' }}>
                    Administradores não têm assinatura/plano
                  </p>
                </div>
              )}
            </div>

            <button onClick={() => setShowUserModal(false)} className="btn-close">
              Fechar
            </button>
          </div>
        </div>
      )}

      {showCreateAdminModal && (
        <div className="modal-overlay" onClick={() => { setShowCreateAdminModal(false); setNewAdminData({ name: '', email: '', password: '' }); }}>
          <div className="modal-content admin-modal" onClick={(e) => e.stopPropagation()}>
            <h2>Novo Administrador</h2>
            <form onSubmit={handleCreateAdmin} style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
              <div className="modal-scrollable-content">
                <div className="form-group">
                  <label>Nome</label>
                  <input
                    type="text"
                    value={newAdminData.name}
                    onChange={(e) => setNewAdminData({ ...newAdminData, name: e.target.value })}
                    required
                    placeholder="Nome do administrador"
                  />
                </div>
                <div className="form-group">
                  <label>Email</label>
                  <input
                    type="email"
                    value={newAdminData.email}
                    onChange={(e) => setNewAdminData({ ...newAdminData, email: e.target.value })}
                    required
                    placeholder="email@exemplo.com"
                  />
                </div>
                <div className="form-group">
                  <label>Senha</label>
                  <input
                    type="password"
                    value={newAdminData.password}
                    onChange={(e) => setNewAdminData({ ...newAdminData, password: e.target.value })}
                    required
                    placeholder="Senha (mínimo 6 caracteres)"
                    minLength={6}
                  />
                  <small>O administrador terá acesso apenas ao painel administrativo</small>
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowCreateAdminModal(false); setNewAdminData({ name: '', email: '', password: '' }); }}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary">
                  Criar Administrador
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Admin;

