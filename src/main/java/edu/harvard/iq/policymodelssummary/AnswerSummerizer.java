package edu.harvard.iq.policymodelssummary;

import com.github.jferard.fastods.AnonymousOdsFileWriter;
import com.github.jferard.fastods.OdsDocument;
import com.github.jferard.fastods.OdsFactory;
import com.github.jferard.fastods.Table;
import com.github.jferard.fastods.TableRowImpl;
import com.opencsv.CSVReader;
import edu.harvard.iq.policymodels.externaltexts.Localization;
import edu.harvard.iq.policymodels.externaltexts.LocalizationLoader;
import edu.harvard.iq.policymodels.externaltexts.TrivialLocalization;
import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.AskNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.Node;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;

/**
 * A run mode for summarizing answers for many interviews over the same questionnaire.
 * 
 * 
 * 
 * @author michael
 */
public class AnswerSummerizer extends RunMode {
    
    private final Map<String, String> filenameToHRName = new HashMap<>();
    
    public AnswerSummerizer() {
        super("Answer-Sum");
    }
    
    /**
     * Makes this run mode go.
     * @param model The PolicyModels model
     * @param transcriptDir directory holding the XML transcripts
     * @param fn2hrPath An optional CSV file translating the transcript filename to human readable name.
     * @throws Exception 
     */
    public void go( Path model, Path transcriptDir, Optional<Path> fn2hrPath) throws Exception {
        o.println("Loading model:");
        PolicyModel mdl = loadModel(model);
        o.println(" - Done");
        
        // See if we have localizations to load
        Localization loc;
        if ( mdl.getLocalizations().isEmpty() ) {
            loc = new TrivialLocalization(mdl);
        } else {
            String locName = mdl.getLocalizations().iterator().next();
            o.println("Defaultling to localization " + locName );
            var locLdr = new LocalizationLoader();
            loc = locLdr.load(mdl, locName);
        }
            
        
        final TranscriptSummary smry = new TranscriptSummary(transcriptDir, mdl);
        
        TranscriptParser psr = new TranscriptParser(mdl);
        for (Path tspt : Files.newDirectoryStream(transcriptDir)) {
            if ( tspt.getFileName().toString().endsWith(".xml") ) {
                o.print("Reading " + tspt.toAbsolutePath() + "...");
                smry.add(psr.parse(tspt));
                o.println("OK");
            } 
        }
        
        // handle human readable names
        fn2hrPath.ifPresent(this::loadHrNames);
        if ( fn2hrPath.isPresent() ) {
            smry.getTranscripts().forEach( tsp -> {
                String hrName = filenameToHRName.get(tsp.getName());
                if ( hrName != null ) {
                    tsp.setHrName(hrName);
                    o.println(" - " + tsp.getName() + " -> " + tsp.getHrName() );
                } else {
                    o.println(" - No HR name for " + tsp.getName() );
                }
            });
        }
        
        // Sort the transcripts by name
        List<Transcript> transcripts = smry.getTranscripts().stream()
                .sorted((t1, t2)->t1.getHrName().compareTo(t2.getHrName()))
                .collect( toList() );
        
        // Now start writing the output .ods file
        final OdsFactory odsFactory = OdsFactory.create(Logger.getLogger("answers"), Locale.US);
        final AnonymousOdsFileWriter writer = odsFactory.createWriter();
        final OdsDocument document = writer.document();
        final Table table = document.addTable("answers-by-repo");
        
        // header line
        TableRowImpl row1 = table.getRow(0);
        TableRowImpl row2 = table.getRow(1);
        row1.getOrCreateCell(0).setStringValue("question id");
        row1.getOrCreateCell(1).setStringValue("question text");
        int colIdx = 2;
        for ( Transcript t : transcripts ) {
            row1.getOrCreateCell(colIdx).setStringValue(t.getHrName());
            if ( fn2hrPath.isPresent() ) {
                // possible filename line
                row2.getOrCreateCell(colIdx).setStringValue(t.getName());
            }
            colIdx++;
        }
        
        // iterate over the questions
        int rowIdx = fn2hrPath.isPresent() ? 2 : 1;
        for ( String questionKey : smry.questionOrder() ) {
            o.println("Question " + questionKey);
            TableRowImpl row = table.getRow(rowIdx++);
            row.getOrCreateCell(0).setStringValue(questionKey);
            row.getOrCreateCell(1).setStringValue(
                loc.getNodeText(questionKey).orElse(getNodeText(questionKey, mdl))
            );
            colIdx = 2;
            for ( Transcript t : transcripts ) {
                final Transcript.SingleQandA qna = t.get(questionKey);
                String value = (qna==null) ? "-" : loc.localizeAnswer(qna.answerText);                
                row.getOrCreateCell(colIdx).setStringValue(value);
                colIdx++;
            }
        }
        
        // Generate the file
        o.println("Creating ODS file");
        Path outFile = transcriptDir.resolve("answer-summary.ods");
        if ( Files.exists(outFile) ) Files.delete(outFile);
        
        writer.saveAs(outFile.toFile());
        o.println("DONE");
    }
    
    private String getNodeText( String nodeId, PolicyModel mdl ) {
        Node nd = mdl.getDecisionGraph().getNode(nodeId);
        if ( nd == null ) return "[not found]";
        if ( nd instanceof AskNode askNode ) {
            return askNode.getText();
        } else {
            return nd.toString();
        }
    }
    
    private void loadHrNames(Path p) {
        o.println("Reading filename  -> human readable name file:");
        o.println(p.toAbsolutePath().toString());
        try(Reader rdr = Files.newBufferedReader(p)) {
            var csvReader = new CSVReader(rdr);

            String[] record;
            int counter=0;
            while ( (record = csvReader.readNext()) != null ) {
                counter++;
                final String filename = record[0];
                final String repoName = record[1];
                filenameToHRName.put(filename, repoName);
                o.println(filename + " -> " + repoName );
            }
            o.println("Read " + counter + " rows.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
