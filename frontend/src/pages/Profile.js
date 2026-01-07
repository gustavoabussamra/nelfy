import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './Profile.css';

const Profile = () => {
  const { user, setUser } = useAuth();
  const [subscription, setSubscription] = useState(null);
  const [planLimits, setPlanLimits] = useState(null);
  const [loading, setLoading] = useState(true);

  const isAdmin = (user?.role || '').toUpperCase().trim() === 'ADMIN';

  useEffect(() => {
    // Admin não carrega subscription
    if (!isAdmin) {
      loadSubscription();
    } else {
      setLoading(false);
    }
  }, [isAdmin]);

  const loadSubscription = async () => {
    try {
      const [subscriptionRes, limitsRes] = await Promise.all([
        api.get('/subscriptions/me'),
        api.get('/plan-limits'),
      ]);
      setSubscription(subscriptionRes.data);
      setPlanLimits(limitsRes.data);
    } catch (error) {
      toast.error('Erro ao carregar assinatura');
    } finally {
      setLoading(false);
    }
  };

  const [processingPayment, setProcessingPayment] = useState(false);

  const handleUpgrade = async (plan) => {
    if (processingPayment) return;
    
    setProcessingPayment(true);
    try {
      // Criar solicitação de pagamento
      const paymentResponse = await api.post('/payments/create', {
        plan: plan,
        paymentMethod: 'CREDIT_CARD',
        returnUrl: window.location.origin + '/profile',
      });

      if (paymentResponse.data.paymentUrl) {
        // Redirecionar para gateway de pagamento
        window.location.href = paymentResponse.data.paymentUrl;
      } else if (paymentResponse.data.status === 'APPROVED') {
        // Pagamento aprovado (modo de teste)
        toast.success('Pagamento aprovado! Assinatura atualizada com sucesso!');
        await loadSubscription();
      } else {
        toast.info(paymentResponse.data.message || 'Processando pagamento...');
      }
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao processar pagamento');
    } finally {
      setProcessingPayment(false);
    }
  };

  if (loading) {
    return <div className="loading">Carregando...</div>;
  }

  const plans = [
    { name: 'FREE', label: 'Grátis', price: 0, features: ['30 dias grátis', 'Transações ilimitadas', '3 categorias', '1 conta', '3 metas', '1 orçamento'] },
    { name: 'BASIC', label: 'Básico', price: 29.90, features: ['Transações ilimitadas', 'Categorias ilimitadas', '3 contas', '10 anexos', 'Metas ilimitadas', 'Orçamentos ilimitados', 'Exportação Excel'] },
    { name: 'PREMIUM', label: 'Premium', price: 59.90, features: ['Tudo do Básico', 'Contas ilimitadas', 'Anexos ilimitados', 'Análise com IA', 'Exportação Excel'] },
    { name: 'ENTERPRISE', label: 'Empresarial', price: 149.90, features: ['Tudo do Premium', 'Múltiplos usuários', 'Colaboração', 'API personalizada', 'Suporte 24/7'] },
  ];

  const currentPlan = subscription?.plan || 'FREE';
  const isActive = subscription?.isActive || false;
  const endDate = subscription?.endDate ? format(new Date(subscription.endDate), 'dd/MM/yyyy', { locale: ptBR }) : null;

  return (
    <div className="profile-page">
      <div className="profile-header">
        <h1>Perfil</h1>
      </div>

      <div className="profile-content">
        <div className="profile-card">
          <h2>Informações Pessoais</h2>
          <div className="profile-info">
            <div className="info-item">
              <label>Nome</label>
              <p>{user?.name}</p>
            </div>
            <div className="info-item">
              <label>Email</label>
              <p>{user?.email}</p>
            </div>
          </div>
        </div>

        {!isAdmin && (
          <>
            <div className="profile-card">
              <h2>Assinatura</h2>
              <div className="subscription-info">
                <div className="subscription-status">
                  <span className={`status-badge ${isActive ? 'active' : 'inactive'}`}>
                    {isActive ? '✅ Ativa' : '❌ Expirada'}
                  </span>
                  {endDate && (
                    <p className="end-date">
                      {isActive ? 'Válida até' : 'Expirou em'} {endDate}
                    </p>
                  )}
                </div>
                <div className="current-plan">
                  <h3>Plano Atual: {plans.find((p) => p.name === currentPlan)?.label}</h3>
                </div>
              </div>
            </div>

            {planLimits && (
              <div className="profile-card">
                <h2>Limites do Plano</h2>
                <div className="plan-limits-grid">
                  <div className="limit-item">
                    <span className="limit-label">Transações:</span>
                    <span className="limit-value">
                      {planLimits.maxTransactions === 2147483647 ? 'Ilimitado' : planLimits.maxTransactions}
                    </span>
                  </div>
                  <div className="limit-item">
                    <span className="limit-label">Categorias:</span>
                    <span className="limit-value">
                      {planLimits.maxCategories === 2147483647 ? 'Ilimitado' : planLimits.maxCategories}
                    </span>
                  </div>
                  <div className="limit-item">
                    <span className="limit-label">Contas:</span>
                    <span className="limit-value">
                      {planLimits.maxAccounts === 2147483647 ? 'Ilimitado' : planLimits.maxAccounts}
                    </span>
                  </div>
                  <div className="limit-item">
                    <span className="limit-label">Anexos:</span>
                    <span className="limit-value">
                      {planLimits.maxAttachments === 2147483647 ? 'Ilimitado' : planLimits.maxAttachments === 0 ? 'Não disponível' : planLimits.maxAttachments}
                    </span>
                  </div>
                  <div className="limit-item">
                    <span className="limit-label">Metas:</span>
                    <span className="limit-value">
                      {planLimits.maxGoals === 2147483647 ? 'Ilimitado' : planLimits.maxGoals}
                    </span>
                  </div>
                  <div className="limit-item">
                    <span className="limit-label">Orçamentos:</span>
                    <span className="limit-value">
                      {planLimits.maxBudgets === 2147483647 ? 'Ilimitado' : planLimits.maxBudgets}
                    </span>
                  </div>
                  <div className="limit-item feature">
                    <span className="limit-label">Exportação Excel:</span>
                    <span className={`limit-value ${planLimits.canExportExcel ? 'enabled' : 'disabled'}`}>
                      {planLimits.canExportExcel ? '✅ Disponível' : '❌ Indisponível'}
                    </span>
                  </div>
                  <div className="limit-item feature">
                    <span className="limit-label">Análise com IA:</span>
                    <span className={`limit-value ${planLimits.canUseAI ? 'enabled' : 'disabled'}`}>
                      {planLimits.canUseAI ? '✅ Disponível' : '❌ Indisponível'}
                    </span>
                  </div>
                  <div className="limit-item feature">
                    <span className="limit-label">Colaboração:</span>
                    <span className={`limit-value ${planLimits.canCollaborate ? 'enabled' : 'disabled'}`}>
                      {planLimits.canCollaborate ? '✅ Disponível' : '❌ Indisponível'}
                    </span>
                  </div>
                </div>
              </div>
            )}

            <div className="profile-card">
              <h2>Planos Disponíveis</h2>
              <div className="plans-grid">
                {plans.map((plan) => (
                  <div
                    key={plan.name}
                    className={`plan-card ${currentPlan === plan.name ? 'current' : ''}`}
                  >
                    <div className="plan-header">
                      <h3>{plan.label}</h3>
                      <div className="plan-price">
                        <span className="price">R$ {plan.price.toFixed(2)}</span>
                        {plan.price > 0 && <span className="period">/mês</span>}
                      </div>
                    </div>
                    <ul className="plan-features">
                      {plan.features.map((feature, index) => (
                        <li key={index}>✓ {feature}</li>
                      ))}
                    </ul>
                    {currentPlan !== plan.name && (
                      <button
                        className="upgrade-btn"
                        onClick={() => handleUpgrade(plan.name)}
                        disabled={processingPayment}
                      >
                        {processingPayment ? 'Processando...' : (currentPlan === 'FREE' ? 'Assinar' : 'Atualizar')}
                      </button>
                    )}
                    {currentPlan === plan.name && (
                      <div className="current-badge">Plano Atual</div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </>
        )}

        {isAdmin && (
          <div className="profile-card">
            <h2>Status de Administrador</h2>
            <div className="subscription-info">
              <div className="subscription-status">
                <span className="status-badge active">
                  ⚙️ Administrador do Sistema
                </span>
                <p className="end-date">
                  Acesso total ao painel administrativo
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Profile;

