import React, { useState } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format, subDays, startOfMonth, endOfMonth } from 'date-fns';
import './FinancialAnalysis.css';

const FinancialAnalysis = () => {
  const [showAnalysis, setShowAnalysis] = useState(false);
  const [loading, setLoading] = useState(false);
  const [analysis, setAnalysis] = useState(null);
  const [period, setPeriod] = useState('30'); // 30, 60, 90, ou 'custom'

  const handleGenerateAnalysis = async () => {
    setLoading(true);
    try {
      let startDate, endDate;
      
      switch (period) {
        case '30':
          startDate = format(subDays(new Date(), 30), 'yyyy-MM-dd');
          endDate = format(new Date(), 'yyyy-MM-dd');
          break;
        case '60':
          startDate = format(subDays(new Date(), 60), 'yyyy-MM-dd');
          endDate = format(new Date(), 'yyyy-MM-dd');
          break;
        case '90':
          startDate = format(subDays(new Date(), 90), 'yyyy-MM-dd');
          endDate = format(new Date(), 'yyyy-MM-dd');
          break;
        case 'month':
          startDate = format(startOfMonth(new Date()), 'yyyy-MM-dd');
          endDate = format(endOfMonth(new Date()), 'yyyy-MM-dd');
          break;
        default:
          startDate = format(subDays(new Date(), 30), 'yyyy-MM-dd');
          endDate = format(new Date(), 'yyyy-MM-dd');
      }

      const response = await api.get('/transactions/ai/analysis', {
        params: { startDate, endDate }
      });
      
      setAnalysis(response.data);
      setShowAnalysis(true);
    } catch (error) {
      console.error('Erro ao gerar análise:', error);
      toast.error('Erro ao gerar análise financeira. ' + (error.response?.data?.message || ''));
    } finally {
      setLoading(false);
    }
  };

  if (!showAnalysis) {
    return (
      <div className="financial-analysis-trigger">
        <button 
          className="analysis-trigger-btn" 
          onClick={handleGenerateAnalysis}
          disabled={loading}
          title="Análise Financeira com IA"
        >
          <span className="analysis-icon">📊</span>
          <span>{loading ? 'Analisando...' : 'Análise Financeira com IA'}</span>
        </button>
        <div className="period-selector">
          <label>Período:</label>
          <select 
            value={period} 
            onChange={(e) => setPeriod(e.target.value)}
            disabled={loading}
          >
            <option value="30">Últimos 30 dias</option>
            <option value="60">Últimos 60 dias</option>
            <option value="90">Últimos 90 dias</option>
            <option value="month">Mês atual</option>
          </select>
        </div>
      </div>
    );
  }

  return (
    <div className="financial-analysis">
      <div className="analysis-header">
        <h3>📊 Análise Financeira com IA</h3>
        <button 
          className="analysis-close-btn" 
          onClick={() => {
            setShowAnalysis(false);
            setAnalysis(null);
          }}
        >
          ×
        </button>
      </div>

      {analysis && (
        <div className="analysis-content">
          {/* Resumo Executivo */}
          {analysis.summary && (
            <div className="analysis-section summary-section">
              <h4>📋 Resumo Executivo</h4>
              <p>{analysis.summary}</p>
            </div>
          )}

          {/* Potencial de Economia */}
          {analysis.potentialSavings && analysis.potentialSavings > 0 && (
            <div className="analysis-section savings-section">
              <h4>💰 Potencial de Economia</h4>
              <div className="savings-amount">
                R$ {analysis.potentialSavings.toFixed(2)}
              </div>
              <p>Seguindo as recomendações abaixo, você pode economizar até este valor.</p>
            </div>
          )}

          {/* Análise Completa */}
          {analysis.analysis && (
            <div className="analysis-section full-analysis">
              <h4>📈 Análise Detalhada</h4>
              <div className="analysis-text">
                {analysis.analysis.split('\n').map((line, index) => (
                  <React.Fragment key={index}>
                    {line}
                    {index < analysis.analysis.split('\n').length - 1 && <br />}
                  </React.Fragment>
                ))}
              </div>
            </div>
          )}

          {/* Recomendações */}
          {analysis.recommendations && analysis.recommendations.length > 0 && (
            <div className="analysis-section recommendations-section">
              <h4>💡 Recomendações</h4>
              <div className="recommendations-list">
                {analysis.recommendations.map((rec, index) => (
                  <div key={index} className="recommendation-card">
                    <div className="recommendation-header">
                      <span className="recommendation-number">{index + 1}</span>
                      <span className="recommendation-category">{rec.category}</span>
                      {rec.estimatedSavings > 0 && (
                        <span className="recommendation-savings">
                          💰 R$ {rec.estimatedSavings.toFixed(2)}
                        </span>
                      )}
                    </div>
                    <div className="recommendation-suggestion">
                      <strong>Sugestão:</strong> {rec.suggestion}
                    </div>
                    {rec.impact && (
                      <div className="recommendation-impact">
                        <strong>Impacto:</strong> {rec.impact}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className="analysis-actions">
            <button 
              className="analysis-back-btn"
              onClick={() => {
                setShowAnalysis(false);
                setAnalysis(null);
              }}
            >
              Fechar
            </button>
            <button 
              className="analysis-refresh-btn"
              onClick={handleGenerateAnalysis}
              disabled={loading}
            >
              {loading ? 'Atualizando...' : '🔄 Atualizar Análise'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default FinancialAnalysis;









