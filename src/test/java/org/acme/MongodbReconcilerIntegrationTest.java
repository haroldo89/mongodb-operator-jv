package org.acme;

import static org.acme.ConfigMapDependentResource.KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

class MongodbReconcilerIntegrationTest {

    public static final String RESOURCE_NAME = "test1";
    public static final String INITIAL_VALUE = "initial value";
    public static final String CHANGED_VALUE = "changed value";

    @RegisterExtension
    LocallyRunOperatorExtension extension =
            LocallyRunOperatorExtension.builder()
                    .withReconciler(MongodbReconciler.class)
                    .build();

    @Test
    void testCRUDOperations() {
        var cr = extension.create(testResource());

        await().untilAsserted(() -> {
            var cm = extension.get(ConfigMap.class, RESOURCE_NAME);
            assertThat(cm).isNotNull();
            assertThat(cm.getData()).containsEntry(KEY, INITIAL_VALUE);
        });

        cr.getSpec().setDatabase(CHANGED_VALUE);
        cr = extension.replace(cr);

        await().untilAsserted(() -> {
            var cm = extension.get(ConfigMap.class, RESOURCE_NAME);
            assertThat(cm.getData()).containsEntry(KEY, CHANGED_VALUE);
        });

        extension.delete(cr);

        await().untilAsserted(() -> {
            var cm = extension.get(ConfigMap.class, RESOURCE_NAME);
            assertThat(cm).isNull();
        });
    }

    Mongodb testResource() {
        var resource = new Mongodb();
        resource.setMetadata(new ObjectMetaBuilder()
                .withName(RESOURCE_NAME)
                .build());
        resource.setSpec(new MongodbSpec());
        resource.getSpec().setDatabase(INITIAL_VALUE);
        return resource;
    }
}
