import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './AdminWithdrawals.css';

const AdminWithdrawals = () => {
  const [withdrawals, setWithdrawals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState('all'); // all, pending
  const [selectedWithdrawal, setSelectedWithdrawal] = useState(null);
  const [showReceiptModal, setShowReceiptModal] = useState(false);
  const [receiptFile, setReceiptFile] = useState(null);
  const [notes, setNotes] = useState('');
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [showReceiptViewModal, setShowReceiptViewModal] = useState(false);
  const [receiptImageUrl, setReceiptImageUrl] = useState(null);
  const [viewingWithdrawalId, setViewingWithdrawalId] = useState(null);

  useEffect(() => {
    loadWithdrawals();
  }, [page, filter]);

  const loadWithdrawals = async () => {
    try {
      setLoading(true);
      const endpoint = filter === 'pending' 
        ? `/withdrawals/admin/pending?page=${page}&size=20`
        : `/withdrawals/admin/all?page=${page}&size=20`;
      
      const response = await api.get(endpoint);
      setWithdrawals(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
    } catch (error) {
      toast.error('Erro ao carregar solicitações de saque');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (withdrawalId, newStatus) => {
    try {
      await api.put(`/withdrawals/admin/${withdrawalId}/status`, {
        status: newStatus,
        notes: notes || '',
      });
      
      toast.success('Status atualizado com sucesso');
      setSelectedWithdrawal(null);
      setNotes('');
      loadWithdrawals();
    } catch (error) {
      toast.error('Erro ao atualizar status');
    }
  };

  const handleReceiptUpload = async (withdrawalId) => {
    if (!receiptFile) {
      toast.error('Selecione um arquivo');
      return;
    }

    try {
      const formData = new FormData();
      formData.append('file', receiptFile);
      
      await api.post(`/withdrawals/admin/${withdrawalId}/receipt`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      toast.success('Comprovante enviado com sucesso');
      setReceiptFile(null);
      setShowReceiptModal(false);
      setSelectedWithdrawal(null);
      loadWithdrawals();
    } catch (error) {
      toast.error('Erro ao enviar comprovante');
    }
  };

  const viewReceipt = async (withdrawalId) => {
    try {
      const response = await api.get(`/withdrawals/${withdrawalId}/receipt`, {
        responseType: 'blob',
      });
      
      // Obter Content-Type do header da resposta
      const contentType = response.headers['content-type'] || response.headers['Content-Type'] || 'image/jpeg';
      
      // Criar blob com o tipo MIME correto
      const blob = new Blob([response.data], { type: contentType });
      const url = window.URL.createObjectURL(blob);
      
      // Abrir modal com a imagem
      setReceiptImageUrl(url);
      setViewingWithdrawalId(withdrawalId);
      setShowReceiptViewModal(true);
    } catch (error) {
      console.error('Erro ao visualizar comprovante:', error);
      toast.error('Erro ao visualizar comprovante');
    }
  };

  const closeReceiptModal = () => {
    if (receiptImageUrl) {
      window.URL.revokeObjectURL(receiptImageUrl);
    }
    setShowReceiptViewModal(false);
    setReceiptImageUrl(null);
    setViewingWithdrawalId(null);
  };

  if (loading) {
    return <div className="admin-withdrawals-page">Carregando...</div>;
  }

  return (
    <div className="admin-withdrawals-page">
      <div className="page-header">
        <h1>💵 Gerenciar Saques</h1>
        <p>Gerencie todas as solicitações de saque de comissões</p>
      </div>

      <div className="filters">
        <button
          className={`filter-btn ${filter === 'all' ? 'active' : ''}`}
          onClick={() => {
            setFilter('all');
            setPage(0);
          }}
        >
          Todas
        </button>
        <button
          className={`filter-btn ${filter === 'pending' ? 'active' : ''}`}
          onClick={() => {
            setFilter('pending');
            setPage(0);
          }}
        >
          Pendentes
        </button>
      </div>

      <div className="withdrawals-table">
        <table>
          <thead>
            <tr>
              <th>Usuário</th>
              <th>Valor</th>
              <th>Chave PIX</th>
              <th>Status</th>
              <th>Data Solicitação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {withdrawals.length === 0 ? (
              <tr>
                <td colSpan="6" className="empty-state">
                  Nenhuma solicitação encontrada
                </td>
              </tr>
            ) : (
              withdrawals.map((withdrawal) => (
                <tr key={withdrawal.id}>
                  <td>
                    <div className="user-info">
                      <strong>{withdrawal.userName}</strong>
                      <span className="user-email">{withdrawal.userEmail}</span>
                    </div>
                  </td>
                  <td className="amount-cell">
                    R$ {withdrawal.amount.toFixed(2).replace('.', ',')}
                  </td>
                  <td>
                    <div className="pix-info">
                      <span className="pix-type">{withdrawal.pixKeyType}</span>
                      <span className="pix-key">{withdrawal.pixKey}</span>
                    </div>
                  </td>
                  <td>
                    <span className={`status-badge ${withdrawal.status.toLowerCase()}`}>
                      {withdrawal.status === 'PENDING' && '⏳ Pendente'}
                      {withdrawal.status === 'PROCESSING' && '🔄 Processando'}
                      {withdrawal.status === 'COMPLETED' && '✅ Concluído'}
                      {withdrawal.status === 'REJECTED' && '❌ Rejeitado'}
                    </span>
                  </td>
                  <td>
                    {format(new Date(withdrawal.createdAt), "dd/MM/yyyy HH:mm", { locale: ptBR })}
                  </td>
                  <td>
                    <div className="action-buttons">
                      {withdrawal.status === 'PENDING' && (
                        <>
                          <button
                            className="btn-action btn-process"
                            onClick={() => {
                              setSelectedWithdrawal(withdrawal);
                              setStatusFilter('PROCESSING');
                            }}
                          >
                            Processar
                          </button>
                          <button
                            className="btn-action btn-reject"
                            onClick={() => {
                              setSelectedWithdrawal(withdrawal);
                              setStatusFilter('REJECTED');
                            }}
                          >
                            Rejeitar
                          </button>
                        </>
                      )}
                      {withdrawal.status === 'PROCESSING' && (
                        <button
                          className="btn-action btn-upload"
                          onClick={() => {
                            setSelectedWithdrawal(withdrawal);
                            setShowReceiptModal(true);
                          }}
                        >
                          Enviar Comprovante
                        </button>
                      )}
                      {withdrawal.receiptFileUrl && (
                        <button
                          className="btn-action btn-view"
                          onClick={() => viewReceipt(withdrawal.id)}
                        >
                          Ver Comprovante
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            Anterior
          </button>
          <span>
            Página {page + 1} de {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => setPage(page + 1)}
          >
            Próxima
          </button>
        </div>
      )}

      {selectedWithdrawal && !showReceiptModal && (
        <div className="modal-overlay" onClick={() => setSelectedWithdrawal(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Alterar Status</h2>
            <div className="withdrawal-details-modal">
              <p><strong>Usuário:</strong> {selectedWithdrawal.userName}</p>
              <p><strong>Valor:</strong> R$ {selectedWithdrawal.amount.toFixed(2).replace('.', ',')}</p>
              <p><strong>Chave PIX:</strong> {selectedWithdrawal.pixKey} ({selectedWithdrawal.pixKeyType})</p>
            </div>
            <div className="form-group">
              <label>Observações (opcional)</label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Adicione observações sobre o processamento..."
                rows="4"
              />
            </div>
            <div className="modal-actions">
              <button onClick={() => setSelectedWithdrawal(null)}>Cancelar</button>
              <button
                className={statusFilter === 'REJECTED' ? 'btn-reject' : 'btn-process'}
                onClick={() => handleStatusChange(selectedWithdrawal.id, statusFilter)}
              >
                {statusFilter === 'REJECTED' ? 'Rejeitar' : 'Processar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showReceiptModal && selectedWithdrawal && (
        <div className="modal-overlay" onClick={() => {
          setShowReceiptModal(false);
          setSelectedWithdrawal(null);
          setReceiptFile(null);
        }}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Enviar Comprovante PIX</h2>
            <div className="withdrawal-details-modal">
              <p><strong>Usuário:</strong> {selectedWithdrawal.userName}</p>
              <p><strong>Valor:</strong> R$ {selectedWithdrawal.amount.toFixed(2).replace('.', ',')}</p>
            </div>
            <div className="form-group">
              <label>Arquivo do Comprovante</label>
              <input
                type="file"
                accept="image/*,.pdf"
                onChange={(e) => setReceiptFile(e.target.files[0])}
              />
            </div>
            <div className="modal-actions">
              <button onClick={() => {
                setShowReceiptModal(false);
                setSelectedWithdrawal(null);
                setReceiptFile(null);
              }}>
                Cancelar
              </button>
              <button
                className="btn-upload"
                onClick={() => handleReceiptUpload(selectedWithdrawal.id)}
                disabled={!receiptFile}
              >
                Enviar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal para visualizar comprovante */}
      {showReceiptViewModal && receiptImageUrl && (
        <div className="modal-overlay" onClick={closeReceiptModal}>
          <div className="modal-content receipt-view-modal" onClick={(e) => e.stopPropagation()}>
            <div className="receipt-modal-header">
              <h2>Comprovante PIX</h2>
              <button className="close-modal-btn" onClick={closeReceiptModal}>✕</button>
            </div>
            <div className="receipt-modal-body">
              <img 
                src={receiptImageUrl} 
                alt="Comprovante PIX" 
                style={{ maxWidth: '100%', maxHeight: '80vh', objectFit: 'contain' }}
              />
            </div>
            <div className="receipt-modal-footer">
              <button className="btn-secondary" onClick={closeReceiptModal}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminWithdrawals;



