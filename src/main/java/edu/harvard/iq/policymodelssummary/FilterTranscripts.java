package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * 
 * Takes a threshold coordinate and a list of other coordinates, and 
 * breaks that list to those that support the threshold, and the rest.
 * Formally:
 * <code>
 * t = threshold
 * filter: 
 *   \all c \in list, where t \in support(c)
 * - or (semnatically equal) -
 *   \all c \in list, where c \in compliance(p)
 * </code>
 * 
 * @author michael
 */
public class FilterTranscripts extends RunMode {
    
    final PolicyModel model;
    final Transcript threshold;
    final Path itemsDir;
    final Map<String,String> transcriptToName = new HashMap<>();
    final private TranscriptParser tsptParser;

    public FilterTranscripts(Path modelPath, Path thresholdPath, Path itemsDir) throws Exception {
        super("Filter");
        model = loadModel(modelPath);
        o.println("Model " + model.getMetadata().getTitle() + " loaded.");
        
        tsptParser = new TranscriptParser(model);
        threshold = tsptParser.parse(thresholdPath);
        o.println("Threshold file " + threshold.getName() + " loaded.");
        
        this.itemsDir = itemsDir;
        Optional<Path> indexFile = Files.list(itemsDir).filter( p->p.getFileName().toString().toLowerCase().endsWith(".tsv") ).findFirst();
        indexFile.ifPresent(p->{
            try {
                Files.lines(p, StandardCharsets.UTF_8).map(l->l.trim())
                    .filter(l->!l.isEmpty())
                    .map(l->l.split("\t"))
                    .filter(a->a.length==2)
                    .forEach(a->{
                      transcriptToName.put(a[1], a[0]);
                    });
            } catch (IOException ex) {
                o.println("Error reading index file: " + ex.getMessage());
            }
            o.println("Index loaded.");
        });
    }
    
    
    public void go() throws Exception {
        o.println("Loading transcripts");
        
        var tsptFiles = Files.list(itemsDir).filter(p->p.getFileName().toString().toLowerCase().endsWith(".xml"));
        var tspts = tsptFiles.map( p -> {
            try {
                return tsptParser.parse(p);
            } catch (IOException | SAXException | ParserConfigurationException ex) {
                o.println("Error loading transcript file " + p + ": " + ex.getMessage());
            }
            return null;
        }).filter(i->i!=null).collect( toList() );
        o.println("Loaded " + tspts.size() + " transcripts.");
        
        // filter based on tspt coordinate
        
        // send report to stdout, possibly explain why things did not work.
    }
    
}
