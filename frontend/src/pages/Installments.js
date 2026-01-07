import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './Transactions.css';

const Installments = () => {
  const [transactions, setTransactions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState(null);
  const [expandedTransactions, setExpandedTransactions] = useState(new Set());
  const [installmentsMap, setInstallmentsMap] = useState({});
  const [formData, setFormData] = useState({
    description: '',
    amount: '',
    type: 'EXPENSE',
    dueDate: format(new Date(), 'yyyy-MM-dd'),
    categoryId: '',
    isPaid: true,
    totalInstallments: 1,
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [transactionsRes, categoriesRes] = await Promise.all([
        api.get('/transactions/installments'),
        api.get('/categories'),
      ]);
      setTransactions(transactionsRes.data);
      setCategories(categoriesRes.data);
      console.log('Total de transações parceladas carregadas:', transactionsRes.data.length);
      
      // Carregar automaticamente as parcelas de todas as transações parceladas
      if (transactionsRes.data.length > 0) {
        console.log('Carregando parcelas automaticamente para', transactionsRes.data.length, 'transações parceladas...');
        // Carregar parcelas em paralelo para todas as transações parceladas
        const installmentPromises = transactionsRes.data.map(t => loadInstallments(t.id));
        await Promise.all(installmentPromises);
        console.log('Parcelas carregadas automaticamente');
      }
    } catch (error) {
      toast.error('Erro ao carregar dados');
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
        category: formData.categoryId ? { id: parseInt(formData.categoryId) } : null,
        isPaid: formData.isPaid,
        totalInstallments: formData.totalInstallments > 1 ? parseInt(formData.totalInstallments) : null,
        dueDate: formData.dueDate,
        transactionDate: formData.dueDate,
      };

      if (editingTransaction) {
        await api.put(`/transactions/${editingTransaction.id}`, payload);
        toast.success('Transação atualizada com sucesso!');
      } else {
        console.log('Enviando payload para criar transação parcelada:', payload);
        const response = await api.post('/transactions', payload);
        console.log('Resposta da criação:', response.data);
        toast.success('Transação parcelada criada com sucesso!');
      }

      setShowModal(false);
      resetForm();
      loadData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao salvar transação');
    }
  };

  const handleEdit = (transaction) => {
    setEditingTransaction(transaction);
    const effectiveDate = transaction.dueDate || transaction.transactionDate;
    setFormData({
      description: transaction.description,
      amount: transaction.amount.toString(),
      type: transaction.type,
      dueDate: effectiveDate ? format(new Date(effectiveDate), 'yyyy-MM-dd') : format(new Date(), 'yyyy-MM-dd'),
      categoryId: transaction.category?.id?.toString() || '',
      isPaid: transaction.isPaid !== undefined ? transaction.isPaid : true,
      totalInstallments: transaction.totalInstallments || 1,
    });
    setShowModal(true);
  };
  
  const handleMarkAsPaid = async (id) => {
    try {
      await api.put(`/transactions/${id}/mark-paid`);
      toast.success('Transação marcada como paga!');
      await loadData();
      const expandedIds = Array.from(expandedTransactions);
      for (const parentId of expandedIds) {
        if (installmentsMap[parentId]) {
          await loadInstallments(parentId);
        }
      }
    } catch (error) {
      toast.error('Erro ao atualizar status');
    }
  };
  
  const handleMarkAsUnpaid = async (id) => {
    try {
      await api.put(`/transactions/${id}/mark-unpaid`);
      toast.success('Transação marcada como não paga!');
      await loadData();
      const expandedIds = Array.from(expandedTransactions);
      for (const parentId of expandedIds) {
        if (installmentsMap[parentId]) {
          await loadInstallments(parentId);
        }
      }
    } catch (error) {
      toast.error('Erro ao atualizar status');
    }
  };

  const toggleInstallments = async (transactionId) => {
    const newExpanded = new Set(expandedTransactions);
    if (newExpanded.has(transactionId)) {
      newExpanded.delete(transactionId);
    } else {
      newExpanded.add(transactionId);
      if (!installmentsMap[transactionId]) {
        await loadInstallments(transactionId);
      }
    }
    setExpandedTransactions(newExpanded);
  };

  const loadInstallments = async (parentId) => {
    try {
      const response = await api.get(`/transactions/${parentId}/installments`);
      setInstallmentsMap(prev => ({ ...prev, [parentId]: response.data }));
      console.log(`Carregadas ${response.data.length} parcelas para a transação ${parentId}`);
    } catch (error) {
      console.error('Erro ao carregar parcelas:', error);
      toast.error('Erro ao carregar parcelas');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Tem certeza que deseja excluir esta transação parcelada? Todas as parcelas serão excluídas também.')) {
      try {
        await api.delete(`/transactions/${id}`);
        toast.success('Transação excluída com sucesso!');
        loadData();
      } catch (error) {
        toast.error('Erro ao excluir transação');
      }
    }
  };

  const resetForm = () => {
    setFormData({
      description: '',
      amount: '',
      type: 'EXPENSE',
      dueDate: format(new Date(), 'yyyy-MM-dd'),
      categoryId: '',
      isPaid: true,
      totalInstallments: 1,
    });
    setEditingTransaction(null);
  };

  const filteredCategories = categories.filter(
    (cat) => cat.type === formData.type
  );

  if (loading) {
    return <div className="loading">Carregando...</div>;
  }

  return (
    <div className="transactions-page">
      <div className="page-header">
        <h1>Compras Parceladas</h1>
        <button className="btn-primary" onClick={() => { resetForm(); setShowModal(true); }}>
          + Nova Compra Parcelada
        </button>
      </div>

      <div className="transactions-table-container">
        <table className="transactions-table">
          <thead>
            <tr>
              <th>Vencimento</th>
              <th>Descrição</th>
              <th>Categoria</th>
              <th>Tipo</th>
              <th>Valor</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {transactions.length === 0 ? (
              <tr>
                <td colSpan="7" className="empty-state">
                  Nenhuma compra parcelada encontrada
                </td>
              </tr>
            ) : (
              transactions.map((transaction) => {
                const isExpanded = expandedTransactions.has(transaction.id);
                const installments = installmentsMap[transaction.id] || [];
                const isInstallmentTransaction = transaction.totalInstallments && transaction.totalInstallments > 1;
                
                const paidCount = (transaction.paidInstallmentsCount !== undefined && transaction.paidInstallmentsCount !== null)
                  ? transaction.paidInstallmentsCount 
                  : installments.filter(i => i.isPaid).length;
                const totalCount = (transaction.totalInstallmentsCount !== undefined && transaction.totalInstallmentsCount !== null)
                  ? transaction.totalInstallmentsCount 
                  : (installments.length > 0 ? installments.length : (transaction.totalInstallments || 0));
                
                // Calcular valor total e valor pendente
                let totalAmount = 0;
                let pendingAmount = 0;
                
                if (isInstallmentTransaction) {
                  if (installments.length > 0) {
                    // Se parcelas foram carregadas, calcular valor total e pendente com precisão
                    totalAmount = installments.reduce((sum, inst) => sum + parseFloat(inst.amount || 0), 0);
                    pendingAmount = installments
                      .filter(inst => !inst.isPaid)
                      .reduce((sum, inst) => sum + parseFloat(inst.amount || 0), 0);
                  } else {
                    // Se parcelas ainda não foram carregadas, usar valor do backend
                    // O backend já calcula o total corretamente e coloca em transaction.amount
                    totalAmount = parseFloat(transaction.amount || 0);
                    
                    // Calcular valor pendente usando dados do backend
                    if (totalAmount > 0 && totalCount > 0 && paidCount !== undefined && paidCount !== null) {
                      const unpaidCount = totalCount - paidCount;
                      if (unpaidCount > 0) {
                        // Assumir que todas as parcelas têm o mesmo valor (aproximação até carregar)
                        const amountPerInstallment = totalAmount / totalCount;
                        pendingAmount = amountPerInstallment * unpaidCount;
                      } else {
                        // Todas as parcelas já foram pagas
                        pendingAmount = 0;
                      }
                    } else {
                      // Se não temos dados suficientes, usar o valor total como fallback
                      pendingAmount = totalAmount;
                    }
                  }
                } else {
                  // Transação simples
                  totalAmount = parseFloat(transaction.amount || 0);
                }
                
                // Para transações parceladas, SEMPRE mostrar valor pendente (não pago) no registro pai
                const displayAmount = isInstallmentTransaction 
                  ? pendingAmount  // Sempre mostrar valor pendente para transações parceladas
                  : totalAmount;    // Mostrar valor total para transações simples
                
                const isCompleted = isInstallmentTransaction && paidCount === totalCount && totalCount > 0;
                
                return (
                  <React.Fragment key={transaction.id}>
                    <tr className={transaction.isOverdue ? 'overdue' : ''} style={{ backgroundColor: isInstallmentTransaction ? (isCompleted ? '#e8f5e9' : '#fff3e0') : 'transparent' }}>
                      <td>
                        {isInstallmentTransaction && (
                          <button 
                            onClick={() => toggleInstallments(transaction.id)}
                            className="expand-btn"
                            style={{ marginRight: '8px', background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', padding: '0 4px' }}
                            title={isExpanded ? 'Recolher parcelas' : 'Expandir parcelas'}
                          >
                            {isExpanded ? '▼' : '▶'}
                          </button>
                        )}
                        {(() => {
                          const dateToShow = transaction.dueDate || transaction.transactionDate;
                          return dateToShow ? (
                            <span className={transaction.isOverdue ? 'overdue-date' : ''}>
                              {format(new Date(dateToShow), 'dd/MM/yyyy', { locale: ptBR })}
                            </span>
                          ) : (
                            '-'
                          );
                        })()}
                      </td>
                      <td>
                        <strong>{transaction.description}</strong>
                        {isInstallmentTransaction && (
                          <span className="installment-badge" style={{ marginLeft: '8px', fontSize: '12px', color: '#666' }}>
                            ({transaction.totalInstallments}x parcelas)
                          </span>
                        )}
                      </td>
                      <td>
                        <span className="category-badge" style={{ backgroundColor: transaction.category?.color + '20', color: transaction.category?.color }}>
                          {transaction.category?.icon} {transaction.category?.name || 'Sem categoria'}
                        </span>
                      </td>
                      <td>
                        <span className={`type-badge ${transaction.type.toLowerCase()}`}>
                          {transaction.type === 'INCOME' ? 'Receita' : 'Despesa'}
                        </span>
                      </td>
                      <td className={`amount ${transaction.type.toLowerCase()}`}>
                        {isInstallmentTransaction ? (
                          <div>
                            <div style={{ fontWeight: 'bold' }}>
                              {transaction.type === 'INCOME' ? '+' : '-'} R$ {displayAmount.toFixed(2)}
                              {installments.length > 0 && pendingAmount > 0 && (
                                <span style={{ fontSize: '11px', color: '#666', marginLeft: '4px' }}>
                                  (pendente)
                                </span>
                              )}
                            </div>
                            {installments.length > 0 && (
                              <small style={{ fontSize: '11px', color: '#666' }}>
                                ({paidCount}/{totalCount} pagas) • Total: R$ {totalAmount.toFixed(2)}
                              </small>
                            )}
                          </div>
                        ) : (
                          <>
                            {transaction.type === 'INCOME' ? '+' : '-'} R$ {parseFloat(transaction.amount).toFixed(2)}
                          </>
                        )}
                      </td>
                      <td>
                        {isInstallmentTransaction ? (
                          <span className={`status-badge ${isCompleted ? 'paid' : 'unpaid'}`}>
                            {isCompleted ? '✓ Concluída' : `⏳ ${paidCount}/${totalCount} Pagas`}
                          </span>
                        ) : (
                          <span className={`status-badge ${transaction.isPaid ? 'paid' : 'unpaid'}`}>
                            {transaction.isPaid ? '✓ Pago' : '⏳ Pendente'}
                          </span>
                        )}
                      </td>
                      <td>
                        <div className="action-buttons">
                          <button onClick={() => handleEdit(transaction)} className="btn-edit">✏️</button>
                          <button onClick={() => handleDelete(transaction.id)} className="btn-delete">🗑️</button>
                        </div>
                      </td>
                    </tr>
                    {isExpanded && isInstallmentTransaction && installments.length > 0 && installments.map((installment) => (
                      <tr key={installment.id} className="installment-row" style={{ backgroundColor: '#f9f9f9' }}>
                        <td style={{ paddingLeft: '40px' }}>
                          <span className={installment.isOverdue ? 'overdue-date' : ''}>
                            {format(new Date(installment.dueDate || installment.transactionDate), 'dd/MM/yyyy', { locale: ptBR })}
                          </span>
                        </td>
                        <td style={{ paddingLeft: '20px' }}>
                          <span style={{ fontSize: '13px', color: '#666' }}>
                            Parcela {installment.installmentNumber}/{installment.totalInstallments}
                          </span>
                        </td>
                        <td></td>
                        <td></td>
                        <td className={`amount ${installment.type.toLowerCase()}`}>
                          {installment.type === 'INCOME' ? '+' : '-'} R$ {parseFloat(installment.amount).toFixed(2)}
                        </td>
                        <td>
                          <span className={`status-badge ${installment.isPaid ? 'paid' : 'unpaid'}`}>
                            {installment.isPaid ? '✓ Pago' : '⏳ Pendente'}
                          </span>
                        </td>
                        <td>
                          <div className="action-buttons">
                            {!installment.isPaid ? (
                              <button onClick={() => handleMarkAsPaid(installment.id)} className="btn-paid" title="Marcar como pago">
                                ✓
                              </button>
                            ) : (
                              <button onClick={() => handleMarkAsUnpaid(installment.id)} className="btn-unpaid" title="Marcar como não pago">
                                ✗
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </React.Fragment>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={() => { setShowModal(false); resetForm(); }}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>{editingTransaction ? 'Editar Compra Parcelada' : 'Nova Compra Parcelada'}</h2>
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="modal-scrollable-content">
                <div className="form-group">
                  <label>Descrição</label>
                  <input
                    type="text"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    required
                  />
                </div>
              <div className="form-group">
                <label>Valor da Parcela</label>
                <input
                  type="number"
                  step="0.01"
                  value={formData.amount}
                  onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                  required
                />
                <small>Este valor será repetido em todas as parcelas</small>
              </div>
                <div className="form-group">
                  <label>Tipo</label>
                  <select
                    value={formData.type}
                    onChange={(e) => setFormData({ ...formData, type: e.target.value, categoryId: '' })}
                    required
                  >
                    <option value="INCOME">Receita</option>
                    <option value="EXPENSE">Despesa</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Categoria</label>
                  <select
                    value={formData.categoryId}
                    onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                  >
                    <option value="">Sem categoria</option>
                    {filteredCategories.map((cat) => (
                      <option key={cat.id} value={cat.id}>
                        {cat.icon} {cat.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Data de Início (Primeira Parcela)</label>
                  <input
                    type="date"
                    value={formData.dueDate}
                    onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
                    required
                  />
                  <small>Data da primeira parcela. As demais serão criadas nos meses seguintes</small>
                </div>
              <div className="form-group">
                <label>Número de Parcelas</label>
                <input
                  type="number"
                  min="2"
                  max="60"
                  value={formData.totalInstallments}
                  onChange={(e) => setFormData({ ...formData, totalInstallments: parseInt(e.target.value) || 2 })}
                  required
                />
                <small>
                  {formData.totalInstallments > 1 && formData.amount && (
                    <>Serão criadas {formData.totalInstallments} parcelas de R$ {parseFloat(formData.amount).toFixed(2)} cada (Total: R$ {(parseFloat(formData.amount) * formData.totalInstallments).toFixed(2)})</>
                  )}
                </small>
              </div>
                <div className="form-group">
                  <label>
                    <input
                      type="checkbox"
                      checked={formData.isPaid}
                      onChange={(e) => setFormData({ ...formData, isPaid: e.target.checked })}
                    />
                    {' '}Marcar primeira parcela como paga
                  </label>
                  <small>Se marcado, a primeira parcela será marcada como paga. As demais ficarão pendentes</small>
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => { setShowModal(false); resetForm(); }}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary">
                  {editingTransaction ? 'Atualizar' : 'Criar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Installments;

