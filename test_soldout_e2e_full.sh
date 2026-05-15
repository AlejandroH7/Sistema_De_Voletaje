#!/usr/bin/env bash
set -u

BASE="http://localhost:8080"
RUN_ID="$(date +%s)"
OK=0
FAIL=0
WARN=0

green(){ printf "\033[32m%s\033[0m\n" "$1"; }
red(){ printf "\033[31m%s\033[0m\n" "$1"; }
yellow(){ printf "\033[33m%s\033[0m\n" "$1"; }
section(){ printf "\n════════════════════════════════════════\n%s\n════════════════════════════════════════\n" "$1"; }

print_json() {
  python3 -m json.tool 2>/dev/null || cat
}

json_get() {
  local json="$1"
  local path="$2"
  JSON_INPUT="$json" JSON_PATH="$path" python3 - <<'PY'
import json, os, sys
data = os.environ.get("JSON_INPUT", "")
path = os.environ.get("JSON_PATH", "")
try:
    obj = json.loads(data)
    cur = obj
    for part in path.split("."):
        if part == "":
            continue
        if part.isdigit():
            cur = cur[int(part)]
        else:
            cur = cur[part]
    if cur is None:
        print("")
    elif isinstance(cur, (dict, list)):
        print(json.dumps(cur))
    else:
        print(cur)
except Exception:
    print("")
PY
}

assert_code() {
  local name="$1"
  local actual="$2"
  local expected="$3"
  if [ "$actual" = "$expected" ]; then
    green "OK  - $name => HTTP $actual"
    OK=$((OK+1))
  else
    red "FAIL - $name => HTTP $actual, esperado $expected"
    FAIL=$((FAIL+1))
  fi
}

assert_contains() {
  local name="$1"
  local value="$2"
  local expected="$3"
  if echo "$value" | grep -qE "$expected"; then
    green "OK  - $name contiene '$expected'"
    OK=$((OK+1))
  else
    red "FAIL - $name no contiene '$expected'"
    FAIL=$((FAIL+1))
  fi
}

wait_for_inventory() {
  local evento_id="$1"
  local token="$2"
  local resp=""
  for i in $(seq 1 20); do
    resp="$(curl -s "$BASE/api/inventario/$evento_id/secciones" \
      -H "Authorization: Bearer $token")"
    local count
    count="$(JSON_INPUT="$resp" python3 - <<'PY'
import json, os
try:
    data=json.loads(os.environ["JSON_INPUT"])
    print(len(data.get("datos") or []))
except Exception:
    print(0)
PY
)"
    if [ "$count" -gt 0 ]; then
      echo "$resp"
      return 0
    fi
    sleep 1
  done
  echo "$resp"
  return 1
}

section "0. PRECHECK DOCKER Y CONTENEDORES"
docker-compose ps
RUNNING_COUNT="$(docker-compose ps --services --filter status=running | wc -l | tr -d ' ')"
if [ "$RUNNING_COUNT" -ge 10 ]; then
  green "OK  - contenedores principales corriendo ($RUNNING_COUNT)"
  OK=$((OK+1))
else
  red "FAIL - pocos contenedores corriendo ($RUNNING_COUNT)"
  FAIL=$((FAIL+1))
fi

section "1. HEALTH CHECKS DIRECTOS"
for item in \
  "api-gateway 8080" \
  "ms-usuarios 8081" \
  "ms-eventos 8082" \
  "ms-inventario 8083" \
  "ms-reservas 8084" \
  "ms-pagos 8085" \
  "ms-notificaciones 8086"
do
  name="$(echo "$item" | awk '{print $1}')"
  port="$(echo "$item" | awk '{print $2}')"
  code="$(curl -s -o /tmp/health_${name}.json -w "%{http_code}" "http://localhost:$port/actuator/health")"
  assert_code "health $name" "$code" "200"
done

section "2. PROMETHEUS ENDPOINTS"
for item in \
  "api-gateway 8080" \
  "ms-usuarios 8081" \
  "ms-eventos 8082" \
  "ms-inventario 8083" \
  "ms-reservas 8084" \
  "ms-pagos 8085" \
  "ms-notificaciones 8086"
do
  name="$(echo "$item" | awk '{print $1}')"
  port="$(echo "$item" | awk '{print $2}')"
  code="$(curl -s -o /tmp/prom_${name}.txt -w "%{http_code}" "http://localhost:$port/actuator/prometheus")"
  assert_code "prometheus $name" "$code" "200"
done

section "3. SEGURIDAD GATEWAY SIN TOKEN"
code="$(curl -s -o /tmp/no_token_reservas.json -w "%{http_code}" "$BASE/api/reservas")"
assert_code "GET /api/reservas sin token debe rechazar" "$code" "401"
code="$(curl -s -o /tmp/no_token_pagos.json -w "%{http_code}" -X POST "$BASE/api/pagos" -H "Content-Type: application/json" -d '{}')"
assert_code "POST /api/pagos sin token debe rechazar" "$code" "401"

section "4. REGISTRO, LOGIN Y PERFIL"
ADMIN_EMAIL="admin_${RUN_ID}@soldout.com"
CLIENT_EMAIL="cliente_${RUN_ID}@soldout.com"
PASS="password123"

ADMIN_REG="$(curl -s -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Admin Test\",\"email\":\"$ADMIN_EMAIL\",\"contrasena\":\"$PASS\",\"telefono\":\"55550001\",\"rol\":\"ADMIN\"}")"
echo "$ADMIN_REG" | print_json
assert_contains "registro admin" "$ADMIN_REG" '"exito"[[:space:]]*:[[:space:]]*true'

CLIENT_REG="$(curl -s -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Cliente Test\",\"email\":\"$CLIENT_EMAIL\",\"contrasena\":\"$PASS\",\"telefono\":\"55550002\",\"rol\":\"CLIENTE\"}")"
echo "$CLIENT_REG" | print_json
assert_contains "registro cliente" "$CLIENT_REG" '"exito"[[:space:]]*:[[:space:]]*true'

DUP_CODE="$(curl -s -o /tmp/dup_email.json -w "%{http_code}" -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Duplicado\",\"email\":\"$CLIENT_EMAIL\",\"contrasena\":\"$PASS\",\"telefono\":\"55550003\",\"rol\":\"CLIENTE\"}")"
assert_code "registro duplicado debe dar 409" "$DUP_CODE" "409"
cat /tmp/dup_email.json | print_json

BAD_LOGIN_CODE="$(curl -s -o /tmp/bad_login.json -w "%{http_code}" -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$CLIENT_EMAIL\",\"contrasena\":\"wrong\"}")"
assert_code "login incorrecto debe dar 401" "$BAD_LOGIN_CODE" "401"
cat /tmp/bad_login.json | print_json

ADMIN_LOGIN="$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"contrasena\":\"$PASS\"}")"
echo "$ADMIN_LOGIN" | print_json
ADMIN_TOKEN="$(json_get "$ADMIN_LOGIN" "datos.token")"
ADMIN_ID="$(json_get "$ADMIN_LOGIN" "datos.usuario.id")"
[ -n "$ADMIN_TOKEN" ] && green "OK  - token admin obtenido" && OK=$((OK+1)) || { red "FAIL - no se obtuvo token admin"; FAIL=$((FAIL+1)); exit 1; }

CLIENT_LOGIN="$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$CLIENT_EMAIL\",\"contrasena\":\"$PASS\"}")"
echo "$CLIENT_LOGIN" | print_json
CLIENT_TOKEN="$(json_get "$CLIENT_LOGIN" "datos.token")"
CLIENT_ID="$(json_get "$CLIENT_LOGIN" "datos.usuario.id")"
[ -n "$CLIENT_TOKEN" ] && green "OK  - token cliente obtenido" && OK=$((OK+1)) || { red "FAIL - no se obtuvo token cliente"; FAIL=$((FAIL+1)); exit 1; }

PERFIL_CODE="$(curl -s -o /tmp/perfil_cliente.json -w "%{http_code}" "$BASE/api/usuarios/perfil" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
assert_code "perfil cliente con token" "$PERFIL_CODE" "200"
cat /tmp/perfil_cliente.json | print_json

section "5. EVENTOS: LUGAR, EVENTO, PUBLICAR Y KAFKA evento.creado"
LUGAR_RESP="$(curl -s -X POST "$BASE/api/lugares" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"nombre\":\"Venue Test $RUN_ID\",
    \"direccion\":\"Zona 10\",
    \"ciudad\":\"Guatemala\",
    \"capacidadMaxima\":5000
  }")"
echo "$LUGAR_RESP" | print_json
LUGAR_ID="$(json_get "$LUGAR_RESP" "datos.id")"
[ -n "$LUGAR_ID" ] && green "OK  - lugar creado $LUGAR_ID" && OK=$((OK+1)) || { red "FAIL - no se creó lugar"; FAIL=$((FAIL+1)); exit 1; }

EVENTO_RESP="$(curl -s -X POST "$BASE/api/eventos" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"nombre\":\"Concierto E2E $RUN_ID\",
    \"descripcion\":\"Prueba end to end completa\",
    \"lugarId\":\"$LUGAR_ID\",
    \"fechaEvento\":\"2026-12-31T20:00:00\",
    \"tipoEvento\":\"MIXTO\",
    \"secciones\":[
      {\"nombre\":\"General E2E $RUN_ID\",\"tipo\":\"GENERAL\",\"capacidad\":80,\"precio\":150.00},
      {\"nombre\":\"VIP E2E $RUN_ID\",\"tipo\":\"NUMERADO\",\"capacidad\":20,\"precio\":500.00}
    ]
  }")"
echo "$EVENTO_RESP" | print_json
EVENTO_ID="$(json_get "$EVENTO_RESP" "datos.id")"
EVENTO_ESTADO="$(json_get "$EVENTO_RESP" "datos.estado")"
[ -n "$EVENTO_ID" ] && [ "$EVENTO_ESTADO" = "BORRADOR" ] && green "OK  - evento creado BORRADOR" && OK=$((OK+1)) || { red "FAIL - evento no quedó BORRADOR"; FAIL=$((FAIL+1)); exit 1; }

PUBLICAR_RESP="$(curl -s -X PUT "$BASE/api/eventos/$EVENTO_ID/publicar" \
  -H "Authorization: Bearer $ADMIN_TOKEN")"
echo "$PUBLICAR_RESP" | print_json
PUBLICAR_ESTADO="$(json_get "$PUBLICAR_RESP" "datos.estado")"
[ "$PUBLICAR_ESTADO" = "ACTIVO" ] && green "OK  - evento publicado ACTIVO" && OK=$((OK+1)) || { red "FAIL - evento no quedó ACTIVO"; FAIL=$((FAIL+1)); exit 1; }

echo "Esperando Kafka evento.creado -> ms-inventario..."
INV_RESP="$(wait_for_inventory "$EVENTO_ID" "$ADMIN_TOKEN")"
echo "$INV_RESP" | print_json
SECCION_ID="$(json_get "$INV_RESP" "datos.0.seccion.id")"
SECCION_PRECIO="$(json_get "$INV_RESP" "datos.0.seccion.precio")"
SECCION_DISP="$(json_get "$INV_RESP" "datos.0.asientosDisponibles")"
[ -n "$SECCION_ID" ] && green "OK  - inventario inicializado seccion=$SECCION_ID precio=$SECCION_PRECIO disp=$SECCION_DISP" && OK=$((OK+1)) || { red "FAIL - inventario no inicializó"; FAIL=$((FAIL+1)); exit 1; }

section "6. INVENTARIO DIRECTO: RESERVAR, LIBERAR Y SOBREVENTA"
RESERVA_INV_CODE="$(curl -s -o /tmp/inv_reservar.json -w "%{http_code}" -X POST "$BASE/api/inventario/$SECCION_ID/reservar" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"reservaId\":\"00000000-0000-0000-0000-000000000001\",\"cantidad\":1}")"
assert_code "inventario reservar 1 asiento" "$RESERVA_INV_CODE" "200"
cat /tmp/inv_reservar.json | print_json

LIBERAR_INV_CODE="$(curl -s -o /tmp/inv_liberar.json -w "%{http_code}" -X POST "$BASE/api/inventario/$SECCION_ID/liberar" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"reservaId\":\"00000000-0000-0000-0000-000000000001\",\"cantidad\":1}")"
assert_code "inventario liberar 1 asiento" "$LIBERAR_INV_CODE" "200"
cat /tmp/inv_liberar.json | print_json

SOBREVENTA_CODE="$(curl -s -o /tmp/inv_sobreventa.json -w "%{http_code}" -X POST "$BASE/api/inventario/$SECCION_ID/reservar" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"reservaId\":\"00000000-0000-0000-0000-000000000002\",\"cantidad\":999999}")"
assert_code "inventario sobreventa debe dar 409" "$SOBREVENTA_CODE" "409"
cat /tmp/inv_sobreventa.json | print_json

section "7. RESERVAS: CREAR, IDEMPOTENCIA Y CONSULTA"
RES_IDEM="idem-res-${RUN_ID}"
RESERVA_RESP="$(curl -s -X POST "$BASE/api/reservas" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Idempotency-Key: $RES_IDEM" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventoId\":\"$EVENTO_ID\",
    \"items\":[
      {\"seccionId\":\"$SECCION_ID\",\"tipoAsiento\":\"GENERAL\",\"cantidad\":1}
    ]
  }")"
echo "$RESERVA_RESP" | print_json
RESERVA_ID="$(json_get "$RESERVA_RESP" "datos.id")"
RESERVA_PRECIO="$(json_get "$RESERVA_RESP" "datos.precioTotal")"
[ -n "$RESERVA_ID" ] && green "OK  - reserva creada $RESERVA_ID precio=$RESERVA_PRECIO" && OK=$((OK+1)) || { red "FAIL - no se creó reserva"; FAIL=$((FAIL+1)); exit 1; }

RESERVA_IDEM_RESP="$(curl -s -X POST "$BASE/api/reservas" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Idempotency-Key: $RES_IDEM" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventoId\":\"$EVENTO_ID\",
    \"items\":[
      {\"seccionId\":\"$SECCION_ID\",\"tipoAsiento\":\"GENERAL\",\"cantidad\":1}
    ]
  }")"
RESERVA_ID_2="$(json_get "$RESERVA_IDEM_RESP" "datos.id")"
[ "$RESERVA_ID" = "$RESERVA_ID_2" ] && green "OK  - reserva idempotente retorna mismo ID" && OK=$((OK+1)) || { red "FAIL - reserva idempotente no retorna mismo ID"; FAIL=$((FAIL+1)); }

GET_RES_CODE="$(curl -s -o /tmp/get_reserva.json -w "%{http_code}" "$BASE/api/reservas/$RESERVA_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
assert_code "GET reserva por id" "$GET_RES_CODE" "200"
cat /tmp/get_reserva.json | print_json

section "8. PAGOS: IDEMPOTENCIA, OUTBOX Y TRANSICIONES KAFKA"
PAY_RESP="$(curl -s -X POST "$BASE/api/pagos" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Idempotency-Key: idem-pago-${RUN_ID}" \
  -H "Content-Type: application/json" \
  -d "{
    \"reserva_id\":\"$RESERVA_ID\",
    \"usuario_id\":\"$CLIENT_ID\",
    \"monto\":\"$RESERVA_PRECIO\",
    \"metodo_pago\":\"TARJETA\"
  }")"
echo "$PAY_RESP" | print_json
PAGO_ID="$(json_get "$PAY_RESP" "datos.id")"
PAGO_ESTADO="$(json_get "$PAY_RESP" "datos.estado")"
[ -n "$PAGO_ID" ] && green "OK  - pago procesado estado=$PAGO_ESTADO" && OK=$((OK+1)) || { red "FAIL - pago no procesado"; FAIL=$((FAIL+1)); exit 1; }

PAY_IDEM_RESP="$(curl -s -X POST "$BASE/api/pagos" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Idempotency-Key: idem-pago-${RUN_ID}" \
  -H "Content-Type: application/json" \
  -d "{
    \"reserva_id\":\"$RESERVA_ID\",
    \"usuario_id\":\"$CLIENT_ID\",
    \"monto\":\"$RESERVA_PRECIO\",
    \"metodo_pago\":\"TARJETA\"
  }")"
PAGO_ID_2="$(json_get "$PAY_IDEM_RESP" "datos.id")"
[ "$PAGO_ID" = "$PAGO_ID_2" ] && green "OK  - pago idempotente retorna mismo ID" && OK=$((OK+1)) || { red "FAIL - pago idempotente no retorna mismo ID"; FAIL=$((FAIL+1)); }

GET_PAGO_CODE="$(curl -s -o /tmp/get_pago.json -w "%{http_code}" "$BASE/api/pagos/$PAGO_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
assert_code "GET pago por id" "$GET_PAGO_CODE" "200"
cat /tmp/get_pago.json | print_json

GET_PAGO_RES_CODE="$(curl -s -o /tmp/get_pago_reserva.json -w "%{http_code}" "$BASE/api/pagos/reserva/$RESERVA_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
assert_code "GET pago por reserva" "$GET_PAGO_RES_CODE" "200"
cat /tmp/get_pago_reserva.json | print_json

echo "Esperando outbox ms-pagos -> Kafka -> reservas/notificaciones..."
sleep 12
RESERVA_POST_PAGO="$(curl -s "$BASE/api/reservas/$RESERVA_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
echo "$RESERVA_POST_PAGO" | print_json
RESERVA_ESTADO_POST="$(json_get "$RESERVA_POST_PAGO" "datos.estado")"

if [ "$PAGO_ESTADO" = "COMPLETADO" ]; then
  [ "$RESERVA_ESTADO_POST" = "CONFIRMADO" ] && green "OK  - pago completado cambió reserva a CONFIRMADO" && OK=$((OK+1)) || { red "FAIL - reserva no quedó CONFIRMADO"; FAIL=$((FAIL+1)); }
elif [ "$PAGO_ESTADO" = "FALLIDO" ]; then
  [ "$RESERVA_ESTADO_POST" = "EXPIRADO" ] && green "OK  - pago fallido cambió reserva a EXPIRADO" && OK=$((OK+1)) || { red "FAIL - reserva no quedó EXPIRADO"; FAIL=$((FAIL+1)); }
else
  red "FAIL - estado de pago inesperado: $PAGO_ESTADO"
  FAIL=$((FAIL+1))
fi

section "9. PAGOS 70/30: BUSCAR AL MENOS UN COMPLETADO Y UN FALLIDO"
SEEN_COMPLETADO=0
SEEN_FALLIDO=0
[ "$PAGO_ESTADO" = "COMPLETADO" ] && SEEN_COMPLETADO=1
[ "$PAGO_ESTADO" = "FALLIDO" ] && SEEN_FALLIDO=1

for i in $(seq 1 12); do
  if [ "$SEEN_COMPLETADO" -eq 1 ] && [ "$SEEN_FALLIDO" -eq 1 ]; then
    break
  fi

  RES_LOOP_IDEM="idem-res-${RUN_ID}-loop-$i"
  RES_LOOP="$(curl -s -X POST "$BASE/api/reservas" \
    -H "Authorization: Bearer $CLIENT_TOKEN" \
    -H "Idempotency-Key: $RES_LOOP_IDEM" \
    -H "Content-Type: application/json" \
    -d "{
      \"eventoId\":\"$EVENTO_ID\",
      \"items\":[
        {\"seccionId\":\"$SECCION_ID\",\"tipoAsiento\":\"GENERAL\",\"cantidad\":1}
      ]
    }")"
  R_ID="$(json_get "$RES_LOOP" "datos.id")"
  R_MONTO="$(json_get "$RES_LOOP" "datos.precioTotal")"

  if [ -z "$R_ID" ]; then
    red "FAIL - no se pudo crear reserva loop $i"
    FAIL=$((FAIL+1))
    continue
  fi

  PAY_LOOP="$(curl -s -X POST "$BASE/api/pagos" \
    -H "Authorization: Bearer $CLIENT_TOKEN" \
    -H "Idempotency-Key: idem-pago-${RUN_ID}-loop-$i" \
    -H "Content-Type: application/json" \
    -d "{
      \"reserva_id\":\"$R_ID\",
      \"usuario_id\":\"$CLIENT_ID\",
      \"monto\":\"$R_MONTO\",
      \"metodo_pago\":\"TARJETA\"
    }")"
  P_STATE="$(json_get "$PAY_LOOP" "datos.estado")"
  echo "Intento pago loop $i => $P_STATE"

  [ "$P_STATE" = "COMPLETADO" ] && SEEN_COMPLETADO=1
  [ "$P_STATE" = "FALLIDO" ] && SEEN_FALLIDO=1
done

[ "$SEEN_COMPLETADO" -eq 1 ] && green "OK  - se observó al menos un pago COMPLETADO" && OK=$((OK+1)) || { yellow "WARN - no se observó pago COMPLETADO en los intentos"; WARN=$((WARN+1)); }
[ "$SEEN_FALLIDO" -eq 1 ] && green "OK  - se observó al menos un pago FALLIDO" && OK=$((OK+1)) || { yellow "WARN - no se observó pago FALLIDO en los intentos"; WARN=$((WARN+1)); }

section "10. NOTIFICACIONES"
sleep 12
NOTIF_RESP="$(curl -s "$BASE/api/notificaciones/usuario/$CLIENT_ID" \
  -H "Authorization: Bearer $CLIENT_TOKEN")"
echo "$NOTIF_RESP" | print_json
NOTIF_FIRST="$(json_get "$NOTIF_RESP" "datos.0.id")"
[ -n "$NOTIF_FIRST" ] && green "OK  - notificación creada para usuario" && OK=$((OK+1)) || { red "FAIL - no se encontró notificación"; FAIL=$((FAIL+1)); }

section "11. ROLES"
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

ADMIN_RESERVA_CODE="$(curl -s -o /tmp/admin_reserva.json -w "%{http_code}" -X POST "$BASE/api/reservas" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Idempotency-Key: idem-admin-res-${RUN_ID}" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventoId\":\"$EVENTO_ID\",
    \"items\":[
      {\"seccionId\":\"$SECCION_ID\",\"tipoAsiento\":\"GENERAL\",\"cantidad\":1}
    ]
  }")"
assert_code "ADMIN puede POST /api/reservas" "$ADMIN_RESERVA_CODE" "201"
cat /tmp/admin_reserva.json | print_json

section "12. DOCKER LOGS RECIENTES PARA KAFKA"
echo "--- ms-usuarios usuario.registrado ---"
docker logs soldout-ms-usuarios --tail 80 2>/dev/null | grep -E "usuario.registrado|ERROR" || true
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
echo "WARN=$WARN"
echo "FAIL=$FAIL"

if [ "$FAIL" -eq 0 ]; then
  if [ "$WARN" -eq 0 ]; then
    green "RESULTADO FINAL: TODOS LOS TESTS PASARON"
  else
    yellow "RESULTADO FINAL: TESTS CRÍTICOS PASARON, PERO HAY WARNINGS"
  fi
  exit 0
else
  red "RESULTADO FINAL: HAY FALLAS. Pásame toda esta salida."
  exit 1
fi
