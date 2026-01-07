import React from 'react'
import { motion } from 'framer-motion'
import { FiArrowRight } from 'react-icons/fi'
import './CTA.css'

const CTA = () => {
  const scrollToPricing = () => {
    document.getElementById('pricing').scrollIntoView({ behavior: 'smooth' })
  }

  return (
    <section id="cta" className="cta">
      <div className="cta-container">
        <motion.div
          className="cta-content"
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="cta-title">
            Pronto para Transformar suas Finanças?
          </h2>
          <p className="cta-description">
            Junte-se a mais de 10.000 pessoas que já estão no controle das suas finanças.
            Comece grátis hoje mesmo!
          </p>
          <motion.button
            className="btn-cta"
            onClick={scrollToPricing}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            Começar Agora - Grátis
            <FiArrowRight />
          </motion.button>
          <p className="cta-note">
            ✓ Sem cartão de crédito • ✓ 30 dias grátis • ✓ Cancele quando quiser
          </p>
        </motion.div>
      </div>
    </section>
  )
}

export default CTA




