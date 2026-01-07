import React from 'react'
import { motion } from 'framer-motion'
import { FiStar } from 'react-icons/fi'
import './Testimonials.css'

const testimonials = [
  {
    name: 'Maria Silva',
    role: 'Empresária',
    avatar: '👩‍💼',
    rating: 5,
    text: 'O Nelfy transformou completamente como eu gerencio minhas finanças. A automação inteligente economiza horas do meu tempo toda semana!'
  },
  {
    name: 'João Santos',
    role: 'Freelancer',
    avatar: '👨‍💻',
    rating: 5,
    text: 'As sugestões de metas baseadas no meu histórico são incríveis! Consegui economizar 30% a mais este ano graças às recomendações da IA.'
  },
  {
    name: 'Ana Costa',
    role: 'Contadora',
    avatar: '👩‍💼',
    rating: 5,
    text: 'Como profissional da área, posso dizer que o dashboard executivo é de nível empresarial. Os relatórios são detalhados e precisos.'
  },
  {
    name: 'Carlos Oliveira',
    role: 'Investidor',
    avatar: '👨‍💼',
    rating: 5,
    text: 'A capacidade de gerenciar múltiplas contas e investimentos em um só lugar é fantástica. A análise preditiva me ajuda muito nas decisões.'
  }
]

const Testimonials = () => {
  return (
    <section id="testimonials" className="testimonials">
      <div className="testimonials-container">
        <motion.div
          className="testimonials-header"
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="section-title">O que Nossos Usuários Dizem</h2>
          <p className="section-description">
            Mais de 10.000 pessoas já transformaram suas finanças com o Nelfy
          </p>
        </motion.div>

        <div className="testimonials-grid">
          {testimonials.map((testimonial, index) => (
            <motion.div
              key={index}
              className="testimonial-card"
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1, duration: 0.6 }}
              whileHover={{ y: -5 }}
            >
              <div className="testimonial-rating">
                {[...Array(testimonial.rating)].map((_, i) => (
                  <FiStar key={i} fill="#fbbf24" color="#fbbf24" />
                ))}
              </div>
              <p className="testimonial-text">"{testimonial.text}"</p>
              <div className="testimonial-author">
                <div className="author-avatar">{testimonial.avatar}</div>
                <div className="author-info">
                  <div className="author-name">{testimonial.name}</div>
                  <div className="author-role">{testimonial.role}</div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}

export default Testimonials




