#!/usr/bin/env bash
set -u

BASE="http://localhost:8080"
USUARIOS="http://localhost:8081"
EVENTOS="http://localhost:8082"
INVENTARIO="http://localhost:8083"
RESERVAS="http://localhost:8084"
PAGOS="http://localhost:8085"
NOTIFS="http://localhost:8086"

RUN_ID="$(date +%s)"
ADMIN_EMAIL="admin_${RUN_ID}@soldout.com"
CLIENT_EMAIL="cliente_${RUN_ID}@soldout.com"
PASS="password123"

OK=0
FAIL=0

green() { printf "\033[32m%s\033[0m\n" "$1"; }
red() { printf "\033[31m%s\033[0m\n" "$1"; }
blue() { printf "\033[34m%s\033[0m\n" "$1"; }

assert_code() {
  local name="$1"
  local got="$2"
  local expected="$3"
  if [ "$got" = "$expected" ]; then
    green "OK  - $name => HTTP $got"
    OK=$((OK+1))
  else
    red "FAIL - $name => HTTP $got, esperado $expected"
    FAIL=$((FAIL+1))
  fi
}

assert_contains() {
  local name="$1"
  local text="$2"
  local expected="$3"
  if printf "%s" "$text" | grep -q "$expected"; then
    green "OK  - $name contiene '$expected'"
    OK=$((OK+1))
  else
    red "FAIL - $name no contiene '$expected'"
    echo "$text"
    FAIL=$((FAIL+1))
  fi
}

json_get() {
  python3 - "$1" "$2" <<'PY'
import json, sys
raw = sys.argv[1]
path = sys.argv[2].split(".")
try:
    data = json.loads(raw)
    cur = data
    for p in path:
        if p.isdigit():
            cur = cur[int(p)]
        else:
            cur = cur[p]
    print(cur)
except Exception:
    print("")
PY
}

print_json() {
  python3 -m json.tool 2>/dev/null || cat
}

section() {
  echo
  blue "════════════════════════════════════════"
  blue "$1"
  blue "════════════════════════════════════════"
}

section "0. PRECHECK DOCKER Y CONTENEDORES"
docker-compose ps

section "1. HEALTH CHECKS DIRECTOS"
for svc in \
  "api-gateway:$BASE" \
  "ms-usuarios:$USUARIOS" \
  "ms-eventos:$EVENTOS" \
  "ms-inventario:$INVENTARIO" \
  "ms-reservas:$RESERVAS" \
  "ms-pagos:$PAGOS" \
  "ms-notificaciones:$NOTIFS"
do
  name="${svc%%:*}"
  url="${svc#*:}"
  code="$(curl -s -o /dev/null -w "%{http_code}" "$url/actuator/health")"
  assert_code "health $name" "$code" "200"
done

section "2. PROMETHEUS ENDPOINTS"
for svc in \
  "api-gateway:$BASE" \
  "ms-usuarios:$USUARIOS" \
  "ms-eventos:$EVENTOS" \
  "ms-inventario:$INVENTARIO" \
  "ms-reservas:$RESERVAS" \
  "ms-pagos:$PAGOS" \
  "ms-notificaciones:$NOTIFS"
do
  name="${svc%%:*}"
  url="${svc#*:}"
  code="$(curl -s -o /dev/null -w "%{http_code}" "$url/actuator/prometheus")"
  assert_code "prometheus $name" "$code" "200"
done

section "3. SEGURIDAD GATEWAY SIN TOKEN"
code="$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/reservas")"
assert_code "GET /api/reservas sin token debe rechazar" "$code" "401"

code="$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/pagos" \
  -H "Content-Type: application/json" \
  -d '{"reserva_id":"00000000-0000-0000-0000-000000000000","usuario_id":"00000000-0000-0000-0000-000000000000","monto":100}')"
assert_code "POST /api/pagos sin token debe rechazar" "$code" "401"

section "4. REGISTRO Y LOGIN"
ADMIN_REG="$(curl -s -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Admin Test\",\"email\":\"$ADMIN_EMAIL\",\"contrasena\":\"$PASS\",\"telefono\":\"55550001\",\"rol\":\"ADMIN\"}")"
echo "$ADMIN_REG" | print_json
assert_contains "registro admin" "$ADMIN_REG" '"exito" : true\|"exito":true'

CLIENT_REG="$(curl -s -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Cliente Test\",\"email\":\"$CLIENT_EMAIL\",\"contrasena\":\"$PASS\",\"telefono\":\"55550002\",\"rol\":\"CLIENTE\"}")"
echo "$CLIENT_REG" | print_json
assert_contains "registro cliente" "$CLIENT_REG" '"exito" : true\|"exito":true'

DUP_REG_CODE="$(curl -s -o /tmp/dup_reg.json -w "%{http_code}" -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Cliente Test\",\"email\":\"$CLIENT_EMAIL\",\"contrasena\":\"$PASS\",\"telefono\":\"55550002\",\"rol\":\"CLIENTE\"}")"
assert_code "registro duplicado debe dar 409" "$DUP_REG_CODE" "409"
cat /tmp/dup_reg.json | print_json

BAD_LOGIN_CODE="$(curl -s -o /tmp/bad_login.json -w "%{http_code}" -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$CLIENT_EMAIL\",\"contrasena\":\"wrongpass\"}")"
assert_code "login incorrecto debe dar 401" "$BAD_LOGIN_CODE" "401"
cat /tmp/bad_login.json | print_json

ADMIN_LOGIN="$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"contrasena\":\"$PASS\"}")"
ADMIN_TOKEN="$(json_get "$ADMIN_LOGIN" "datos.token")"
ADMIN_ID="$(json_get "$ADMIN_LOGIN" "datos.usuario.id")"
echo "$ADMIN_LOGIN" | print_json
[ -n "$ADMIN_TOKEN" ] && green "OK  - token admin obtenido" && OK=$((OK+1)) || { red "FAIL - token admin vacío"; FAIL=$((FAIL+1)); }

CLIENT_LOGIN="$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$CLIENT_EMAIL\",\"contrasena\":\"$PASS\"}")"
CLIENT_TOKEN="$(json_get "$CLIENT_LOGIN" "datos.token")"
CLIENT_ID="$(json_get "$CLIENT_LOGIN" "datos.usuario.id")"
echo "$CLIENT_LOGIN" | print_json
[ -n "$CLIENT_TOKEN" ] && green "OK  - token cliente obtenido" && OK=$((OK+1)) || { red "FAIL - token cliente vacío"; FAIL=$((FAIL+1)); }

PROFILE_CODE="$(curl -s -o /tmp/profile.json -w "%{http_code}" "$BASE/api/usuarios/perfil" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
assert_code "perfil cliente con token" "$PROFILE_CODE" "200"
cat /tmp/profile.json | print_json

section "5. EVENTOS: LUGAR, EVENTO, PUBLICAR Y KAFKA evento.creado"
LUGAR_RESP="$(curl -s -X POST "$BASE/api/lugares" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Venue Test $RUN_ID\",\"direccion\":\"Zona 10\",\"ciudad\":\"Guatemala\",\"capacidadMaxima\":5000}")"
LUGAR_ID="$(json_get "$LUGAR_RESP" "datos.id")"
echo "$LUGAR_RESP" | print_json
[ -n "$LUGAR_ID" ] && green "OK  - lugar creado $LUGAR_ID" && OK=$((OK+1)) || { red "FAIL - lugar no creado"; FAIL=$((FAIL+1)); }

EVENTO_RESP="$(curl -s -X POST "$BASE/api/eventos" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"nombre\":\"Concierto E2E $RUN_ID\",
    \"descripcion\":\"Prueba end to end\",
    \"lugarId\":\"$LUGAR_ID\",
    \"fechaEvento\":\"2026-12-31T20:00:00\",
    \"tipoEvento\":\"MIXTO\"
  }")"
EVENTO_ID="$(json_get "$EVENTO_RESP" "datos.id")"
EVENTO_ESTADO="$(json_get "$EVENTO_RESP" "datos.estado")"
echo "$EVENTO_RESP" | print_json
[ "$EVENTO_ESTADO" = "BORRADOR" ] && green "OK  - evento creado BORRADOR" && OK=$((OK+1)) || { red "FAIL - evento no está BORRADOR"; FAIL=$((FAIL+1)); }

PUB_RESP="$(curl -s -X PUT "$BASE/api/eventos/$EVENTO_ID/publicar" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"secciones\": [
      {\"nombre\":\"General E2E $RUN_ID\",\"tipo\":\"GENERAL\",\"capacidad\":20,\"precio\":150.00,\"descripcion\":\"General\"},
      {\"nombre\":\"VIP E2E $RUN_ID\",\"tipo\":\"NUMERADO\",\"capacidad\":5,\"precio\":500.00,\"descripcion\":\"VIP\"}
    ]
  }")"
PUB_ESTADO="$(json_get "$PUB_RESP" "datos.estado")"
echo "$PUB_RESP" | print_json
[ "$PUB_ESTADO" = "ACTIVO" ] && green "OK  - evento publicado ACTIVO" && OK=$((OK+1)) || { red "FAIL - evento no quedó ACTIVO"; FAIL=$((FAIL+1)); }

echo "Esperando Kafka evento.creado -> ms-inventario..."
sleep 8

INV_RESP="$(curl -s "$BASE/api/inventario/$EVENTO_ID/secciones")"
echo "$INV_RESP" | print_json
SECCION_0_ID="$(json_get "$INV_RESP" "datos.0.seccion.id")"
SECCION_0_PRECIO="$(json_get "$INV_RESP" "datos.0.seccion.precio")"
SECCION_0_DISP="$(json_get "$INV_RESP" "datos.0.asientosDisponibles")"
[ -n "$SECCION_0_ID" ] && green "OK  - inventario inicializado seccion=$SECCION_0_ID precio=$SECCION_0_PRECIO disp=$SECCION_0_DISP" && OK=$((OK+1)) || { red "FAIL - inventario no inicializado"; FAIL=$((FAIL+1)); }

section "6. INVENTARIO DIRECTO: RESERVAR, LIBERAR Y SOBREVENTA"
RES_DIRECT_ID="$(python3 - <<'PY'
import uuid
print(uuid.uuid4())
PY
)"
RES_INV_CODE="$(curl -s -o /tmp/res_inv.json -w "%{http_code}" -X POST "$INVENTARIO/api/inventario/$SECCION_0_ID/reservar" \
  -H "Content-Type: application/json" \
  -d "{\"reservaId\":\"$RES_DIRECT_ID\",\"cantidad\":1}")"
assert_code "inventario reservar 1 asiento" "$RES_INV_CODE" "200"
cat /tmp/res_inv.json | print_json

LIB_INV_CODE="$(curl -s -o /tmp/lib_inv.json -w "%{http_code}" -X POST "$INVENTARIO/api/inventario/$SECCION_0_ID/liberar" \
  -H "Content-Type: application/json" \
  -d "{\"reservaId\":\"$RES_DIRECT_ID\",\"cantidad\":1}")"
assert_code "inventario liberar 1 asiento" "$LIB_INV_CODE" "200"
cat /tmp/lib_inv.json | print_json

OVER_CODE="$(curl -s -o /tmp/over_inv.json -w "%{http_code}" -X POST "$INVENTARIO/api/inventario/$SECCION_0_ID/reservar" \
  -H "Content-Type: application/json" \
  -d "{\"reservaId\":\"$RES_DIRECT_ID\",\"cantidad\":999999}")"
assert_code "inventario sobreventa debe dar 409" "$OVER_CODE" "409"
cat /tmp/over_inv.json | print_json

section "7. RESERVAS: CREAR, IDEMPOTENCIA, CONSULTA"
IDEM_RES="idem-res-$RUN_ID"
RESERVA_RESP="$(curl -s -X POST "$BASE/api/reservas" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Idempotency-Key: $IDEM_RES" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventoId\":\"$EVENTO_ID\",
    \"items\":[
      {\"seccionId\":\"$SECCION_0_ID\",\"tipoAsiento\":\"GENERAL\",\"cantidad\":1}
    ]
  }")"
RESERVA_ID="$(json_get "$RESERVA_RESP" "datos.id")"
RESERVA_PRECIO="$(json_get "$RESERVA_RESP" "datos.precioTotal")"
echo "$RESERVA_RESP" | print_json
[ -n "$RESERVA_ID" ] && green "OK  - reserva creada $RESERVA_ID precio=$RESERVA_PRECIO" && OK=$((OK+1)) || { red "FAIL - reserva no creada"; FAIL=$((FAIL+1)); }

RESERVA_DUP="$(curl -s -X POST "$BASE/api/reservas" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Idempotency-Key: $IDEM_RES" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventoId\":\"$EVENTO_ID\",
    \"items\":[
      {\"seccionId\":\"$SECCION_0_ID\",\"tipoAsiento\":\"GENERAL\",\"cantidad\":1}
    ]
  }")"
RESERVA_DUP_ID="$(json_get "$RESERVA_DUP" "datos.id")"
[ "$RESERVA_ID" = "$RESERVA_DUP_ID" ] && green "OK  - reserva idempotente retorna mismo ID" && OK=$((OK+1)) || { red "FAIL - reserva idempotente devolvió otro ID"; FAIL=$((FAIL+1)); }

GET_RES_CODE="$(curl -s -o /tmp/get_reserva.json -w "%{http_code}" "$BASE/api/reservas/$RESERVA_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
assert_code "GET reserva por id" "$GET_RES_CODE" "200"
cat /tmp/get_reserva.json | print_json

section "8. PAGOS: PROCESAR, IDEMPOTENCIA, OUTBOX"
IDEM_PAGO="idem-pago-$RUN_ID"
PAGO_RESP="$(curl -s -X POST "$BASE/api/pagos" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Idempotency-Key: $IDEM_PAGO" \
  -H "Content-Type: application/json" \
  -d "{
    \"reserva_id\":\"$RESERVA_ID\",
    \"usuario_id\":\"$CLIENT_ID\",
    \"monto\":\"$RESERVA_PRECIO\",
    \"metodo_pago\":\"TARJETA\"
  }")"
PAGO_ID="$(json_get "$PAGO_RESP" "datos.id")"
PAGO_ESTADO="$(json_get "$PAGO_RESP" "datos.estado")"
echo "$PAGO_RESP" | print_json
if [ "$PAGO_ESTADO" = "COMPLETADO" ] || [ "$PAGO_ESTADO" = "FALLIDO" ]; then
  green "OK  - pago procesado estado=$PAGO_ESTADO"
  OK=$((OK+1))
else
  red "FAIL - pago estado inesperado: $PAGO_ESTADO"
  FAIL=$((FAIL+1))
fi

PAGO_DUP="$(curl -s -X POST "$BASE/api/pagos" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Idempotency-Key: $IDEM_PAGO" \
  -H "Content-Type: application/json" \
  -d "{
    \"reserva_id\":\"$RESERVA_ID\",
    \"usuario_id\":\"$CLIENT_ID\",
    \"monto\":\"$RESERVA_PRECIO\",
    \"metodo_pago\":\"TARJETA\"
  }")"
PAGO_DUP_ID="$(json_get "$PAGO_DUP" "datos.id")"
[ "$PAGO_ID" = "$PAGO_DUP_ID" ] && green "OK  - pago idempotente retorna mismo ID" && OK=$((OK+1)) || { red "FAIL - pago idempotente devolvió otro ID"; FAIL=$((FAIL+1)); }

GET_PAGO_CODE="$(curl -s -o /tmp/get_pago.json -w "%{http_code}" "$BASE/api/pagos/$PAGO_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
assert_code "GET pago por id" "$GET_PAGO_CODE" "200"
cat /tmp/get_pago.json | print_json

GET_PAGO_RES_CODE="$(curl -s -o /tmp/get_pago_res.json -w "%{http_code}" "$BASE/api/pagos/reserva/$RESERVA_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
assert_code "GET pago por reserva" "$GET_PAGO_RES_CODE" "200"
cat /tmp/get_pago_res.json | print_json

section "9. KAFKA POST-PAGO: RESERVA Y NOTIFICACIONES"
echo "Esperando outbox ms-pagos -> Kafka -> reservas/notificaciones..."
sleep 12

GET_RES_AFTER="$(curl -s "$BASE/api/reservas/$RESERVA_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
RES_ESTADO_AFTER="$(json_get "$GET_RES_AFTER" "datos.estado")"
echo "$GET_RES_AFTER" | print_json
if [ "$PAGO_ESTADO" = "COMPLETADO" ]; then
  [ "$RES_ESTADO_AFTER" = "CONFIRMADO" ] && green "OK  - pago confirmado cambió reserva a CONFIRMADO" && OK=$((OK+1)) || { red "FAIL - reserva no quedó CONFIRMADO"; FAIL=$((FAIL+1)); }
else
  [ "$RES_ESTADO_AFTER" = "EXPIRADO" ] && green "OK  - pago fallido cambió reserva a EXPIRADO" && OK=$((OK+1)) || { red "FAIL - reserva no quedó EXPIRADO tras pago fallido"; FAIL=$((FAIL+1)); }
fi

NOTIF_RESP="$(curl -s "$BASE/api/notificaciones/usuario/$CLIENT_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
echo "$NOTIF_RESP" | print_json
NOTIF_FIRST="$(json_get "$NOTIF_RESP" "datos.0.id")"
[ -n "$NOTIF_FIRST" ] && green "OK  - notificación creada para usuario" && OK=$((OK+1)) || { red "FAIL - no se encontró notificación"; FAIL=$((FAIL+1)); }

section "10. ROLES: CLIENTE NO DEBE CREAR EVENTO"
CLIENT_CREATE_EVENT_CODE="$(curl -s -o /tmp/client_create_event.json -w "%{http_code}" -X POST "$BASE/api/eventos" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"nombre\":\"Evento No Permitido $RUN_ID\",
    \"descripcion\":\"Debe fallar\",
    \"lugarId\":\"$LUGAR_ID\",
    \"fechaEvento\":\"2026-12-31T20:00:00\",
    \"tipoEvento\":\"MIXTO\"
  }")"
assert_code "CLIENTE no puede POST /api/eventos" "$CLIENT_CREATE_EVENT_CODE" "401"
cat /tmp/client_create_event.json | print_json

section "11. DOCKER LOGS RECIENTES PARA KAFKA"
echo "--- ms-eventos evento.creado ---"
docker logs soldout-ms-eventos --tail 80 2>/dev/null | grep -E "evento.creado|ERROR" || true
echo "--- ms-inventario evento.creado ---"
docker logs soldout-ms-inventario --tail 120 2>/dev/null | grep -E "evento.creado|INVENTARIO INICIALIZADO|ERROR" || true
echo "--- ms-pagos outbox ---"
docker logs soldout-ms-pagos --tail 120 2>/dev/null | grep -E "OUTBOX|PAGO PROCESADO|ERROR" || true
echo "--- ms-reservas pago confirmado/fallido ---"
docker logs soldout-ms-reservas --tail 120 2>/dev/null | grep -E "pago.confirmado|pago.fallido|RESERVA CONFIRMADA|RESERVA EXPIRADA|ERROR" || true
echo "--- ms-notificaciones ---"
docker logs soldout-ms-notificaciones --tail 120 2>/dev/null | grep -E "NOTIFICACION|pago.confirmado|pago.fallido|reserva.expirada|ERROR" || true

section "RESUMEN"
echo "OK=$OK"
echo "FAIL=$FAIL"

if [ "$FAIL" -eq 0 ]; then
  green "RESULTADO FINAL: TODOS LOS TESTS PASARON"
  exit 0
else
  red "RESULTADO FINAL: HAY FALLAS. Pásame toda esta salida."
  exit 1
fi
