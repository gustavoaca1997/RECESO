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
                RecommenderSession.agingCoefficient * 1D + // Because Bahramian et al don't use aging.
                RecommenderSession.distanceCoefficient * distance/RecommenderSession.distance;
    }

    private String trimString(String str) {
        int MAX_LENGTH = 17;
        String suff = str.length() > MAX_LENGTH ? "..." : "";
        return str.substring(0, Math.min(str.length(), MAX_LENGTH)) + suff;
    }

    public String toString() {
        return String.format("%s\t(%s) <%s km> (preference: %s) (aging: %s) (activation: %s) (score: %s)",
                trimString(label),
                trimString(uriClass.split("#")[1]),
                distance,
                predictedPreference,
                agingValue,
                activation,
                score());
    }

    public String toLatexItem() {
        return String.format(" \\scriptsize{%s} & \\scriptsize{%s} & %.4f & %.4f & %.4f & %.4f & %.4f",
                trimString(label), trimString(uriClass.split("#")[1]),
                distance, predictedPreference, agingValue, activation, score());
    }
}
