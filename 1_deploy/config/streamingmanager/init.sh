#!/bin/bash
set -e

exec rpk redpanda start \
  --overprovisioned \
  --smp 1 \
  --memory 1G \
  --reserve-memory 0M \
  --node-id 0 \
  --kafka-addr PLAINTEXT://0.0.0.0:${STREAMINGMANAGER_PORT} \
  --advertise-kafka-addr PLAINTEXT://streamingmanager:${STREAMINGMANAGER_PORT}
