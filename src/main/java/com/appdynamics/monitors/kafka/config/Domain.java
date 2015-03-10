package com.appdynamics.monitors.kafka.config;


import java.util.List;

public class Domain {
    private String name;
    private List<String> excludeObjects;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getExcludeObjects() {
        return excludeObjects;
    }

    public void setExcludeObjects(List<String> excludeObjects) {
        this.excludeObjects = excludeObjects;
    }
}
