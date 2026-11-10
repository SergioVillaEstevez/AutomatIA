.PHONY: test test-backend test-frontend backend frontend db

test: test-backend test-frontend

test-backend:
	mvn -f backend/pom.xml test

test-frontend:
	cd frontend && npx ng test --watch=false

backend:
	mvn -f backend/pom.xml spring-boot:run

frontend:
	cd frontend && npm start

db:
	./scripts/init-local-db.sh
