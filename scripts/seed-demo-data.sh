#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ -f "$PROJECT_ROOT/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$PROJECT_ROOT/.env"
    set +a
fi

FLIGHT_API_URL="${FLIGHT_API_URL:-http://localhost:8082}"
REFERENCE_API_URL="${REFERENCE_API_URL:-http://localhost:8081}"

for command in curl jq; do
    if ! command -v "$command" >/dev/null 2>&1; then
        echo "Hata: '$command' komutu bulunamadı." >&2
        exit 1
    fi
done

: "${ADMIN_USERNAME:?ADMIN_USERNAME .env içinde veya ortam değişkeni olarak tanımlanmalı}"
: "${ADMIN_PASSWORD:?ADMIN_PASSWORD .env içinde veya ortam değişkeni olarak tanımlanmalı}"

echo "Admin oturumu açılıyor..."
login_payload="$(jq -cn \
    --arg userName "$ADMIN_USERNAME" \
    --arg userPassword "$ADMIN_PASSWORD" \
    '{userName: $userName, userPassword: $userPassword}')"

access_token="$(curl --fail-with-body --silent --show-error \
    -H 'Content-Type: application/json' \
    -d "$login_payload" \
    "$FLIGHT_API_URL/api/auth/login" | jq -er '.accessToken')"

api_get() {
    curl --fail-with-body --silent --show-error \
        -H "Authorization: Bearer $access_token" \
        "$REFERENCE_API_URL$1"
}

api_post() {
    curl --fail-with-body --silent --show-error \
        -X POST \
        -H "Authorization: Bearer $access_token" \
        -H 'Content-Type: application/json' \
        -d "$2" \
        "$REFERENCE_API_URL$1"
}

ensure_entity() {
    local label="$1"
    local endpoint="$2"
    local key_field="$3"
    local id_field="$4"
    local key_value="$5"
    local payload="$6"
    local existing_id

    existing_id="$(api_get "$endpoint" | jq -r \
        --arg key "$key_value" \
        --arg keyField "$key_field" \
        --arg idField "$id_field" \
        '[.[] | select(.[$keyField] == $key)][0][$idField] // empty')"

    if [[ -n "$existing_id" ]]; then
        echo "Mevcut, atlandı: $label ($key_value)" >&2
        printf '%s\n' "$existing_id"
        return
    fi

    local created_id
    created_id="$(api_post "$endpoint" "$payload" | jq -er --arg idField "$id_field" '.[$idField]')"
    echo "Eklendi: $label ($key_value)" >&2
    printf '%s\n' "$created_id"
}

ensure_route() {
    local origin_id="$1"
    local destination_id="$2"
    local route_key="$3"
    local existing_id

    existing_id="$(api_get '/api/routes' | jq -r \
        --argjson originId "$origin_id" \
        --argjson destinationId "$destination_id" \
        '[.[] | select(.originAirportId == $originId and .destinationAirportId == $destinationId)][0].routeId // empty')"

    if [[ -n "$existing_id" ]]; then
        echo "Mevcut, atlandı: rota ($route_key)" >&2
        return
    fi

    local payload
    payload="$(jq -cn \
        --argjson originAirportId "$origin_id" \
        --argjson destinationAirportId "$destination_id" \
        '{originAirportId: $originAirportId, destinationAirportId: $destinationAirportId, routeStatus: "ACTIVE"}')"
    api_post '/api/routes' "$payload" >/dev/null
    echo "Eklendi: rota ($route_key)" >&2
}

echo "Havayolları hazırlanıyor..."
tk_id="$(ensure_entity 'havayolu' '/api/airlines' 'airlineIataCode' 'airlineId' 'TK' \
    '{"airlineName":"Turkish Airlines","airlineIcaoCode":"THY","airlineIataCode":"TK","airlineCountry":"Türkiye","airlineStatus":"ACTIVE"}')"
pc_id="$(ensure_entity 'havayolu' '/api/airlines' 'airlineIataCode' 'airlineId' 'PC' \
    '{"airlineName":"Pegasus Airlines","airlineIcaoCode":"PGT","airlineIataCode":"PC","airlineCountry":"Türkiye","airlineStatus":"ACTIVE"}')"
xq_id="$(ensure_entity 'havayolu' '/api/airlines' 'airlineIataCode' 'airlineId' 'XQ' \
    '{"airlineName":"SunExpress","airlineIcaoCode":"SXS","airlineIataCode":"XQ","airlineCountry":"Türkiye","airlineStatus":"ACTIVE"}')"
lh_id="$(ensure_entity 'havayolu' '/api/airlines' 'airlineIataCode' 'airlineId' 'LH' \
    '{"airlineName":"Lufthansa","airlineIcaoCode":"DLH","airlineIataCode":"LH","airlineCountry":"Almanya","airlineStatus":"ACTIVE"}')"

echo "Havalimanları hazırlanıyor..."
ist_id="$(ensure_entity 'havalimanı' '/api/airports' 'airportIataCode' 'airportId' 'IST' \
    '{"airportName":"İstanbul Havalimanı","airportCity":"İstanbul","airportCountry":"Türkiye","airportIataCode":"IST","airportIcaoCode":"LTFM","airportTimezone":"Europe/Istanbul","airportStatus":"OPERATIONAL"}')"
saw_id="$(ensure_entity 'havalimanı' '/api/airports' 'airportIataCode' 'airportId' 'SAW' \
    '{"airportName":"Sabiha Gökçen Uluslararası Havalimanı","airportCity":"İstanbul","airportCountry":"Türkiye","airportIataCode":"SAW","airportIcaoCode":"LTFJ","airportTimezone":"Europe/Istanbul","airportStatus":"OPERATIONAL"}')"
esb_id="$(ensure_entity 'havalimanı' '/api/airports' 'airportIataCode' 'airportId' 'ESB' \
    '{"airportName":"Esenboğa Havalimanı","airportCity":"Ankara","airportCountry":"Türkiye","airportIataCode":"ESB","airportIcaoCode":"LTAC","airportTimezone":"Europe/Istanbul","airportStatus":"OPERATIONAL"}')"
adb_id="$(ensure_entity 'havalimanı' '/api/airports' 'airportIataCode' 'airportId' 'ADB' \
    '{"airportName":"Adnan Menderes Havalimanı","airportCity":"İzmir","airportCountry":"Türkiye","airportIataCode":"ADB","airportIcaoCode":"LTBJ","airportTimezone":"Europe/Istanbul","airportStatus":"OPERATIONAL"}')"
ayt_id="$(ensure_entity 'havalimanı' '/api/airports' 'airportIataCode' 'airportId' 'AYT' \
    '{"airportName":"Antalya Havalimanı","airportCity":"Antalya","airportCountry":"Türkiye","airportIataCode":"AYT","airportIcaoCode":"LTAI","airportTimezone":"Europe/Istanbul","airportStatus":"OPERATIONAL"}')"
fra_id="$(ensure_entity 'havalimanı' '/api/airports' 'airportIataCode' 'airportId' 'FRA' \
    '{"airportName":"Frankfurt Airport","airportCity":"Frankfurt","airportCountry":"Almanya","airportIataCode":"FRA","airportIcaoCode":"EDDF","airportTimezone":"Europe/Berlin","airportStatus":"OPERATIONAL"}')"
lhr_id="$(ensure_entity 'havalimanı' '/api/airports' 'airportIataCode' 'airportId' 'LHR' \
    '{"airportName":"Heathrow Airport","airportCity":"Londra","airportCountry":"Birleşik Krallık","airportIataCode":"LHR","airportIcaoCode":"EGLL","airportTimezone":"Europe/London","airportStatus":"OPERATIONAL"}')"

echo "Uçak tipleri hazırlanıyor..."
a20n_id="$(ensure_entity 'uçak tipi' '/api/aircraft-types' 'aircraftTypeIcaoCode' 'aircraftTypeId' 'A20N' \
    '{"aircraftTypeManufacturer":"Airbus","aircraftTypeModel":"A320neo","aircraftTypeIcaoCode":"A20N","aircraftTypeCategory":"NARROW_BODY","aircraftTypeStatus":"ACTIVE"}')"
b738_id="$(ensure_entity 'uçak tipi' '/api/aircraft-types' 'aircraftTypeIcaoCode' 'aircraftTypeId' 'B738' \
    '{"aircraftTypeManufacturer":"Boeing","aircraftTypeModel":"737-800","aircraftTypeIcaoCode":"B738","aircraftTypeCategory":"NARROW_BODY","aircraftTypeStatus":"ACTIVE"}')"
a333_id="$(ensure_entity 'uçak tipi' '/api/aircraft-types' 'aircraftTypeIcaoCode' 'aircraftTypeId' 'A333' \
    '{"aircraftTypeManufacturer":"Airbus","aircraftTypeModel":"A330-300","aircraftTypeIcaoCode":"A333","aircraftTypeCategory":"WIDE_BODY","aircraftTypeStatus":"ACTIVE"}')"
at76_id="$(ensure_entity 'uçak tipi' '/api/aircraft-types' 'aircraftTypeIcaoCode' 'aircraftTypeId' 'AT76' \
    '{"aircraftTypeManufacturer":"ATR","aircraftTypeModel":"ATR 72-600","aircraftTypeIcaoCode":"AT76","aircraftTypeCategory":"TURBOPROP","aircraftTypeStatus":"ACTIVE"}')"
e190_id="$(ensure_entity 'uçak tipi' '/api/aircraft-types' 'aircraftTypeIcaoCode' 'aircraftTypeId' 'E190' \
    '{"aircraftTypeManufacturer":"Embraer","aircraftTypeModel":"E190","aircraftTypeIcaoCode":"E190","aircraftTypeCategory":"REGIONAL","aircraftTypeStatus":"ACTIVE"}')"

echo "Uçaklar hazırlanıyor..."
ensure_entity 'uçak' '/api/aircrafts' 'aircraftRegistrationNumber' 'aircraftId' 'TC-LAA' \
    "$(jq -cn --argjson airlineId "$tk_id" --argjson typeId "$a20n_id" '{aircraftRegistrationNumber:"TC-LAA",operatorAirlineId:$airlineId,aircraftTypeId:$typeId,aircraftCapacity:186,aircraftManufactureYear:2022,aircraftStatus:"ACTIVE"}')" >/dev/null
ensure_entity 'uçak' '/api/aircrafts' 'aircraftRegistrationNumber' 'aircraftId' 'TC-LAB' \
    "$(jq -cn --argjson airlineId "$tk_id" --argjson typeId "$a333_id" '{aircraftRegistrationNumber:"TC-LAB",operatorAirlineId:$airlineId,aircraftTypeId:$typeId,aircraftCapacity:289,aircraftManufactureYear:2020,aircraftStatus:"ACTIVE"}')" >/dev/null
ensure_entity 'uçak' '/api/aircrafts' 'aircraftRegistrationNumber' 'aircraftId' 'TC-NBA' \
    "$(jq -cn --argjson airlineId "$pc_id" --argjson typeId "$a20n_id" '{aircraftRegistrationNumber:"TC-NBA",operatorAirlineId:$airlineId,aircraftTypeId:$typeId,aircraftCapacity:186,aircraftManufactureYear:2021,aircraftStatus:"ACTIVE"}')" >/dev/null
ensure_entity 'uçak' '/api/aircrafts' 'aircraftRegistrationNumber' 'aircraftId' 'TC-SOA' \
    "$(jq -cn --argjson airlineId "$xq_id" --argjson typeId "$b738_id" '{aircraftRegistrationNumber:"TC-SOA",operatorAirlineId:$airlineId,aircraftTypeId:$typeId,aircraftCapacity:189,aircraftManufactureYear:2019,aircraftStatus:"ACTIVE"}')" >/dev/null
ensure_entity 'uçak' '/api/aircrafts' 'aircraftRegistrationNumber' 'aircraftId' 'D-AIXA' \
    "$(jq -cn --argjson airlineId "$lh_id" --argjson typeId "$a333_id" '{aircraftRegistrationNumber:"D-AIXA",operatorAirlineId:$airlineId,aircraftTypeId:$typeId,aircraftCapacity:255,aircraftManufactureYear:2017,aircraftStatus:"ACTIVE"}')" >/dev/null
ensure_entity 'uçak' '/api/aircrafts' 'aircraftRegistrationNumber' 'aircraftId' 'TC-EJA' \
    "$(jq -cn --argjson airlineId "$tk_id" --argjson typeId "$e190_id" '{aircraftRegistrationNumber:"TC-EJA",operatorAirlineId:$airlineId,aircraftTypeId:$typeId,aircraftCapacity:104,aircraftManufactureYear:2018,aircraftStatus:"ACTIVE"}')" >/dev/null

echo "Rotalar hazırlanıyor..."
ensure_route "$ist_id" "$esb_id" 'IST -> ESB'
ensure_route "$esb_id" "$ist_id" 'ESB -> IST'
ensure_route "$ist_id" "$adb_id" 'IST -> ADB'
ensure_route "$adb_id" "$ist_id" 'ADB -> IST'
ensure_route "$ist_id" "$ayt_id" 'IST -> AYT'
ensure_route "$ayt_id" "$ist_id" 'AYT -> IST'
ensure_route "$saw_id" "$esb_id" 'SAW -> ESB'
ensure_route "$esb_id" "$saw_id" 'ESB -> SAW'
ensure_route "$ist_id" "$fra_id" 'IST -> FRA'
ensure_route "$fra_id" "$ist_id" 'FRA -> IST'
ensure_route "$ist_id" "$lhr_id" 'IST -> LHR'
ensure_route "$lhr_id" "$ist_id" 'LHR -> IST'

echo "Uçuş tipleri hazırlanıyor..."
ensure_entity 'uçuş tipi' '/api/flight-types' 'flightTypeCode' 'flightTypeId' 'DOMESTIC' \
    '{"flightTypeName":"İç Hat","flightTypeCode":"DOMESTIC","flightTypeStatus":"ACTIVE"}' >/dev/null
ensure_entity 'uçuş tipi' '/api/flight-types' 'flightTypeCode' 'flightTypeId' 'INTERNATIONAL' \
    '{"flightTypeName":"Dış Hat","flightTypeCode":"INTERNATIONAL","flightTypeStatus":"ACTIVE"}' >/dev/null
ensure_entity 'uçuş tipi' '/api/flight-types' 'flightTypeCode' 'flightTypeId' 'CARGO' \
    '{"flightTypeName":"Kargo","flightTypeCode":"CARGO","flightTypeStatus":"ACTIVE"}' >/dev/null

echo "Demo referans verileri hazır. Script tekrar güvenle çalıştırılabilir."
