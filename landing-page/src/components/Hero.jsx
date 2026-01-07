import React from 'react'
import { motion } from 'framer-motion'
import { FiArrowRight, FiPlay } from 'react-icons/fi'
import './Hero.css'

const Hero = () => {
  const scrollToCTA = () => {
    document.getElementById('cta').scrollIntoView({ behavior: 'smooth' })
  }

  return (
    <section className="hero">
      <div className="hero-container">
        <motion.div 
          className="hero-content"
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
        >
          <motion.div
            className="hero-badge"
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2, duration: 0.5 }}
          >
            <span className="badge-icon">✨</span>
            <span>30 dias grátis para testar</span>
          </motion.div>

          <h1 className="hero-title">
            Controle Financeiro
            <span className="gradient-text"> Inteligente</span>
            <br />com IA e Automação
          </h1>

          <p className="hero-description">
            Gerencie suas finanças de forma automática com categorização inteligente,
            análises preditivas e sugestões personalizadas. Tome decisões melhores
            com dados em tempo real.
          </p>

          <div className="hero-actions">
            <motion.button
              className="btn-primary btn-large"
              onClick={scrollToCTA}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              Começar Grátis
              <FiArrowRight />
            </motion.button>
            <motion.button
              className="btn-secondary btn-large"
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              <FiPlay />
              Ver Demonstração
            </motion.button>
          </div>

          <div className="hero-stats">
            <div className="stat-item">
              <div className="stat-number">10k+</div>
              <div className="stat-label">Usuários Ativos</div>
            </div>
            <div className="stat-item">
              <div className="stat-number">R$ 50M+</div>
              <div className="stat-label">Gerenciados</div>
            </div>
            <div className="stat-item">
              <div className="stat-number">4.9★</div>
              <div className="stat-label">Avaliação</div>
            </div>
          </div>
        </motion.div>

        <motion.div 
          className="hero-visual"
          initial={{ opacity: 0, x: 30 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.4, duration: 0.8 }}
        >
          <div className="dashboard-preview">
            <div className="dashboard-header">
              <div className="dashboard-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
            <div className="dashboard-content">
              <div className="dashboard-card">
                <div className="card-header">
                  <span className="card-icon">💰</span>
                  <span>Saldo Total</span>
                </div>
                <div className="card-value">R$ 12.450,00</div>
                <div className="card-change positive">+12.5% este mês</div>
              </div>
              <div className="dashboard-card">
                <div className="card-header">
                  <span className="card-icon">📈</span>
                  <span>Receitas</span>
                </div>
                <div className="card-value">R$ 8.500,00</div>
                <div className="card-change positive">+8.2% vs mês anterior</div>
              </div>
              <div className="dashboard-card">
                <div className="card-header">
                  <span className="card-icon">📉</span>
                  <span>Despesas</span>
                </div>
                <div className="card-value">R$ 4.050,00</div>
                <div className="card-change negative">-5.1% vs mês anterior</div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>

      <div className="hero-background">
        <div className="gradient-orb orb-1"></div>
        <div className="gradient-orb orb-2"></div>
        <div className="gradient-orb orb-3"></div>
      </div>
    </section>
  )
}

export default Hero




