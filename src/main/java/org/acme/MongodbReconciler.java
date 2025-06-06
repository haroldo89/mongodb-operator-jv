package org.acme;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ControllerConfiguration
public class MongodbReconciler implements Reconciler<MongodbCustomResource> {

    private static final Logger log = LoggerFactory.getLogger(MongodbReconciler.class);
    private final KubernetesClient client;

    public MongodbReconciler(KubernetesClient client) {
        this.client = client;
    }

    public UpdateControl<MongodbCustomResource> reconcile(MongodbCustomResource resource, Context<MongodbCustomResource> context) {

        final String name = resource.getMetadata().getName();
        final String namespace = resource.getMetadata().getNamespace();
        final String database = resource.getSpec().getDatabase();

        // 1. Crear ConfigMap
        ConfigMap configMap = new ConfigMapBuilder()
            .withNewMetadata().withName(name + "-config").withNamespace(namespace).endMetadata()
            .withData(Map.of("database", database))
            .build();
        client.configMaps().inNamespace(namespace).createOrReplace(configMap);

        // 2. Crear StatefulSet
        StatefulSet statefulSet = new StatefulSetBuilder()
            .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
            .withNewSpec()
                .withServiceName(name)
                .withReplicas(1)
                .withNewSelector().addToMatchLabels("app", name).endSelector()
                .withNewTemplate()
                    .withNewMetadata().addToLabels("app", name).endMetadata()
                    .withNewSpec()
                        .addNewContainer()
                            .withName("mongo")
                            .withImage("mongo:5.0")
                            .addNewPort().withContainerPort(27017).endPort()
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
        client.apps().statefulSets().inNamespace(namespace).createOrReplace(statefulSet);

        // 3. Actualizar estado
        resource.getStatus().setStatus("MongoDB instance created");
        return UpdateControl.patchStatus(resource);
    }
}
