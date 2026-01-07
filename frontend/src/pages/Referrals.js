import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import jsPDF from 'jspdf';
import 'jspdf-autotable';
import * as XLSX from 'xlsx';
import './Referrals.css';

const Referrals = () => {
  const [referralCode, setReferralCode] = useState('');
  const [referrals, setReferrals] = useState([]);
  const [stats, setStats] = useState({ 
    totalReferrals: 0, 
    totalCommissions: 0, 
    expectedFutureCommissions: 0 
  });
  const [loading, setLoading] = useState(true);
  const [copied, setCopied] = useState(false);
  const [availableBalance, setAvailableBalance] = useState(0);
  const [showWithdrawalModal, setShowWithdrawalModal] = useState(false);
  const [withdrawalAmount, setWithdrawalAmount] = useState('');
  const [pixKey, setPixKey] = useState('');
  const [pixKeyType, setPixKeyType] = useState('CPF');
  const [myWithdrawals, setMyWithdrawals] = useState([]);
  const [showReceiptViewModal, setShowReceiptViewModal] = useState(false);
  const [receiptImageUrl, setReceiptImageUrl] = useState(null);
  const [showAllWithdrawalsModal, setShowAllWithdrawalsModal] = useState(false);
  const [allWithdrawals, setAllWithdrawals] = useState([]);
  const [withdrawalsPage, setWithdrawalsPage] = useState(0);
  const [withdrawalsTotalPages, setWithdrawalsTotalPages] = useState(0);
  const [loadingWithdrawals, setLoadingWithdrawals] = useState(false);
  const [selectedReferral, setSelectedReferral] = useState(null);
  const [showReferralDetailsModal, setShowReferralDetailsModal] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      // Carregar dados essenciais primeiro
      const [codeRes, statsRes, balanceRes] = await Promise.all([
        api.get('/referrals/my-code'),
        api.get('/referrals/stats'),
        api.get('/withdrawals/available-balance'),
      ]);
      setReferralCode(codeRes.data.referralCode);
      setStats(statsRes.data);
      setAvailableBalance(balanceRes.data.availableBalance || 0);
      
      // Carregar referrals e withdrawals separadamente para melhor tratamento de erro
      try {
        const referralsRes = await api.get('/referrals/my-referrals', { timeout: 60000 });
        setReferrals(referralsRes.data || []);
      } catch (error) {
        console.error('Erro ao carregar referrals:', error);
        if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
          toast.error('Timeout ao carregar convites. Tente recarregar a página.');
        } else {
          toast.error('Erro ao carregar convites');
        }
        setReferrals([]);
      }
      
      try {
        // Carregar apenas os primeiros 12 saques para a visualização inicial
        const withdrawalsRes = await api.get('/withdrawals/my-withdrawals', { timeout: 30000 });
        const allWithdrawals = withdrawalsRes.data || [];
        // Limitar a 12 para a visualização inicial
        setMyWithdrawals(allWithdrawals.slice(0, 12));
      } catch (error) {
        console.error('Erro ao carregar saques:', error);
        if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
          toast.error('Timeout ao carregar saques. Tente recarregar a página.');
        } else {
          toast.error('Erro ao carregar saques');
        }
        setMyWithdrawals([]);
      }
    } catch (error) {
      console.error('Erro ao carregar dados:', error);
      toast.error('Erro ao carregar dados de referência');
    } finally {
      setLoading(false);
    }
  };
  
  const handleRequestWithdrawal = async (e) => {
    e.preventDefault();
    
    if (!withdrawalAmount || parseFloat(withdrawalAmount) <= 0) {
      toast.error('Informe um valor válido');
      return;
    }
    
    if (!pixKey || pixKey.trim() === '') {
      toast.error('Informe a chave PIX');
      return;
    }
    
    try {
      await api.post('/withdrawals', {
        amount: parseFloat(withdrawalAmount),
        pixKey: pixKey.trim(),
        pixKeyType: pixKeyType,
      });
      
      toast.success('Solicitação de saque criada com sucesso! O processamento pode levar até 3 dias úteis.');
      setShowWithdrawalModal(false);
      setWithdrawalAmount('');
      setPixKey('');
      loadData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao solicitar saque');
    }
  };

  const copyToClipboard = () => {
    const fullUrl = `${window.location.origin}/register?ref=${referralCode}`;
    navigator.clipboard.writeText(fullUrl);
    setCopied(true);
    toast.success('Link copiado para a área de transferência!');
    setTimeout(() => setCopied(false), 2000);
  };

  const loadAllWithdrawals = async (page = 0) => {
    try {
      setLoadingWithdrawals(true);
      const response = await api.get(`/withdrawals/my-withdrawals?page=${page}&size=25`);
      setAllWithdrawals(response.data.content || response.data || []);
      setWithdrawalsTotalPages(response.data.totalPages || 1);
      setWithdrawalsPage(page);
    } catch (error) {
      toast.error('Erro ao carregar saques');
    } finally {
      setLoadingWithdrawals(false);
    }
  };

  const handleOpenAllWithdrawals = async () => {
    setShowAllWithdrawalsModal(true);
    await loadAllWithdrawals(0);
  };

  const loadAllWithdrawalsForExport = async () => {
    try {
      const response = await api.get('/withdrawals/my-withdrawals');
      return response.data || [];
    } catch (error) {
      console.error('Erro ao carregar saques para exportação:', error);
      return [];
    }
  };

  const handleExportWithdrawalsPDF = async () => {
    try {
      toast.info('Carregando dados para exportação...');
      const allData = await loadAllWithdrawalsForExport();
      
      const doc = new jsPDF();
      
      doc.setFontSize(18);
      doc.text('Relatório de Saques', 14, 20);
      doc.setFontSize(12);
      doc.text(`Gerado em: ${format(new Date(), 'dd/MM/yyyy HH:mm', { locale: ptBR })}`, 14, 28);
      
      let yPos = 40;
      
      if (allData.length > 0) {
        const tableData = allData.map(w => [
          format(new Date(w.createdAt), 'dd/MM/yyyy', { locale: ptBR }),
          `R$ ${w.amount.toFixed(2).replace('.', ',')}`,
          w.pixKeyType,
          w.pixKey,
          w.status === 'PENDING' ? 'Pendente' : 
          w.status === 'PROCESSING' ? 'Processando' :
          w.status === 'COMPLETED' ? 'Concluído' : 'Rejeitado',
          w.processedAt ? format(new Date(w.processedAt), 'dd/MM/yyyy', { locale: ptBR }) : '-'
        ]);
        
        doc.autoTable({
          startY: yPos,
          head: [['Data Solicitação', 'Valor', 'Tipo PIX', 'Chave PIX', 'Status', 'Data Processamento']],
          body: tableData,
          theme: 'striped',
          headStyles: { fillColor: [99, 102, 241] },
          styles: { fontSize: 8 },
          margin: { left: 14, right: 14 }
        });
      }
      
      const fileName = `saques-${format(new Date(), 'yyyy-MM-dd', { locale: ptBR })}.pdf`;
      doc.save(fileName);
      toast.success('Relatório PDF exportado com sucesso!');
    } catch (error) {
      console.error('Erro ao exportar PDF:', error);
      toast.error('Erro ao exportar PDF');
    }
  };

  const handleExportWithdrawalsExcel = async () => {
    try {
      toast.info('Carregando dados para exportação...');
      const allData = await loadAllWithdrawalsForExport();
      
      const data = allData.map(w => ({
        'Data Solicitação': format(new Date(w.createdAt), 'dd/MM/yyyy HH:mm', { locale: ptBR }),
        'Valor': w.amount,
        'Tipo PIX': w.pixKeyType,
        'Chave PIX': w.pixKey,
        'Status': w.status === 'PENDING' ? 'Pendente' : 
                 w.status === 'PROCESSING' ? 'Processando' :
                 w.status === 'COMPLETED' ? 'Concluído' : 'Rejeitado',
        'Data Processamento': w.processedAt ? format(new Date(w.processedAt), 'dd/MM/yyyy HH:mm', { locale: ptBR }) : '-'
      }));
      
      const ws = XLSX.utils.json_to_sheet(data);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, 'Saques');
      
      const fileName = `saques-${format(new Date(), 'yyyy-MM-dd', { locale: ptBR })}.xlsx`;
      XLSX.writeFile(wb, fileName);
      toast.success('Relatório Excel exportado com sucesso!');
    } catch (error) {
      console.error('Erro ao exportar Excel:', error);
      toast.error('Erro ao exportar Excel');
    }
  };

  if (loading) {
    return <div className="referrals-page">Carregando...</div>;
  }

  return (
    <div className="referrals-page">
      <div className="page-header">
        <h1>💰 Programa de Afiliado</h1>
        <p>Convide amigos e ganhe 10% de comissão sobre cada mensalidade paga!</p>
      </div>

      <div className="referral-stats">
        <div className="stat-card">
          <div className="stat-icon">👥</div>
          <div className="stat-info">
            <div className="stat-value">{stats.totalReferrals}</div>
            <div className="stat-label">Amigos Convidados</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">💰</div>
          <div className="stat-info">
            <div className="stat-value">
              R$ {stats.totalCommissions ? stats.totalCommissions.toFixed(2).replace('.', ',') : '0,00'}
            </div>
            <div className="stat-label">Total em Comissões (Já Recebidas)</div>
          </div>
        </div>
        {stats.expectedFutureCommissions > 0 && (
          <div className="stat-card future-stat">
            <div className="stat-icon">📈</div>
            <div className="stat-info">
              <div className="stat-value">
                R$ {stats.expectedFutureCommissions.toFixed(2).replace('.', ',')}
              </div>
              <div className="stat-label">Total Esperado (A Receber)</div>
            </div>
          </div>
        )}
        <div className="stat-card withdrawal-stat">
          <div className="stat-content">
            <div className="stat-icon">💵</div>
            <div className="stat-info">
              <div className="stat-value">
                R$ {availableBalance.toFixed(2).replace('.', ',')}
              </div>
              <div className="stat-label">Disponível para Saque</div>
            </div>
          </div>
          {availableBalance > 0 && (
            <button 
              className="btn-withdraw"
              onClick={() => setShowWithdrawalModal(true)}
            >
              Solicitar Saque
            </button>
          )}
        </div>
      </div>
      
      {myWithdrawals.length > 0 && (
        <div className="withdrawals-section">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h2>Meus Saques</h2>
            {myWithdrawals.length > 12 && (
              <button 
                className="btn-view-all"
                onClick={handleOpenAllWithdrawals}
              >
                Ver Todos os Saques ({myWithdrawals.length})
              </button>
            )}
          </div>
          <div className="withdrawals-list">
            {myWithdrawals.slice(0, 12).map((withdrawal) => (
              <div key={withdrawal.id} className={`withdrawal-item ${withdrawal.status.toLowerCase()}`}>
                <div className="withdrawal-header">
                  <span className="withdrawal-amount">R$ {withdrawal.amount.toFixed(2).replace('.', ',')}</span>
                  <span className={`status-badge ${withdrawal.status.toLowerCase()}`}>
                    {withdrawal.status === 'PENDING' && '⏳ Pendente'}
                    {withdrawal.status === 'PROCESSING' && '🔄 Em Processamento'}
                    {withdrawal.status === 'COMPLETED' && '✅ Concluído'}
                    {withdrawal.status === 'REJECTED' && '❌ Rejeitado'}
                  </span>
                </div>
                <div className="withdrawal-details">
                  <div>Solicitado em: {format(new Date(withdrawal.createdAt), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })}</div>
                  {withdrawal.processedAt && (
                    <div>Processado em: {format(new Date(withdrawal.processedAt), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })}</div>
                  )}
                  {withdrawal.receiptFileUrl && (
                    <a 
                      href="#"
                      onClick={async (e) => {
                        e.preventDefault();
                        try {
                          const response = await api.get(`/withdrawals/${withdrawal.id}/receipt`, {
                            responseType: 'blob',
                          });
                          const contentType = response.headers['content-type'] || response.headers['Content-Type'] || 'image/jpeg';
                          const blob = new Blob([response.data], { type: contentType });
                          const url = window.URL.createObjectURL(blob);
                          setReceiptImageUrl(url);
                          setShowReceiptViewModal(true);
                        } catch (error) {
                          console.error('Erro ao visualizar comprovante:', error);
                          toast.error('Erro ao visualizar comprovante');
                        }
                      }}
                      className="receipt-link"
                    >
                      📄 Ver Comprovante
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
      
      {showWithdrawalModal && (
        <div className="modal-overlay" onClick={() => setShowWithdrawalModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Solicitar Saque</h2>
            <div className="withdrawal-warning">
              ⚠️ O processamento do saque pode levar até 3 dias úteis.
            </div>
            <div className="withdrawal-warning">
              ⚠️ Você só pode solicitar 1 saque por dia.
            </div>
            <form onSubmit={handleRequestWithdrawal}>
              <div className="form-group">
                <label>Valor Disponível: R$ {availableBalance.toFixed(2).replace('.', ',')}</label>
                <input
                  type="number"
                  step="0.01"
                  min="10"
                  max={availableBalance}
                  placeholder="Valor a sacar (mínimo R$ 10,00)"
                  value={withdrawalAmount}
                  onChange={(e) => setWithdrawalAmount(e.target.value)}
                  required
                />
              </div>
              <div className="form-group">
                <label>Tipo de Chave PIX</label>
                <select
                  value={pixKeyType}
                  onChange={(e) => setPixKeyType(e.target.value)}
                  required
                >
                  <option value="CPF">CPF</option>
                  <option value="EMAIL">E-mail</option>
                  <option value="TELEFONE">Telefone</option>
                  <option value="ALEATORIA">Chave Aleatória</option>
                </select>
              </div>
              <div className="form-group">
                <label>Chave PIX</label>
                <input
                  type="text"
                  placeholder="Digite sua chave PIX"
                  value={pixKey}
                  onChange={(e) => setPixKey(e.target.value)}
                  required
                />
              </div>
              <div className="modal-actions">
                <button type="button" onClick={() => setShowWithdrawalModal(false)}>
                  Cancelar
                </button>
                <button type="submit">Solicitar Saque</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="referral-code-section">
        <h2>Seu Código de Referência</h2>
        <div className="code-display">
          <div className="code-value">{referralCode}</div>
          <button 
            className={`btn-copy ${copied ? 'copied' : ''}`}
            onClick={copyToClipboard}
          >
            {copied ? '✓ Copiado!' : '📋 Copiar Link'}
          </button>
        </div>
        <p className="code-hint">
          Compartilhe este link: <strong>{window.location.origin}/register?ref={referralCode}</strong>
        </p>
      </div>

      <div className="referrals-list">
        <h2>Seus Convites</h2>
        {referrals.length === 0 ? (
          <div className="empty-state">
            <p>Você ainda não convidou ninguém.</p>
            <p>Compartilhe seu código e comece a ganhar prêmios!</p>
          </div>
        ) : (
          <div className="referrals-table-container">
            <table className="referrals-table">
              <thead>
                <tr>
                  <th>Usuário</th>
                  <th>Plano</th>
                  <th>Data do Convite</th>
                  <th>Pagamentos</th>
                  <th>Comissões Recebidas</th>
                  <th>Comissões Futuras</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {referrals.map((referral) => (
                  <tr key={referral.id}>
                    <td>
                      <div className="referral-user">
                        <span className="user-icon">👤</span>
                        <span className="user-name">{referral.referredName}</span>
                      </div>
                    </td>
                    <td>
                      {referral.referredPlan && referral.referredPlan !== 'FREE' ? (
                        <span className="plan-badge">{referral.referredPlan}</span>
                      ) : (
                        <span>-</span>
                      )}
                    </td>
                    <td>{format(new Date(referral.createdAt), "dd/MM/yyyy", { locale: ptBR })}</td>
                    <td>{referral.totalPayments || 0}</td>
                    <td className="commission-paid">
                      R$ {referral.totalCommission ? referral.totalCommission.toFixed(2).replace('.', ',') : '0,00'}
                    </td>
                    <td className="commission-future">
                      {referral.expectedFutureCommission && referral.expectedFutureCommission > 0 ? (
                        <>R$ {referral.expectedFutureCommission.toFixed(2).replace('.', ',')}</>
                      ) : (
                        <>-</>
                      )}
                    </td>
                    <td>
                      <button 
                        className="btn-details"
                        onClick={() => {
                          setSelectedReferral(referral);
                          setShowReferralDetailsModal(true);
                        }}
                      >
                        Ver Detalhes
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="referral-info">
        <h3>Como Funciona?</h3>
        <ul>
          <li>📧 Compartilhe seu código de referência com amigos</li>
          <li>✅ Quando um amigo se cadastrar usando seu código e assinar qualquer plano, você ganha 10% de comissão</li>
          <li>💰 Você ganha 10% de cada mensalidade paga pelo seu indicado, mês a mês</li>
          <li>📊 Acompanhe todos os pagamentos e comissões em tempo real</li>
          <li>♾️ Não há limite de convites! Quanto mais indicar, mais você ganha!</li>
        </ul>
      </div>

      {/* Modal para listar todos os saques */}
      {showAllWithdrawalsModal && (
        <div className="modal-overlay" onClick={() => setShowAllWithdrawalsModal(false)}>
          <div className="modal-content all-withdrawals-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-with-actions">
              <h2>Todos os Meus Saques</h2>
              <div className="export-buttons">
                <button className="btn-export btn-export-pdf" onClick={handleExportWithdrawalsPDF}>
                  📄 Exportar PDF
                </button>
                <button className="btn-export btn-export-excel" onClick={handleExportWithdrawalsExcel}>
                  📊 Exportar Excel
                </button>
              </div>
            </div>
            {loadingWithdrawals ? (
              <div style={{ textAlign: 'center', padding: '2rem' }}>Carregando...</div>
            ) : (
              <>
                <div className="withdrawals-table-container">
                  <table className="withdrawals-table">
                    <thead>
                      <tr>
                        <th>Data Solicitação</th>
                        <th>Valor</th>
                        <th>Tipo PIX</th>
                        <th>Chave PIX</th>
                        <th>Status</th>
                        <th>Data Processamento</th>
                        <th>Ações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {allWithdrawals.length === 0 ? (
                        <tr>
                          <td colSpan="7" style={{ textAlign: 'center', padding: '2rem' }}>
                            Nenhum saque encontrado
                          </td>
                        </tr>
                      ) : (
                        allWithdrawals.map((withdrawal) => (
                          <tr key={withdrawal.id}>
                            <td>{format(new Date(withdrawal.createdAt), "dd/MM/yyyy HH:mm", { locale: ptBR })}</td>
                            <td className="amount-cell">R$ {withdrawal.amount.toFixed(2).replace('.', ',')}</td>
                            <td>{withdrawal.pixKeyType}</td>
                            <td>{withdrawal.pixKey}</td>
                            <td>
                              <span className={`status-badge ${withdrawal.status.toLowerCase()}`}>
                                {withdrawal.status === 'PENDING' && '⏳ Pendente'}
                                {withdrawal.status === 'PROCESSING' && '🔄 Processando'}
                                {withdrawal.status === 'COMPLETED' && '✅ Concluído'}
                                {withdrawal.status === 'REJECTED' && '❌ Rejeitado'}
                              </span>
                            </td>
                            <td>
                              {withdrawal.processedAt 
                                ? format(new Date(withdrawal.processedAt), "dd/MM/yyyy HH:mm", { locale: ptBR })
                                : '-'
                              }
                            </td>
                            <td>
                              {withdrawal.receiptFileUrl && (
                                <button 
                                  className="btn-view-receipt"
                                  onClick={async () => {
                                    try {
                                      const response = await api.get(`/withdrawals/${withdrawal.id}/receipt`, {
                                        responseType: 'blob',
                                      });
                                      const contentType = response.headers['content-type'] || response.headers['Content-Type'] || 'image/jpeg';
                                      const blob = new Blob([response.data], { type: contentType });
                                      const url = window.URL.createObjectURL(blob);
                                      setReceiptImageUrl(url);
                                      setShowReceiptViewModal(true);
                                    } catch (error) {
                                      toast.error('Erro ao visualizar comprovante');
                                    }
                                  }}
                                >
                                  Ver Comprovante
                                </button>
                              )}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
                {withdrawalsTotalPages > 1 && (
                  <div className="pagination">
                    <button
                      disabled={withdrawalsPage === 0}
                      onClick={() => loadAllWithdrawals(withdrawalsPage - 1)}
                    >
                      Anterior
                    </button>
                    <span>
                      Página {withdrawalsPage + 1} de {withdrawalsTotalPages}
                    </span>
                    <button
                      disabled={withdrawalsPage >= withdrawalsTotalPages - 1}
                      onClick={() => loadAllWithdrawals(withdrawalsPage + 1)}
                    >
                      Próxima
                    </button>
                  </div>
                )}
              </>
            )}
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowAllWithdrawalsModal(false)}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal para detalhes do usuário indicado */}
      {showReferralDetailsModal && selectedReferral && (
        <div className="modal-overlay" onClick={() => {
          setShowReferralDetailsModal(false);
          setSelectedReferral(null);
        }}>
          <div className="modal-content referral-details-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header-with-actions">
              <h2>Detalhes do Usuário Indicado</h2>
              <button className="close-modal-btn" onClick={() => {
                setShowReferralDetailsModal(false);
                setSelectedReferral(null);
              }}>✕</button>
            </div>
            <div className="referral-details-content">
              <div className="detail-section">
                <h3>Informações do Usuário</h3>
                <div className="detail-row">
                  <span className="detail-label">Nome:</span>
                  <span className="detail-value">{selectedReferral.referredName}</span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Data do Convite:</span>
                  <span className="detail-value">
                    {format(new Date(selectedReferral.createdAt), "dd/MM/yyyy", { locale: ptBR })}
                  </span>
                </div>
                {selectedReferral.referredPlan && (
                  <>
                    <div className="detail-row">
                      <span className="detail-label">Plano Atual:</span>
                      <span className="detail-value">
                        {selectedReferral.referredPlan}
                        {selectedReferral.referredPlanPrice && 
                          ` - R$ ${selectedReferral.referredPlanPrice.toFixed(2).replace('.', ',')}/mês`
                        }
                      </span>
                    </div>
                    {selectedReferral.subscriptionEndDate && (
                      <div className="detail-row">
                        <span className="detail-label">Assinatura até:</span>
                        <span className="detail-value">
                          {format(new Date(selectedReferral.subscriptionEndDate), "dd/MM/yyyy", { locale: ptBR })}
                        </span>
                      </div>
                    )}
                  </>
                )}
                <div className="detail-row">
                  <span className="detail-label">Total de Pagamentos:</span>
                  <span className="detail-value">{selectedReferral.totalPayments || 0}</span>
                </div>
                <div className="detail-row highlight">
                  <span className="detail-label">Total em Comissões (Já Recebidas):</span>
                  <span className="detail-value">
                    R$ {selectedReferral.totalCommission ? selectedReferral.totalCommission.toFixed(2).replace('.', ',') : '0,00'}
                  </span>
                </div>
                {selectedReferral.expectedFutureCommission && selectedReferral.expectedFutureCommission > 0 && (
                  <div className="detail-row highlight future">
                    <span className="detail-label">Total Esperado (A Receber):</span>
                    <span className="detail-value">
                      R$ {selectedReferral.expectedFutureCommission.toFixed(2).replace('.', ',')}
                    </span>
                  </div>
                )}
              </div>

              {selectedReferral.commissions && selectedReferral.commissions.length > 0 && (
                <div className="detail-section">
                  <h3>Histórico de Pagamentos e Comissões (Mês a Mês)</h3>
                  <div className="commissions-list">
                    {selectedReferral.commissions.map((commission, index) => (
                      <div 
                        key={commission.id || `future-${index}`} 
                        className={`commission-item ${!commission.isPaid ? 'future' : ''}`}
                      >
                        <div className="commission-date">
                          {String(commission.paymentMonth).padStart(2, '0')}/{commission.paymentYear}
                        </div>
                        <div className="commission-details">
                          <div className="commission-header">
                            <div className="commission-plan">{commission.subscriptionPlan}</div>
                            {!commission.isPaid && (
                              <span className="future-badge">A Receber</span>
                            )}
                            {commission.isPaid && (
                              <span className="paid-badge">✓ Pago</span>
                            )}
                          </div>
                          <div className="commission-amounts">
                            <span className="monthly-amount">
                              Mensalidade: R$ {commission.monthlyAmount.toFixed(2).replace('.', ',')}
                            </span>
                            <span className={`commission-amount ${!commission.isPaid ? 'future-amount' : ''}`}>
                              {commission.isPaid ? 'Sua comissão: ' : 'Comissão esperada: '}
                              R$ {commission.commissionAmount.toFixed(2).replace('.', ',')}
                            </span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => {
                setShowReferralDetailsModal(false);
                setSelectedReferral(null);
              }}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal para visualizar comprovante */}
      {showReceiptViewModal && receiptImageUrl && (
        <div className="modal-overlay" onClick={() => {
          if (receiptImageUrl) {
            window.URL.revokeObjectURL(receiptImageUrl);
          }
          setShowReceiptViewModal(false);
          setReceiptImageUrl(null);
        }}>
          <div className="modal-content receipt-view-modal" onClick={(e) => e.stopPropagation()}>
            <div className="receipt-modal-header">
              <h2>Comprovante PIX</h2>
              <button className="close-modal-btn" onClick={() => {
                if (receiptImageUrl) {
                  window.URL.revokeObjectURL(receiptImageUrl);
                }
                setShowReceiptViewModal(false);
                setReceiptImageUrl(null);
              }}>✕</button>
            </div>
            <div className="receipt-modal-body">
              <img 
                src={receiptImageUrl} 
                alt="Comprovante PIX" 
                style={{ maxWidth: '100%', maxHeight: '80vh', objectFit: 'contain' }}
              />
            </div>
            <div className="receipt-modal-footer">
              <button className="btn-secondary" onClick={() => {
                if (receiptImageUrl) {
                  window.URL.revokeObjectURL(receiptImageUrl);
                }
                setShowReceiptViewModal(false);
                setReceiptImageUrl(null);
              }}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Referrals;

