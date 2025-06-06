package org.acme;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

@ControllerConfiguration
public class MongodbReconciler implements Reconciler<Mongodb>, Cleaner<Mongodb>  {

    private static final Logger log = LoggerFactory.getLogger(MongodbReconciler.class);
    private final KubernetesClient client;

    public MongodbReconciler(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public UpdateControl<Mongodb> reconcile(Mongodb resource, Context<Mongodb> context) {
        final String name = resource.getMetadata().getName();
        final String namespace = resource.getMetadata().getNamespace();
        final String database = resource.getSpec().getDatabase();
        MongodbSpec spec = resource.getSpec();

        // 1. Verificar y crear ConfigMap si no existe
        String configMapName = name + "-config";
        ConfigMap existingConfigMap = client.configMaps().inNamespace(namespace).withName(configMapName).get();
        if (existingConfigMap == null) {
            log.info("Creating ConfigMap {}", configMapName);
            ConfigMap configMap = new ConfigMapBuilder()
                    .withNewMetadata().withName(configMapName).withNamespace(namespace).endMetadata()
                    .withData(Map.of("database", database))
                    .build();
            client.configMaps().inNamespace(namespace).create(configMap);
        } else {
            log.info("ConfigMap {} already exists", configMapName);
        }

        // 1b. Verificar y crear segundo ConfigMap con config (si existe)
        String customConfigMapName = name + "-custom-config";
        String configText = spec.getConfig(); // asegúrate que no sea null

        if (configText != null && !configText.trim().isEmpty()) {
            ConfigMap existingCustomConfig = client.configMaps()
                    .inNamespace(namespace)
                    .withName(customConfigMapName)
                    .get();

            boolean needsUpdate = false;

            if (existingCustomConfig == null) {
                log.info("Creating custom ConfigMap {}", customConfigMapName);
                needsUpdate = true;
            } else {
                String existingContent = existingCustomConfig.getData() != null
                        ? existingCustomConfig.getData().get("mongod.conf")
                        : null;

                if (!configText.equals(existingContent)) {
                    log.info("Updating ConfigMap {} due to config change", customConfigMapName);
                    needsUpdate = true;
                }
            }

            if (needsUpdate) {
                ConfigMap updatedConfigMap = new ConfigMapBuilder()
                        .withNewMetadata()
                        .withName(customConfigMapName)
                        .withNamespace(namespace)
                        .endMetadata()
                        .withData(Map.of("mongod.conf", configText))
                        .build();

                client.configMaps()
                        .inNamespace(namespace)
                        .createOrReplace(updatedConfigMap);
            } else {
                log.info("ConfigMap {} is up to date", customConfigMapName);
            }
        }

        // 2. Verificar y crear StatefulSet si no existe
        StatefulSet existingSS = client.apps().statefulSets().inNamespace(namespace).withName(name).get();
        if (existingSS == null) {
            log.info("Creating StatefulSet {}", name);
            StatefulSet statefulSet = new StatefulSetBuilder()
                    .withNewMetadata()
                    .withName(name)
                    .withNamespace(namespace)
                    .withLabels(spec.getLabels())
                    .endMetadata()
                    .withNewSpec()
                    .withServiceName(name)
                    .withReplicas(spec.getReplicas())
                    .withNewSelector()
                    .addToMatchLabels("app", name)
                    .endSelector()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", name)
                    .addToLabels(spec.getLabels())
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName("mongo")
                    .withImage(spec.getImage())
                    .addNewPort().withContainerPort(27017).endPort()
                    .withResources(new ResourceRequirementsBuilder()
                            .withLimits(stringMapToQuantityMap(spec.getResources().getLimits()))
                            .withRequests(stringMapToQuantityMap(spec.getResources().getRequests()))
                            .build())
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();
            client.apps().statefulSets().inNamespace(namespace).create(statefulSet);
        } else {
            log.info("StatefulSet {} already exists", name);
        }

        // 3. Actualizar estado
        ensureStatus(resource).setStatus("MongoDB instance created or already exists");
        log.info("Reconciliation completed for Mongodb: {}", name);
        return UpdateControl.patchStatus(resource).rescheduleAfter(50, TimeUnit.SECONDS);
    }

    @Override
    public DeleteControl cleanup(Mongodb resource, Context<Mongodb> context) {
        final String name = resource.getMetadata().getName();
        final String namespace = resource.getMetadata().getNamespace();

        log.info("Cleaning up resources for Mongodb: {}", name);

        // Eliminar ConfigMaps
        client.configMaps().inNamespace(namespace).withName(name + "-config").delete();
        client.configMaps().inNamespace(namespace).withName(name + "-custom-config").delete();

        // Eliminar StatefulSet
        client.apps().statefulSets().inNamespace(namespace).withName(name).delete();

        // Eliminar PVCs si deletePVC está en true
        if (Boolean.TRUE.equals(resource.getSpec().getDeletePVC())) {
            client.persistentVolumeClaims().inNamespace(namespace)
                    .withLabel("app", name) // Asegúrate que los PVCs tengan esa label
                    .delete();
        }

        // También podrías eliminar Services si los creas
        return DeleteControl.defaultDelete();
    }

    /**
     * Convierte un mapa de cadenas a un mapa de Quantity.
     *
     * @param input el mapa de cadenas a convertir
     * @return un mapa de Quantity
     */
    private Map<String, Quantity> stringMapToQuantityMap(Map<String, String> input) {
        if (input == null) {
            return Collections.emptyMap();
        }
        return input.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new Quantity(e.getValue())
                ));
    }
    /**
     * Asegura que el estado del recurso Mongodb no sea nulo.
     *
     * @param resource el recurso Mongodb
     * @return el estado del recurso, nunca nulo
     */
    private MongodbStatus ensureStatus(Mongodb resource) {
        if (resource.getStatus() == null) {
            resource.setStatus(new MongodbStatus());
        }
        return resource.getStatus();
    }
}
