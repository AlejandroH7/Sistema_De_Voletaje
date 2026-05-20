#!/bin/bash
set -e

# Configuración
BACKUP_DIR="/backups"
DB_NAME="soldout_db"
DB_USER="${POSTGRES_USER:-admin}"
DB_HOST="${POSTGRES_HOST:-postgres}"
DB_PORT="${POSTGRES_PORT:-5432}"
PGPASSWORD="${POSTGRES_PASSWORD:-admin123}"

# Verificar argumento
if [ -z "$1" ]; then
  echo "Uso: restore.sh <archivo_backup.sql.gz>"
  echo "Backups disponibles:"
  ls -lh "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null || echo "No hay backups"
  exit 1
fi

BACKUP_FILE="$1"

# Verificar que el archivo existe
if [ ! -f "$BACKUP_FILE" ]; then
  BACKUP_FILE="$BACKUP_DIR/$1"
  if [ ! -f "$BACKUP_FILE" ]; then
    echo "ERROR: Archivo no encontrado: $1"
    exit 1
  fi
fi

echo "======================================"
echo "RESTAURACIÓN INICIADA"
echo "Archivo: $BACKUP_FILE"
echo "Base de datos: $DB_NAME"
echo "======================================"

export PGPASSWORD

# Restaurar
gunzip -c "$BACKUP_FILE" | psql \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  --no-password

echo "======================================"
echo "RESTAURACIÓN COMPLETADA: $(date)"
echo "======================================"
