import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { Line, Bar } from 'react-chartjs-2';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './ExecutiveDashboard.css';

const ExecutiveDashboard = () => {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboard();
    const interval = setInterval(loadDashboard, 60000); // Atualizar a cada minuto
    return () => clearInterval(interval);
  }, []);

  const loadDashboard = async () => {
    try {
      const response = await api.get('/dashboard/executive');
      setDashboard(response.data);
    } catch (error) {
      toast.error('Erro ao carregar dashboard executivo');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  const formatPercent = (value) => {
    const num = parseFloat(value);
    const sign = num >= 0 ? '+' : '';
    return `${sign}${num.toFixed(1)}%`;
  };

  if (loading || !dashboard) {
    return <div className="executive-dashboard-loading">Carregando dashboard executivo...</div>;
  }

  // Dados para gráfico de tendências
  const trendsData = {
    labels: dashboard.monthlyTrends.map(t => {
      const [year, month] = t.month.split('-');
      return format(new Date(parseInt(year), parseInt(month) - 1), 'MMM/yyyy', { locale: ptBR });
    }),
    datasets: [
      {
        label: 'Receitas',
        data: dashboard.monthlyTrends.map(t => parseFloat(t.income)),
        borderColor: '#10b981',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        tension: 0.4,
      },
      {
        label: 'Despesas',
        data: dashboard.monthlyTrends.map(t => parseFloat(t.expense)),
        borderColor: '#ef4444',
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        tension: 0.4,
      },
      {
        label: 'Fluxo Líquido',
        data: dashboard.monthlyTrends.map(t => parseFloat(t.netFlow)),
        borderColor: '#6366f1',
        backgroundColor: 'rgba(99, 102, 241, 0.1)',
        tension: 0.4,
      },
    ],
  };

  // Dados para gráfico de categorias
  const categoriesData = {
    labels: dashboard.topCategories.map(c => c.categoryName),
    datasets: [
      {
        label: 'Mês Atual',
        data: dashboard.topCategories.map(c => parseFloat(c.currentMonth)),
        backgroundColor: '#6366f1',
      },
      {
        label: 'Mês Anterior',
        data: dashboard.topCategories.map(c => parseFloat(c.previousMonth)),
        backgroundColor: '#94a3b8',
      },
    ],
  };

  return (
    <div className="executive-dashboard">
      <div className="dashboard-header">
        <h1>📊 Dashboard Executivo</h1>
        <p>Visão completa e inteligente das suas finanças</p>
      </div>

      {/* KPIs Principais */}
      <div className="kpi-grid">
        <div className="kpi-card primary">
          <div className="kpi-icon">💰</div>
          <div className="kpi-content">
            <div className="kpi-label">Saldo Total</div>
            <div className="kpi-value">{formatCurrency(dashboard.totalBalance)}</div>
          </div>
        </div>
        <div className="kpi-card success">
          <div className="kpi-icon">📈</div>
          <div className="kpi-content">
            <div className="kpi-label">Receitas do Mês</div>
            <div className="kpi-value">{formatCurrency(dashboard.monthlyIncome)}</div>
            <div className="kpi-change positive">
              {formatPercent(dashboard.monthOverMonth.incomeChange)} vs mês anterior
            </div>
          </div>
        </div>
        <div className="kpi-card danger">
          <div className="kpi-icon">📉</div>
          <div className="kpi-content">
            <div className="kpi-label">Despesas do Mês</div>
            <div className="kpi-value">{formatCurrency(dashboard.monthlyExpense)}</div>
            <div className={`kpi-change ${dashboard.monthOverMonth.expenseChange >= 0 ? 'negative' : 'positive'}`}>
              {formatPercent(dashboard.monthOverMonth.expenseChange)} vs mês anterior
            </div>
          </div>
        </div>
        <div className="kpi-card info">
          <div className="kpi-icon">💵</div>
          <div className="kpi-content">
            <div className="kpi-label">Fluxo Líquido</div>
            <div className={`kpi-value ${dashboard.netFlow >= 0 ? 'positive' : 'negative'}`}>
              {formatCurrency(dashboard.netFlow)}
            </div>
            <div className="kpi-label">Taxa de Poupança: {parseFloat(dashboard.savingsRate).toFixed(1)}%</div>
          </div>
        </div>
      </div>

      {/* Comparações */}
      <div className="comparison-section">
        <h2>📊 Comparações</h2>
        <div className="comparison-grid">
          <div className="comparison-card">
            <h3>Mês a Mês</h3>
            <div className="comparison-item">
              <span>Receitas:</span>
              <span className={dashboard.monthOverMonth.incomeChange >= 0 ? 'positive' : 'negative'}>
                {formatPercent(dashboard.monthOverMonth.incomeChange)}
              </span>
            </div>
            <div className="comparison-item">
              <span>Despesas:</span>
              <span className={dashboard.monthOverMonth.expenseChange >= 0 ? 'negative' : 'positive'}>
                {formatPercent(dashboard.monthOverMonth.expenseChange)}
              </span>
            </div>
            <div className="comparison-item">
              <span>Fluxo Líquido:</span>
              <span className={dashboard.monthOverMonth.netFlowChange >= 0 ? 'positive' : 'negative'}>
                {formatPercent(dashboard.monthOverMonth.netFlowChange)}
              </span>
            </div>
          </div>
          <div className="comparison-card">
            <h3>Ano a Ano</h3>
            <div className="comparison-item">
              <span>Receitas:</span>
              <span className={dashboard.yearOverYear.incomeChange >= 0 ? 'positive' : 'negative'}>
                {formatPercent(dashboard.yearOverYear.incomeChange)}
              </span>
            </div>
            <div className="comparison-item">
              <span>Despesas:</span>
              <span className={dashboard.yearOverYear.expenseChange >= 0 ? 'negative' : 'positive'}>
                {formatPercent(dashboard.yearOverYear.expenseChange)}
              </span>
            </div>
            <div className="comparison-item">
              <span>Fluxo Líquido:</span>
              <span className={dashboard.yearOverYear.netFlowChange >= 0 ? 'positive' : 'negative'}>
                {formatPercent(dashboard.yearOverYear.netFlowChange)}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Gráficos */}
      <div className="charts-section">
        <div className="chart-card">
          <h3>Tendências (Últimos 12 Meses)</h3>
          <Line data={trendsData} options={{
            responsive: true,
            plugins: {
              legend: { position: 'top' },
              tooltip: {
                callbacks: {
                  label: (context) => formatCurrency(context.parsed.y)
                }
              }
            },
            scales: {
              y: {
                beginAtZero: true,
                ticks: {
                  callback: (value) => formatCurrency(value)
                }
              }
            }
          }} />
        </div>
        <div className="chart-card">
          <h3>Top 5 Categorias - Comparação Mensal</h3>
          <Bar data={categoriesData} options={{
            responsive: true,
            plugins: {
              legend: { position: 'top' },
              tooltip: {
                callbacks: {
                  label: (context) => formatCurrency(context.parsed.y)
                }
              }
            },
            scales: {
              y: {
                beginAtZero: true,
                ticks: {
                  callback: (value) => formatCurrency(value)
                }
              }
            }
          }} />
        </div>
      </div>

      {/* Anomalias */}
      {dashboard.anomalies && dashboard.anomalies.length > 0 && (
        <div className="anomalies-section">
          <h2>⚠️ Alertas e Anomalias Detectadas</h2>
          <div className="anomalies-list">
            {dashboard.anomalies.map((anomaly, idx) => (
              <div key={idx} className={`anomaly-card ${anomaly.severity.toLowerCase()}`}>
                <div className="anomaly-icon">
                  {anomaly.severity === 'HIGH' ? '🚨' : '⚠️'}
                </div>
                <div className="anomaly-content">
                  <div className="anomaly-title">{anomaly.description}</div>
                  {anomaly.amount && (
                    <div className="anomaly-amount">{formatCurrency(anomaly.amount)}</div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Resumo de Metas e Orçamentos */}
      <div className="summary-section">
        <div className="summary-card">
          <h3>🎯 Metas</h3>
          <div className="summary-stats">
            <div className="stat-item">
              <span className="stat-value">{dashboard.activeGoals}</span>
              <span className="stat-label">Ativas</span>
            </div>
            <div className="stat-item">
              <span className="stat-value">{dashboard.completedGoals}</span>
              <span className="stat-label">Concluídas</span>
            </div>
          </div>
        </div>
        <div className="summary-card">
          <h3>💰 Orçamentos</h3>
          <div className="summary-stats">
            <div className="stat-item">
              <span className="stat-value warning">{dashboard.budgetsAtRisk}</span>
              <span className="stat-label">Em Risco</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ExecutiveDashboard;




