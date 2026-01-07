import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './AutomationRules.css';

const AutomationRules = () => {
  const [rules, setRules] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    conditionType: 'DESCRIPTION_CONTAINS',
    conditionValue: '',
    actionType: 'AUTO_CATEGORIZE',
    actionValue: '',
    isActive: true,
    priority: 0,
  });

  const conditionTypes = [
    { value: 'DESCRIPTION_CONTAINS', label: 'Descrição contém', placeholder: 'Ex: Supermercado' },
    { value: 'AMOUNT_RANGE', label: 'Valor entre', placeholder: 'Ex: 100:500 ou 100: ou :500' },
    { value: 'MERCHANT', label: 'Estabelecimento', placeholder: 'Ex: Amazon' },
  ];

  const actionTypes = [
    { value: 'AUTO_CATEGORIZE', label: 'Categorizar automaticamente' },
    { value: 'AUTO_APPROVE', label: 'Marcar como pago automaticamente' },
  ];

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [rulesRes, categoriesRes] = await Promise.all([
        api.get('/automation-rules'),
        api.get('/categories'),
      ]);
      setRules(rulesRes.data);
      setCategories(categoriesRes.data);
    } catch (error) {
      toast.error('Erro ao carregar regras');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...formData,
        priority: parseInt(formData.priority) || 0,
      };

      if (editingRule) {
        await api.put(`/automation-rules/${editingRule.id}`, payload);
        toast.success('Regra atualizada com sucesso!');
      } else {
        await api.post('/automation-rules', payload);
        toast.success('Regra criada com sucesso!');
      }

      setShowModal(false);
      resetForm();
      loadData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao salvar regra');
    }
  };

  const handleEdit = (rule) => {
    setEditingRule(rule);
    setFormData({
      name: rule.name,
      description: rule.description,
      conditionType: rule.conditionType,
      conditionValue: rule.conditionValue,
      actionType: rule.actionType,
      actionValue: rule.actionValue,
      isActive: rule.isActive,
      priority: rule.priority?.toString() || '0',
    });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Tem certeza que deseja excluir esta regra?')) {
      return;
    }

    try {
      await api.delete(`/automation-rules/${id}`);
      toast.success('Regra excluída com sucesso!');
      loadData();
    } catch (error) {
      toast.error('Erro ao excluir regra');
    }
  };

  const resetForm = () => {
    setFormData({
      name: '',
      description: '',
      conditionType: 'DESCRIPTION_CONTAINS',
      conditionValue: '',
      actionType: 'AUTO_CATEGORIZE',
      actionValue: '',
      isActive: true,
      priority: 0,
    });
    setEditingRule(null);
  };

  const getConditionTypeLabel = (type) => {
    return conditionTypes.find(t => t.value === type)?.label || type;
  };

  const getActionTypeLabel = (type) => {
    return actionTypes.find(t => t.value === type)?.label || type;
  };

  if (loading) {
    return <div className="automation-rules-page">Carregando...</div>;
  }

  return (
    <div className="automation-rules-page">
      <div className="page-header">
        <div>
          <h1>🤖 Regras de Automação</h1>
          <p>Crie regras para categorizar e processar transações automaticamente</p>
        </div>
        <button className="btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
          + Nova Regra
        </button>
      </div>

      <div className="rules-grid">
        {rules.length === 0 ? (
          <div className="empty-state">
            <p>Você ainda não tem regras de automação.</p>
            <p>Crie regras para automatizar a categorização e processamento de suas transações!</p>
          </div>
        ) : (
          rules.map((rule) => (
            <div key={rule.id} className={`rule-card ${!rule.isActive ? 'inactive' : ''}`}>
              <div className="rule-header">
                <div>
                  <h3>{rule.name}</h3>
                  <span className="rule-priority">Prioridade: {rule.priority}</span>
                </div>
                <span className={`rule-status ${rule.isActive ? 'active' : 'inactive'}`}>
                  {rule.isActive ? '✅ Ativa' : '⏸️ Inativa'}
                </span>
              </div>
              <div className="rule-description">{rule.description}</div>
              <div className="rule-details">
                <div className="rule-detail-item">
                  <span className="detail-label">Condição:</span>
                  <span className="detail-value">
                    {getConditionTypeLabel(rule.conditionType)} = "{rule.conditionValue}"
                  </span>
                </div>
                <div className="rule-detail-item">
                  <span className="detail-label">Ação:</span>
                  <span className="detail-value">
                    {getActionTypeLabel(rule.actionType)}
                    {rule.actionType === 'AUTO_CATEGORIZE' && rule.actionValue && (
                      <span> → Categoria ID: {rule.actionValue}</span>
                    )}
                  </span>
                </div>
                {rule.executionCount > 0 && (
                  <div className="rule-detail-item">
                    <span className="detail-label">Execuções:</span>
                    <span className="detail-value">{rule.executionCount}x</span>
                  </div>
                )}
                {rule.lastExecution && (
                  <div className="rule-detail-item">
                    <span className="detail-label">Última execução:</span>
                    <span className="detail-value">
                      {format(new Date(rule.lastExecution), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
                    </span>
                  </div>
                )}
              </div>
              <div className="rule-actions">
                <button className="btn-edit" onClick={() => handleEdit(rule)}>
                  ✏️ Editar
                </button>
                <button className="btn-delete" onClick={() => handleDelete(rule.id)}>
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
            <h2>{editingRule ? 'Editar Regra' : 'Nova Regra'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Nome da Regra *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                  placeholder="Ex: Categorizar Supermercado"
                />
              </div>
              <div className="form-group">
                <label>Descrição</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows="2"
                  placeholder="Descreva o que esta regra faz..."
                />
              </div>
              <div className="form-group">
                <label>Condição *</label>
                <select
                  value={formData.conditionType}
                  onChange={(e) => setFormData({ ...formData, conditionType: e.target.value })}
                  required
                >
                  {conditionTypes.map(type => (
                    <option key={type.value} value={type.value}>
                      {type.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Valor da Condição *</label>
                <input
                  type="text"
                  value={formData.conditionValue}
                  onChange={(e) => setFormData({ ...formData, conditionValue: e.target.value })}
                  required
                  placeholder={conditionTypes.find(t => t.value === formData.conditionType)?.placeholder}
                />
                {formData.conditionType === 'AMOUNT_RANGE' && (
                  <small>Formato: min:max (ex: 100:500) ou min: (ex: 100:) ou :max (ex: :500)</small>
                )}
              </div>
              <div className="form-group">
                <label>Ação *</label>
                <select
                  value={formData.actionType}
                  onChange={(e) => setFormData({ ...formData, actionType: e.target.value, actionValue: '' })}
                  required
                >
                  {actionTypes.map(type => (
                    <option key={type.value} value={type.value}>
                      {type.label}
                    </option>
                  ))}
                </select>
              </div>
              {formData.actionType === 'AUTO_CATEGORIZE' && (
                <div className="form-group">
                  <label>Categoria *</label>
                  <select
                    value={formData.actionValue}
                    onChange={(e) => setFormData({ ...formData, actionValue: e.target.value })}
                    required
                  >
                    <option value="">Selecione uma categoria...</option>
                    {categories.map(cat => (
                      <option key={cat.id} value={cat.id}>
                        {cat.icon} {cat.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
              <div className="form-row">
                <div className="form-group">
                  <label>Prioridade</label>
                  <input
                    type="number"
                    min="0"
                    value={formData.priority}
                    onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
                    placeholder="0"
                  />
                  <small>Maior prioridade = executa primeiro</small>
                </div>
                <div className="form-group">
                  <label>
                    <input
                      type="checkbox"
                      checked={formData.isActive}
                      onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                    />
                    {' '}Ativa
                  </label>
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowModal(false); resetForm(); }}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary">
                  {editingRule ? 'Atualizar' : 'Criar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AutomationRules;




