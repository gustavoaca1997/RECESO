package recommender.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private int uid;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private Set<ClassProperties> classPropertiesSet;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private Set<Relevance> relevanceSet;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private Set<Aging> agingSet;
}
