
# Stage 1: Build React application
FROM node:20-alpine AS build

WORKDIR /app

# Copy package definitions first
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci

# Copy application source
COPY . .

# API endpoint supplied during Docker build
ARG VITE_API_ORIGIN

ENV VITE_API_ORIGIN=${VITE_API_ORIGIN}

# Generate production build
RUN npm run build

# Stage 2: Nginx production server
FROM nginx:alpine

# Copy Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy only production frontend files
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
