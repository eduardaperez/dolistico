#!/bin/bash
set -e

# -----------------------------------------------------------------------------------
# Users and passwords
# -----------------------------------------------------------------------------------
ACCOUNTS_CACHEMANAGER_USER="${ACCOUNTS_CACHEMANAGER_USER}"
ACCOUNTS_CACHEMANAGER_PASSWORD="${ACCOUNTS_CACHEMANAGER_PASSWORD}"
ACCOUNTS_TOPIC_INIT="${ACCOUNTS_TOPIC_INIT}"

TASKS_CACHEMANAGER_USER="${TASKS_CACHEMANAGER_USER}"
TASKS_CACHEMANAGER_PASSWORD="${TASKS_CACHEMANAGER_PASSWORD}"
TASKS_TOPIC_INIT="${TASKS_TOPIC_INIT}"

# -----------------------------------------------------------------------------------
# Create ACL users in Redis
# -----------------------------------------------------------------------------------
REDIS_CLI="redis-cli -a ${CACHEMANAGER_ADMIN_PASSWORD}"

# Create Accounts user
$REDIS_CLI ACL SETUSER "$ACCOUNTS_CACHEMANAGER_USER" on ">$ACCOUNTS_CACHEMANAGER_PASSWORD" "~$ACCOUNTS_TOPIC_INIT:*" "+@all"

# Create Tasks user
$REDIS_CLI ACL SETUSER "$TASKS_CACHEMANAGER_USER" on ">$TASKS_CACHEMANAGER_PASSWORD" "~$TASKS_TOPIC_INIT:*" "+@all"