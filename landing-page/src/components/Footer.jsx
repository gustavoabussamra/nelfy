import React from 'react'
import { FiGithub, FiTwitter, FiLinkedin, FiMail } from 'react-icons/fi'
import NelfyLogo from './NelfyLogo'
import './Footer.css'

const Footer = () => {
  return (
    <footer className="footer">
      <div className="footer-container">
        <div className="footer-content">
          <div className="footer-section">
            <div className="footer-brand">
              <NelfyLogo size="small" showTagline={true} />
            </div>
            <p className="footer-description">
              Controle financeiro inteligente para pessoas e empresas.
            </p>
            <div className="footer-social">
              <a href="#" aria-label="GitHub">
                <FiGithub />
              </a>
              <a href="#" aria-label="Twitter">
                <FiTwitter />
              </a>
              <a href="#" aria-label="LinkedIn">
                <FiLinkedin />
              </a>
              <a href="#" aria-label="Email">
                <FiMail />
              </a>
            </div>
          </div>

          <div className="footer-section">
            <h4 className="footer-title">Produto</h4>
            <ul className="footer-links">
              <li><a href="#features">Recursos</a></li>
              <li><a href="#pricing">Planos</a></li>
              <li><a href="#testimonials">Depoimentos</a></li>
              <li><a href="#">Atualizações</a></li>
            </ul>
          </div>

          <div className="footer-section">
            <h4 className="footer-title">Empresa</h4>
            <ul className="footer-links">
              <li><a href="#">Sobre</a></li>
              <li><a href="#">Blog</a></li>
              <li><a href="#">Carreiras</a></li>
              <li><a href="#">Contato</a></li>
            </ul>
          </div>

          <div className="footer-section">
            <h4 className="footer-title">Suporte</h4>
            <ul className="footer-links">
              <li><a href="#">Central de Ajuda</a></li>
              <li><a href="#">Documentação</a></li>
              <li><a href="#">Status</a></li>
              <li><a href="#">Privacidade</a></li>
            </ul>
          </div>
        </div>

        <div className="footer-bottom">
          <p>&copy; 2024 Nelfy. Todos os direitos reservados.</p>
        </div>
      </div>
    </footer>
  )
}

export default Footer




