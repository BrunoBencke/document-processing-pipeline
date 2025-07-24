#!/bin/bash

set -e

print_status() {
    echo -e "$[INFO]${NC} $1"
}

print_success() {
    echo -e "$[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "$[WARNING]${NC} $1"
}

print_error() {
    echo -e "$[ERROR]${NC} $1"
}

check_docker() {
    if command -v docker &> /dev/null; then
        print_success "Docker is installed"
        docker --version
    else
        print_error "Docker is not installed"
        echo "Please install Docker from: https://docs.docker.com/get-docker/"
        exit 1
    fi
}

check_docker_compose() {
    if docker compose version &> /dev/null; then
        print_success "Docker Compose is available"
        docker compose version
    elif command -v docker-compose &> /dev/null; then
        print_success "Docker Compose (standalone) is available"
        docker-compose --version
        COMPOSE_CMD="docker-compose"
    else
        print_error "Docker Compose is not available"
        echo "Please install Docker Compose"
        exit 1
    fi
}

check_java() {
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        print_success "Java is installed: $JAVA_VERSION"
    else
        print_warning "Java is not installed"
        echo "Please install Java 17+ from: https://adoptium.net/"
    fi
}

check_node() {
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node --version)
        print_success "Node.js is installed: $NODE_VERSION"
    else
        print_warning "Node.js is not installed"
        echo "Please install Node.js 18+ from: https://nodejs.org/"
    fi
}

start_infrastructure() {
    print_status "Starting infrastructure services..."
    
    if [ "$COMPOSE_CMD" = "docker-compose" ]; then
        docker-compose -f docker-compose.dev.yml up -d
    else
        docker compose -f docker-compose.dev.yml up -d
    fi
    
    echo "  MongoDB: mongodb://localhost:27017"
    echo "  RabbitMQ Management: http://localhost:15672 (admin/admin123)"

}

stop_infrastructure() {   
    if [ "$COMPOSE_CMD" = "docker-compose" ]; then
        docker-compose -f docker-compose.dev.yml down
    else
        docker compose -f docker-compose.dev.yml down
    fi
    
    print_success "Infrastructure services stopped!"
}

show_status() {
    print_status "Infrastructure services status:"
    
    if [ "$COMPOSE_CMD" = "docker-compose" ]; then
        docker-compose -f docker-compose.dev.yml ps
    else
        docker compose -f docker-compose.dev.yml ps
    fi
}

cleanup() {
    print_status "Cleaning up containers and volumes..."
    
    if [ "$COMPOSE_CMD" = "docker-compose" ]; then
        docker-compose -f docker-compose.dev.yml down -v
    else
        docker compose -f docker-compose.dev.yml down -v
    fi
    
    print_success "Cleanup completed!"
}

main() {
    case "${1:-help}" in
        "start")
            check_docker
            check_docker_compose
            start_infrastructure
            ;;
        "stop")
            check_docker
            check_docker_compose
            stop_infrastructure
            ;;
        "status")
            check_docker
            check_docker_compose
            show_status
            ;;
        "logs")
            check_docker
            check_docker_compose
            view_logs "$2"
            ;;
        "cleanup")
            check_docker
            check_docker_compose
            cleanup
            ;;
        "check")
            check_docker
            check_docker_compose
            check_java
            check_node
            ;;
    esac
}

COMPOSE_CMD="docker compose"

main "$@"