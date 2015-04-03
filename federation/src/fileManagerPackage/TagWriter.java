package fileManagerPackage;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import changeServerPackage.ChangeCapsule;

/**
 * Created by IntelliJ IDEA.
 * User: candidasa
 * Date: Jan 11, 2008
 * Time: 3:45:43 PM
 * Applies a series of changes to a baseline ontology to create a new "tag", i.e. version of the ontology with those changes applied
 */
public class TagWriter {
    public static final String TAGPREFIX = "tag";
    public static final String TAGEXTENSION = ".owl";
    OWLOntologyManager manager;
    OWLOntology latestOntology;
    File latestOntologyFile;    //latest tag or the baseline, if not tags have yet been created

    public TagWriter(File ontologyFile) throws OWLOntologyCreationException {
        latestOntologyFile = ontologyFile;

        manager = OWLManager.createOWLOntologyManager();
        latestOntology = manager.loadOntologyFromOntologyDocument(ontologyFile);
    }


    public void applyChanges(String changeFile) throws URISyntaxException, OWLOntologyChangeException {

    }


    public void applyChanges(ArrayList<ChangeCapsule> changes) throws URISyntaxException, OWLOntologyChangeException {
        for(ChangeCapsule cp : changes) {
            List<OWLOntologyChange> ontoChanges = cp.getChangeOWL(manager);
            manager.applyChanges(ontoChanges);
        }
    }


    /** creates a new file one count up from the latest file this object was instantiated with and saves it to disk.
     * Returns a reference to the new file */
    public File saveNewTag() throws OWLOntologyStorageException {
        OWLDataFactory df = manager.getOWLDataFactory();

        //increment the version count of the file name
        Integer sequenceNumber = -1;
        if (latestOntologyFile.getName().startsWith(TAGPREFIX)) {
            //only get the sequence number if there are already tags, otherwise just start sequence numbering at zero
            sequenceNumber = new Integer(latestOntologyFile.getName().substring(TAGPREFIX.length(), latestOntologyFile.getName().length()-TAGEXTENSION.length()));  //cut off the extension
        }

        sequenceNumber++;   //increase the sequence number count for the tag
        String sequenceString = ChangeWriter.addLeadingZeros(sequenceNumber.toString(), 5);
        String newFileName = TAGPREFIX + sequenceString + TAGEXTENSION;

        //remove existing change version annotation
        Set<OWLAnnotation> allAnnotations = latestOntology.getAnnotations();
        for (OWLAnnotation annotation : allAnnotations) {
            if (annotation.getProperty().getIRI()
                    .compareTo(OWLRDFVocabulary.OWL_VERSION_INFO.getIRI()) == 0) {
                if (annotation.getValue() instanceof OWLLiteral) {
                    String literal = ((OWLLiteral) annotation.getValue())
                            .getLiteral();
                    if (literal.startsWith(TagReader.CHANGEAXIOMPREFIX)) {
                        try {
                            manager.applyChange(new RemoveOntologyAnnotation(
                                    latestOntology, annotation));
                        } catch (OWLOntologyChangeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        //add an annotation to indicate the version of this tag
        OWLLiteral cons = df.getOWLLiteral(TagReader.CHANGEAXIOMPREFIX
                + sequenceNumber);
        OWLAnnotation anno = df.getOWLAnnotation(df
                .getOWLAnnotationProperty(OWLRDFVocabulary.OWL_VERSION_INFO
                        .getIRI()), cons);

        try {
            manager.applyChange(new AddOntologyAnnotation(latestOntology, anno));
        } catch (OWLOntologyChangeException e) {
            e.printStackTrace();
        }

        //save the tag to disk
        File newFile = new File(latestOntologyFile.getParentFile().getAbsoluteFile().getPath(), newFileName);
        manager.saveOntology(latestOntology, new OWLXMLOntologyFormat(),
                IRI.create(newFile));
        manager.removeOntology(latestOntology.getOntologyID());

        return newFile;
    }
    

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TagWriter <ontology-base> <change-file1> <change-file2> ...");
            System.exit(1);
        }

        try {
            TagWriter writer = new TagWriter(new File(args[0]));
            ChangeReader cr = new ChangeReader(new File(args[1]));  //the base file passed in here is not taken into account, since the getChange method is directly called
            ArrayList<ChangeCapsule> changeCaps = new ArrayList<ChangeCapsule>();
            for(int i=1; i < args.length; i++) {
                ChangeCapsule cap = cr.getChange(new File(args[i]));
                changeCaps.add(cap);
            }

            writer.applyChanges(changeCaps);
            writer.saveNewTag();

        //} catch (IOException e) {
        //    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (OWLOntologyChangeException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
