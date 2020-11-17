package recommender.persistence.manager.dto.class_properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetClassProperties {
    private Integer pid;
    private String uri;
    private Integer uid;
    private Double preference;
    private Double confidence;
}
