#!/bin/bash

# ============================================================== ( set default init )
set -e
REDIS_CLI="redis-cli"
echo "[CACHEMANAGER INIT] Testing connection..."
$REDIS_CLI PING || { echo "Redis not accessible"; exit 1; }

echo "[CACHEMANAGER INIT] Setting password for default user..."
$REDIS_CLI ACL SETUSER default on ">$CACHEMANAGER_ADMIN_PASSWORD"
export REDISCLI_AUTH="$CACHEMANAGER_ADMIN_PASSWORD"
redis-cli PING
# =============================================================== ( set default end )

# ================================================================= ( accounts init )
if [ -n "$ACCOUNTS_CACHEMANAGER_USER" ] && [ -n "$ACCOUNTS_CACHEMANAGER_PASSWORD" ]; then
  echo "[CACHEMANAGER INIT] Creating Accounts user..."

  redis-cli ACL SETUSER "$ACCOUNTS_CACHEMANAGER_USER" on \
      ">$ACCOUNTS_CACHEMANAGER_PASSWORD" \
      resetkeys resetchannels \
      ~accounts-* \
      +@read +@write \
      +publish +subscribe +psubscribe \
      -scan -keys -type -ttl -pttl -@dangerous -@admin \
      "&__keyevent@0__:expired"
fi
# ================================================================== ( accounts end )

# ==================================================================== ( tasks init )
if [ -n "$TASKS_CACHEMANAGER_USER" ] && [ -n "$TASKS_CACHEMANAGER_PASSWORD" ]; then
  echo "[CACHEMANAGER INIT] Creating Tasks user..."

  redis-cli ACL SETUSER "$TASKS_CACHEMANAGER_USER" on \
      ">$TASKS_CACHEMANAGER_PASSWORD" \
      resetkeys resetchannels \
      ~tasks-* \
      +@read +@write \
      +publish +subscribe +psubscribe \
      -scan -keys -type -ttl -pttl -@dangerous -@admin \
      "&__keyevent@0__:expired"
fi
# ===================================================================== ( tasks end )

echo "[CACHEMANAGER INIT] Completed successfully"