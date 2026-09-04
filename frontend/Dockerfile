FROM public.ecr.aws/docker/library/node:22-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com

COPY . ./
RUN npm run build

FROM public.ecr.aws/docker/library/caddy:2-alpine

COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv

EXPOSE 80 443 443/udp
