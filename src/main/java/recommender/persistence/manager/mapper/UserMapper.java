package recommender.persistence.manager.mapper;

import recommender.persistence.entity.User;
import recommender.persistence.manager.dto.user.CreateUser;
import recommender.persistence.manager.dto.user.GetUser;
import recommender.persistence.manager.dto.user.UpdateUser;

public final class UserMapper {
    public static GetUser toDTO(User user) {
        return GetUser.builder()
                .username(user.getUsername())
                .uid(user.getUid())
                .build();
    }

    public static User fromDTO(CreateUser createUser) {
        return User.builder()
                .username(createUser.getUsername())
                .build();
    }

    public static void fromDTO(User user, UpdateUser createDTO) {
        user.setUsername(createDTO.getUsername());
    }
}
