import React, { useState, useRef, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { FiMessageCircle, FiX, FiSend, FiMinimize2 } from 'react-icons/fi'
import { toast } from 'react-toastify'
import axios from 'axios'
import './ChatSupport.css'

const ChatSupport = () => {
  const [isOpen, setIsOpen] = useState(false)
  const [isMinimized, setIsMinimized] = useState(false)
  const [messages, setMessages] = useState([
    {
      id: 1,
      text: 'Olá! 👋 Bem-vindo ao Nelfy. Como posso ajudá-lo hoje?',
      sender: 'bot',
      timestamp: new Date()
    }
  ])
  const [inputValue, setInputValue] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const messagesEndRef = useRef(null)
  const inputRef = useRef(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  useEffect(() => {
    if (isOpen && !isMinimized) {
      inputRef.current?.focus()
    }
  }, [isOpen, isMinimized])

  const handleTransferToWhatsApp = (number = null) => {
    // Número do WhatsApp (formato internacional sem +)
    // Pode ser configurado via variável de ambiente
    const whatsappNumber = number || import.meta.env.VITE_WHATSAPP_NUMBER || '5511999999999'
    const message = encodeURIComponent('Olá! Vim do site do Nelfy e gostaria de falar com um atendente.')
    const whatsappUrl = `https://wa.me/${whatsappNumber}?text=${message}`
    window.open(whatsappUrl, '_blank')
    
    // Adicionar mensagem de confirmação
    const confirmationMessage = {
      id: Date.now(),
      text: '✅ Redirecionando você para o WhatsApp... Se não abrir automaticamente, clique aqui: https://wa.me/' + whatsappNumber,
      sender: 'bot',
      timestamp: new Date()
    }
    setMessages(prev => [...prev, confirmationMessage])
  }

  const handleSend = async (e) => {
    e.preventDefault()
    if (!inputValue.trim()) return

    const userMessage = {
      id: Date.now(),
      text: inputValue,
      sender: 'user',
      timestamp: new Date()
    }

    setMessages(prev => [...prev, userMessage])
    const currentInput = inputValue
    setInputValue('')
    setIsTyping(true)

    // Tentar usar API do backend, se falhar usa resposta local
    const apiUrl = import.meta.env.VITE_API_URL || 'http://72.61.134.94:8085'
    axios.post(`${apiUrl}/api/chat/message`, { message: currentInput })
      .then(response => {
        const responseText = response.data.response
        
        // Verificar se é redirecionamento para WhatsApp
        if (responseText.startsWith('REDIRECT_WHATSAPP:')) {
          const whatsappNumber = responseText.split(':')[1]
          handleTransferToWhatsApp(whatsappNumber)
          const botResponse = {
            id: Date.now(),
            text: 'Claro! Vou te transferir para nosso atendimento via WhatsApp. Um momento... 📱',
            sender: 'bot',
            timestamp: new Date(response.data.timestamp)
          }
          setMessages(prev => [...prev, botResponse])
          setIsTyping(false)
          return
        }
        
        const botResponse = {
          id: Date.now(),
          text: responseText,
          sender: 'bot',
          timestamp: new Date(response.data.timestamp)
        }
        setMessages(prev => [...prev, botResponse])
        setIsTyping(false)
      })
      .catch(() => {
        // Fallback para resposta local se API falhar
        const botResponse = generateBotResponse(currentInput)
        setMessages(prev => [...prev, botResponse])
        setIsTyping(false)
      })
  }

  const generateBotResponse = (userMessage) => {
    const message = userMessage.toLowerCase()
    
    // Detectar pedido para falar com humano
    if (message.includes('humano') || message.includes('atendente') || message.includes('pessoa') || 
        message.includes('operador') || message.includes('suporte humano') || message.includes('falar com alguém') ||
        message.includes('conversar com') || message.includes('whatsapp') || message.includes('whats')) {
      handleTransferToWhatsApp()
      return {
        id: Date.now(),
        text: 'Claro! Vou te transferir para nosso atendimento via WhatsApp. Um momento... 📱',
        sender: 'bot',
        timestamp: new Date()
      }
    }
    
    // Respostas automáticas baseadas em palavras-chave
    if (message.includes('preço') || message.includes('valor') || message.includes('quanto')) {
      return {
        id: Date.now(),
        text: 'Temos planos a partir de R$ 0 (grátis)! O plano Básico custa R$ 29,90/mês, Premium R$ 59,90/mês e Empresarial R$ 149,90/mês. Todos incluem 30 dias grátis para testar! 🎉',
        sender: 'bot',
        timestamp: new Date()
      }
    }

    if (message.includes('trial') || message.includes('teste') || message.includes('grátis')) {
      return {
        id: Date.now(),
        text: 'Sim! Oferecemos 30 dias grátis em todos os planos. Você pode testar todas as funcionalidades sem compromisso. Não precisa de cartão de crédito para começar! ✨',
        sender: 'bot',
        timestamp: new Date()
      }
    }

    if (message.includes('funcionalidade') || message.includes('recurso') || message.includes('faz')) {
      return {
        id: Date.now(),
        text: 'O Nelfy oferece: automação inteligente com IA, dashboard executivo, múltiplas contas, metas financeiras, orçamentos, relatórios avançados, detecção de anomalias e muito mais! Quer saber mais sobre alguma funcionalidade específica? 🚀',
        sender: 'bot',
        timestamp: new Date()
      }
    }

    if (message.includes('suporte') || message.includes('ajuda') || message.includes('problema')) {
      return {
        id: Date.now(),
        text: 'Estou aqui para ajudar! Você pode me perguntar sobre planos, funcionalidades, preços ou qualquer dúvida. Se precisar falar com um atendente humano, digite "falar com humano" ou "whatsapp" e eu te transfiro! 💬',
        sender: 'bot',
        timestamp: new Date()
      }
    }

    if (message.includes('cadastro') || message.includes('registro') || message.includes('criar conta')) {
      return {
        id: Date.now(),
        text: 'Para criar sua conta, clique no botão "Começar Grátis" no topo da página ou acesse http://72.61.134.94:3002/register. É rápido e fácil! 🎯',
        sender: 'bot',
        timestamp: new Date()
      }
    }

    if (message.includes('pagamento') || message.includes('cartão') || message.includes('pagar')) {
      return {
        id: Date.now(),
        text: 'Aceitamos cartão de crédito através do Mercado Pago. O pagamento é seguro e processado automaticamente. Você pode cancelar a qualquer momento! 💳',
        sender: 'bot',
        timestamp: new Date()
      }
    }

    // Resposta padrão
    const defaultResponses = [
      'Entendi! Posso ajudá-lo com informações sobre nossos planos, funcionalidades ou qualquer dúvida sobre o Nelfy. O que você gostaria de saber? 😊',
      'Ótima pergunta! Deixe-me ajudar você. Você tem interesse em algum plano específico ou quer saber mais sobre nossas funcionalidades? 💡',
      'Claro! Estou aqui para ajudar. Posso falar sobre preços, funcionalidades, planos ou qualquer outra dúvida. O que você precisa? 🤔'
    ]

    return {
      id: Date.now(),
      text: defaultResponses[Math.floor(Math.random() * defaultResponses.length)],
      sender: 'bot',
      timestamp: new Date()
    }
  }

  const formatTime = (date) => {
    return new Date(date).toLocaleTimeString('pt-BR', { 
      hour: '2-digit', 
      minute: '2-digit' 
    })
  }

  return (
    <>
      {/* Botão flutuante */}
      <AnimatePresence>
        {!isOpen && (
          <motion.button
            className="chat-button"
            onClick={() => setIsOpen(true)}
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            exit={{ scale: 0 }}
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.9 }}
          >
            <FiMessageCircle />
            <span className="chat-badge">💬</span>
          </motion.button>
        )}
      </AnimatePresence>

      {/* Janela do chat */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            className={`chat-window ${isMinimized ? 'minimized' : ''}`}
            initial={{ opacity: 0, y: 20, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.9 }}
            transition={{ type: 'spring', damping: 25, stiffness: 300 }}
          >
            {/* Header */}
            <div className="chat-header">
              <div className="chat-header-info">
                <div className="chat-avatar">🤖</div>
                <div>
                  <div className="chat-name">Suporte Nelfy</div>
                  <div className="chat-status">
                    <span className="status-dot"></span>
                    Online agora
                  </div>
                </div>
              </div>
              <div className="chat-actions">
                <button
                  className="chat-action-btn"
                  onClick={() => setIsMinimized(!isMinimized)}
                  title={isMinimized ? 'Expandir' : 'Minimizar'}
                >
                  <FiMinimize2 />
                </button>
                <button
                  className="chat-action-btn"
                  onClick={() => setIsOpen(false)}
                  title="Fechar"
                >
                  <FiX />
                </button>
              </div>
            </div>

            {/* Messages */}
            {!isMinimized && (
              <>
                <div className="chat-messages">
                  {messages.map((message) => (
                    <div
                      key={message.id}
                      className={`chat-message ${message.sender === 'user' ? 'user' : 'bot'}`}
                    >
                      {message.sender === 'bot' && (
                        <div className="message-avatar">🤖</div>
                      )}
                      <div className="message-content">
                        <div className="message-bubble">
                          {message.text}
                        </div>
                        <div className="message-time">
                          {formatTime(message.timestamp)}
                        </div>
                      </div>
                    </div>
                  ))}
                  {isTyping && (
                    <div className="chat-message bot">
                      <div className="message-avatar">🤖</div>
                      <div className="message-content">
                        <div className="message-bubble typing">
                          <span></span>
                          <span></span>
                          <span></span>
                        </div>
                      </div>
                    </div>
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* Input */}
                <div className="chat-input-wrapper">
                  <button
                    className="chat-whatsapp-btn"
                    onClick={handleTransferToWhatsApp}
                    title="Falar com atendente humano no WhatsApp"
                  >
                    💬 WhatsApp
                  </button>
                  <form className="chat-input-form" onSubmit={handleSend}>
                    <input
                      ref={inputRef}
                      type="text"
                      className="chat-input"
                      placeholder="Digite sua mensagem..."
                      value={inputValue}
                      onChange={(e) => setInputValue(e.target.value)}
                    />
                    <button
                      type="submit"
                      className="chat-send-btn"
                      disabled={!inputValue.trim()}
                    >
                      <FiSend />
                    </button>
                  </form>
                </div>
              </>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </>
  )
}

export default ChatSupport

