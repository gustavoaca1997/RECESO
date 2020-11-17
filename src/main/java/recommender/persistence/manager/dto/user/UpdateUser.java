package recommender.persistence.manager.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderMethodName = "updateBuilder")
public class UpdateUser {
    String username;
    Integer uid;
}
