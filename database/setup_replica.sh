#!/bin/bash
set -e

echo "Iniciando configuración de réplica..."

# Limpiar directorio de datos
rm -rf /var/lib/postgresql/data/*

# Hacer copia base del Primary
PGPASSWORD=$REPLICATION_PASSWORD pg_basebackup \
  -h $PRIMARY_HOST \
  -p $PRIMARY_PORT \
  -U $REPLICATION_USER \
  -D /var/lib/postgresql/data \
  -Fp -Xs -R -P

# Copiar configuración de replica
cp /etc/postgresql/postgresql.conf /var/lib/postgresql/data/postgresql.conf

echo "Réplica configurada exitosamente"
