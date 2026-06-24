# Use the official image
FROM quay.io/keycloak/keycloak:latest

ENV KC_DB=mysql
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_PROXY_HEADERS=xforwarded

# Build once so the Droplet doesn't have to
RUN /opt/keycloak/bin/kc.sh build

# The optimized command to run at startup
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized"]