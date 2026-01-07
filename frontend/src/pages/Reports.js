import React, { useState, useEffect } from 'react';
import { format, startOfMonth, endOfMonth, subMonths, addMonths } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { Doughnut } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend
} from 'chart.js';
import jsPDF from 'jspdf';
import 'jspdf-autotable';
import * as XLSX from 'xlsx';
import api from '../services/api';
import { toast } from 'react-toastify';
import './Reports.css';

ChartJS.register(ArcElement, Tooltip, Legend);

const Reports = () => {
  const [selectedMonth, setSelectedMonth] = useState(new Date());
  const [transactions, setTransactions] = useState([]);
  const [categoryStats, setCategoryStats] = useState([]);
  const [loading, setLoading] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      // Usar o primeiro dia do mês selecionado para garantir compatibilidade com o backend
      const monthStart = startOfMonth(selectedMonth);
      const monthDate = format(monthStart, 'yyyy-MM-dd');
      
      console.log('Loading data for month:', monthDate);
      
      const [transactionsRes, statsRes] = await Promise.all([
        api.get('/transactions/monthly', { params: { month: monthDate } }),
        api.get('/transactions/monthly/category-stats', { params: { month: monthDate } })
      ]);

      setTransactions(transactionsRes.data);
      setCategoryStats(statsRes.data);
    } catch (error) {
      console.error('Erro ao carregar dados:', error);
      toast.error('Erro ao carregar relatórios');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [selectedMonth]);

  const handlePreviousMonth = () => {
    setSelectedMonth(subMonths(selectedMonth, 1));
  };

  const handleNextMonth = () => {
    setSelectedMonth(addMonths(selectedMonth, 1));
  };

  const handleCurrentMonth = () => {
    setSelectedMonth(new Date());
  };

  const isCurrentMonth = format(selectedMonth, 'yyyy-MM') === format(new Date(), 'yyyy-MM');

  // Função para exportar PDF
  const handleExportPDF = () => {
    try {
      const doc = new jsPDF();
      const monthName = format(selectedMonth, 'MMMM yyyy', { locale: ptBR });
      
      // Título
      doc.setFontSize(18);
      doc.text('Relatório Financeiro Mensal', 14, 20);
      doc.setFontSize(12);
      doc.text(`Período: ${monthName}`, 14, 28);
      
      let yPos = 40;
      
      // Resumo
      doc.setFontSize(14);
      doc.text('Resumo Financeiro', 14, yPos);
      yPos += 10;
      
      doc.setFontSize(11);
      doc.text(`Receitas: R$ ${incomeTotal.toFixed(2).replace('.', ',')}`, 14, yPos);
      yPos += 7;
      doc.text(`Despesas: R$ ${expenseTotal.toFixed(2).replace('.', ',')}`, 14, yPos);
      yPos += 7;
      doc.text(`Saldo: R$ ${balance.toFixed(2).replace('.', ',')}`, 14, yPos);
      yPos += 15;
      
      // Tabela de transações
      if (transactions.length > 0) {
        const tableData = transactions.map(t => [
          t.dueDate || t.transactionDate 
            ? format(new Date(t.dueDate || t.transactionDate), 'dd/MM/yyyy', { locale: ptBR })
            : '-',
          t.description || '-',
          t.category ? t.category.name : 'Sem categoria',
          t.type === 'INCOME' ? 'Receita' : 'Despesa',
          `R$ ${parseFloat(t.amount || 0).toFixed(2).replace('.', ',')}`,
          t.isPaid ? 'Pago' : 'Pendente'
        ]);
        
        doc.autoTable({
          startY: yPos,
          head: [['Data', 'Descrição', 'Categoria', 'Tipo', 'Valor', 'Status']],
          body: tableData,
          theme: 'striped',
          headStyles: { fillColor: [99, 102, 241] },
          styles: { fontSize: 9 },
          margin: { left: 14, right: 14 }
        });
        
        yPos = doc.lastAutoTable.finalY + 15;
      }
      
      // Estatísticas por categoria
      if (categoryStats.length > 0) {
        doc.setFontSize(14);
        doc.text('Gastos por Categoria', 14, yPos);
        yPos += 10;
        
        const categoryData = categoryStats.map(cat => [
          cat.categoryName || 'Sem categoria',
          `R$ ${parseFloat(cat.totalAmount || 0).toFixed(2).replace('.', ',')}`,
          `${cat.transactionCount || 0} transações`
        ]);
        
        doc.autoTable({
          startY: yPos,
          head: [['Categoria', 'Total Gasto', 'Transações']],
          body: categoryData,
          theme: 'striped',
          headStyles: { fillColor: [16, 185, 129] },
          styles: { fontSize: 9 },
          margin: { left: 14, right: 14 }
        });
      }
      
      // Rodapé
      const pageCount = doc.internal.getNumberOfPages();
      for (let i = 1; i <= pageCount; i++) {
        doc.setPage(i);
        doc.setFontSize(8);
        doc.text(
          `Página ${i} de ${pageCount} - Gerado em ${format(new Date(), 'dd/MM/yyyy HH:mm', { locale: ptBR })}`,
          14,
          doc.internal.pageSize.height - 10
        );
      }
      
      const fileName = `relatorio-financeiro-${format(selectedMonth, 'yyyy-MM', { locale: ptBR })}.pdf`;
      doc.save(fileName);
      toast.success('Relatório PDF exportado com sucesso!');
    } catch (error) {
      console.error('Erro ao exportar PDF:', error);
      toast.error('Erro ao exportar PDF');
    }
  };

  // Função para exportar Excel
  const handleExportExcel = () => {
    try {
      const monthName = format(selectedMonth, 'MMMM yyyy', { locale: ptBR });
      
      // Criar workbook
      const wb = XLSX.utils.book_new();
      
      // Planilha 1: Resumo
      const summaryData = [
        ['Relatório Financeiro Mensal'],
        [`Período: ${monthName}`],
        [],
        ['Resumo Financeiro'],
        ['Receitas', `R$ ${incomeTotal.toFixed(2).replace('.', ',')}`],
        ['Despesas', `R$ ${expenseTotal.toFixed(2).replace('.', ',')}`],
        ['Saldo', `R$ ${balance.toFixed(2).replace('.', ',')}`],
      ];
      
      const ws1 = XLSX.utils.aoa_to_sheet(summaryData);
      XLSX.utils.book_append_sheet(wb, ws1, 'Resumo');
      
      // Planilha 2: Transações
      if (transactions.length > 0) {
        const transactionsData = [
          ['Data', 'Descrição', 'Categoria', 'Tipo', 'Valor', 'Status']
        ];
        
        transactions.forEach(t => {
          transactionsData.push([
            t.dueDate || t.transactionDate 
              ? format(new Date(t.dueDate || t.transactionDate), 'dd/MM/yyyy', { locale: ptBR })
              : '-',
            t.description || '-',
            t.category ? `${t.category.name}` : 'Sem categoria',
            t.type === 'INCOME' ? 'Receita' : 'Despesa',
            parseFloat(t.amount || 0),
            t.isPaid ? 'Pago' : 'Pendente'
          ]);
        });
        
        const ws2 = XLSX.utils.aoa_to_sheet(transactionsData);
        XLSX.utils.book_append_sheet(wb, ws2, 'Transações');
      }
      
      // Planilha 3: Categorias
      if (categoryStats.length > 0) {
        const categoriesData = [
          ['Categoria', 'Total Gasto', 'Transações', 'Percentual']
        ];
        
        categoryStats.forEach(cat => {
          const percentage = expenseTotal > 0 
            ? ((parseFloat(cat.totalAmount) / expenseTotal) * 100).toFixed(2)
            : 0;
          categoriesData.push([
            cat.categoryName || 'Sem categoria',
            parseFloat(cat.totalAmount || 0),
            cat.transactionCount || 0,
            `${percentage}%`
          ]);
        });
        
        const ws3 = XLSX.utils.aoa_to_sheet(categoriesData);
        XLSX.utils.book_append_sheet(wb, ws3, 'Categorias');
      }
      
      const fileName = `relatorio-financeiro-${format(selectedMonth, 'yyyy-MM', { locale: ptBR })}.xlsx`;
      XLSX.writeFile(wb, fileName);
      toast.success('Relatório Excel exportado com sucesso!');
    } catch (error) {
      console.error('Erro ao exportar Excel:', error);
      toast.error('Erro ao exportar Excel');
    }
  };

  // Calcular totais
  const incomeTotal = transactions
    .filter(t => t.type === 'INCOME' && t.isPaid === true)
    .reduce((sum, t) => sum + parseFloat(t.amount || 0), 0);

  const expenseTotal = transactions
    .filter(t => t.type === 'EXPENSE' && t.isPaid === true)
    .reduce((sum, t) => sum + parseFloat(t.amount || 0), 0);

  const balance = incomeTotal - expenseTotal;

  // Preparar dados para o gráfico
  const chartData = {
    labels: categoryStats.map(cat => cat.categoryName),
    datasets: [{
      label: 'Gastos por Categoria',
      data: categoryStats.map(cat => parseFloat(cat.totalAmount || 0)),
      backgroundColor: categoryStats.map(cat => cat.categoryColor || '#6c757d'),
      borderColor: '#fff',
      borderWidth: 2
    }]
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          padding: 15,
          font: {
            size: 12
          }
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const label = context.label || '';
            const value = context.parsed || 0;
            const total = context.dataset.data.reduce((a, b) => a + b, 0);
            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
            return `${label}: R$ ${value.toFixed(2).replace('.', ',')} (${percentage}%)`;
          }
        }
      }
    }
  };

  return (
    <div className="reports">
      <div className="reports-header">
        <h1>📊 Relatórios Mensais</h1>
        <div className="header-actions">
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
          <div className="export-buttons">
            <button onClick={handleExportPDF} className="btn-export btn-export-pdf" title="Exportar PDF">
              📄 PDF
            </button>
            <button onClick={handleExportExcel} className="btn-export btn-export-excel" title="Exportar Excel">
              📊 Excel
            </button>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="loading">Carregando...</div>
      ) : (
        <>
          {/* Cards de Resumo */}
          <div className="summary-cards">
            <div className="summary-card income">
              <div className="card-icon">💰</div>
              <div className="card-content">
                <h3>Receitas</h3>
                <p className="card-value">R$ {incomeTotal.toFixed(2).replace('.', ',')}</p>
              </div>
            </div>
            <div className="summary-card expense">
              <div className="card-icon">💸</div>
              <div className="card-content">
                <h3>Despesas</h3>
                <p className="card-value">R$ {expenseTotal.toFixed(2).replace('.', ',')}</p>
              </div>
            </div>
            <div className={`summary-card balance ${balance >= 0 ? 'positive' : 'negative'}`}>
              <div className="card-icon">{balance >= 0 ? '✅' : '⚠️'}</div>
              <div className="card-content">
                <h3>Saldo</h3>
                <p className="card-value">R$ {balance.toFixed(2).replace('.', ',')}</p>
              </div>
            </div>
          </div>

          <div className="reports-content">
            {/* Gráfico de Categorias */}
            <div className="chart-section">
              <h2>Gastos por Categoria</h2>
              {categoryStats.length > 0 ? (
                <div className="chart-container">
                  <Doughnut data={chartData} options={chartOptions} />
                </div>
              ) : (
                <div className="no-data">Nenhuma despesa paga neste mês</div>
              )}
            </div>

            {/* Lista de Categorias */}
            <div className="categories-section">
              <h2>Top Categorias</h2>
              {categoryStats.length > 0 ? (
                <div className="categories-list">
                  {categoryStats.map((cat, index) => {
                    const percentage = expenseTotal > 0 
                      ? ((parseFloat(cat.totalAmount) / expenseTotal) * 100).toFixed(1) 
                      : 0;
                    return (
                      <div key={cat.categoryId} className="category-item">
                        <div className="category-rank">#{index + 1}</div>
                        <div className="category-icon" style={{ backgroundColor: cat.categoryColor || '#6c757d' }}>
                          {cat.categoryIcon || '📁'}
                        </div>
                        <div className="category-info">
                          <h4>{cat.categoryName}</h4>
                          <p>{cat.transactionCount} {cat.transactionCount === 1 ? 'transação' : 'transações'}</p>
                        </div>
                        <div className="category-amount">
                          <strong>R$ {parseFloat(cat.totalAmount).toFixed(2).replace('.', ',')}</strong>
                          <span className="percentage">{percentage}%</span>
                        </div>
                        <div className="category-bar">
                          <div 
                            className="category-bar-fill" 
                            style={{ 
                              width: `${percentage}%`,
                              backgroundColor: cat.categoryColor || '#6c757d'
                            }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="no-data">Nenhuma categoria com gastos neste mês</div>
              )}
            </div>

            {/* Lista de Transações */}
            <div className="transactions-section">
              <h2>Transações do Mês ({transactions.length})</h2>
              {transactions.length > 0 ? (
                <div className="transactions-list">
                  <table>
                    <thead>
                      <tr>
                        <th>Data</th>
                        <th>Descrição</th>
                        <th>Categoria</th>
                        <th>Tipo</th>
                        <th>Valor</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {transactions.map(transaction => (
                        <tr key={transaction.id}>
                          <td>
                            {transaction.dueDate 
                              ? format(new Date(transaction.dueDate), 'dd/MM/yyyy', { locale: ptBR })
                              : transaction.transactionDate 
                              ? format(new Date(transaction.transactionDate), 'dd/MM/yyyy', { locale: ptBR })
                              : '-'
                            }
                          </td>
                          <td>{transaction.description}</td>
                          <td>
                            {transaction.category ? (
                              <span className="category-badge" style={{ backgroundColor: transaction.category.color }}>
                                {transaction.category.icon} {transaction.category.name}
                              </span>
                            ) : (
                              <span className="no-category">Sem categoria</span>
                            )}
                          </td>
                          <td>
                            <span className={`type-badge ${transaction.type.toLowerCase()}`}>
                              {transaction.type === 'INCOME' ? '💰 Receita' : '💸 Despesa'}
                            </span>
                          </td>
                          <td className={transaction.type === 'INCOME' ? 'income-value' : 'expense-value'}>
                            {transaction.type === 'INCOME' ? '+' : '-'} R$ {parseFloat(transaction.amount || 0).toFixed(2).replace('.', ',')}
                          </td>
                          <td>
                            <span className={`status-badge ${transaction.isPaid ? 'paid' : 'pending'}`}>
                              {transaction.isPaid ? '✅ Pago' : '⏳ Pendente'}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="no-data">Nenhuma transação neste mês</div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default Reports;

