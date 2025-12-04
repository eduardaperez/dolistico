#!/bin/bash
set -e

REDIS_CLI="redis-cli"

echo "[CACHEMANAGER INIT] Testing connection"
$REDIS_CLI PING || { echo "Redis is not accessible!"; exit 1; }

# ================================================================= ( accounts init )
if [ -n "$ACCOUNTS_CACHEMANAGER_USER" ] && [ -n "$ACCOUNTS_CACHEMANAGER_PASSWORD" ]; then
  echo "[CACHEMANAGER INIT] Creating Accounts user..."
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
  echo "[CACHEMANAGER INIT] Creating Tasks user..."
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
echo "[CACHEMANAGER INIT] Setting password for default user..."
$REDIS_CLI ACL SETUSER default on ">$CACHEMANAGER_ADMIN_PASSWORD"

echo "[CACHEMANAGER INIT] Completed successfully"
# ============================================================= ( default user end )