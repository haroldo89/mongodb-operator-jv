# mongodb-operator-jv

Operador para mongodb desarrollado en java, con javaoperatorsdk 
https://javaoperatorsdk.io/docs/getting-started/


## Paso 1: Pre-requisitos

Asegúrate de tener instalado:

* Java 17+
* Maven
* Docker
* [Kind](https://kind.sigs.k8s.io/) (`brew install kind` en macOS)
* `kubectl` configurado

---

## Paso 2: Crear el proyecto (si no lo hiciste ya)

```bash
mvn io.javaoperatorsdk:bootstrapper:5.1.1:create \
  -DprojectGroupId=org.acme \
  -DprojectArtifactId=mongodb-operator
cd mongodb-operator
```

---

## Paso 3: Implementar tu operador

1. Define tu `CustomResource` (`MongodbCustomResource`, `MongodbSpec`, `MongodbStatus`)
2. Implementa tu `MongodbReconciler`
3. Agrega la clase `Runner` con:

```java
public class Runner {
    public static void main(String[] args) {
        KubernetesClient client = new KubernetesClientBuilder().build();
        Operator operator = Operator.create(client);
        operator.register(new MongodbReconciler(client));
        operator.start();
    }
}
```

---

## Paso 4: Agrega estas dependencias y plugins al `pom.xml`

### Dependencias clave

```xml
<dependencies>
  <dependency>
    <groupId>io.javaoperatorsdk</groupId>
    <artifactId>operator-framework</artifactId>
    <version>5.1.1</version>
  </dependency>
  <dependency>
    <groupId>io.fabric8</groupId>
    <artifactId>kubernetes-client</artifactId>
    <version>6.9.0</version>
  </dependency>
</dependencies>
```

### ⚙️ Plugin para ejecutar `Runner` localmente

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <version>3.1.0</version>
  <configuration>
    <mainClass>org.acme.Runner</mainClass>
  </configuration>
</plugin>
```

---

## Paso 5: Generar el CRD

```bash
mvn clean compile
```

El CRD se genera en:

```
target/classes/META-INF/fabric8/*.yaml
```

Aplica el CRD en Kind:

```bash
kubectl apply -f target/classes/META-INF/fabric8/<archivo_crd>.yml
```

---

## Paso 6: Crea el clúster Kind (si no lo tienes)

```bash
kind create cluster --name java-operator
kubectl cluster-info --context kind-java-operator
```

---

## Paso 7: Crear imagen Docker del operador

1. Crea el Dockerfile:

```Dockerfile
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/mongodb-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

2. Construye la imagen local:

```bash
mvn clean package -DskipTests
docker build -t mongodb-operator:latest .
```

3. Carga la imagen en Kind:

```bash
kind load docker-image mongodb-operator:latest --name java-operator
```

---

## Paso 8: Despliega el operador en Kubernetes

Crea un manifiesto de `Deployment` (por ejemplo, `operator-deployment.yaml`):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mongodb-operator
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mongodb-operator
  template:
    metadata:
      labels:
        app: mongodb-operator
    spec:
      containers:
        - name: operator
          image: mongodb-operator:latest
          imagePullPolicy: IfNotPresent
```

Despliega:

```bash
kubectl apply -f operator-deployment.yaml
```

---

## Paso 9: Crear un recurso `Mongodb`

Ejemplo YAML `mongodb-sample.yaml`:

```yaml
apiVersion: org.acme/v1
kind: MongodbCustomResource
metadata:
  name: test-mongo
spec:
  database: mydb
```

Aplica:

```bash
kubectl apply -f test-resource.yaml
```

---

## Paso 10: Verifica el operador

```bash
kubectl logs deployment/mongodb-operator
kubectl get pods
kubectl get mongodb
```

---

<!-- mvn clean compile exec:java -Dexec.mainClass="org.acme.Runner" -->

## 📦 Makefile: Tareas automatizadas

El proyecto incluye un `Makefile` que simplifica la compilación, construcción de imágenes y despliegue del operador en Kubernetes.

### ⚙️ Tareas disponibles

| Comando              | Descripción                                                                      |
| -------------------- | -------------------------------------------------------------------------------- |
| `make build`         | Compila el proyecto con Maven, omitiendo tests                                   |
| `make image`         | Construye la imagen Docker del operador                                          |
| `make push`          | Sube la imagen al registro configurado                                           |
| `make kind-load`     | Carga la imagen Docker al clúster Kind                                           |
| `make deploy`        | Aplica el `Deployment` en Kubernetes usando la imagen especificada               |
| `make deploy-crd`    | Aplica el CRD generado automáticamente                                           |
| `make deploy-roles`  | Aplica roles, bindings y permisos necesarios para ejecutar el operador           |
| `make deploy-sample` | Aplica un ejemplo de recurso personalizado (`Mongodb`)                           |
| `make logs`          | Muestra los logs del operador desde Kubernetes                                   |
| `make clean`         | Elimina los recursos y archivos temporales generados                             |
| `make all`           | Ejecuta `build`, `image`, `push` y `deploy`                                      |
| `make all-local`     | Ejecuta todo el flujo local: build, image, load en Kind, CRD, roles y despliegue |

> 📄 El archivo `k8s/operator-deployment.yaml` contiene el manifiesto base del `Deployment`. Durante `make deploy`, se reemplaza dinámicamente `__IMAGE__` por la imagen construida (`mongodb-operator:0.1.0` por defecto).

### ✅ Ejemplo de uso

```bash
make all-local
```

Esto compila, construye la imagen, la carga en Kind, aplica permisos, el CRD y despliega el operador en Kubernetes local.

---

