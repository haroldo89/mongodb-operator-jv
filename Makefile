APP_NAME = mongodb-operator
VERSION = 0.1.0
IMAGE = $(APP_NAME):$(VERSION)
NAMESPACE = default
DEPLOYMENT_YAML = ./k8s/operator-deployment.yaml
TMP_YAML = ./k8s/operator-deployment.generated.yaml

.PHONY: all build image push deploy clean logs kind-load deploy-crd deploy-roles all-local

all: build image push deploy

all-local: build image kind-load deploy-roles deploy-crd deploy

## Compila el proyecto con Maven
build:
	mvn clean package -DskipTests

## Construye la imagen Docker
image:
	docker build -t $(IMAGE) .

## Sube la imagen al registro (Docker Hub, GHCR, etc.)
push:
	docker push $(IMAGE)

deploy-crd:
	kubectl apply -f ./target/classes/META-INF/fabric8/mongodbs.org.acme-v1.yml

deploy-roles:
	kubectl apply -f ./k8s/role.yaml
	kubectl apply -f ./k8s/role-binding.yaml
	kubectl apply -f ./k8s/cluster-role.yaml
	kubectl apply -f ./k8s/cluster-role-binding.yaml

deploy-sample:
	kubectl apply -f ./samples/mongodb-sample.yaml

## Aplica los manifiestos de Kubernetes
deploy:
	sed "s|__IMAGE__|$(IMAGE)|g" $(DEPLOYMENT_YAML) > $(TMP_YAML)
	kubectl apply -f $(TMP_YAML)

## Elimina los recursos desplegados
clean:
	kubectl delete -f $(TMP_YAML) || true
	rm -f $(TMP_YAML)

## Muestra los logs del pod del operador
logs:
	kubectl logs -f deployment/$(APP_NAME) -n $(NAMESPACE)

## Etiqueta la imagen para usar con Kind (local)
kind-load:
	kind load docker-image $(IMAGE) --name kind
