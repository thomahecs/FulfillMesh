.PHONY: local-up benchmark-routing infra-up infra-down reset test coverage docker-up docker-down demo frontend-install frontend frontend-build

infra-up:
	./scripts/infra-up.sh

infra-down:
	./scripts/infra-down.sh

reset:
	./scripts/reset-local.sh

test:
	mvn clean test

coverage:
	mvn -Pcoverage clean verify

docker-up:
	docker compose up -d --build

docker-down:
	docker compose down

demo:
	./scripts/demo-flow.sh

frontend-install:
	cd frontend && npm install

frontend:
	cd frontend && npm run dev

frontend-build:
	cd frontend && npm run build

local-up:
	./scripts/local-up.sh

benchmark-routing:
	./scripts/benchmark-routing.sh
