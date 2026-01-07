import React, { useState } from 'react'
import { motion } from 'framer-motion'
import { FiCheck, FiArrowRight } from 'react-icons/fi'
import { toast } from 'react-toastify'
import axios from 'axios'
import './Pricing.css'

const plans = [
  {
    name: 'FREE',
    label: 'Grátis',
    price: 0,
    period: 'sempre',
    description: 'Perfeito para começar',
    features: [
      'Transações ilimitadas',
      '3 categorias personalizadas',
      '1 conta/carteira',
      '3 metas financeiras',
      '1 orçamento mensal',
      'Dashboard básico',
      'Relatórios mensais'
    ],
    popular: false,
    color: '#6b7280'
  },
  {
    name: 'BASIC',
    label: 'Básico',
    price: 29.90,
    period: 'mês',
    description: 'Para quem quer mais controle',
    features: [
      'Tudo do plano Grátis',
      'Categorias ilimitadas',
      '3 contas/carteiras',
      '10 anexos por mês',
      'Metas ilimitadas',
      'Orçamentos ilimitados',
      'Exportação Excel',
      'Suporte por email'
    ],
    popular: true,
    color: '#6366f1'
  },
  {
    name: 'PREMIUM',
    label: 'Premium',
    price: 59.90,
    period: 'mês',
    description: 'Máximo poder e automação',
    features: [
      'Tudo do plano Básico',
      'Contas ilimitadas',
      'Anexos ilimitados',
      'Análise com IA',
      'Automação inteligente',
      'Dashboard executivo',
      'Sugestões de metas',
      'Detecção de anomalias',
      'Suporte prioritário'
    ],
    popular: false,
    color: '#8b5cf6'
  },
  {
    name: 'ENTERPRISE',
    label: 'Empresarial',
    price: 149.90,
    period: 'mês',
    description: 'Para equipes e empresas',
    features: [
      'Tudo do plano Premium',
      'Múltiplos usuários',
      'Colaboração em tempo real',
      'Permissões granulares',
      'API personalizada',
      'Integrações avançadas',
      'Suporte 24/7',
      'Treinamento dedicado'
    ],
    popular: false,
    color: '#ec4899'
  }
]

const Pricing = () => {
  const [loading, setLoading] = useState(null)

  const handleSubscribe = async (planName) => {
    if (planName === 'FREE') {
      const frontendUrl = import.meta.env.VITE_FRONTEND_URL || 'http://72.61.134.94:3002'
      window.location.href = `${frontendUrl}/register`
      return
    }

    setLoading(planName)
    try {
      // Para produção, usar variável de ambiente
      const apiUrl = import.meta.env.VITE_API_URL || 'http://72.61.134.94:8085'
      const response = await axios.post(`${apiUrl}/api/payments/create`, {
        plan: planName,
        paymentMethod: 'CREDIT_CARD',
        returnUrl: `${window.location.origin}/success`
      }, {
        headers: {
          'Content-Type': 'application/json'
        },
        withCredentials: true
      })

      if (response.data.paymentUrl) {
        window.location.href = response.data.paymentUrl
      } else if (response.data.status === 'APPROVED') {
        toast.success('Pagamento aprovado! Redirecionando...')
        setTimeout(() => {
          const frontendUrl = import.meta.env.VITE_FRONTEND_URL || 'http://72.61.134.94:3002'
          window.location.href = `${frontendUrl}/login`
        }, 2000)
      }
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao processar pagamento')
      setLoading(null)
    }
  }

  return (
    <section id="pricing" className="pricing">
      <div className="pricing-container">
        <motion.div
          className="pricing-header"
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="section-title">Planos que Cabe no Seu Bolso</h2>
          <p className="section-description">
            Escolha o plano ideal para suas necessidades. Todos incluem 30 dias grátis!
          </p>
        </motion.div>

        <div className="pricing-grid">
          {plans.map((plan, index) => (
            <motion.div
              key={plan.name}
              className={`pricing-card ${plan.popular ? 'popular' : ''}`}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1, duration: 0.6 }}
              whileHover={{ y: -10 }}
            >
              {plan.popular && (
                <div className="popular-badge" style={{ background: plan.color }}>
                  Mais Popular
                </div>
              )}

              <div className="plan-header">
                <h3 className="plan-name">{plan.label}</h3>
                <p className="plan-description">{plan.description}</p>
              </div>

              <div className="plan-price">
                <span className="price-value">R$ {plan.price.toFixed(2)}</span>
                {plan.price > 0 && <span className="price-period">/{plan.period}</span>}
              </div>

              <ul className="plan-features">
                {plan.features.map((feature, idx) => (
                  <li key={idx}>
                    <FiCheck />
                    {feature}
                  </li>
                ))}
              </ul>

              <button
                className="plan-button"
                style={{ 
                  background: plan.color,
                  opacity: loading === plan.name ? 0.7 : 1
                }}
                onClick={() => handleSubscribe(plan.name)}
                disabled={loading === plan.name}
              >
                {loading === plan.name ? (
                  'Processando...'
                ) : plan.name === 'FREE' ? (
                  'Começar Grátis'
                ) : (
                  <>
                    Assinar Agora
                    <FiArrowRight />
                  </>
                )}
              </button>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}

export default Pricing

