package recommender;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class RDFResult {
    private final String label;
    private final String uri;

    private final String uriClass;
    private Double activation;
    private Double predictedPreference;

    private final Double latitude;
    private final Double longitude;
    private Double distance;

    private Double agingValue;

    public Double score() {
        return RecommenderSession.preferenceCoefficient * predictedPreference +
                RecommenderSession.activationCoefficient * activation +
                RecommenderSession.agingCoefficient * agingValue +
                RecommenderSession.distanceCoefficient * distance/RecommenderSession.distance;
    }

    public String toString() {
        return String.format("%s\t(%s) <%s km> (preference: %s) (aging: %s) (activation: %s) (score: %s)",
                label, uriClass.split("#")[1], distance, predictedPreference, agingValue, activation, score());
    }
}
