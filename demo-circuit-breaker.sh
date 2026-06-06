#!/usr/bin/env bash
# Circuit Breaker Demo — GreenGrub Donation Service
# Shows CLOSED → OPEN → HALF_OPEN → CLOSED using Chaos Monkey + Resilience4j
#
# Prerequisites:
#   - DonationService running on :8083 with profiles local,chaos-monkey
#   - Start with: cd GreenGrubDonationService && mvn spring-boot:run -Dspring-boot.run.profiles=local,chaos-monkey

BASE=http://localhost:8083
DONATIONS="$BASE/api/v1/donations"
CHAOS="$BASE/actuator/chaosmonkey"
BREAKERS="$BASE/actuator/circuitbreakers"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

print_state() {
    local json
    json=$(curl -s "$BREAKERS")
    local db_state kafka_state food_state
    db_state=$(echo "$json"    | python3 -c "import sys,json; d=json.load(sys.stdin)['circuitBreakers']; print(d.get('dbBreaker',{}).get('state','N/A'))" 2>/dev/null)
    kafka_state=$(echo "$json" | python3 -c "import sys,json; d=json.load(sys.stdin)['circuitBreakers']; print(d.get('kafkaPublishBreaker',{}).get('state','N/A'))" 2>/dev/null)
    food_state=$(echo "$json"  | python3 -c "import sys,json; d=json.load(sys.stdin)['circuitBreakers']; print(d.get('foodServiceBreaker',{}).get('state','N/A'))" 2>/dev/null)

    color_state() {
        case "$1" in
            CLOSED)    echo -e "${GREEN}CLOSED${RESET}" ;;
            OPEN)      echo -e "${RED}OPEN${RESET}" ;;
            HALF_OPEN) echo -e "${YELLOW}HALF_OPEN${RESET}" ;;
            *)         echo -e "${CYAN}$1${RESET}" ;;
        esac
    }

    echo -e "  dbBreaker:         $(color_state "$db_state")"
    echo -e "  kafkaPublishBreaker: $(color_state "$kafka_state")"
    echo -e "  foodServiceBreaker:  $(color_state "$food_state")"
}

send_requests() {
    local count=$1
    local label=$2
    echo -e "\n${CYAN}Sending $count requests — $label${RESET}"
    for i in $(seq 1 "$count"); do
        code=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "X-User-Id: test-user" \
            -H "X-User-Role: DONOR" \
            "$DONATIONS")
        if   [ "$code" = "200" ]; then echo -e "  [$i] ${GREEN}$code OK${RESET}"
        elif [ "$code" = "503" ]; then echo -e "  [$i] ${RED}$code CIRCUIT OPEN — call blocked${RESET}"
        else                           echo -e "  [$i] ${YELLOW}$code (fault injected)${RESET}"
        fi
        sleep 0.3
    done
}

wait_and_poll() {
    local target_state=$1
    local max_wait=${2:-15}
    echo -e "\n${CYAN}Waiting for $target_state (up to ${max_wait}s)...${RESET}"
    for i in $(seq 1 "$max_wait"); do
        sleep 1
        state=$(curl -s "$BREAKERS" | python3 -c \
            "import sys,json; print(json.load(sys.stdin)['circuitBreakers']['dbBreaker']['state'])" 2>/dev/null)
        echo -ne "  ${i}s — dbBreaker: $state\r"
        if [ "$state" = "$target_state" ]; then
            echo -e "\n  ${BOLD}Reached $target_state after ${i}s${RESET}"
            return 0
        fi
    done
    echo -e "\n  ${RED}Did not reach $target_state within ${max_wait}s${RESET}"
}

# ─── Check service is up ────────────────────────────────────────────────────
echo -e "\n${BOLD}=== Checking service health ===${RESET}"
if ! curl -s --max-time 3 "$BASE/actuator/health" > /dev/null; then
    echo -e "${RED}DonationService not reachable at $BASE${RESET}"
    echo "Start it with: cd GreenGrubDonationService && mvn spring-boot:run -Dspring-boot.run.profiles=local,chaos-monkey"
    exit 1
fi
echo -e "${GREEN}Service is up${RESET}"

# ─── Check chaos-monkey profile is active ───────────────────────────────────
if ! curl -s --max-time 3 "$CHAOS" > /dev/null 2>&1; then
    echo -e "${RED}Chaos Monkey actuator not available at $CHAOS${RESET}"
    echo -e "Service must be started with the chaos-monkey profile:"
    echo -e "  cd GreenGrubDonationService && mvn spring-boot:run -Dspring-boot.run.profiles=local,chaos-monkey"
    exit 1
fi

# ─── STEP 1: CLOSED ─────────────────────────────────────────────────────────
echo -e "\n${BOLD}=== STEP 1: Initial state (should be CLOSED) ===${RESET}"
curl -s -X POST "$CHAOS/disable" > /dev/null
print_state

read -rp $'\nPress Enter to start the demo...'

# ─── STEP 2: Enable Chaos Monkey ────────────────────────────────────────────
echo -e "\n${BOLD}=== STEP 2: Enabling Chaos Monkey (exception assault) ===${RESET}"
curl -s -X POST "$CHAOS/enable" > /dev/null
curl -s -X POST "$CHAOS/assaults" \
    -H "Content-Type: application/json" \
    -d '{"level":1,"exceptionsActive":true,"latencyActive":false}' > /dev/null
echo -e "${RED}Chaos Monkey enabled — every service call will throw an exception${RESET}"

# ─── STEP 3: Trip the breaker → OPEN ────────────────────────────────────────
echo -e "\n${BOLD}=== STEP 3: Sending requests to trip the circuit breaker ===${RESET}"
echo    "  (dbBreaker opens after 60% failures in a 5-call sliding window)"
send_requests 8 "fault injection active"

echo -e "\n${BOLD}--- Circuit Breaker State ---${RESET}"
print_state

# ─── STEP 4: HALF_OPEN ──────────────────────────────────────────────────────
echo -e "\n${BOLD}=== STEP 4: Waiting for HALF_OPEN (wait-duration-in-open-state = 10s) ===${RESET}"
wait_and_poll "HALF_OPEN" 15

echo -e "\n${BOLD}--- Circuit Breaker State ---${RESET}"
print_state

# ─── STEP 5: Recover → CLOSED ───────────────────────────────────────────────
echo -e "\n${BOLD}=== STEP 5: Disabling Chaos Monkey — service recovers ===${RESET}"
curl -s -X POST "$CHAOS/disable" > /dev/null
echo -e "${GREEN}Chaos Monkey disabled${RESET}"

send_requests 3 "probe calls — should succeed and close the breaker"

echo -e "\n${BOLD}--- Final Circuit Breaker State ---${RESET}"
print_state

echo -e "\n${BOLD}=== Demo complete ===${RESET}"
echo -e "  CLOSED → OPEN → HALF_OPEN → CLOSED\n"
