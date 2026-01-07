import React, { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { FiCheckCircle } from 'react-icons/fi'
import './Success.css'

const Success = () => {
  const navigate = useNavigate()

  useEffect(() => {
    const frontendUrl = import.meta.env.VITE_FRONTEND_URL || 'http://72.61.134.94:3002'
    const timer = setTimeout(() => {
      window.location.href = `${frontendUrl}/login`
    }, 5000)

    return () => clearTimeout(timer)
  }, [])

  return (
    <div className="success-page">
      <div className="success-container">
        <div className="success-icon">
          <FiCheckCircle />
        </div>
        <h1 className="success-title">Pagamento Confirmado!</h1>
        <p className="success-message">
          Sua assinatura foi ativada com sucesso. Você será redirecionado para o login em instantes.
        </p>
        <button className="btn-primary" onClick={() => {
          const frontendUrl = import.meta.env.VITE_FRONTEND_URL || 'http://72.61.134.94:3002'
          window.location.href = `${frontendUrl}/login`
        }}>
          Ir para o Login
        </button>
      </div>
    </div>
  )
}

export default Success




