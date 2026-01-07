import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import './Categories.css';

const Categories = () => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    icon: '📁',
    color: '#6366f1',
    type: 'EXPENSE',
  });

  const icons = ['📁', '🍔', '🚗', '🏠', '💊', '🎓', '🎮', '👕', '💼', '✈️', '💳', '💰', '🛒', '📱', '💡', '🍕', '☕', '🍎', '🏋️', '🎬', '📚', '🎵', '🎨', '⚽', '🏥', '🚌', '⛽', '🛍️', '💇', '🎁', '🍰', '🍺', '🍷', '🌮', '🍜', '🏊', '🚴', '🎯', '🎪', '🎭', '📷', '🎤', '🎸', '🎹', '🎺', '🏄', '⛷️', '🏂', '🎿', '🏌️', '🖼️', '✏️', '📝', '📊', '📈', '📉', '💉', '🩹', '🩺', '🚪', '🛏️', '🛋️', '🚽', '🚿', '🛁', '🧴', '🧹', '🧺', '🧼', '🧽', '🧯', '🏧', '🎲', '🎡', '🎢', '🎠', '🔧', '🛠️', '🧰', '🔬', '📡', '🧪', '⚗️', '🎦', '📶', '💱', '💲', '⚕️', '🔔', '📣', '📢'];
  const colors = ['#6366f1', '#10b981', '#ef4444', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'];

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    try {
      const response = await api.get('/categories');
      setCategories(response.data);
    } catch (error) {
      toast.error('Erro ao carregar categorias');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingCategory) {
        await api.put(`/categories/${editingCategory.id}`, formData);
        toast.success('Categoria atualizada com sucesso!');
      } else {
        await api.post('/categories', formData);
        toast.success('Categoria criada com sucesso!');
      }

      setShowModal(false);
      resetForm();
      loadCategories();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao salvar categoria');
    }
  };

  const handleEdit = (category) => {
    setEditingCategory(category);
    setFormData({
      name: category.name,
      icon: category.icon,
      color: category.color,
      type: category.type,
    });
    setShowModal(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Tem certeza que deseja excluir esta categoria?')) {
      try {
        await api.delete(`/categories/${id}`);
        toast.success('Categoria excluída com sucesso!');
        loadCategories();
      } catch (error) {
        toast.error('Erro ao excluir categoria');
      }
    }
  };

  const resetForm = () => {
    setFormData({
      name: '',
      icon: '📁',
      color: '#6366f1',
      type: 'EXPENSE',
    });
    setEditingCategory(null);
  };

  const incomeCategories = categories.filter((cat) => cat.type === 'INCOME');
  const expenseCategories = categories.filter((cat) => cat.type === 'EXPENSE');

  if (loading) {
    return <div className="loading">Carregando...</div>;
  }

  return (
    <div className="categories-page">
      <div className="page-header">
        <h1>Categorias</h1>
        <button className="btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
          + Nova Categoria
        </button>
      </div>

      <div className="categories-grid">
        <div className="category-section">
          <h2>💰 Receitas</h2>
          <div className="categories-list">
            {incomeCategories.length === 0 ? (
              <p className="empty-state">Nenhuma categoria de receita</p>
            ) : (
              incomeCategories.map((category) => (
                <div key={category.id} className="category-card" style={{ borderLeftColor: category.color }}>
                  <div className="category-header">
                    <span className="category-icon" style={{ backgroundColor: category.color + '20' }}>
                      {category.icon}
                    </span>
                    <div className="category-info">
                      <h3>{category.name}</h3>
                    </div>
                  </div>
                  <div className="category-actions">
                    <button onClick={() => handleEdit(category)} className="btn-edit">✏️</button>
                    <button onClick={() => handleDelete(category.id)} className="btn-delete">🗑️</button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="category-section">
          <h2>💸 Despesas</h2>
          <div className="categories-list">
            {expenseCategories.length === 0 ? (
              <p className="empty-state">Nenhuma categoria de despesa</p>
            ) : (
              expenseCategories.map((category) => (
                <div key={category.id} className="category-card" style={{ borderLeftColor: category.color }}>
                  <div className="category-header">
                    <span className="category-icon" style={{ backgroundColor: category.color + '20' }}>
                      {category.icon}
                    </span>
                    <div className="category-info">
                      <h3>{category.name}</h3>
                    </div>
                  </div>
                  <div className="category-actions">
                    <button onClick={() => handleEdit(category)} className="btn-edit">✏️</button>
                    <button onClick={() => handleDelete(category.id)} className="btn-delete">🗑️</button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={() => { setShowModal(false); resetForm(); }}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>{editingCategory ? 'Editar Categoria' : 'Nova Categoria'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Nome</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label>Tipo</label>
                <select
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                  required
                >
                  <option value="INCOME">Receita</option>
                  <option value="EXPENSE">Despesa</option>
                </select>
              </div>
              <div className="form-group">
                <label>Ícone</label>
                <div className="icon-selector">
                  {icons.map((icon) => (
                    <button
                      key={icon}
                      type="button"
                      className={`icon-option ${formData.icon === icon ? 'selected' : ''}`}
                      onClick={() => setFormData({ ...formData, icon })}
                    >
                      {icon}
                    </button>
                  ))}
                </div>
              </div>
              <div className="form-group">
                <label>Cor</label>
                <div className="color-selector">
                  {colors.map((color) => (
                    <button
                      key={color}
                      type="button"
                      className={`color-option ${formData.color === color ? 'selected' : ''}`}
                      style={{ backgroundColor: color }}
                      onClick={() => setFormData({ ...formData, color })}
                    />
                  ))}
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowModal(false); resetForm(); }}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary">
                  {editingCategory ? 'Atualizar' : 'Criar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Categories;





