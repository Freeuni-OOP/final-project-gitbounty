# Use the official image
FROM quay.io/keycloak/keycloak:latest

# 2. Set the build-time configuration to match your compose.yaml
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_PROXY_HEADERS=xforwarded
# Add any features you specifically enabled in compose.yaml
ENV KC_FEATURES=hostname

# 3. Build once so the Droplet doesn't have to (this bakes the settings above)
RUN /opt/keycloak/bin/kc.sh build
# The optimized command to run at startup
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized"]