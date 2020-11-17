package recommender.persistence.manager.mapper;

import recommender.persistence.entity.ClassProperties;
import recommender.persistence.manager.dto.class_properties.AddClassProperties;
import recommender.persistence.manager.dto.class_properties.GetClassProperties;

public final class ClassPropertiesMapper {
    public static ClassProperties fromDTO(AddClassProperties addClassProperties) {
        return ClassProperties.builder()
                .uri(addClassProperties.getUri())
                .preference(addClassProperties.getPreference())
                .confidence(addClassProperties.getConfidence())
                .build();
    }

    public static GetClassProperties toDTO(ClassProperties classProperties) {
        return GetClassProperties.builder()
                .pid(classProperties.getPid())
                .uri(classProperties.getUri())
                .uid(classProperties.getUser().getUid())
                .preference(classProperties.getPreference())
                .confidence(classProperties.getConfidence())
                .build();
    }
}
