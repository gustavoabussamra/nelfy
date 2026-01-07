import React, { useState } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import './AiTransactionInput.css';

const AiTransactionInput = ({ onSuccess }) => {
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const [showInput, setShowInput] = useState(false);
  const [conversation, setConversation] = useState([]);
  const [fullText, setFullText] = useState(''); // Acumula todo o contexto da conversa
  const [pendingTransaction, setPendingTransaction] = useState(null); // Transação aguardando confirmação
  const [needsCategory, setNeedsCategory] = useState(false); // Se precisa selecionar categoria
  const [availableCategories, setAvailableCategories] = useState([]); // Categorias disponíveis
  const [showNewCategoryInput, setShowNewCategoryInput] = useState(false); // Mostrar input de nova categoria
  const [newCategoryName, setNewCategoryName] = useState(''); // Nome da nova categoria
  const [newCategoryIcon, setNewCategoryIcon] = useState('📁'); // Ícone da nova categoria
  const [newCategoryColor, setNewCategoryColor] = useState('#6366f1'); // Cor da nova categoria
  const [showCategoryCustomization, setShowCategoryCustomization] = useState(false); // Mostrar personalização de categoria

  // Ícones e cores disponíveis (mesmos do Categories.js)
  const icons = ['📁', '🍔', '🚗', '🏠', '💊', '🎓', '🎮', '👕', '💼', '✈️', '💳', '💰', '🛒', '📱', '💡', '🍕', '☕', '🍎', '🏋️', '🎬', '📚', '🎵', '🎨', '⚽', '🏥', '🚌', '⛽', '🛍️', '💇', '🎁', '🍰', '🍺', '🍷', '🌮', '🍜', '🏊', '🚴', '🎯', '🎪', '🎭', '📷', '🎤', '🎸', '🎹', '🎺', '🏄', '⛷️', '🏂', '🎿', '🏌️', '🖼️', '✏️', '📝', '📊', '📈', '📉', '💉', '🩹', '🩺', '🚪', '🛏️', '🛋️', '🚽', '🚿', '🛁', '🧴', '🧹', '🧺', '🧼', '🧽', '🧯', '🏧', '🎲', '🎡', '🎢', '🎠', '🔧', '🛠️', '🧰', '🔬', '📡', '🧪', '⚗️', '🎦', '📶', '💱', '💲', '⚕️', '🔔', '📣', '📢'];
  const colors = ['#6366f1', '#10b981', '#ef4444', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'];

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!text.trim()) {
      toast.error('Por favor, digite algo para criar a transação');
      return;
    }

    const userMessage = text.trim();
    setText('');
    setLoading(true);

    // Adicionar mensagem do usuário à conversa
    const newConversation = [...conversation, { type: 'user', text: userMessage }];
    setConversation(newConversation);

    // Acumular texto completo para o contexto
    const contextText = fullText ? `${fullText}\n${userMessage}` : userMessage;
    setFullText(contextText);

    try {
      console.log('=== AI Transaction Request ===');
      console.log('URL: /transactions/ai/create');
      console.log('Text:', contextText);
      console.log('Token:', localStorage.getItem('token') ? 'Present' : 'Missing');
      
      const response = await api.post('/transactions/ai/create', { text: contextText });
      console.log('Response status:', response.status);
      console.log('Response data:', response.data);
      const data = response.data;

      if (data.success) {
        // Transação criada com sucesso
        toast.success(data.message || 'Transação criada com sucesso!');
        setText('');
        setConversation([]);
        setFullText('');
        setPendingTransaction(null);
        setNeedsCategory(false);
        setAvailableCategories([]);
        setShowNewCategoryInput(false);
        setNewCategoryName('');
        setShowCategoryCustomization(false);
        setNewCategoryIcon('📁');
        setNewCategoryColor('#6366f1');
        setShowInput(false);
        if (onSuccess) {
          onSuccess();
        }
      } else if (data.needsCategory && data.transaction) {
        // Precisa selecionar categoria primeiro
        setPendingTransaction(data.transaction);
        setNeedsCategory(true);
        setAvailableCategories(data.availableCategories || []);
        const aiMessage = data.message || 'Qual categoria para esta despesa?';
        setConversation([...newConversation, { 
          type: 'ai', 
          text: aiMessage,
          needsCategory: true,
          availableCategories: data.availableCategories || []
        }]);
      } else if (data.transaction) {
        // Precisa de confirmação - tem dados da transação
        setPendingTransaction(data.transaction);
        setNeedsCategory(false);
        const aiMessage = data.message || 'Por favor, confirme os dados da transação.';
        setConversation([...newConversation, { 
          type: 'ai', 
          text: aiMessage,
          needsConfirmation: true,
          transaction: data.transaction
        }]);
      } else {
        // Precisa de mais informações
        const aiMessage = data.suggestedQuestion || data.message || 'Preciso de mais informações.';
        setConversation([...newConversation, { type: 'ai', text: aiMessage }]);
        // Manter o input aberto para o usuário responder
      }
    } catch (error) {
      console.error('=== AI Transaction Error ===');
      console.error('Error:', error);
      console.error('Response:', error.response);
      console.error('Status:', error.response?.status);
      console.error('Data:', error.response?.data);
      
      const errorMessage = error.response?.data?.message || error.response?.data?.error || 
                          error.message || 'Erro ao criar transação';
      
      // Se for 403, dar mensagem mais específica
      if (error.response?.status === 403) {
        toast.error('Acesso negado. Verifique se você está autenticado.');
      } else {
        toast.error(errorMessage);
      }
      
      setConversation([...newConversation, { 
        type: 'ai', 
        text: `❌ Erro: ${errorMessage}`,
        isError: true 
      }]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const handleReset = () => {
    setShowInput(false);
    setText('');
    setConversation([]);
    setFullText('');
    setPendingTransaction(null);
    setNeedsCategory(false);
    setAvailableCategories([]);
    setShowNewCategoryInput(false);
    setNewCategoryName('');
    setShowCategoryCustomization(false);
    setNewCategoryIcon('📁');
    setNewCategoryColor('#6366f1');
  };

  const handleConfirmTransaction = async () => {
    if (!pendingTransaction) return;
    
    setLoading(true);
    try {
      // Garantir que as datas estão no formato correto (YYYY-MM-DD sem hora/timezone)
      const formatDateToString = (date) => {
        if (!date) return null;
        // Se já é uma string no formato YYYY-MM-DD, retornar como está
        if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
          return date;
        }
        // Se é um objeto Date ou string ISO, converter para YYYY-MM-DD
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
      };
      
      const transactionToConfirm = {
        ...pendingTransaction,
        dueDate: formatDateToString(pendingTransaction.dueDate),
        transactionDate: formatDateToString(pendingTransaction.transactionDate),
      };
      
      console.log('Confirmando transação:', transactionToConfirm);
      console.log('DueDate:', transactionToConfirm.dueDate);
      console.log('TransactionDate:', transactionToConfirm.transactionDate);
      
      const response = await api.post('/transactions/ai/confirm', transactionToConfirm);
      const data = response.data;
      
      if (data.success) {
        toast.success(data.message || 'Transação criada com sucesso!');
        setText('');
        setConversation([]);
        setFullText('');
        setPendingTransaction(null);
        setShowInput(false);
        if (onSuccess) {
          onSuccess();
        }
      } else {
        toast.error(data.message || 'Erro ao confirmar transação');
      }
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Erro ao confirmar transação';
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleRejectTransaction = () => {
    setPendingTransaction(null);
    setNeedsCategory(false);
    // Remover a última mensagem de confirmação da conversa
    const newConversation = conversation.filter((msg, index) => {
      return !(index === conversation.length - 1 && (msg.needsConfirmation || msg.needsCategory));
    });
    setConversation(newConversation);
    setText('');
  };

  // Capitalizar primeira letra de cada palavra
  const capitalizeWords = (text) => {
    if (!text) return '';
    return text.toLowerCase()
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  };

  const handleSelectCategory = async (category) => {
    if (!pendingTransaction) return;
    
    // Atualizar transação com categoria selecionada
    const updatedTransaction = {
      ...pendingTransaction,
      category: category
    };
    setPendingTransaction(updatedTransaction);
    
    // Se categoria é null, foi "sem categoria", então ir direto para confirmação
    if (!category) {
      // Ir direto para confirmação
      await proceedToConfirmation(updatedTransaction);
      return;
    }
    
    // Se tem categoria, ir para confirmação
    await proceedToConfirmation(updatedTransaction);
  };

  const handleCreateNewCategory = async () => {
    if (!newCategoryName.trim()) {
      toast.error('Por favor, digite um nome para a categoria');
      return;
    }

    // Se ainda não mostrou a personalização, mostrar primeiro
    if (!showCategoryCustomization) {
      setShowCategoryCustomization(true);
      return;
    }

    setLoading(true);
    try {
      const capitalizedName = capitalizeWords(newCategoryName.trim());
      
      const categoryData = {
        name: capitalizedName,
        icon: newCategoryIcon,
        color: newCategoryColor,
        type: 'EXPENSE' // Sempre EXPENSE para despesas
      };

      const response = await api.post('/categories', categoryData);
      const newCategory = response.data;
      
      toast.success('Categoria criada com sucesso!');
      
      // Atualizar lista de categorias disponíveis
      setAvailableCategories([...availableCategories, newCategory]);
      
      // Selecionar a categoria recém-criada
      await handleSelectCategory(newCategory);
      
      // Limpar estado
      setShowNewCategoryInput(false);
      setNewCategoryName('');
      setShowCategoryCustomization(false);
      setNewCategoryIcon('📁');
      setNewCategoryColor('#6366f1');
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Erro ao criar categoria';
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const proceedToConfirmation = async (transaction) => {
    // Construir mensagem de confirmação
    let confirmationMessage = "Por favor, confirme os dados da transação:\n\n";
    confirmationMessage += `• Descrição: ${transaction.description}\n`;
    
    if (transaction.totalInstallments && transaction.totalInstallments > 1) {
      const totalAmount = transaction.amount * transaction.totalInstallments;
      confirmationMessage += `• Valor por parcela: R$ ${transaction.amount.toFixed(2)}\n`;
      confirmationMessage += `• Parcelas: ${transaction.totalInstallments}x de R$ ${transaction.amount.toFixed(2)} (Total: R$ ${totalAmount.toFixed(2)})\n`;
    } else {
      confirmationMessage += `• Valor: R$ ${transaction.amount.toFixed(2)}\n`;
    }
    
    const formatDate = (dateStr) => {
      if (!dateStr) return 'N/A';
      // Usar date-fns com timezone de São Paulo
      const date = new Date(dateStr);
      return format(date, 'dd/MM/yyyy', { locale: ptBR });
    };
    
    confirmationMessage += `• Data: ${formatDate(transaction.transactionDate || transaction.dueDate)}\n`;
    confirmationMessage += `• Tipo: ${transaction.type === 'EXPENSE' ? 'Despesa' : 'Receita'}\n`;
    
    if (transaction.category) {
      confirmationMessage += `• Categoria: ${transaction.category.name}\n`;
    }
    
    confirmationMessage += "\nOs dados estão corretos?";
    
    // Atualizar conversa
    setNeedsCategory(false);
    const newConversation = [...conversation];
    // Remover última mensagem de categoria e adicionar confirmação
    const lastIndex = newConversation.length - 1;
    if (lastIndex >= 0 && newConversation[lastIndex].needsCategory) {
      newConversation[lastIndex] = {
        type: 'ai',
        text: confirmationMessage,
        needsConfirmation: true,
        transaction: transaction
      };
    } else {
      newConversation.push({
        type: 'ai',
        text: confirmationMessage,
        needsConfirmation: true,
        transaction: transaction
      });
    }
    setConversation(newConversation);
  };

  if (!showInput) {
    return (
      <div className="ai-transaction-trigger">
        <button 
          className="ai-trigger-btn" 
          onClick={() => setShowInput(true)}
          title="Criar transação com IA"
        >
          <span className="ai-icon">🤖</span>
          <span>Falar com a IA</span>
        </button>
      </div>
    );
  }

  return (
    <div className="ai-transaction-input">
      <div className="ai-input-header">
        <h3>🤖 Criar Transação com IA</h3>
        <button 
          className="ai-close-btn" 
          onClick={handleReset}
        >
          ×
        </button>
      </div>
      
      {/* Área de conversa */}
      {conversation.length > 0 && (
        <div className="ai-conversation">
          {conversation.map((msg, index) => (
            <div key={index} className={`ai-message ai-message-${msg.type} ${msg.isError ? 'ai-message-error' : ''}`}>
              <div className="ai-message-content">
                {msg.type === 'ai' && <span className="ai-message-icon">🤖</span>}
                {msg.type === 'user' && <span className="ai-message-icon">👤</span>}
                <div className="ai-message-text">
                  {msg.text.split('\n').map((line, i) => (
                    <React.Fragment key={i}>
                      {line}
                      {i < msg.text.split('\n').length - 1 && <br />}
                    </React.Fragment>
                  ))}
                </div>
              </div>
              {msg.needsCategory && msg.availableCategories && msg.availableCategories.length > 0 && (
                <div className="ai-category-selection">
                  <div className="ai-category-list">
                    {(msg.availableCategories || availableCategories).map((cat) => (
                      <button
                        key={cat.id}
                        type="button"
                        className="ai-category-btn"
                        onClick={() => handleSelectCategory(cat)}
                        disabled={loading}
                        style={{ 
                          borderColor: cat.color,
                          color: cat.color
                        }}
                      >
                        <span>{cat.icon}</span>
                        <span>{cat.name}</span>
                      </button>
                    ))}
                  </div>
                  <div className="ai-category-actions">
                    <button
                      type="button"
                      className="ai-no-category-btn"
                      onClick={() => handleSelectCategory(null)}
                      disabled={loading}
                    >
                      Sem categoria
                    </button>
                    {!showNewCategoryInput ? (
                      <button
                        type="button"
                        className="ai-new-category-btn"
                        onClick={() => setShowNewCategoryInput(true)}
                        disabled={loading}
                      >
                        + Cadastrar nova categoria
                      </button>
                    ) : (
                      <div className="ai-new-category-input">
                        <div className="ai-category-name-section">
                          <label>Categoria: {newCategoryName || '(sem nome)'}</label>
                          {!showCategoryCustomization && (
                            <input
                              type="text"
                              value={newCategoryName}
                              onChange={(e) => setNewCategoryName(e.target.value)}
                              placeholder="Nome da categoria"
                              className="ai-category-input"
                              disabled={loading}
                              onKeyPress={(e) => {
                                if (e.key === 'Enter') {
                                  e.preventDefault();
                                  handleCreateNewCategory();
                                }
                              }}
                              autoFocus
                            />
                          )}
                        </div>
                        {!showCategoryCustomization ? (
                          <>
                            <button
                              type="button"
                              className="ai-save-category-btn"
                              onClick={handleCreateNewCategory}
                              disabled={loading || !newCategoryName.trim()}
                            >
                              Próximo: Escolher Ícone e Cor
                            </button>
                            <button
                              type="button"
                              className="ai-cancel-category-btn"
                              onClick={() => {
                                setShowNewCategoryInput(false);
                                setNewCategoryName('');
                              }}
                              disabled={loading}
                            >
                              Cancelar
                            </button>
                          </>
                        ) : (
                          <>
                            <div className="ai-category-customization">
                              <div className="ai-customization-section">
                                <label>Escolha um ícone:</label>
                                <div className="ai-icon-selector">
                                  {icons.map((icon) => (
                                    <button
                                      key={icon}
                                      type="button"
                                      className={`ai-icon-option ${newCategoryIcon === icon ? 'selected' : ''}`}
                                      onClick={() => setNewCategoryIcon(icon)}
                                      disabled={loading}
                                    >
                                      {icon}
                                    </button>
                                  ))}
                                </div>
                              </div>
                              <div className="ai-customization-section">
                                <label>Escolha uma cor:</label>
                                <div className="ai-color-selector">
                                  {colors.map((color) => (
                                    <button
                                      key={color}
                                      type="button"
                                      className={`ai-color-option ${newCategoryColor === color ? 'selected' : ''}`}
                                      style={{ backgroundColor: color }}
                                      onClick={() => setNewCategoryColor(color)}
                                      disabled={loading}
                                    />
                                  ))}
                                </div>
                              </div>
                            </div>
                            <div className="ai-customization-actions">
                              <button
                                type="button"
                                className="ai-save-category-btn"
                                onClick={handleCreateNewCategory}
                                disabled={loading || !newCategoryName.trim()}
                              >
                                Criar Categoria
                              </button>
                              <button
                                type="button"
                                className="ai-cancel-category-btn"
                                onClick={() => {
                                  setShowCategoryCustomization(false);
                                }}
                                disabled={loading}
                              >
                                Voltar
                              </button>
                            </div>
                          </>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              )}
              {msg.needsConfirmation && pendingTransaction && (
                <div className="ai-confirmation-buttons">
                  <button
                    type="button"
                    className="ai-confirm-btn"
                    onClick={handleConfirmTransaction}
                    disabled={loading}
                  >
                    ✅ Confirmar
                  </button>
                  <button
                    type="button"
                    className="ai-reject-btn"
                    onClick={handleRejectTransaction}
                    disabled={loading}
                  >
                    ❌ Cancelar
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <form onSubmit={handleSubmit} className="ai-form">
        <div className="ai-input-wrapper">
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder={
              conversation.length === 0 
                ? "Ex: gastei com mercado o valor de 50 reais\nou: fiz uma compra de uma televisão no valor de 1500 em 10x começando no dia 15\nou: recebi um valor de 500 reais"
                : "Responda à pergunta da IA ou forneça mais informações..."
            }
            className="ai-textarea"
            rows="3"
            disabled={loading}
            autoFocus
          />
        </div>
        
        {conversation.length === 0 && (
          <div className="ai-examples">
            <p className="ai-examples-title">Exemplos:</p>
            <div className="ai-examples-list">
              <button 
                type="button"
                className="ai-example-btn"
                onClick={() => setText('gastei com mercado o valor de 50 reais')}
                disabled={loading}
              >
                💰 Gastei com mercado o valor de 50 reais
              </button>
              <button 
                type="button"
                className="ai-example-btn"
                onClick={() => setText('fiz uma compra de uma televisão no valor de 1500 em 10x começando no dia 15')}
                disabled={loading}
              >
                📺 Compra parcelada de 1500 em 10x (com data)
              </button>
              <button 
                type="button"
                className="ai-example-btn"
                onClick={() => setText('recebi um valor de 500 reais')}
                disabled={loading}
              >
                💵 Recebi um valor de 500 reais
              </button>
            </div>
          </div>
        )}
        
        <div className="ai-actions">
          <button 
            type="button" 
            className="ai-cancel-btn"
            onClick={handleReset}
            disabled={loading}
          >
            {conversation.length > 0 ? 'Cancelar' : 'Fechar'}
          </button>
          <button 
            type="submit" 
            className="ai-submit-btn"
            disabled={loading || !text.trim()}
          >
            {loading ? 'Processando...' : conversation.length === 0 ? 'Criar Transação' : 'Enviar'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default AiTransactionInput;

