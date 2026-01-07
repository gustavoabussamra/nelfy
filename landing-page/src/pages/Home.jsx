import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { 
  FiArrowRight, 
  FiCheck, 
  FiStar, 
  FiTrendingUp, 
  FiShield, 
  FiZap,
  FiBarChart2,
  FiTarget,
  FiUsers,
  FiCreditCard,
  FiMenu,
  FiX
} from 'react-icons/fi'
import { toast } from 'react-toastify'
import axios from 'axios'
import Navbar from '../components/Navbar'
import Hero from '../components/Hero'
import Features from '../components/Features'
import Pricing from '../components/Pricing'
import Testimonials from '../components/Testimonials'
import CTA from '../components/CTA'
import Footer from '../components/Footer'
import ChatSupport from '../components/ChatSupport'
import './Home.css'

const Home = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  return (
    <div className="home-page">
      <Navbar isMenuOpen={isMenuOpen} setIsMenuOpen={setIsMenuOpen} />
      <Hero />
      <Features />
      <Pricing />
      <Testimonials />
      <CTA />
      <Footer />
      <ChatSupport />
    </div>
  )
}

export default Home

