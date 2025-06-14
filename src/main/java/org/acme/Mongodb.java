package org.acme;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("org.acme")
@Version("v1")
public class Mongodb extends CustomResource<MongodbSpec,MongodbStatus> implements Namespaced {
}
