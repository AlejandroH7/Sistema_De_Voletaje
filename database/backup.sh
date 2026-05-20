#!/bin/bash
set -e

# Configuración
BACKUP_DIR="/backups"
DB_NAME="soldout_db"
DB_USER="${POSTGRES_USER:-admin}"
DB_HOST="${POSTGRES_HOST:-postgres}"
DB_PORT="${POSTGRES_PORT:-5432}"
PGPASSWORD="${POSTGRES_PASSWORD:-admin123}"
RETENTION_DAYS=7
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
BACKUP_FILE="$BACKUP_DIR/backup_${TIMESTAMP}.sql.gz"
TEMP_FILE="$BACKUP_DIR/backup_${TIMESTAMP}.sql"

echo "======================================"
echo "BACKUP INICIADO: $TIMESTAMP"
echo "======================================"

# Crear directorio si no existe
mkdir -p "$BACKUP_DIR"

# Ejecutar pg_dump
export PGPASSWORD
pg_dump \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  --no-password \
  --verbose \
  --format=plain \
  > "$TEMP_FILE"

gzip -c "$TEMP_FILE" > "$BACKUP_FILE"
rm -f "$TEMP_FILE"

# Verificar que el backup se creó correctamente
if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
  SIZE=$(du -sh "$BACKUP_FILE" | cut -f1)
  echo "BACKUP EXITOSO: $BACKUP_FILE ($SIZE)"
else
  echo "ERROR: El backup falló o está vacío"
  exit 1
fi

# Eliminar backups más viejos de RETENTION_DAYS días
echo "Eliminando backups más viejos de $RETENTION_DAYS días..."
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +$RETENTION_DAYS -delete
echo "Limpieza completada"

# Listar backups disponibles
echo "Backups disponibles:"
ls -lh "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null || echo "No hay backups previos"

echo "======================================"
echo "BACKUP COMPLETADO: $(date)"
echo "======================================"
