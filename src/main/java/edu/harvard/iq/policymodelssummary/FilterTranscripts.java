package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodels.model.policyspace.slots.AbstractSlot;
import edu.harvard.iq.policymodels.model.policyspace.values.AbstractValue;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodelssummary.policyspace.ExpandedSlotValue;
import edu.harvard.iq.policymodelssummary.policyspace.SlotPathCollector;
import edu.harvard.iq.policymodelssummary.reports.CoordinateDifference;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private List<String> thresholdSlotNames;
    final Path itemsDir;
    final Map<String,String> transcriptToName = new HashMap<>();
    final private TranscriptParser tsptParser;
    final Path modelPath;

    public FilterTranscripts(Path aModelPath, Path thresholdPath, Path itemsDir) throws Exception {
        super("Filter");
        o.println("Model: " + aModelPath.toAbsolutePath().normalize());
        o.println("Threshold: " + thresholdPath.toAbsolutePath().normalize());
        o.println("Repos: " + itemsDir.toAbsolutePath().normalize());
        
        modelPath = aModelPath;
        model = loadModel(modelPath);
        o.println("Model " + model.getMetadata().getTitle() + " loaded.");
        
        tsptParser = new TranscriptParser(model);
        threshold = tsptParser.parse(thresholdPath);
        thresholdSlotNames = new SlotPathCollector().collect(threshold.getCoordinate());
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
        
        // filter transcripts based on threshold coordinate
        final Set<Transcript> accepted = new HashSet<>();
        final Set<Transcript> refused = new HashSet<>();
        final Map<String, List<CoordinateDifference>> explanations = new HashMap<>();

        tspts.forEach( t -> {
            List<CoordinateDifference> blockers = listBlockerDimensions(t);
            Set<Transcript> dst = blockers.isEmpty() ? accepted : refused;
            dst.add(t);
            if ( ! blockers.isEmpty() ) {
                explanations.put(t.getName(), blockers);
            }
        });
        
        // generate report
        var result = new FilterResult(threshold, itemsDir, accepted, refused, explanations);
        
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        cfg.setClassForTemplateLoading(getClass(), "/");
        
        String destName = threshold.getTranscriptFile().getFileName().toString();
        destName = destName.substring(0, destName.length()-4)+"-filter.html";
        
        Path dest = threshold.getTranscriptFile().resolveSibling(destName);
        o.println("Report file: " + dest.toAbsolutePath().normalize());
        try ( Writer ow = Files.newBufferedWriter(dest) ) {
            Template tpl = cfg.getTemplate("/filter-report.html");
            Map<String,Object> data = new HashMap<>();
            data.put("result", result);
            data.put("time", LocalDateTime.now());
            data.put("modelPath", modelPath);
            data.put("reposPath", itemsDir);
            tpl.process(data, ow);
        }
        
        
    }
    
    /**
     * List all the dimensions along which <code>aTspt</code> is not enough.
     * @param aTspt
     * @return 
     */
    private List<CoordinateDifference> listBlockerDimensions( Transcript aTspt ) {
        var retVal = new ArrayList<CoordinateDifference>();
        o.println("Checking " + aTspt.getName());
        thresholdSlotNames.forEach( path -> {
            var vThreshold = ExpandedSlotValue.lookup(threshold.getCoordinate(), path, true);
            var vTranscript = ExpandedSlotValue.lookup(aTspt.getCoordinate(), path, true);
            o.println(path + "  r:" + vThreshold + " a:" + vTranscript);
            if ( vThreshold.ordinal()>vTranscript.ordinal() &&             // base case: tspt is below requirement
                 ! (vThreshold.ordinal()==0 && vTranscript.ordinal()==-1 ) // edge case: tspt is unspecified, and requirement is minimal. So any tspt value fits.
            ) {
                retVal.add(new CoordinateDifference(path, vThreshold, vTranscript));
            }
        });
        o.println("  done");
        return retVal;
    }
    
}
