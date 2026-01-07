import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format, startOfMonth, endOfMonth } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './Budgets.css';

const Budgets = () => {
  const [budgets, setBudgets] = useState([]);
  const [categories, setCategories] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [showForecastModal, setShowForecastModal] = useState(false);
  const [showScenarioModal, setShowScenarioModal] = useState(false);
  const [forecast, setForecast] = useState(null);
  const [scenario, setScenario] = useState(null);
  const [forecastLoading, setForecastLoading] = useState(false);
  const [editingBudget, setEditingBudget] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    limitAmount: '',
    startDate: format(startOfMonth(new Date()), 'yyyy-MM-dd'),
    endDate: format(endOfMonth(new Date()), 'yyyy-MM-dd'),
    categoryId: '',
    alertPercentage: 80,
    isActive: true,
  });

  useEffect(() => {
    loadData();
    checkAlerts();
    // Verificar alertas a cada 30 segundos
    const interval = setInterval(checkAlerts, 30000);
    return () => clearInterval(interval);
  }, []);

  const loadData = async () => {
    try {
      const [budgetsRes, categoriesRes, accountsRes] = await Promise.all([
        api.get('/budgets'),
        api.get('/categories'),
        api.get('/accounts'),
      ]);
      setBudgets(budgetsRes.data);
      setCategories(categoriesRes.data.filter(cat => cat.type === 'EXPENSE'));
      setAccounts(accountsRes.data);
    } catch (error) {
      toast.error('Erro ao carregar dados');
    } finally {
      setLoading(false);
    }
  };

  const checkAlerts = async () => {
    try {
      const response = await api.get('/budgets/alerts');
      setAlerts(response.data);
      
      // Mostrar notificação para cada alerta
      response.data.forEach(budget => {
        toast.warning(
          `⚠️ Atenção! Você atingiu ${budget.percentageUsed?.toFixed(0)}% do orçamento "${budget.name}"`,
          { autoClose: 5000 }
        );
      });
    } catch (error) {
      // Silencioso - não precisa mostrar erro se não houver alertas
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...formData,
        limitAmount: parseFloat(formData.limitAmount),
        category: formData.categoryId ? { id: parseInt(formData.categoryId) } : null,
      };

      if (editingBudget) {
        await api.put(`/budgets/${editingBudget.id}`, payload);
        toast.success('Orçamento atualizado com sucesso!');
      } else {
        await api.post('/budgets', payload);
        toast.success('Orçamento criado com sucesso!');
      }

      setShowModal(false);
      resetForm();
      loadData();
      checkAlerts();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao salvar orçamento');
    }
  };

  const handleEdit = (budget) => {
    setEditingBudget(budget);
    setFormData({
      name: budget.name,
      limitAmount: budget.limitAmount.toString(),
      startDate: format(new Date(budget.startDate), 'yyyy-MM-dd'),
      endDate: format(new Date(budget.endDate), 'yyyy-MM-dd'),
      categoryId: budget.category?.id?.toString() || '',
      alertPercentage: budget.alertPercentage || 80,
      isActive: budget.isActive !== undefined ? budget.isActive : true,
    });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Tem certeza que deseja excluir este orçamento?')) {
      try {
        await api.delete(`/budgets/${id}`);
        toast.success('Orçamento excluído com sucesso!');
        loadData();
      } catch (error) {
        toast.error('Erro ao excluir orçamento');
      }
    }
  };

  const resetForm = () => {
    setFormData({
      name: '',
      limitAmount: '',
      startDate: format(startOfMonth(new Date()), 'yyyy-MM-dd'),
      endDate: format(endOfMonth(new Date()), 'yyyy-MM-dd'),
      categoryId: '',
      alertPercentage: 80,
      isActive: true,
    });
    setEditingBudget(null);
  };

  const getProgressColor = (percentage) => {
    if (percentage >= 100) return 'var(--danger-color)';
    if (percentage >= 80) return 'var(--warning-color)';
    return 'var(--secondary-color)';
  };

  const handleForecast = async () => {
    const startDate = format(startOfMonth(new Date()), 'yyyy-MM-dd');
    const endDate = format(endOfMonth(new Date()), 'yyyy-MM-dd');
    
    setForecastLoading(true);
    try {
      const response = await api.get('/budgets/cash-flow-forecast', {
        params: { startDate, endDate }
      });
      setForecast(response.data);
      setShowForecastModal(true);
    } catch (error) {
      toast.error('Erro ao gerar previsão de fluxo de caixa');
    } finally {
      setForecastLoading(false);
    }
  };

  const handleScenario = async (scenarioData) => {
    setForecastLoading(true);
    try {
      const response = await api.post('/budgets/scenarios', scenarioData);
      setScenario(response.data);
      setShowScenarioModal(true);
    } catch (error) {
      toast.error('Erro ao simular cenário');
    } finally {
      setForecastLoading(false);
    }
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  if (loading) {
    return <div className="loading">Carregando...</div>;
  }

  return (
    <div className="budgets-page">
      <div className="page-header">
        <div>
          <h1>Metas e Orçamentos</h1>
          <p>Defina limites de gastos por categoria e receba alertas</p>
        </div>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <button className="btn-secondary" onClick={handleForecast} disabled={forecastLoading}>
            📊 Fluxo de Caixa
          </button>
          <button className="btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
            + Nova Meta
          </button>
        </div>
      </div>

      {alerts.length > 0 && (
        <div className="alerts-section">
          <h2>⚠️ Alertas Ativos</h2>
          <div className="alerts-list">
            {alerts.map((alert) => (
              <div key={alert.id} className="alert-card danger">
                <div className="alert-icon">⚠️</div>
                <div className="alert-content">
                  <h3>{alert.name}</h3>
                  <p>
                    Você já gastou {alert.percentageUsed?.toFixed(0)}% do limite de R$ {parseFloat(alert.limitAmount).toFixed(2)}
                  </p>
                  <p className="alert-detail">
                    Gasto: R$ {parseFloat(alert.currentSpent).toFixed(2)} / Limite: R$ {parseFloat(alert.limitAmount).toFixed(2)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="budgets-grid">
        {budgets.length === 0 ? (
          <div className="empty-state">
            <p>Nenhum orçamento criado ainda</p>
            <p>Crie uma meta para começar a controlar seus gastos!</p>
          </div>
        ) : (
          budgets.map((budget) => {
            const percentage = parseFloat(budget.percentageUsed) || 0;
            const progressColor = getProgressColor(percentage);
            
            return (
              <div key={budget.id} className={`budget-card ${budget.alertTriggered ? 'alert' : ''}`}>
                <div className="budget-header">
                  <div>
                    <h3>{budget.name}</h3>
                    {budget.category && (
                      <span className="budget-category">
                        {budget.category.icon} {budget.category.name}
                      </span>
                    )}
                  </div>
                  <span className={`budget-status ${budget.isActive ? 'active' : 'inactive'}`}>
                    {budget.isActive ? 'Ativo' : 'Inativo'}
                  </span>
                </div>

                <div className="budget-progress">
                  <div className="progress-info">
                    <span className="progress-label">Gasto / Limite</span>
                    <span className="progress-percentage">{percentage.toFixed(1)}%</span>
                  </div>
                  <div className="progress-bar-container">
                    <div
                      className="progress-bar"
                      style={{
                        width: `${Math.min(percentage, 100)}%`,
                        backgroundColor: progressColor,
                      }}
                    />
                  </div>
                  <div className="progress-amounts">
                    <span>R$ {parseFloat(budget.currentSpent || 0).toFixed(2)}</span>
                    <span>R$ {parseFloat(budget.limitAmount).toFixed(2)}</span>
                  </div>
                </div>

                <div className="budget-details">
                  <div className="detail-row">
                    <span>Período:</span>
                    <span>
                      {format(new Date(budget.startDate), 'dd/MM', { locale: ptBR })} -{' '}
                      {format(new Date(budget.endDate), 'dd/MM/yyyy', { locale: ptBR })}
                    </span>
                  </div>
                  <div className="detail-row">
                    <span>Restante:</span>
                    <span className={`remaining ${parseFloat(budget.remaining) < 0 ? 'negative' : ''}`}>
                      R$ {parseFloat(budget.remaining || 0).toFixed(2)}
                    </span>
                  </div>
                  <div className="detail-row">
                    <span>Alerta em:</span>
                    <span>{budget.alertPercentage}%</span>
                  </div>
                </div>

                <div className="budget-actions">
                  <button onClick={() => handleEdit(budget)} className="btn-edit">
                    ✏️ Editar
                  </button>
                  <button onClick={() => handleDelete(budget.id)} className="btn-delete">
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
            <h2>{editingBudget ? 'Editar Meta' : 'Nova Meta'}</h2>
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="modal-scrollable-content">
                <div className="form-group">
                  <label>Nome da Meta</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    required
                    placeholder="Ex: Alimentação mensal"
                  />
                </div>
                <div className="form-group">
                  <label>Limite (R$)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.limitAmount}
                    onChange={(e) => setFormData({ ...formData, limitAmount: e.target.value })}
                    required
                    placeholder="1000.00"
                  />
                </div>
                <div className="form-group">
                  <label>Categoria (opcional)</label>
                  <select
                    value={formData.categoryId}
                    onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                  >
                    <option value="">Todas as despesas</option>
                    {categories.map((cat) => (
                      <option key={cat.id} value={cat.id}>
                        {cat.icon} {cat.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Data Início</label>
                    <input
                      type="date"
                      value={formData.startDate}
                      onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>Data Fim</label>
                    <input
                      type="date"
                      value={formData.endDate}
                      onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                      required
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label>Alerta em (%)</label>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={formData.alertPercentage}
                    onChange={(e) => setFormData({ ...formData, alertPercentage: parseInt(e.target.value) })}
                    required
                  />
                  <small>Você receberá um alerta quando atingir esta porcentagem do limite</small>
                </div>
                <div className="form-group">
                  <label>
                    <input
                      type="checkbox"
                      checked={formData.isActive}
                      onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                    />
                    {' '}Ativo
                  </label>
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowModal(false); resetForm(); }}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary">
                  {editingBudget ? 'Atualizar' : 'Criar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal de Cenário */}
      {showScenarioModal && (
        <div className="modal-overlay" onClick={() => { setShowScenarioModal(false); setScenario(null); }}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>🎯 Simular Cenário "E Se..."</h2>
            {!scenario ? (
              <ScenarioForm 
                accounts={accounts}
                onSubmit={handleScenario}
                onCancel={() => { setShowScenarioModal(false); setScenario(null); }}
              />
            ) : (
              <div className="scenario-results">
                <h3>Resultados do Cenário: {scenario.name}</h3>
                <div className="forecast-summary">
                  <div className="forecast-item">
                    <span className="forecast-label">Saldo Final:</span>
                    <span className={`forecast-value ${scenario.forecast.endingBalance >= 0 ? 'positive' : 'negative'}`}>
                      {formatCurrency(scenario.forecast.endingBalance)}
                    </span>
                  </div>
                  <div className="forecast-item">
                    <span className="forecast-label">Fluxo Líquido:</span>
                    <span className={`forecast-value ${scenario.forecast.netFlow >= 0 ? 'positive' : 'negative'}`}>
                      {formatCurrency(scenario.forecast.netFlow)}
                    </span>
                  </div>
                </div>
                <div className="modal-actions">
                  <button className="btn-secondary" onClick={() => { setShowScenarioModal(false); setScenario(null); }}>
                    Fechar
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

const ScenarioForm = ({ accounts, onSubmit, onCancel }) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    startDate: format(startOfMonth(new Date()), 'yyyy-MM-dd'),
    endDate: format(endOfMonth(new Date()), 'yyyy-MM-dd'),
    incomeAdjustment: '',
    expenseAdjustment: '',
    accountId: '',
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      ...formData,
      incomeAdjustment: formData.incomeAdjustment ? parseFloat(formData.incomeAdjustment) : null,
      expenseAdjustment: formData.expenseAdjustment ? parseFloat(formData.expenseAdjustment) : null,
      accountId: formData.accountId ? parseInt(formData.accountId) : null,
    });
  };

  return (
    <form onSubmit={handleSubmit}>
      <div className="form-group">
        <label>Nome do Cenário *</label>
        <input
          type="text"
          value={formData.name}
          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          required
          placeholder="Ex: Aumento de 10% nas receitas"
        />
      </div>
      <div className="form-group">
        <label>Descrição</label>
        <textarea
          value={formData.description}
          onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          rows="3"
          placeholder="Descreva o cenário..."
        />
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
          <label>Data Final *</label>
          <input
            type="date"
            value={formData.endDate}
            onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
            required
          />
        </div>
      </div>
      <div className="form-row">
        <div className="form-group">
          <label>Ajuste de Receitas (%)</label>
          <input
            type="number"
            step="0.01"
            value={formData.incomeAdjustment}
            onChange={(e) => setFormData({ ...formData, incomeAdjustment: e.target.value })}
            placeholder="Ex: 10 para +10%"
          />
          <small>Use valores positivos para aumentar, negativos para diminuir</small>
        </div>
        <div className="form-group">
          <label>Ajuste de Despesas (%)</label>
          <input
            type="number"
            step="0.01"
            value={formData.expenseAdjustment}
            onChange={(e) => setFormData({ ...formData, expenseAdjustment: e.target.value })}
            placeholder="Ex: -5 para -5%"
          />
        </div>
      </div>
      <div className="form-group">
        <label>Conta (opcional)</label>
        <select
          value={formData.accountId}
          onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
        >
          <option value="">Todas as contas</option>
          {accounts.map(acc => (
            <option key={acc.id} value={acc.id}>{acc.name}</option>
          ))}
        </select>
      </div>
      <div className="modal-actions">
        <button type="button" className="btn-secondary" onClick={onCancel}>
          Cancelar
        </button>
        <button type="submit" className="btn-primary">
          Simular
        </button>
      </div>
    </form>
  );
};

export default Budgets;

