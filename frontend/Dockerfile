# --- Build stage: Node + JRE for shadow-cljs compilation ---
FROM node:20-slim AS builder

# Install JRE (needed by shadow-cljs / ClojureScript compiler)
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install npm dependencies first (layer cache)
COPY package.json package-lock.json ./
RUN npm ci

# Copy source
COPY shadow-cljs.edn ./
COPY src/ src/
COPY public/ public/

# Build args injected at build time
ARG API_BASE=""
ARG GOOGLE_MAPS_API_KEY=""
ARG APP_ENV="prod"

# Compile ClojureScript release build with overridden closure-defines
RUN npx shadow-cljs release app \
    --config-merge "{:closure-defines {app.config/API_BASE \"${API_BASE}\" app.config/GOOGLE_MAPS_API_KEY \"${GOOGLE_MAPS_API_KEY}\" app.config/APP_ENV \"${APP_ENV}\"}}"

# --- Runtime stage: Nginx serves static assets ---
FROM nginx:1-alpine

# Remove default config
RUN rm /etc/nginx/conf.d/default.conf

# Copy our nginx config (uses envsubst for PORT)
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Copy built assets
COPY --from=builder /app/public /usr/share/nginx/html

EXPOSE 8080
ENV PORT=8080
