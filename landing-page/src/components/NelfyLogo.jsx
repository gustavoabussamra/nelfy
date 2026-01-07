import React from 'react'
import './NelfyLogo.css'

const NelfyLogo = ({ showTagline = false, size = 'medium' }) => {
  return (
    <div className={`nelfy-logo ${size}`}>
      <svg 
        className="nelfy-logo-mark" 
        viewBox="0 0 120 120" 
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <linearGradient id="blueGradient" x1="0%" y1="100%" x2="0%" y2="0%">
            <stop offset="0%" stopColor="#1E3A8A" />
            <stop offset="100%" stopColor="#4A90E2" />
          </linearGradient>
          <linearGradient id="greenGradient" x1="0%" y1="100%" x2="0%" y2="0%">
            <stop offset="0%" stopColor="#4CAF50" />
            <stop offset="100%" stopColor="#A8E063" />
          </linearGradient>
        </defs>
        
        {/* Left vertical stroke and upper diagonal - Blue to Teal */}
        <path
          d="M 20 20 L 20 100 L 45 50 L 45 20 Z"
          fill="url(#blueGradient)"
          stroke="none"
        />
        
        {/* Right vertical stroke with arrow - Green gradient */}
        <path
          d="M 75 20 L 100 20 L 100 50 L 90 40 L 100 30 L 100 20 L 75 20 Z"
          fill="url(#greenGradient)"
          stroke="none"
        />
        
        {/* Lower diagonal part of N with arrow continuation */}
        <path
          d="M 45 50 L 75 100 L 100 100 L 100 50 L 90 60 L 75 75 L 45 50 Z"
          fill="url(#greenGradient)"
          stroke="none"
        />
      </svg>
      <div className="nelfy-logo-text">
        <span className="nelfy-brand-name">Nelfy</span>
        {showTagline && (
          <span className="nelfy-tagline">Sua vida e seu negócio em equilíbrio.</span>
        )}
      </div>
    </div>
  )
}

export default NelfyLogo
