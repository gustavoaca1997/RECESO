package recommender.semantic.util.constants;

import java.util.Arrays;
import java.util.List;

public final class OntologyConstants {
    public static List<String> baseUris = Arrays.asList(
            "https://www.datatourisme.gouv.fr/ontology/core#CulturalSite",
            "https://www.datatourisme.gouv.fr/ontology/core#FoodEstablishment",
            "https://www.datatourisme.gouv.fr/ontology/core#NaturalHeritage",
            "https://www.datatourisme.gouv.fr/ontology/core#LeisurePlace",
            "https://www.datatourisme.gouv.fr/ontology/core#Sports",
            "https://www.datatourisme.gouv.fr/ontology/core#Store"
    );

    public static List<String> formUris = Arrays.asList(
            "https://www.datatourisme.gouv.fr/ontology/core#Museum",
            "https://www.datatourisme.gouv.fr/ontology/core#InterpretationCentre",
            "https://www.datatourisme.gouv.fr/ontology/core#Library",
            "https://www.datatourisme.gouv.fr/ontology/core#ParkAndGarden",
            "https://www.datatourisme.gouv.fr/ontology/core#ArcheologicalSite",
            "https://www.datatourisme.gouv.fr/ontology/core#ReligiousSite",
            "https://www.datatourisme.gouv.fr/ontology/core#RemarkableBuilding",
            "https://www.datatourisme.gouv.fr/ontology/core#CityHeritage",
            "https://www.datatourisme.gouv.fr/ontology/core#DefenceSite",
            "https://www.datatourisme.gouv.fr/ontology/core#RemembranceSite",
            "https://www.datatourisme.gouv.fr/ontology/core#TechnicalHeritage",
            "https://www.datatourisme.gouv.fr/ontology/core#FoodEstablishment",
            "https://www.datatourisme.gouv.fr/ontology/core#NaturalHeritage",
            "https://www.datatourisme.gouv.fr/ontology/core#LeisurePlace",
            "https://www.datatourisme.gouv.fr/ontology/core#Sports",
            "https://www.datatourisme.gouv.fr/ontology/core#Store"
    );

    public static final String CULTURAL_SITE_URI = "https://www.datatourisme.gouv.fr/ontology/core#CulturalSite";

    public static final String PLACE_URI = "https://www.datatourisme.gouv.fr/ontology/core#PlaceOfInterest";

    public static final String MODEL_FILE = "datatourisme.ttl";
}
