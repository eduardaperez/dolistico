#!/bin/bash
set -e

echo "[CACHEMANAGER INIT] Aguardando Redis inicializar..."
sleep 2

REDIS_CLI="redis-cli"

echo "[CACHEMANAGER INIT] Testando conexão"
$REDIS_CLI PING || { echo "Redis não está acessível!"; exit 1; }

echo "[CACHEMANAGER INIT] Criando usuários e permissões..."

# ================================================================= ( accounts init )
if [ -n "$ACCOUNTS_CACHEMANAGER_USER" ] && [ -n "$ACCOUNTS_CACHEMANAGER_PASSWORD" ]; then
  echo "[CACHEMANAGER INIT] Criando usuário do Accounts..."
  $REDIS_CLI ACL SETUSER "$ACCOUNTS_CACHEMANAGER_USER" on \
      ">$ACCOUNTS_CACHEMANAGER_PASSWORD" \
      resetkeys resetchannels \
      +publish +subscribe +psubscribe \
      +@read +@write \
      "~${ACCOUNTS_CHANNEL_INIT}" \
      "~${ACCOUNTS_CHANNEL_INIT}:*" \
      "~__keyevent@0__:expired" \
      "~__keyevent@0__:*"
fi
# ================================================================== ( accounts end )

# ==================================================================== ( tasks init )
if [ -n "$TASKS_CACHEMANAGER_USER" ] && [ -n "$TASKS_CACHEMANAGER_PASSWORD" ]; then
  echo "[CACHEMANAGER INIT] Criando usuário do Tasks..."
  $REDIS_CLI ACL SETUSER "$TASKS_CACHEMANAGER_USER" on \
      ">$TASKS_CACHEMANAGER_PASSWORD" \
      resetkeys resetchannels \
      +publish +subscribe +psubscribe \
      +@read +@write \
      "~${TASKS_CHANNEL_INIT}" \
      "~${TASKS_CHANNEL_INIT}:*" \
      "~__keyevent@0__:expired" \
      "~__keyevent@0__:*"
fi
# ===================================================================== ( tasks end )

# ============================================================= ( default user init )
echo "[CACHEMANAGER INIT] Desativando usuário default..."
$REDIS_CLI ACL SETUSER default off

echo "[CACHEMANAGER INIT] Finalizado com sucesso"
# ============================================================= ( default user end )
