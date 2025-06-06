package org.acme;

// import io.javaoperatorsdk.operator.Operator;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Runner {

    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) {
        // Operator operator = new Operator();
        // operator.register(new MongodbReconciler());
        // operator.start();
         // 1. Crear cliente de Kubernetes (usa config del sistema)
        KubernetesClient client = new KubernetesClientBuilder().build();
        Operator operator = new Operator();
        operator.register(new MongodbReconciler(client));
        // 4. Arrancar el operador
        operator.start();
        log.info("Operator started.");
    }
}
