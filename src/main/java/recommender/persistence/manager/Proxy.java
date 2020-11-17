package recommender.persistence.manager;

import lombok.RequiredArgsConstructor;
import recommender.persistence.entity.ClassProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class Proxy {
    private final ClassPropertiesManager propertiesManager;
    private final Map<String, ClassProperties> propertiesMap;

    public Proxy(ClassPropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
        this.propertiesMap = new HashMap<>();
    }

    public ClassProperties addClassProperties(ClassProperties classProperties) {
        propertiesManager.addClassProperties(classProperties);
        propertiesMap.put(classProperties.getUri(), classProperties);
        return classProperties;
    }

    public Set<ClassProperties> listClassPropertiesByUserAsSet(Integer uid) {
       return propertiesManager.listClassPropertiesByUserAsSet(uid);
    }

    public Map<String, ClassProperties> listClassPropertiesByUserAsMap(Integer uid) {
        return propertiesManager.listClassPropertiesByUserAsMap(uid);
    }

    public ClassProperties getClassProperties(String uri, Integer uid) {
        return propertiesMap.computeIfAbsent(uri, k -> propertiesManager.getClassProperties(uri, uid));
    }

    public ClassProperties updateClassProperties(ClassProperties updatedProperties) {
        propertiesManager.updateClassProperties(updatedProperties);
        propertiesMap.put(updatedProperties.getUri(), updatedProperties);
        return updatedProperties;
    }
}
