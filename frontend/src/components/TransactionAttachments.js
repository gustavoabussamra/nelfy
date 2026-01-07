import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { toast } from 'react-toastify';
import './TransactionAttachments.css';

const TransactionAttachments = ({ transactionId }) => {
  const [attachments, setAttachments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [showUpload, setShowUpload] = useState(false);

  useEffect(() => {
    if (transactionId) {
      loadAttachments();
    }
  }, [transactionId]);

  const loadAttachments = async () => {
    try {
      setLoading(true);
      const response = await api.get(`/attachments/transaction/${transactionId}`);
      setAttachments(response.data);
    } catch (error) {
      console.error('Erro ao carregar anexos:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (file.size > 10 * 1024 * 1024) {
      toast.error('Arquivo muito grande. Tamanho máximo: 10MB');
      return;
    }

    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('description', '');

      await api.post(`/attachments/transaction/${transactionId}`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      toast.success('Anexo enviado com sucesso!');
      setShowUpload(false);
      loadAttachments();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao enviar anexo');
    } finally {
      setUploading(false);
      e.target.value = ''; // Reset input
    }
  };

  const handleDownload = async (attachmentId, fileName) => {
    try {
      const response = await api.get(`/attachments/${attachmentId}/download`, {
        responseType: 'blob',
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      toast.error('Erro ao baixar anexo');
    }
  };

  const handleDelete = async (attachmentId) => {
    if (!window.confirm('Tem certeza que deseja excluir este anexo?')) {
      return;
    }

    try {
      await api.delete(`/attachments/${attachmentId}`);
      toast.success('Anexo excluído com sucesso!');
      loadAttachments();
    } catch (error) {
      toast.error('Erro ao excluir anexo');
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  const getFileIcon = (fileType) => {
    if (fileType?.includes('image')) return '🖼️';
    if (fileType?.includes('pdf')) return '📄';
    if (fileType?.includes('excel') || fileType?.includes('spreadsheet')) return '📊';
    return '📎';
  };

  if (!transactionId) return null;

  return (
    <div className="transaction-attachments">
      <div className="attachments-header">
        <h4>📎 Anexos</h4>
        <button
          className="btn-upload"
          onClick={() => setShowUpload(!showUpload)}
          disabled={uploading}
        >
          {uploading ? 'Enviando...' : '+ Adicionar'}
        </button>
      </div>

      {showUpload && (
        <div className="upload-section">
          <input
            type="file"
            id={`file-upload-${transactionId}`}
            onChange={handleUpload}
            accept="image/*,application/pdf,.xlsx,.xls"
            disabled={uploading}
            style={{ display: 'block', marginTop: '8px', padding: '8px', width: '100%' }}
          />
          {uploading && (
            <div style={{ marginTop: '8px', color: '#6366f1' }}>
              Enviando arquivo...
            </div>
          )}
        </div>
      )}

      {loading ? (
        <div className="attachments-loading">Carregando anexos...</div>
      ) : attachments.length === 0 ? (
        <div className="attachments-empty">Nenhum anexo adicionado</div>
      ) : (
        <div className="attachments-list">
          {attachments.map((attachment) => (
            <div key={attachment.id} className="attachment-item">
              <div className="attachment-icon">
                {getFileIcon(attachment.fileType)}
              </div>
              <div className="attachment-info">
                <div className="attachment-name">{attachment.fileName}</div>
                <div className="attachment-meta">
                  {formatFileSize(attachment.fileSize)} • {new Date(attachment.createdAt).toLocaleDateString('pt-BR')}
                </div>
              </div>
              <div className="attachment-actions">
                <button
                  className="btn-download"
                  onClick={() => handleDownload(attachment.id, attachment.fileName)}
                  title="Baixar"
                >
                  ⬇️
                </button>
                <button
                  className="btn-delete-attachment"
                  onClick={() => handleDelete(attachment.id)}
                  title="Excluir"
                >
                  🗑️
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default TransactionAttachments;



