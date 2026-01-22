#!/bin/bash
set -e

############################################################### (Start Redpanda init)
rpk redpanda start \
  --overprovisioned \
  --smp 1 \
  --memory 1G \
  --reserve-memory 0M \
  --node-id 0 \
  --kafka-addr PLAINTEXT://0.0.0.0:${STREAMINGMANAGER_PORT} \
  --advertise-kafka-addr PLAINTEXT://streamingmanager:${STREAMINGMANAGER_PORT} \
  --set redpanda.enable_sasl=true \
  --set "redpanda.superusers=[\"${STREAMINGMANAGER_ADMIN_USER}\"]" &

REDPANDA_PID=$!

# Give Redpanda time to fully start
sleep 12
################################################################ (Start Redpanda end)

#################################################################### ( RPK Auth init)
export RPK_BROKERS=streamingmanager:${STREAMINGMANAGER_PORT}
export RPK_SASL_MECHANISM=SCRAM-SHA-256
export RPK_SASL_USERNAME=${STREAMINGMANAGER_ADMIN_USER}
export RPK_SASL_PASSWORD=${STREAMINGMANAGER_ADMIN_PASSWORD}
############################################################### ##### ( RPK Auth end)

###################################################################### ( Admin user )

# Create user
# -----------------------------------------------------------------------------------
rpk acl user create "${STREAMINGMANAGER_ADMIN_USER}" \
  --password "${STREAMINGMANAGER_ADMIN_PASSWORD}" \
  --mechanism SCRAM-SHA-256 || true
# -----------------------------------------------------------------------------------

# Cluster admin
rpk acl create \
  --allow-principal "User:${STREAMINGMANAGER_ADMIN_USER}" \
  --operation ALL \
  --cluster || true

# All topics
rpk acl create \
  --allow-principal "User:${STREAMINGMANAGER_ADMIN_USER}" \
  --operation ALL \
  --topic '*' || true

# All consumer groups
rpk acl create \
  --allow-principal "User:${STREAMINGMANAGER_ADMIN_USER}" \
  --operation ALL \
  --group '*' || true

# All transactional IDs
rpk acl create \
  --allow-principal "User:${STREAMINGMANAGER_ADMIN_USER}" \
  --operation ALL \
  --transactional-id '*' || true

# Admin topic bootstrap
rpk topic create "send.simple.email.v1" || true
####################################################################### ( Admin user)

#################################################################### ( Accounts init)

# Create user
# -----------------------------------------------------------------------------------
rpk acl user create "${ACCOUNTS_STREAMINGMANAGER_USER}" \
  --password "${ACCOUNTS_STREAMINGMANAGER_PASSWORD}" \
  --mechanism SCRAM-SHA-256 || true
# -----------------------------------------------------------------------------------

rpk acl create \
  --allow-principal "User:${ACCOUNTS_STREAMINGMANAGER_USER}" \
  --operation CREATE \
  --topic "accounts." \
  --resource-pattern-type prefixed || true

rpk acl create \
  --allow-principal "User:${ACCOUNTS_STREAMINGMANAGER_USER}" \
  --operation ALL \
  --topic "accounts." \
  --resource-pattern-type prefixed || true

rpk acl create \
  --allow-principal "User:${ACCOUNTS_STREAMINGMANAGER_USER}" \
  --operation ALL \
  --group "accounts." \
  --resource-pattern-type prefixed || true
##################################################################### ( Accounts end)

echo "#########################################"
echo "Redpanda and users successfully configured"
echo "#########################################"

wait $REDPANDA_PID