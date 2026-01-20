#!/bin/bash
set -e

echo "#########################################"
echo "Starting Redpanda with SASL..."
echo "#########################################"

rpk redpanda start \
  --overprovisioned \
  --smp 1 \
  --memory 1G \
  --reserve-memory 0M \
  --node-id 0 \
  --kafka-addr PLAINTEXT://0.0.0.0:${STREAMINGMANAGER_PORT} \
  --advertise-kafka-addr PLAINTEXT://streamingmanager:${STREAMINGMANAGER_PORT} \
  --set redpanda.enable_sasl=true \
  --set redpanda.superusers='["admin"]' &

REDPANDA_PID=$!

echo "Waiting for Redpanda..."
sleep 10

echo "Creating admin user..."

rpk acl user create admin \
  --password admin123 \
  --mechanism SCRAM-SHA-256 || true

echo "Applying full ACL permissions to admin user..."

# Cluster admin
rpk acl create \
  --allow-principal User:admin \
  --operation ALL \
  --cluster || true

# All topics
rpk acl create \
  --allow-principal User:admin \
  --operation ALL \
  --topic '*' || true

# All consumer groups
rpk acl create \
  --allow-principal User:admin \
  --operation ALL \
  --group '*' || true

# All transactional IDs
rpk acl create \
  --allow-principal User:admin \
  --operation ALL \
  --transactional-id '*' || true

echo "#########################################"
echo "Admin user ready with FULL access"
echo "#########################################"

wait $REDPANDA_PID
