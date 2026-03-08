#!/usr/bin/env bash
# Generates self-signed TLS certificates for the local Mosquitto test broker.
# Run this script once before starting the broker (docker compose up) or running tests.
# Requires: openssl, keytool (from any JDK installation)
set -euo pipefail

CERT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/certs"
mkdir -p "$CERT_DIR"

EXT_FILE="$(mktemp)"
cat > "$EXT_FILE" << 'EOF'
[req]
req_extensions = v3_req
[v3_req]
subjectAltName = @alt_names
[alt_names]
DNS.1 = localhost
IP.1 = 127.0.0.1
EOF

echo "Generating CA key and certificate..."
openssl genrsa -out "$CERT_DIR/ca.key" 2048
openssl req -new -x509 -days 3650 -key "$CERT_DIR/ca.key" \
    -out "$CERT_DIR/ca.crt" \
    -subj "/CN=mktt-test-CA/O=mktt-test/C=IT"

echo "Generating server key and certificate..."
openssl genrsa -out "$CERT_DIR/server.key" 2048
openssl req -new -key "$CERT_DIR/server.key" \
    -out "$CERT_DIR/server.csr" \
    -subj "/CN=localhost/O=mktt-test/C=IT"
openssl x509 -req -days 3650 \
    -in "$CERT_DIR/server.csr" \
    -CA "$CERT_DIR/ca.crt" \
    -CAkey "$CERT_DIR/ca.key" \
    -CAcreateserial \
    -out "$CERT_DIR/server.crt" \
    -extfile "$EXT_FILE" \
    -extensions v3_req

echo "Creating JVM trust store..."
keytool -import -trustcacerts -alias mktt-test-ca \
    -file "$CERT_DIR/ca.crt" \
    -keystore "$CERT_DIR/truststore.jks" \
    -storepass changeit -noprompt

echo "Cleaning up intermediate files..."
rm -f "$CERT_DIR/ca.key" "$CERT_DIR/server.csr" "$CERT_DIR/ca.srl" "$EXT_FILE"

# Set the server key world-readable (0644). This is intentional for this
# test-only key: when Mosquitto runs inside Docker its process user differs from
# the host user that owns the file, so 0600 would prevent it from loading the
# key. The key is self-signed and used only for local testing.
chmod 644 "$CERT_DIR/server.key"

echo "Done. Certificates written to $CERT_DIR"
