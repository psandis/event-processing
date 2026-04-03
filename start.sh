#!/usr/bin/env bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
INGEST_PID_FILE="$ROOT_DIR/.ingest.pid"
ENGINE_PID_FILE="$ROOT_DIR/.engine.pid"
ADMIN_PID_FILE="$ROOT_DIR/.admin.pid"

usage() {
    echo "Event Processing Platform"
    echo ""
    echo "Usage: ./start.sh [command]"
    echo ""
    echo "Commands:"
    echo "  start        Build and start all services locally (default)"
    echo "  stop         Stop all local services"
    echo "  restart      Stop then start"
    echo "  status       Show running services"
    echo "  build        Build all modules without starting"
    echo "  test         Run all tests"
    echo "  docker       Start all services with Docker Compose"
    echo "  docker-stop  Stop Docker Compose services"
    echo ""
    echo "Ports:"
    echo "  Kafka: 9092   Ingest: 8090   Admin: 8091   DB: 5877"
    echo ""
    echo "Examples:"
    echo "  ./start.sh              # build and start locally"
    echo "  ./start.sh stop         # stop local services"
    echo "  ./start.sh docker       # start with Docker"
    echo "  ./start.sh docker -d    # start with Docker (detached)"
    echo "  ./start.sh test         # run all tests"
    echo ""
}

build() {
    echo "Building all modules..."
    cd "$ROOT_DIR"
    ./mvnw package -DskipTests -q
    echo "Build complete."
}

start_service() {
    local name=$1
    local dir=$2
    local pid_file=$3
    local port=$4

    if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
        echo "$name already running (PID $(cat "$pid_file"))"
        return
    fi

    echo "Starting $name..."
    cd "$dir"
    java -jar target/*.jar > "$ROOT_DIR/.$( echo "$name" | tr '[:upper:]' '[:lower:]').log" 2>&1 &
    echo $! > "$pid_file"

    if [ -n "$port" ]; then
        echo "Waiting for $name on port $port..."
        for i in $(seq 1 30); do
            if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1 || \
               curl -s "http://localhost:$port/api/health" > /dev/null 2>&1; then
                echo "$name started (PID $(cat "$pid_file"))"
                return
            fi
            sleep 1
        done
        echo "$name may not have started. Check .$( echo "$name" | tr '[:upper:]' '[:lower:]').log"
    else
        echo "$name started in background (PID $(cat "$pid_file"))"
    fi
}

stop_service() {
    local name=$1
    local pid_file=$2

    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid"
            echo "$name stopped (PID $pid)"
        else
            echo "$name not running"
        fi
        rm -f "$pid_file"
    else
        echo "$name not running"
    fi
}

do_start() {
    do_stop
    build
    start_service "event-ingest" "$ROOT_DIR/event-ingest" "$INGEST_PID_FILE" "8090"
    start_service "event-engine" "$ROOT_DIR/event-engine" "$ENGINE_PID_FILE" ""
    start_service "event-admin" "$ROOT_DIR/event-admin" "$ADMIN_PID_FILE" "8091"
    echo ""
    echo "Services running:"
    echo "  Ingest: http://localhost:8090"
    echo "  Admin:  http://localhost:8091"
    echo "  Swagger (ingest): http://localhost:8090/swagger-ui.html"
    echo "  Swagger (admin):  http://localhost:8091/swagger-ui.html"
    echo ""
    echo "Run ./start.sh stop to shut down."
}

do_stop() {
    stop_service "event-admin" "$ADMIN_PID_FILE"
    stop_service "event-engine" "$ENGINE_PID_FILE"
    stop_service "event-ingest" "$INGEST_PID_FILE"
}

do_status() {
    for svc in event-ingest event-engine event-admin; do
        pid_file="$ROOT_DIR/.$(echo $svc | sed 's/event-//').pid"
        if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
            echo "$svc: running (PID $(cat "$pid_file"))"
        else
            echo "$svc: stopped"
        fi
    done
}

do_docker() {
    do_stop
    echo "Starting with Docker Compose..."
    cd "$ROOT_DIR"
    docker compose up --build "$@"
}

do_docker_stop() {
    cd "$ROOT_DIR"
    docker compose down
}

case "${1:-start}" in
    start)       do_start ;;
    stop)        do_stop ;;
    restart)     do_stop; do_start ;;
    status)      do_status ;;
    build)       build ;;
    test)        cd "$ROOT_DIR" && ./mvnw test ;;
    docker)      shift; do_docker "$@" ;;
    docker-stop) do_docker_stop ;;
    -h|--help|help) usage ;;
    *)           usage ;;
esac
