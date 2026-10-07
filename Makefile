.PHONY: infra-up infra-down reset test coverage docker-up docker-down demo

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
