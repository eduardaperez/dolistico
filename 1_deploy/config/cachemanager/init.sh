#!/bin/bash
set -e

echo "[CACHEMANAGER INIT] Aguardando Redis inicializar..."
sleep 2

# Usar Redis CLI sem senha, já que o Redis não tem --requirepass
REDIS_CLI="redis-cli"

echo "[CACHEMANAGER INIT] Testando conexão"
$REDIS_CLI PING || { echo "Redis não está acessível!"; exit 1; }

echo "[CACHEMANAGER INIT] Criando usuários e permissões..."

# =============================================================================
# ACCOUNTS SERVICE
# =============================================================================
if [ -n "$ACCOUNTS_CACHEMANAGER_USER" ] && [ -n "$ACCOUNTS_CACHEMANAGER_PASSWORD" ]; then
  echo "[CACHEMANAGER INIT] Criando usuário do Accounts..."
  $REDIS_CLI ACL SETUSER "$ACCOUNTS_CACHEMANAGER_USER" on \
      ">$ACCOUNTS_CACHEMANAGER_PASSWORD" \
      resetkeys resetchannels \
      +publish +subscribe +psubscribe +pubsub \
      "~${ACCOUNTS_CHANNEL_INIT}" \
      "~${ACCOUNTS_CHANNEL_INIT}:*" \
      "~__keyevent@0__:*" \
      "~__keyspace@0__:*" \
      "~__redis__:invalidate"
fi

# =============================================================================
# TASKS SERVICE
# =============================================================================
if [ -n "$TASKS_CACHEMANAGER_USER" ] && [ -n "$TASKS_CACHEMANAGER_PASSWORD" ]; then
  echo "[CACHEMANAGER INIT] Criando usuário do Tasks..."
  $REDIS_CLI ACL SETUSER "$TASKS_CACHEMANAGER_USER" on \
      ">$TASKS_CACHEMANAGER_PASSWORD" \
      resetkeys resetchannels \
      +publish +subscribe +psubscribe +pubsub \
      "~${TASKS_CHANNEL_INIT}" \
      "~${TASKS_CHANNEL_INIT}:*" \
      "~__keyevent@0__:*" \
      "~__keyspace@0__:*" \
      "~__redis__:invalidate"
fi

echo "[CACHEMANAGER INIT] Finalizado com sucesso"
