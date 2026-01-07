import React, { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format, startOfMonth, endOfMonth, addMonths, subMonths } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { Line, Doughnut } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import AiTransactionInput from '../components/AiTransactionInput';
import FinancialAnalysis from '../components/FinancialAnalysis';
import ExecutiveDashboard from '../components/ExecutiveDashboard';
import './Dashboard.css';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
);

const Dashboard = () => {
  const { user } = useAuth();
  const [transactions, setTransactions] = useState([]);
  const [balance, setBalance] = useState(0);
  const [income, setIncome] = useState(0);
  const [expense, setExpense] = useState(0);
  const [pendingExpenses, setPendingExpenses] = useState(0);
  const [loading, setLoading] = useState(true);
  const [budgetAlerts, setBudgetAlerts] = useState([]);
  const [upcomingTransactions, setUpcomingTransactions] = useState([]);
  const [overdueTransactions, setOverdueTransactions] = useState([]);
  const [notifiedTransactions, setNotifiedTransactions] = useState(new Set());
  const [selectedMonth, setSelectedMonth] = useState(new Date());
  const [viewMode, setViewMode] = useState('normal'); // 'normal' ou 'executive'

  useEffect(() => {
    loadData();
    loadBudgetAlerts();
    loadTransactionAlerts(true); // Primeira vez mostra notificações
    // Verificar alertas a cada 30 segundos (sem mostrar notificações)
    const interval = setInterval(() => {
      loadBudgetAlerts();
      loadTransactionAlerts(false); // Atualiza mas não mostra notificações
    }, 30000);
    return () => clearInterval(interval);
  }, [selectedMonth]);
  
  const loadBudgetAlerts = async () => {
    try {
      const response = await api.get('/budgets/alerts');
      setBudgetAlerts(response.data);
    } catch (error) {
      // Silencioso
    }
  };
  
  const loadTransactionAlerts = async (showNotifications = false) => {
    try {
      const [upcomingRes, overdueRes] = await Promise.all([
        api.get('/transactions/upcoming'),
        api.get('/transactions/overdue'),
      ]);
      setUpcomingTransactions(upcomingRes.data);
      setOverdueTransactions(overdueRes.data);
      
      // Mostrar notificações apenas na primeira vez
      if (showNotifications) {
        // Mostrar notificações para transações atrasadas (apenas uma vez)
        overdueRes.data.forEach(transaction => {
          const transactionKey = `overdue-${transaction.id}`;
          if (!notifiedTransactions.has(transactionKey)) {
            toast.error(
              `🚨 ATRASADO! ${transaction.description} - R$ ${parseFloat(transaction.amount).toFixed(2)} venceu em ${format(new Date(transaction.dueDate), 'dd/MM/yyyy', { locale: ptBR })}`,
              { autoClose: 8000 }
            );
            setNotifiedTransactions(prev => new Set([...prev, transactionKey]));
          }
        });
        
        // Mostrar notificações para transações próximas (hoje) - apenas uma vez
        upcomingRes.data.forEach(transaction => {
          if (transaction.daysUntilDue === 0) {
            const transactionKey = `upcoming-${transaction.id}`;
            if (!notifiedTransactions.has(transactionKey)) {
              toast.warning(
                `⚠️ Vence hoje! ${transaction.description} - R$ ${parseFloat(transaction.amount).toFixed(2)}`,
                { autoClose: 6000 }
              );
              setNotifiedTransactions(prev => new Set([...prev, transactionKey]));
            }
          }
        });
      }
    } catch (error) {
      // Silencioso
    }
  };

  const loadData = async () => {
    try {
      setLoading(true); // Mostrar loading ao mudar de mês
      
      const startDate = format(startOfMonth(selectedMonth), 'yyyy-MM-dd');
      const endDate = format(endOfMonth(selectedMonth), 'yyyy-MM-dd');
      
      const [transactionsRes, balanceRes] = await Promise.all([
        api.get('/transactions'),
        api.get('/transactions/balance', {
          params: {
            startDate: startDate,
            endDate: endDate,
          },
        }),
      ]);

      // Filtrar transações pelo mês selecionado
      const monthStart = startOfMonth(selectedMonth);
      const monthEnd = endOfMonth(selectedMonth);
      
      const filteredTransactions = transactionsRes.data.filter(t => {
        if (!t.dueDate && !t.transactionDate) return false;
        const transactionDate = t.dueDate ? new Date(t.dueDate) : new Date(t.transactionDate);
        return transactionDate >= monthStart && transactionDate <= monthEnd;
      });

      setTransactions(filteredTransactions);
      
      // Usar o saldo do backend (que já filtra corretamente pelo mês)
      setBalance(balanceRes.data);

      // Filtrar transações principais (não parcelas individuais) para os cálculos de receitas/despesas pagas
      // IMPORTANTE: Não contar transações pai parceladas (isInstallment = true E parentTransactionId = null)
      // Essas transações têm valor zero e são apenas ilustrativas
      const mainTransactions = filteredTransactions.filter(t => {
        // Excluir parcelas individuais (parentTransactionId != null)
        if (t.parentTransactionId) return false;
        // Excluir transações pai parceladas (isInstallment = true E parentTransactionId = null)
        // Essas têm valor zero e não devem ser contadas
        if (t.isInstallment === true && !t.parentTransactionId) return false;
        // Incluir apenas transações simples
        return true;
      });

      // Calcular receitas e despesas pagas (apenas para exibição nos cards)
      // IMPORTANTE: Apenas contar se isPaid for explicitamente true (não null, não false)
      const incomeTotal = mainTransactions
        .filter((t) => {
          return t.type === 'INCOME' && t.isPaid === true; // Apenas true explícito
        })
        .reduce((sum, t) => sum + parseFloat(t.amount || 0), 0);
      const expenseTotal = mainTransactions
        .filter((t) => {
          return t.type === 'EXPENSE' && t.isPaid === true; // Apenas true explícito
        })
        .reduce((sum, t) => sum + parseFloat(t.amount || 0), 0);
      
      // Calcular despesas pendentes (não pagas) - IMPORTANTE: usar TODAS as transações, incluindo parcelas individuais
      // Para meses futuros, precisamos buscar também as parcelas individuais que têm vencimento naquele mês
      let finalPendingExpenses = 0;
      
      try {
        // Buscar TODAS as transações (incluindo parcelas individuais) para calcular despesas pendentes corretamente
        const allTransactionsResponse = await api.get('/transactions/all');
        const allTransactions = allTransactionsResponse.data || [];
        
        // Filtrar transações do mês selecionado que são despesas pendentes
        // IMPORTANTE: Não contar transações pai parceladas (valor zero e ilustrativas)
        const monthPendingExpenses = allTransactions
          .filter(t => {
            // Deve ter data de vencimento
            if (!t.dueDate) return false;
            const transactionDate = new Date(t.dueDate);
            // Deve estar no mês selecionado
            return transactionDate >= monthStart && transactionDate <= monthEnd;
          })
          .filter(t => {
            // NÃO contar transações pai parceladas (isInstallment = true E parentTransactionId = null)
            // Essas têm valor zero e são apenas ilustrativas
            if (t.isInstallment === true && !t.parentTransactionId) return false;
            // Deve ser despesa e estar pendente
            return t.type === 'EXPENSE' && t.isPaid !== true;
          })
          .reduce((sum, t) => sum + parseFloat(t.amount || 0), 0);
        
        finalPendingExpenses = monthPendingExpenses;
        setPendingExpenses(finalPendingExpenses);
      } catch (error) {
        // Fallback: usar apenas as transações principais (não ideial, mas melhor que nada)
        console.error('Erro ao buscar todas as transações:', error);
        const pendingExpensesTotal = filteredTransactions
          .filter((t) => {
            // NÃO contar transações pai parceladas (isInstallment = true E parentTransactionId = null)
            if (t.isInstallment === true && !t.parentTransactionId) return false;
            // Deve ser despesa e estar pendente
            return t.type === 'EXPENSE' && t.isPaid !== true;
          })
          .reduce((sum, t) => sum + parseFloat(t.amount || 0), 0);
        setPendingExpenses(pendingExpensesTotal);
      }

      setIncome(incomeTotal);
      setExpense(expenseTotal);
    } catch (error) {
      toast.error('Erro ao carregar dados');
      console.error('Erro ao carregar dados do dashboard:', error);
    } finally {
      setLoading(false);
    }
  };

  const recentTransactions = useMemo(() => transactions.slice(0, 5), [transactions]);

  // Preparar dados do gráfico apenas com transações pagas dos últimos 7 dias
  const last7DaysTransactions = useMemo(() => {
    return transactions
      .filter(t => t.isPaid) // Apenas transações pagas
      .sort((a, b) => new Date(a.transactionDate) - new Date(b.transactionDate))
      .slice(-7);
  }, [transactions]);

  const lineData = useMemo(() => ({
    labels: last7DaysTransactions.length > 0 
      ? last7DaysTransactions.map((t) => format(new Date(t.transactionDate), 'dd/MM', { locale: ptBR }))
      : ['N/A'],
    datasets: [
      {
        label: 'Saldo',
        data: last7DaysTransactions.length > 0
          ? last7DaysTransactions.map((t) =>
              t.type === 'INCOME' ? parseFloat(t.amount) : -parseFloat(t.amount)
            )
          : [0],
        borderColor: 'rgb(99, 102, 241)',
        backgroundColor: 'rgba(99, 102, 241, 0.1)',
        tension: 0.4,
      },
    ],
  }), [last7DaysTransactions]);

  const doughnutData = useMemo(() => ({
    labels: ['Receitas', 'Despesas'],
    datasets: [
      {
        data: [income, expense],
        backgroundColor: ['rgba(16, 185, 129, 0.8)', 'rgba(239, 68, 68, 0.8)'],
        borderColor: ['rgb(16, 185, 129)', 'rgb(239, 68, 68)'],
        borderWidth: 2,
      },
    ],
  }), [income, expense]);
  
  const chartOptions = useMemo(() => ({
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    plugins: {
      legend: {
        display: true,
      },
    },
  }), []);

  // Admin não tem subscription
  const isAdmin = (user?.role || '').toUpperCase().trim() === 'ADMIN';
  const subscriptionStatus = isAdmin 
    ? 'Administrador'
    : (user?.subscription?.isActive
      ? `Ativa até ${format(new Date(user.subscription.endDate), 'dd/MM/yyyy', { locale: ptBR })}`
      : 'Expirada');

  const handlePreviousMonth = () => {
    setSelectedMonth(subMonths(selectedMonth, 1));
  };

  const handleNextMonth = () => {
    setSelectedMonth(addMonths(selectedMonth, 1));
  };

  const handleCurrentMonth = () => {
    setSelectedMonth(new Date());
  };

  // Se estiver no modo executivo, mostrar o dashboard executivo
  if (viewMode === 'executive') {
    return (
      <div className="dashboard-page">
        <div className="dashboard-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h1>Dashboard</h1>
          <button 
            className="btn-secondary" 
            onClick={() => setViewMode('normal')}
            style={{ padding: '0.5rem 1rem' }}
          >
            📊 Modo Normal
          </button>
        </div>
        <ExecutiveDashboard />
      </div>
    );
  }

  if (loading) {
    return <div className="loading">Carregando...</div>;
  }

  return (
    <div className="dashboard">
      <AiTransactionInput onSuccess={loadData} />
      <FinancialAnalysis />
      <div className="dashboard-header">
        <div>
          <h1>Dashboard</h1>
          <p>Visão geral das suas finanças</p>
        </div>
        <div className="dashboard-header-actions">
          <button 
            className="btn-primary" 
            onClick={() => setViewMode('executive')}
            style={{ padding: '0.5rem 1rem', marginRight: '1rem' }}
          >
            📊 Dashboard Executivo
          </button>
          <div className="month-selector">
            <button onClick={handlePreviousMonth} className="month-nav-btn" title="Mês anterior">
              ←
            </button>
            <button onClick={handleCurrentMonth} className="month-current-btn" title="Ir para o mês atual">
              {format(selectedMonth, 'MMM/yyyy', { locale: ptBR })}
            </button>
            <button onClick={handleNextMonth} className="month-nav-btn" title="Próximo mês">
              →
            </button>
          </div>
          <div className="subscription-badge">
            <span>📅 {subscriptionStatus}</span>
          </div>
        </div>
      </div>

      {(budgetAlerts.length > 0 || overdueTransactions.length > 0 || upcomingTransactions.length > 0) && (
        <div className="dashboard-alerts-section">
          {overdueTransactions.length > 0 && (
            <div className="dashboard-alerts overdue-alerts">
              <h3>🚨 Transações Atrasadas</h3>
              <div className="alerts-grid">
                {overdueTransactions.map((transaction) => (
                  <div key={transaction.id} className="alert-badge overdue">
                    <span className="alert-icon">🚨</span>
                    <div>
                      <strong>{transaction.description}</strong>
                      <p>Venceu em {format(new Date(transaction.dueDate), 'dd/MM/yyyy', { locale: ptBR })} - R$ {parseFloat(transaction.amount).toFixed(2)}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
          
          {upcomingTransactions.length > 0 && (
            <div className="dashboard-alerts upcoming-alerts">
              <h3>⚠️ Próximos Vencimentos (próximos 5 dias)</h3>
              <div className="alerts-grid">
                {upcomingTransactions.map((transaction) => (
                  <div key={transaction.id} className={`alert-badge ${transaction.daysUntilDue === 0 ? 'urgent' : ''}`}>
                    <span className="alert-icon">⚠️</span>
                    <div>
                      <strong>{transaction.description}</strong>
                      <p>
                        Vence em {format(new Date(transaction.dueDate), 'dd/MM/yyyy', { locale: ptBR })} 
                        {transaction.daysUntilDue !== undefined && (
                          <> ({transaction.daysUntilDue === 0 ? 'HOJE' : `${transaction.daysUntilDue} dias`})</>
                        )}
                        {' '}- R$ {parseFloat(transaction.amount).toFixed(2)}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
          
          {budgetAlerts.length > 0 && (
            <div className="dashboard-alerts budget-alerts">
              <h3>⚠️ Alertas de Orçamento</h3>
              <div className="alerts-grid">
                {budgetAlerts.map((alert) => (
                  <div key={alert.id} className="alert-badge">
                    <span className="alert-icon">⚠️</span>
                    <div>
                      <strong>{alert.name}</strong>
                      <p>{alert.percentageUsed?.toFixed(0)}% usado - R$ {parseFloat(alert.currentSpent).toFixed(2)} / R$ {parseFloat(alert.limitAmount).toFixed(2)}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      <div className="stats-grid">
        <div className="stat-card income">
          <div className="stat-icon">💰</div>
          <div className="stat-content">
            <p className="stat-label">Receitas (Pagas)</p>
            <h2 className="stat-value">R$ {income.toFixed(2)}</h2>
          </div>
        </div>
        <div className="stat-card expense">
          <div className="stat-icon">💸</div>
          <div className="stat-content">
            <p className="stat-label">Despesas (Pagas)</p>
            <h2 className="stat-value">R$ {expense.toFixed(2)}</h2>
          </div>
        </div>
        <div className="stat-card balance">
          <div className="stat-icon">💵</div>
          <div className="stat-content">
            <p className="stat-label">Saldo</p>
            <h2 className="stat-value">R$ {balance.toFixed(2)}</h2>
            <small className="stat-subtitle">Apenas transações pagas/recebidas</small>
          </div>
        </div>
        <div className="stat-card pending">
          <div className="stat-icon">⏳</div>
          <div className="stat-content">
            <p className="stat-label">Despesas Pendentes</p>
            <h2 className="stat-value">R$ {pendingExpenses.toFixed(2)}</h2>
            <small className="stat-subtitle">Ainda não pagas</small>
          </div>
        </div>
      </div>

      <div className="charts-grid">
        <div className="chart-card">
          <h3>Últimos 7 dias</h3>
          <div className="chart-container">
            <Line data={lineData} options={chartOptions} />
          </div>
        </div>
        <div className="chart-card">
          <h3>Receitas vs Despesas</h3>
          <div className="chart-container">
            <Doughnut data={doughnutData} options={chartOptions} />
          </div>
        </div>
      </div>

      <div className="recent-transactions">
        <h3>Transações Recentes</h3>
        <div className="transactions-list">
          {recentTransactions.length === 0 ? (
            <p className="empty-state">Nenhuma transação encontrada</p>
          ) : (
            recentTransactions.map((transaction) => (
              <div key={transaction.id} className="transaction-item">
                <div className="transaction-icon">
                  {transaction.category?.icon || '📝'}
                </div>
                <div className="transaction-details">
                  <p className="transaction-description">{transaction.description}</p>
                  <p className="transaction-date">
                    {format(new Date(transaction.transactionDate), 'dd/MM/yyyy', { locale: ptBR })}
                  </p>
                </div>
                <div className={`transaction-amount ${transaction.type.toLowerCase()}`}>
                  {transaction.type === 'INCOME' ? '+' : '-'} R$ {parseFloat(transaction.amount).toFixed(2)}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;

