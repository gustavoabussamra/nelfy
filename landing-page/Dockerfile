FROM node:18-alpine as builder

WORKDIR /app

# Aceitar argumentos de build para variáveis de ambiente
ARG VITE_API_URL=http://72.61.134.94:8085
ARG VITE_FRONTEND_URL=http://72.61.134.94:3002

# Definir variáveis de ambiente para o build
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_FRONTEND_URL=$VITE_FRONTEND_URL

COPY package.json ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

