
# ==========================================
# Stage 1: Build React application
# ==========================================
FROM node:20-alpine AS build

WORKDIR /app

# Copy package definitions
COPY package.json package-lock.json ./

# Install dependencies without automatically executing lifecycle scripts
RUN npm ci --ignore-scripts

# Copy application source
COPY . .

# API endpoint supplied during Docker build
ARG VITE_API_ORIGIN

ENV VITE_API_ORIGIN=${VITE_API_ORIGIN}

# Generate production build
RUN npm run build


# ==========================================
# Stage 2: Nginx production server
# ==========================================
FROM nginxinc/nginx-unprivileged:alpine

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy production frontend files
COPY --from=build /app/dist /usr/share/nginx/html

# Nginx unprivileged image listens on port 8080
EXPOSE 8080
