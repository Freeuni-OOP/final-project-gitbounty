# Stage 1: Build (Keep this as is)
FROM node:24-alpine AS builder
WORKDIR /app

# 1. Copy only manifest files first
# If these haven't changed, Docker keeps the 'npm install' layer
COPY package*.json ./

# 2. Install dependencies
RUN npm install

# 3. Now copy the rest of your source code
COPY . .

# 4. Run the build
RUN npm run build

# Stage 2: Tiny container to hold the files
FROM alpine:latest
# Copy the built assets to a directory
COPY --from=builder /app/dist /www/data