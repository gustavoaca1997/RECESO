package recommender.semantic.network;


import org.apache.jena.ontology.OntClass;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

public class SemanticNetworkTests {
    static private SemanticNetwork semanticNetwork;

    static {
        try {
            semanticNetwork = new SemanticNetwork();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void readModelTest()  {
        Assert.assertNotNull("Failed to read ontology", semanticNetwork.getOntModel());
    }

    @Test
    public void createIteratorTest() {
        Iterator<OntClass> iterator = semanticNetwork.iterator();
        Assert.assertNotNull("Iterator is null", iterator);
        Assert.assertTrue("Iterator is empty", iterator.hasNext());
    }

//    @Test
//    public void rootClassIsPOITest() {
//        Iterator<OntClass> iterator = semanticNetwork.iterator();
//        OntClass root = iterator.next();
//        Assert.assertEquals("Ontology Classes don't match", PLACE_URI, root.getURI());
//    }
}
