FROM node:24-alpine AS builder
WORKDIR /app

COPY package*.json ./

# OPTIMIZATION: Mount the local npm cache directory to host storage.
# This makes 'npm install' incredibly rapid on sequential builds.
RUN --mount=type=cache,target=/root/.npm npm install

COPY . .
RUN npm run build

# Stage 2: Tiny container to hold the files
FROM alpine:latest
# Copy the built assets to a directory
COPY --from=builder /app/dist /www/data