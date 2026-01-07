import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './Accounts.css';

const Accounts = () => {
  const [accounts, setAccounts] = useState([]);
  const [totalBalance, setTotalBalance] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingAccount, setEditingAccount] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    type: 'CHECKING_ACCOUNT',
    balance: '0',
    bankName: '',
    accountNumber: '',
    agency: '',
    description: '',
  });

  const accountTypes = [
    { value: 'CHECKING_ACCOUNT', label: 'Conta Corrente', icon: '🏦' },
    { value: 'SAVINGS_ACCOUNT', label: 'Poupança', icon: '💰' },
    { value: 'CREDIT_CARD', label: 'Cartão de Crédito', icon: '💳' },
    { value: 'DIGITAL_WALLET', label: 'Carteira Digital', icon: '📱' },
    { value: 'INVESTMENT', label: 'Investimento', icon: '📈' },
    { value: 'CASH', label: 'Dinheiro', icon: '💵' },
    { value: 'OTHER', label: 'Outro', icon: '📦' },
  ];

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [accountsRes, balanceRes] = await Promise.all([
        api.get('/accounts'),
        api.get('/accounts/total-balance'),
      ]);
      setAccounts(accountsRes.data);
      setTotalBalance(balanceRes.data.totalBalance);
    } catch (error) {
      toast.error('Erro ao carregar contas');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...formData,
        balance: parseFloat(formData.balance) || 0,
      };

      if (editingAccount) {
        await api.put(`/accounts/${editingAccount.id}`, payload);
        toast.success('Conta atualizada com sucesso!');
      } else {
        await api.post('/accounts', payload);
        toast.success('Conta criada com sucesso!');
      }

      setShowModal(false);
      resetForm();
      loadData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao salvar conta');
    }
  };

  const handleEdit = (account) => {
    setEditingAccount(account);
    setFormData({
      name: account.name,
      type: account.type,
      balance: account.balance.toString(),
      bankName: account.bankName || '',
      accountNumber: account.accountNumber || '',
      agency: account.agency || '',
      description: account.description || '',
    });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Tem certeza que deseja excluir esta conta?')) {
      return;
    }

    try {
      await api.delete(`/accounts/${id}`);
      toast.success('Conta excluída com sucesso!');
      loadData();
    } catch (error) {
      toast.error('Erro ao excluir conta');
    }
  };

  const resetForm = () => {
    setFormData({
      name: '',
      type: 'CHECKING_ACCOUNT',
      balance: '0',
      bankName: '',
      accountNumber: '',
      agency: '',
      description: '',
    });
    setEditingAccount(null);
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  const getAccountTypeInfo = (type) => {
    return accountTypes.find(t => t.value === type) || accountTypes[0];
  };

  if (loading) {
    return <div className="accounts-page">Carregando...</div>;
  }

  return (
    <div className="accounts-page">
      <div className="page-header">
        <h1>💳 Contas e Carteiras</h1>
        <button className="btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
          + Nova Conta
        </button>
      </div>

      <div className="total-balance-card">
        <div className="balance-label">Saldo Total Consolidado</div>
        <div className="balance-value">{formatCurrency(totalBalance)}</div>
      </div>

      <div className="accounts-grid">
        {accounts.length === 0 ? (
          <div className="empty-state">
            <p>Você ainda não tem contas cadastradas.</p>
            <p>Adicione suas contas para ter uma visão consolidada das suas finanças!</p>
          </div>
        ) : (
          accounts.map((account) => {
            const typeInfo = getAccountTypeInfo(account.type);
            return (
              <div key={account.id} className="account-card">
                <div className="account-header">
                  <div className="account-icon">{typeInfo.icon}</div>
                  <div className="account-info">
                    <h3>{account.name}</h3>
                    <span className="account-type">{typeInfo.label}</span>
                  </div>
                </div>
                <div className="account-balance">
                  <span className="balance-label">Saldo:</span>
                  <span className="balance-value">{formatCurrency(account.balance)}</span>
                </div>
                {account.bankName && (
                  <div className="account-detail">
                    <span className="detail-label">Banco:</span>
                    <span className="detail-value">{account.bankName}</span>
                  </div>
                )}
                {account.accountNumber && (
                  <div className="account-detail">
                    <span className="detail-label">Conta:</span>
                    <span className="detail-value">{account.accountNumber}</span>
                  </div>
                )}
                {account.description && (
                  <div className="account-description">{account.description}</div>
                )}
                <div className="account-actions">
                  <button className="btn-edit" onClick={() => handleEdit(account)}>
                    ✏️ Editar
                  </button>
                  <button className="btn-delete" onClick={() => handleDelete(account.id)}>
                    🗑️ Excluir
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={() => { setShowModal(false); resetForm(); }}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>{editingAccount ? 'Editar Conta' : 'Nova Conta'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Nome da Conta *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                  placeholder="Ex: Conta Corrente BB"
                />
              </div>

              <div className="form-group">
                <label>Tipo de Conta *</label>
                <select
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                  required
                >
                  {accountTypes.map((type) => (
                    <option key={type.value} value={type.value}>
                      {type.icon} {type.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Saldo Inicial</label>
                <input
                  type="number"
                  step="0.01"
                  value={formData.balance}
                  onChange={(e) => setFormData({ ...formData, balance: e.target.value })}
                  placeholder="0.00"
                />
              </div>

              <div className="form-group">
                <label>Nome do Banco</label>
                <input
                  type="text"
                  value={formData.bankName}
                  onChange={(e) => setFormData({ ...formData, bankName: e.target.value })}
                  placeholder="Ex: Banco do Brasil"
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Agência</label>
                  <input
                    type="text"
                    value={formData.agency}
                    onChange={(e) => setFormData({ ...formData, agency: e.target.value })}
                    placeholder="0000"
                  />
                </div>
                <div className="form-group">
                  <label>Número da Conta</label>
                  <input
                    type="text"
                    value={formData.accountNumber}
                    onChange={(e) => setFormData({ ...formData, accountNumber: e.target.value })}
                    placeholder="00000-0"
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Descrição</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows="3"
                  placeholder="Observações sobre esta conta..."
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowModal(false); resetForm(); }}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary">
                  {editingAccount ? 'Atualizar' : 'Criar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Accounts;




