import React from 'react'
import { motion } from 'framer-motion'
import { 
  FiZap, 
  FiBarChart2, 
  FiShield, 
  FiTarget,
  FiTrendingUp,
  FiUsers,
  FiCreditCard,
  FiBell
} from 'react-icons/fi'
import './Features.css'

const features = [
  {
    icon: <FiZap />,
    title: 'Automação Inteligente',
    description: 'Categorização automática de transações com IA. Economize horas de trabalho manual.',
    color: '#f59e0b'
  },
  {
    icon: <FiBarChart2 />,
    title: 'Dashboard Executivo',
    description: 'KPIs em tempo real, comparações mês a mês e análises preditivas avançadas.',
    color: '#6366f1'
  },
  {
    icon: <FiShield />,
    title: 'Segurança Total',
    description: 'Seus dados protegidos com criptografia de ponta a ponta e backups automáticos.',
    color: '#10b981'
  },
  {
    icon: <FiTarget />,
    title: 'Metas Inteligentes',
    description: 'Sugestões de metas baseadas no seu histórico. Acompanhe seu progresso em tempo real.',
    color: '#ef4444'
  },
  {
    icon: <FiTrendingUp />,
    title: 'Análise Preditiva',
    description: 'Previsões de gastos futuros e detecção automática de anomalias nos seus gastos.',
    color: '#8b5cf6'
  },
  {
    icon: <FiUsers />,
    title: 'Colaboração',
    description: 'Compartilhe orçamentos com sua família ou equipe. Controle total de permissões.',
    color: '#06b6d4'
  },
  {
    icon: <FiCreditCard />,
    title: 'Múltiplas Contas',
    description: 'Gerencie contas correntes, cartões, investimentos e carteiras digitais em um só lugar.',
    color: '#ec4899'
  },
  {
    icon: <FiBell />,
    title: 'Alertas Proativos',
    description: 'Notificações inteligentes sobre vencimentos, orçamentos e oportunidades de economia.',
    color: '#f97316'
  }
]

const Features = () => {
  return (
    <section id="features" className="features">
      <div className="features-container">
        <motion.div
          className="features-header"
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="section-title">Recursos Poderosos</h2>
          <p className="section-description">
            Tudo que você precisa para ter controle total das suas finanças
          </p>
        </motion.div>

        <div className="features-grid">
          {features.map((feature, index) => (
            <motion.div
              key={index}
              className="feature-card"
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1, duration: 0.6 }}
              whileHover={{ y: -10, scale: 1.02 }}
            >
              <div className="feature-icon" style={{ color: feature.color }}>
                {feature.icon}
              </div>
              <h3 className="feature-title">{feature.title}</h3>
              <p className="feature-description">{feature.description}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}

export default Features

