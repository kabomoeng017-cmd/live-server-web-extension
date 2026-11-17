#!/usr/bin/env bash
set -euo pipefail
REGISTRY="${REGISTRY:-registry.example.gov.bw}"
VERSION="${VERSION:-3.0.0}"

docker build -t "$REGISTRY/dtef/backend:$VERSION" ./backend
docker build --build-arg API_INTERNAL_BASE=http://dtef-backend:8080/api -t "$REGISTRY/dtef/frontend:$VERSION" ./frontend
docker push "$REGISTRY/dtef/backend:$VERSION"
docker push "$REGISTRY/dtef/frontend:$VERSION"
