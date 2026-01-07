import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './Goals.css';

const Goals = () => {
  const [goals, setGoals] = useState([]);
  const [categories, setCategories] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingGoal, setEditingGoal] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    targetAmount: '',
    currentAmount: '0',
    targetDate: '',
    categoryId: '',
    description: '',
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [goalsRes, categoriesRes] = await Promise.all([
        api.get('/goals'),
        api.get('/categories'),
      ]);
      setGoals(goalsRes.data);
      setCategories(categoriesRes.data);
    } catch (error) {
      toast.error('Erro ao carregar metas');
    } finally {
      setLoading(false);
    }
  };

  const loadSuggestions = async () => {
    try {
      const response = await api.get('/goals/suggestions');
      setSuggestions(response.data);
      setShowSuggestions(true);
    } catch (error) {
      toast.error('Erro ao carregar sugestões');
    }
  };

  const applySuggestion = (suggestion) => {
    setFormData({
      name: suggestion.name,
      targetAmount: suggestion.suggestedAmount.toString(),
      currentAmount: '0',
      targetDate: suggestion.suggestedTargetDate,
      categoryId: suggestion.suggestedCategoryId?.toString() || '',
      description: suggestion.description,
    });
    setShowSuggestions(false);
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...formData,
        targetAmount: parseFloat(formData.targetAmount),
        currentAmount: parseFloat(formData.currentAmount) || 0,
        categoryId: formData.categoryId ? parseInt(formData.categoryId) : null,
      };

      if (editingGoal) {
        await api.put(`/goals/${editingGoal.id}`, payload);
        toast.success('Meta atualizada com sucesso!');
      } else {
        await api.post('/goals', payload);
        toast.success('Meta criada com sucesso!');
      }

      setShowModal(false);
      resetForm();
      loadData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao salvar meta');
    }
  };

  const handleEdit = (goal) => {
    setEditingGoal(goal);
    setFormData({
      name: goal.name,
      targetAmount: goal.targetAmount.toString(),
      currentAmount: goal.currentAmount.toString(),
      targetDate: goal.targetDate ? format(new Date(goal.targetDate), 'yyyy-MM-dd') : '',
      categoryId: goal.categoryId?.toString() || '',
      description: goal.description || '',
    });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Tem certeza que deseja excluir esta meta?')) {
      try {
        await api.delete(`/goals/${id}`);
        toast.success('Meta excluída com sucesso!');
        loadData();
      } catch (error) {
        toast.error('Erro ao excluir meta');
      }
    }
  };

  const handleAddProgress = async (goalId) => {
    const amountStr = prompt('Quanto você deseja adicionar ao progresso desta meta?');
    if (amountStr && !isNaN(parseFloat(amountStr))) {
      try {
        await api.put(`/goals/${goalId}/progress?amount=${parseFloat(amountStr)}`);
        toast.success('Progresso atualizado com sucesso!');
        loadData();
      } catch (error) {
        toast.error('Erro ao atualizar progresso');
      }
    }
  };

  const resetForm = () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 30);
    setFormData({
      name: '',
      targetAmount: '',
      currentAmount: '0',
      targetDate: format(tomorrow, 'yyyy-MM-dd'),
      categoryId: '',
      description: '',
    });
    setEditingGoal(null);
  };

  const getProgressColor = (percentage) => {
    if (percentage >= 100) return '#10b981'; // Verde quando completa
    if (percentage >= 75) return '#3b82f6'; // Azul quando próximo
    if (percentage >= 50) return '#f59e0b'; // Laranja no meio
    return '#ef4444'; // Vermelho no início
  };

  const getDaysRemaining = (targetDate) => {
    if (!targetDate) return null;
    const today = new Date();
    const target = new Date(targetDate);
    const diffTime = target - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  if (loading) {
    return <div className="loading">Carregando...</div>;
  }

  const activeGoals = goals.filter(g => !g.isCompleted);
  const completedGoals = goals.filter(g => g.isCompleted);

  return (
    <div className="goals-page">
      <div className="page-header">
        <div>
          <h1>🎯 Metas Financeiras</h1>
        </div>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <button className="btn-secondary" onClick={loadSuggestions}>
            💡 Sugestões Inteligentes
          </button>
          <button className="btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
            + Nova Meta
          </button>
        </div>
      </div>

      {showSuggestions && suggestions.length > 0 && (
        <div className="suggestions-section">
          <div className="suggestions-header">
            <h2>💡 Sugestões de Metas Baseadas no Seu Histórico</h2>
            <button className="btn-close" onClick={() => setShowSuggestions(false)}>×</button>
          </div>
          <div className="suggestions-grid">
            {suggestions.map((suggestion, idx) => (
              <div key={idx} className="suggestion-card">
                <div className="suggestion-header">
                  <h3>{suggestion.name}</h3>
                  <span className="confidence-badge">
                    {Math.round(suggestion.confidence * 100)}% confiança
                  </span>
                </div>
                <p className="suggestion-description">{suggestion.description}</p>
                <div className="suggestion-details">
                  <div className="suggestion-detail">
                    <span className="detail-label">Valor Sugerido:</span>
                    <span className="detail-value">
                      {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(suggestion.suggestedAmount)}
                    </span>
                  </div>
                  <div className="suggestion-detail">
                    <span className="detail-label">Prazo:</span>
                    <span className="detail-value">
                      {format(new Date(suggestion.suggestedTargetDate), 'dd/MM/yyyy', { locale: ptBR })}
                    </span>
                  </div>
                </div>
                <div className="suggestion-reason">
                  <strong>Por quê?</strong> {suggestion.reason}
                </div>
                <button className="btn-apply" onClick={() => applySuggestion(suggestion)}>
                  ✓ Usar Esta Sugestão
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeGoals.length > 0 && (
        <div className="goals-section">
          <h2>Metas Ativas</h2>
          <div className="goals-grid">
            {activeGoals.map((goal) => {
              const percentage = goal.progressPercentage || 0;
              const progressColor = getProgressColor(percentage);
              const daysRemaining = getDaysRemaining(goal.targetDate);
              
              return (
                <div key={goal.id} className={`goal-card ${goal.isCompleted ? 'completed' : ''}`}>
                  <div className="goal-header">
                    <div>
                      <h3>{goal.name}</h3>
                      {goal.category && (
                        <span className="goal-category" style={{ color: goal.category.color }}>
                          {goal.category.icon} {goal.category.name}
                        </span>
                      )}
                    </div>
                    {goal.isCompleted && (
                      <span className="goal-badge completed">✓ Concluída</span>
                    )}
                  </div>

                  {goal.description && (
                    <p className="goal-description">{goal.description}</p>
                  )}

                  <div className="goal-progress">
                    <div className="progress-info">
                      <span className="progress-label">Progresso</span>
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
                      <span>R$ {parseFloat(goal.currentAmount || 0).toFixed(2)}</span>
                      <span>R$ {parseFloat(goal.targetAmount).toFixed(2)}</span>
                    </div>
                  </div>

                  <div className="goal-details">
                    <div className="detail-row">
                      <span>Faltam:</span>
                      <span className="remaining-amount">
                        R$ {parseFloat(goal.targetAmount - goal.currentAmount).toFixed(2)}
                      </span>
                    </div>
                    {daysRemaining !== null && (
                      <div className="detail-row">
                        <span>Prazo:</span>
                        <span className={daysRemaining < 0 ? 'overdue' : ''}>
                          {daysRemaining > 0 
                            ? `${daysRemaining} dias restantes`
                            : daysRemaining === 0
                            ? 'Vence hoje!'
                            : `${Math.abs(daysRemaining)} dias atrasado`
                          }
                        </span>
                      </div>
                    )}
                    {goal.targetDate && (
                      <div className="detail-row">
                        <span>Data limite:</span>
                        <span>{format(new Date(goal.targetDate), 'dd/MM/yyyy', { locale: ptBR })}</span>
                      </div>
                    )}
                  </div>

                  <div className="goal-actions">
                    <button 
                      onClick={() => handleAddProgress(goal.id)} 
                      className="btn-add-progress"
                      disabled={goal.isCompleted}
                    >
                      + Adicionar
                    </button>
                    <button onClick={() => handleEdit(goal)} className="btn-edit">
                      ✏️ Editar
                    </button>
                    <button onClick={() => handleDelete(goal.id)} className="btn-delete">
                      🗑️ Excluir
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {completedGoals.length > 0 && (
        <div className="goals-section">
          <h2>Metas Concluídas</h2>
          <div className="goals-grid">
            {completedGoals.map((goal) => {
              return (
                <div key={goal.id} className="goal-card completed">
                  <div className="goal-header">
                    <div>
                      <h3>{goal.name}</h3>
                      {goal.category && (
                        <span className="goal-category" style={{ color: goal.category.color }}>
                          {goal.category.icon} {goal.category.name}
                        </span>
                      )}
                    </div>
                    <span className="goal-badge completed">✓ Concluída</span>
                  </div>
                  <div className="goal-progress">
                    <div className="progress-info">
                      <span className="progress-label">Progresso</span>
                      <span className="progress-percentage">100%</span>
                    </div>
                    <div className="progress-bar-container">
                      <div
                        className="progress-bar"
                        style={{
                          width: '100%',
                          backgroundColor: '#10b981',
                        }}
                      />
                    </div>
                    <div className="progress-amounts">
                      <span>R$ {parseFloat(goal.currentAmount || 0).toFixed(2)}</span>
                      <span>R$ {parseFloat(goal.targetAmount).toFixed(2)}</span>
                    </div>
                  </div>
                  {goal.completedDate && (
                    <div className="goal-details">
                      <div className="detail-row">
                        <span>Concluída em:</span>
                        <span>{format(new Date(goal.completedDate), 'dd/MM/yyyy', { locale: ptBR })}</span>
                      </div>
                    </div>
                  )}
                  <div className="goal-actions">
                    <button onClick={() => handleDelete(goal.id)} className="btn-delete">
                      🗑️ Excluir
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {goals.length === 0 && (
        <div className="empty-state">
          <div className="empty-icon">🎯</div>
          <p>Nenhuma meta criada ainda</p>
          <p>Crie uma meta para começar a economizar!</p>
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={() => { setShowModal(false); resetForm(); }}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>{editingGoal ? 'Editar Meta' : 'Nova Meta'}</h2>
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="modal-scrollable-content">
                <div className="form-group">
                  <label>Nome da Meta *</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    required
                    placeholder="Ex: Viagem para Europa"
                  />
                </div>
                <div className="form-group">
                  <label>Valor Alvo (R$) *</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={formData.targetAmount}
                    onChange={(e) => setFormData({ ...formData, targetAmount: e.target.value })}
                    required
                    placeholder="5000.00"
                  />
                </div>
                <div className="form-group">
                  <label>Valor Atual (R$)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={formData.currentAmount}
                    onChange={(e) => setFormData({ ...formData, currentAmount: e.target.value })}
                    placeholder="0.00"
                  />
                  <small>Quanto você já economizou para esta meta</small>
                </div>
                <div className="form-group">
                  <label>Data Limite *</label>
                  <input
                    type="date"
                    value={formData.targetDate}
                    onChange={(e) => setFormData({ ...formData, targetDate: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Categoria (opcional)</label>
                  <select
                    value={formData.categoryId}
                    onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                  >
                    <option value="">Sem categoria</option>
                    {categories.map((cat) => (
                      <option key={cat.id} value={cat.id}>
                        {cat.icon} {cat.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Descrição (opcional)</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    rows="3"
                    placeholder="Descreva sua meta..."
                  />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowModal(false); resetForm(); }}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary">
                  {editingGoal ? 'Atualizar' : 'Criar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Goals;

