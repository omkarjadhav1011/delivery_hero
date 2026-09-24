# syntax=docker/dockerfile:1
# Local and CI web image: builds the static frontend from source and serves it with
# production's Nginx settings, headers and routes (document 18).
FROM node:24-slim AS build
ENV NEXT_TELEMETRY_DISABLED=1
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY frontend/ ./
# The postbuild script writes nginx/csp.conf with the inline-script hashes (LLD section 6.6)
RUN npm run build

FROM nginx:stable
RUN rm -f /etc/nginx/conf.d/default.conf
COPY deploy/nginx/nginx.conf /etc/nginx/nginx.conf
COPY deploy/nginx/snippets/ /etc/nginx/snippets/
COPY deploy/local/nginx-local.conf /etc/nginx/conf.d/delivery-hero.conf
COPY --from=build /src/frontend/out/ /usr/share/nginx/html/
COPY --from=build /src/frontend/nginx/csp.conf /etc/nginx/snippets/csp.conf
