#!/usr/bin/env bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
INGEST_PID_FILE="$ROOT_DIR/.ingest.pid"
ADMIN_PID_FILE="$ROOT_DIR/.admin.pid"

usage() {
    echo "Event Processing Platform"
    echo ""
    echo "Usage: ./start.sh [command]"
    echo ""
    echo "Commands:"
    echo "  start              Build and start ingest + admin locally (default)"
    echo "  stop               Stop all local services"
    echo "  restart            Stop then start"
    echo "  status             Show running services"
    echo "  build              Build all modules without starting"
    echo "  test               Run all tests"
    echo "  docker             Start Kafka, PostgreSQL, ingest, admin with Docker"
    echo "  docker-stop        Stop Docker Compose services"
    echo "  engine <name>      Start an engine instance for a pipeline (Docker)"
    echo "  engine-stop <name> Stop an engine instance"
    echo "  engines            List running engine instances"
    echo ""
    echo "Ports:"
    echo "  Kafka: 9092   Ingest: 8090   Admin: 8091   DB: 5877"
    echo ""
    echo "Examples:"
    echo "  ./start.sh docker                        # start infrastructure"
    echo "  ./start.sh engine orders-to-warehouse    # start engine for pipeline"
    echo "  ./start.sh engines                       # list running engines"
    echo "  ./start.sh engine-stop orders-to-warehouse"
    echo "  ./start.sh test                          # run all 39+ tests"
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
    java -jar target/*.jar > "$ROOT_DIR/.$(echo "$name" | tr '[:upper:]' '[:lower:]').log" 2>&1 &
    echo $! > "$pid_file"

    if [ -n "$port" ]; then
        echo "Waiting for $name on port $port..."
        for i in $(seq 1 30); do
            if curl -s "http://localhost:$port/api/health" > /dev/null 2>&1 || \
               curl -s "http://localhost:$port/api/status" > /dev/null 2>&1; then
                echo "$name started (PID $(cat "$pid_file"))"
                return
            fi
            sleep 1
        done
        echo "$name may not have started. Check logs."
    else
        echo "$name started (PID $(cat "$pid_file"))"
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
    start_service "event-admin" "$ROOT_DIR/event-admin" "$ADMIN_PID_FILE" "8091"
    echo ""
    echo "Infrastructure running:"
    echo "  Ingest:  http://localhost:8090"
    echo "  Admin:   http://localhost:8091"
    echo "  Swagger: http://localhost:8090/swagger-ui.html"
    echo ""
    echo "Next: create a pipeline via admin, then start an engine instance."
    echo "Run ./start.sh stop to shut down."
}

do_stop() {
    stop_service "event-admin" "$ADMIN_PID_FILE"
    stop_service "event-ingest" "$INGEST_PID_FILE"
}

do_status() {
    echo "Local services:"
    for svc in ingest admin; do
        pid_file="$ROOT_DIR/.$svc.pid"
        if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
            echo "  event-$svc: running (PID $(cat "$pid_file"))"
        else
            echo "  event-$svc: stopped"
        fi
    done
    echo ""
    echo "Docker containers:"
    cd "$ROOT_DIR"
    docker compose ps 2>/dev/null || echo "  Docker Compose not running"
}

do_docker() {
    do_stop
    echo "Starting with Docker Compose..."
    cd "$ROOT_DIR"
    docker compose up --build -d "$@"
}

do_docker_stop() {
    cd "$ROOT_DIR"
    docker compose down
}

do_engine_start() {
    local pipeline_name=$1
    if [ -z "$pipeline_name" ]; then
        echo "Usage: ./start.sh engine <pipeline-name>"
        echo ""
        echo "Example: ./start.sh engine orders-to-warehouse"
        exit 1
    fi

    echo "Starting engine for pipeline: $pipeline_name"
    cd "$ROOT_DIR"
    PIPELINE_NAME="$pipeline_name" docker compose run -d event-engine
    echo "Engine started for pipeline: $pipeline_name"
}

do_engine_stop() {
    local pipeline_name=$1
    if [ -z "$pipeline_name" ]; then
        echo "Usage: ./start.sh engine-stop <pipeline-name>"
        exit 1
    fi

    echo "Stopping engine for pipeline: $pipeline_name"
    cd "$ROOT_DIR"
    docker ps --filter "label=com.docker.compose.service=event-engine" --format "{{.ID}} {{.Names}}" | while read id name; do
        if docker exec "$id" printenv PIPELINE_NAME 2>/dev/null | grep -q "$pipeline_name"; then
            docker stop "$id"
            docker rm "$id"
            echo "Stopped: $name"
            return
        fi
    done
    echo "No engine found for pipeline: $pipeline_name"
}

do_engines_list() {
    echo "Running engine instances:"
    cd "$ROOT_DIR"
    docker ps --filter "label=com.docker.compose.service=event-engine" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null
    if [ $? -ne 0 ] || [ -z "$(docker ps --filter 'label=com.docker.compose.service=event-engine' -q 2>/dev/null)" ]; then
        echo "  No engines running"
    fi
}

case "${1:-start}" in
    start)        do_start ;;
    stop)         do_stop ;;
    restart)      do_stop; do_start ;;
    status)       do_status ;;
    build)        build ;;
    test)         cd "$ROOT_DIR" && ./mvnw test ;;
    docker)       shift; do_docker "$@" ;;
    docker-stop)  do_docker_stop ;;
    engine)       do_engine_start "$2" ;;
    engine-stop)  do_engine_stop "$2" ;;
    engines)      do_engines_list ;;
    -h|--help|help) usage ;;
    *)            usage ;;
esac
