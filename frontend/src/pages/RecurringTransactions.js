import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './RecurringTransactions.css';

const RecurringTransactions = () => {
  const [recurring, setRecurring] = useState([]);
  const [categories, setCategories] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingRecurring, setEditingRecurring] = useState(null);
  const [formData, setFormData] = useState({
    description: '',
    amount: '',
    type: 'EXPENSE',
    categoryId: '',
    accountId: '',
    recurrenceType: 'MONTHLY',
    recurrenceDay: null,
    startDate: '',
    endDate: '',
    autoCreate: false,
  });

  const recurrenceTypes = [
    { value: 'DAILY', label: 'Diário' },
    { value: 'WEEKLY', label: 'Semanal' },
    { value: 'MONTHLY', label: 'Mensal' },
    { value: 'YEARLY', label: 'Anual' },
  ];

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [recurringRes, categoriesRes, accountsRes] = await Promise.all([
        api.get('/recurring-transactions'),
        api.get('/categories'),
        api.get('/accounts'),
      ]);
      setRecurring(recurringRes.data);
      setCategories(categoriesRes.data);
      setAccounts(accountsRes.data);
    } catch (error) {
      toast.error('Erro ao carregar recorrências');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...formData,
        amount: parseFloat(formData.amount),
        categoryId: formData.categoryId ? parseInt(formData.categoryId) : null,
        accountId: formData.accountId ? parseInt(formData.accountId) : null,
        recurrenceDay: formData.recurrenceDay ? parseInt(formData.recurrenceDay) : null,
        endDate: formData.endDate || null,
      };

      if (editingRecurring) {
        await api.put(`/recurring-transactions/${editingRecurring.id}`, payload);
        toast.success('Recorrência atualizada com sucesso!');
      } else {
        await api.post('/recurring-transactions', payload);
        toast.success('Recorrência criada com sucesso!');
      }

      setShowModal(false);
      resetForm();
      loadData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao salvar recorrência');
    }
  };

  const handleEdit = (item) => {
    setEditingRecurring(item);
    setFormData({
      description: item.description,
      amount: item.amount.toString(),
      type: item.type,
      categoryId: item.categoryId || '',
      accountId: item.accountId || '',
      recurrenceType: item.recurrenceType,
      recurrenceDay: item.recurrenceDay?.toString() || '',
      startDate: item.startDate,
      endDate: item.endDate || '',
      autoCreate: item.autoCreate || false,
    });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Tem certeza que deseja excluir esta recorrência?')) {
      return;
    }

    try {
      await api.delete(`/recurring-transactions/${id}`);
      toast.success('Recorrência excluída com sucesso!');
      loadData();
    } catch (error) {
      toast.error('Erro ao excluir recorrência');
    }
  };

  const resetForm = () => {
    setFormData({
      description: '',
      amount: '',
      type: 'EXPENSE',
      categoryId: '',
      accountId: '',
      recurrenceType: 'MONTHLY',
      recurrenceDay: null,
      startDate: '',
      endDate: '',
      autoCreate: false,
    });
    setEditingRecurring(null);
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  const getRecurrenceLabel = (type, day) => {
    const typeLabel = recurrenceTypes.find(t => t.value === type)?.label || type;
    if (day) {
      if (type === 'WEEKLY') {
        const days = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
        return `${typeLabel} - ${days[day - 1]}`;
      }
      return `${typeLabel} - Dia ${day}`;
    }
    return typeLabel;
  };

  if (loading) {
    return <div className="recurring-page">Carregando...</div>;
  }

  return (
    <div className="recurring-page">
      <div className="page-header">
        <h1>🔄 Transações Recorrentes</h1>
        <button className="btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
          + Nova Recorrência
        </button>
      </div>

      <div className="recurring-grid">
        {recurring.length === 0 ? (
          <div className="empty-state">
            <p>Você ainda não tem recorrências cadastradas.</p>
            <p>Crie recorrências para automatizar suas transações!</p>
          </div>
        ) : (
          recurring.map((item) => (
            <div key={item.id} className="recurring-card">
              <div className="recurring-header">
                <div className="recurring-icon">
                  {item.type === 'INCOME' ? '💰' : '💸'}
                </div>
                <div className="recurring-info">
                  <h3>{item.description}</h3>
                  <span className="recurring-amount">
                    {item.type === 'INCOME' ? '+' : '-'} {formatCurrency(item.amount)}
                  </span>
                </div>
              </div>
              <div className="recurring-details">
                <div className="detail-item">
                  <span className="detail-label">Frequência:</span>
                  <span className="detail-value">
                    {getRecurrenceLabel(item.recurrenceType, item.recurrenceDay)}
                  </span>
                </div>
                {item.nextOccurrenceDate && (
                  <div className="detail-item">
                    <span className="detail-label">Próxima ocorrência:</span>
                    <span className="detail-value">
                      {format(new Date(item.nextOccurrenceDate), "dd/MM/yyyy", { locale: ptBR })}
                    </span>
                  </div>
                )}
                {item.createdCount > 0 && (
                  <div className="detail-item">
                    <span className="detail-label">Transações criadas:</span>
                    <span className="detail-value">{item.createdCount}</span>
                  </div>
                )}
                {item.autoCreate && (
                  <span className="auto-badge">🤖 Automático</span>
                )}
              </div>
              <div className="recurring-actions">
                <button className="btn-edit" onClick={() => handleEdit(item)}>
                  ✏️ Editar
                </button>
                <button className="btn-delete" onClick={() => handleDelete(item.id)}>
                  🗑️ Excluir
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={() => { setShowModal(false); resetForm(); }}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>{editingRecurring ? 'Editar Recorrência' : 'Nova Recorrência'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Descrição *</label>
                <input
                  type="text"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  required
                  placeholder="Ex: Aluguel"
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Valor *</label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.amount}
                    onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                    required
                    placeholder="0.00"
                  />
                </div>
                <div className="form-group">
                  <label>Tipo *</label>
                  <select
                    value={formData.type}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                    required
                  >
                    <option value="EXPENSE">Despesa</option>
                    <option value="INCOME">Receita</option>
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Categoria</label>
                  <select
                    value={formData.categoryId}
                    onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                  >
                    <option value="">Selecione...</option>
                    {categories
                      .filter(c => c.type === formData.type)
                      .map(cat => (
                        <option key={cat.id} value={cat.id}>
                          {cat.icon} {cat.name}
                        </option>
                      ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Conta</label>
                  <select
                    value={formData.accountId}
                    onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
                  >
                    <option value="">Selecione...</option>
                    {accounts.map(acc => (
                      <option key={acc.id} value={acc.id}>
                        {acc.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Frequência *</label>
                  <select
                    value={formData.recurrenceType}
                    onChange={(e) => setFormData({ ...formData, recurrenceType: e.target.value })}
                    required
                  >
                    {recurrenceTypes.map(type => (
                      <option key={type.value} value={type.value}>
                        {type.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Dia (opcional)</label>
                  <input
                    type="number"
                    min="1"
                    max={formData.recurrenceType === 'WEEKLY' ? '7' : '31'}
                    value={formData.recurrenceDay || ''}
                    onChange={(e) => setFormData({ ...formData, recurrenceDay: e.target.value || null })}
                    placeholder={formData.recurrenceType === 'WEEKLY' ? '1-7' : '1-31'}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Data Inicial *</label>
                  <input
                    type="date"
                    value={formData.startDate}
                    onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Data Final (opcional)</label>
                  <input
                    type="date"
                    value={formData.endDate}
                    onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.autoCreate}
                    onChange={(e) => setFormData({ ...formData, autoCreate: e.target.checked })}
                  />
                  <span>Criar transações automaticamente</span>
                </label>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowModal(false); resetForm(); }}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary">
                  {editingRecurring ? 'Atualizar' : 'Criar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default RecurringTransactions;




