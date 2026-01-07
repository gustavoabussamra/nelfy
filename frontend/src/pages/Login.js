import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';
import './Auth.css';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await login(email, password);
      
      console.log('=== LOGIN SUCESSO ===');
      console.log('Response completa:', JSON.stringify(response, null, 2));
      console.log('User recebido:', response.user);
      console.log('Role recebida:', response.user?.role);
      
      toast.success('Login realizado com sucesso!');
      
      // Aguardar um pouco para garantir que o estado foi atualizado
      await new Promise(resolve => setTimeout(resolve, 100));
      
      // Redirecionar baseado na role - verificação mais robusta
      const userRole = (response.user?.role || '').toUpperCase().trim();
      console.log('Role normalizada:', userRole);
      
      if (userRole === 'ADMIN') {
        console.log('>>> REDIRECIONANDO ADMIN PARA /admin');
        navigate('/admin', { replace: true });
      } else {
        console.log('>>> REDIRECIONANDO USER PARA /');
        navigate('/', { replace: true });
      }
      
    } catch (error) {
      console.error('Erro no login:', error);
      console.error('Status:', error.response?.status);
      console.error('Data:', error.response?.data);
      
      let errorMessage = 'Erro ao fazer login';
      
      if (error.response?.status === 403) {
        errorMessage = 'Acesso negado. Verifique se o backend está rodando e configurado corretamente.';
      } else if (error.response?.status === 401) {
        errorMessage = error.response?.data?.message || 'Credenciais inválidas';
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      toast.error(errorMessage);
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1 className="auth-logo">💰 Fin</h1>
          <h2>Bem-vindo de volta!</h2>
          <p>Entre na sua conta para continuar</p>
        </div>
        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="seu@email.com"
            />
          </div>
          <div className="form-group">
            <label>Senha</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="••••••••"
            />
          </div>
          <button type="submit" className="auth-button" disabled={loading}>
            {loading ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
        <div className="auth-footer">
          <p>
            Não tem uma conta? <Link to="/register">Cadastre-se</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;

