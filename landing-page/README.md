# Nelfy Landing Page

Landing page moderna e interativa para venda do sistema Nelfy.

## Tecnologias

- React 18
- Vite
- Framer Motion (animações)
- React Icons
- Axios
- React Router DOM
- React Toastify

## Recursos

- ✅ Design moderno e responsivo
- ✅ Animações suaves com Framer Motion
- ✅ Integração com gateway de pagamento (Mercado Pago)
- ✅ Seções: Hero, Features, Pricing, Testimonials, CTA, Footer
- ✅ Otimizado para SEO
- ✅ Performance otimizada

## Desenvolvimento

```bash
npm install
npm run dev
```

## Build para Produção

```bash
npm run build
```

## Docker

A landing page está configurada para rodar no Docker:

```bash
docker-compose up landing-page
```

Acesse em: http://localhost:3001

## Estrutura

```
landing-page/
├── src/
│   ├── components/     # Componentes reutilizáveis
│   ├── pages/          # Páginas
│   └── main.jsx        # Entry point
├── Dockerfile          # Configuração Docker
├── nginx.conf          # Configuração Nginx
└── package.json        # Dependências
```




