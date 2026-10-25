.DEFAULT_GOAL := help
.PHONY: help build check test dev docker-build clean

help: ## Show this help message with available commands
	@echo "SDD Ecommerce - Available Commands:"
	@echo "===================================="
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: ## Assemble and build the executable application JAR
	./gradlew build -x test

check: ## Run all static quality analysis, linters, and ArchUnit architecture tests
	./gradlew check

test: ## Execute unit and integration test suites against ephemeral TestResources
	./gradlew test

dev: ## Run the application locally in development mode
	./gradlew run

docker-build: ## Build the multi-stage, non-root Docker container image
	docker build -t sdd-ecommerce:latest .

clean: ## Remove build artifacts and temporary directories
	./gradlew clean
