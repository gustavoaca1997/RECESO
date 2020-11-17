package recommender.semantic.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import recommender.semantic.util.constants.OntologyConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
public class SemanticNetwork implements Iterable {
    private final OntModel ontModel;
    /*
        Constructors
     */

    public SemanticNetwork() throws IOException {
        ontModel = ModelFactory.createOntologyModel();
        this.read();
    }

    public SemanticNetwork(String file) throws IOException {
        ontModel = ModelFactory.createOntologyModel();
        this.read(file);
    }

    /*
        Iterable interface methods
     */

    public Iterator<OntClass> iterator() {
        return new SemanticNetworkIterator(this);
    }

    public Iterator<OntClass> iterator(List<String> sources) {
        return new SemanticNetworkIterator(this, sources);
    }
    /*
        Utility methods
     */

    /**
     * Read default ontology model
     *
     * @return always true
     * @throws IOException when there is an error reading file
     */
    private boolean read() throws IOException {
        readOntology(OntologyConstants.MODEL_FILE, ontModel);
        return true;
    }

    /**
     * Read ontology model
     * @param file path to ontology file
     * @return always true
     * @throws IOException when there is an error reading file
     */
    private boolean read(String file) throws IOException {
        readOntology(file, ontModel);
        return true;
    }

    private static void readOntology(String file, OntModel model) throws IOException {
        InputStream in = Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource(file)).openStream();
        model.read(in, null, "TTL");
        in.close();
    }

    /**
     * Return {@code OntClass} with the uri specified.
     * @param uri   Unique Resource Identifier of the Ontology Class
     * @return      a {@code OntClass} with the uri.
     */
    public OntClass getOntClass(String uri) {
        return ontModel.getOntClass(uri);
    }

}
