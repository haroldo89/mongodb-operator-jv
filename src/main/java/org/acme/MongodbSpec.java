package org.acme;

import java.util.List;
import java.util.Map;

public class MongodbSpec {

    private String database;
    private Integer replicas;
    private String storage;
    private String storageClassName;
    private List<String> accessModes;
    private Boolean deletePVC;
    private String image;
    private Boolean purgekeysonrebalance;
    private Map<String, String> labels;
    private Resources resources;
    private String config;

    // Getters y Setters

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getStorageClassName() {
        return storageClassName;
    }

    public void setStorageClassName(String storageClassName) {
        this.storageClassName = storageClassName;
    }

    public List<String> getAccessModes() {
        return accessModes;
    }

    public void setAccessModes(List<String> accessModes) {
        this.accessModes = accessModes;
    }

    public Boolean getDeletePVC() {
        return deletePVC;
    }

    public void setDeletePVC(Boolean deletePVC) {
        this.deletePVC = deletePVC;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Boolean getPurgekeysonrebalance() {
        return purgekeysonrebalance;
    }

    public void setPurgekeysonrebalance(Boolean purgekeysonrebalance) {
        this.purgekeysonrebalance = purgekeysonrebalance;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    // Clase anidada para recursos
    public static class Resources {
        private Map<String, String> limits;
        private Map<String, String> requests;

        public Map<String, String> getLimits() {
            return limits;
        }

        public void setLimits(Map<String, String> limits) {
            this.limits = limits;
        }

        public Map<String, String> getRequests() {
            return requests;
        }

        public void setRequests(Map<String, String> requests) {
            this.requests = requests;
        }
    }
}
