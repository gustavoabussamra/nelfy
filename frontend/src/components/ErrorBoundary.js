import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Erro capturado pelo ErrorBoundary:', error, errorInfo);
    this.setState({
      error,
      errorInfo
    });
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          padding: '20px',
          textAlign: 'center'
        }}>
          <h1 style={{ color: '#ef4444', marginBottom: '20px' }}>❌ Erro na aplicação</h1>
          <p style={{ marginBottom: '20px', color: '#64748b' }}>
            Ocorreu um erro ao carregar a aplicação.
          </p>
          {this.state.error && (
            <div style={{
              backgroundColor: '#fee2e2',
              padding: '15px',
              borderRadius: '8px',
              maxWidth: '600px',
              marginBottom: '20px'
            }}>
              <p style={{ fontWeight: 'bold', marginBottom: '10px' }}>Detalhes do erro:</p>
              <pre style={{ 
                fontSize: '12px', 
                overflow: 'auto',
                textAlign: 'left',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word'
              }}>
                {this.state.error.toString()}
                {this.state.errorInfo && this.state.errorInfo.componentStack}
              </pre>
            </div>
          )}
          <button
            onClick={() => {
              window.location.reload();
            }}
            style={{
              padding: '10px 20px',
              backgroundColor: '#6366f1',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              cursor: 'pointer',
              fontSize: '16px'
            }}
          >
            Recarregar página
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;







