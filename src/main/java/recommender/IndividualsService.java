package recommender;


import lombok.Getter;
import lombok.Setter;
import math.GeoLocation;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import java.util.ArrayList;
import java.util.List;

/**
 * This class will be making the queries to the Fuseki server.
 */

@Getter
@Setter
public class IndividualsService {
    public static String serviceURI = "http://localhost:3030/datatourisme/query";
    public static String preffixes = "PREFIX datatourisme: <https://www.datatourisme.gouv.fr/ontology/core#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

    public static List<RDFResult> getResults(List<String> uriSuperClasses, GeoLocation maxBound, GeoLocation minBound) {
        String query = preffixes + "SELECT ?label ?uri ?uriClass ?latitude ?longitude\n" +
                "WHERE {\n";

        // Get all candidates for each class
        List<String> subSets = new ArrayList<>();
        for (String uriSuperClass : uriSuperClasses) {
            String subset = String.format("{\n" +
                            "  ?uri rdf:type ?uriClass .\n" +
                            "  ?uri ?predicate ?location .\n" +
                            "  ?uri rdfs:label ?label .\n" +
                            "  ?location <http://schema.org/geo> ?geo .\n" +
                            "  ?geo <http://schema.org/latitude> ?latitude .\n" +
                            "  ?geo <http://schema.org/longitude> ?longitude .\n" +
                            "  \n" +
                            "  filter(?uriClass = <%s> && ?latitude <= \"%s\"^^xsd:decimal && ?latitude >= \"%s\"^^xsd:decimal &&\n" +
                            "         ?longitude <= \"%s\"^^xsd:decimal && ?longitude >= \"%s\"^^xsd:decimal)\n" +
                            "}", uriSuperClass, maxBound.getLatitudeInDegrees(), minBound.getLatitudeInDegrees(),
                    maxBound.getLongitudeInDegrees(), minBound.getLongitudeInDegrees());

            subSets.add(subset);
        }

        // Concat into a single query
        query = query.concat(String.join("\nUNION\n", subSets) + "}");

        try (QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, query)) {
            ResultSet result = q.execSelect();
            List<RDFResult> results = new ArrayList<>();
            while (result.hasNext()) {
                QuerySolution soln = result.nextSolution();
                results.add(new RDFResult(
                        soln.get("label").asLiteral().getString(),
                        soln.get("uri").asResource().getURI(),
                        soln.get("uriClass").asResource().getURI(),
                        soln.get("latitude").asLiteral().getDouble(),
                        soln.get("longitude").asLiteral().getDouble()));
            }
            return results;
        }
    }
}
