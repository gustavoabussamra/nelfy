import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { FiMenu, FiX } from 'react-icons/fi'
import NelfyLogo from './NelfyLogo'
import './Navbar.css'

const Navbar = ({ isMenuOpen, setIsMenuOpen }) => {
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 50)
    }
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  return (
    <motion.nav 
      className={`navbar ${scrolled ? 'scrolled' : ''}`}
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      transition={{ duration: 0.5 }}
    >
      <div className="navbar-container">
        <div className="navbar-brand">
          <NelfyLogo size="small" />
        </div>
        
        <div className={`navbar-menu ${isMenuOpen ? 'open' : ''}`}>
          <a href="#features" onClick={() => setIsMenuOpen(false)}>Recursos</a>
          <a href="#pricing" onClick={() => setIsMenuOpen(false)}>Planos</a>
          <a href="#testimonials" onClick={() => setIsMenuOpen(false)}>Depoimentos</a>
          <a href="#cta" className="btn-nav-cta" onClick={() => setIsMenuOpen(false)}>
            Começar Agora
          </a>
        </div>

        <button 
          className="navbar-toggle"
          onClick={() => setIsMenuOpen(!isMenuOpen)}
        >
          {isMenuOpen ? <FiX /> : <FiMenu />}
        </button>
      </div>
    </motion.nav>
  )
}

export default Navbar




