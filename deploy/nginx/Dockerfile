# Nginx image with the static frontend baked in (DEC-67, DEC-137, DEC-151).
# PIN: replace the FROM line with the nginx:stable@sha256:... line from scripts/pin-images.sh
FROM nginx:stable

RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/nginx.conf
COPY bootstrap.conf /etc/nginx/bootstrap.conf
COPY templates/ /etc/nginx/templates/
COPY snippets/ /etc/nginx/snippets/
COPY entrypoint/40-https-or-bootstrap.sh /docker-entrypoint.d/40-https-or-bootstrap.sh
COPY site/ /usr/share/nginx/html/
