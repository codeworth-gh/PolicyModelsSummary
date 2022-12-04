package edu.harvard.iq.policymodelssummary;

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import edu.harvard.iq.policymodels.externaltexts.Localization;
import edu.harvard.iq.policymodels.externaltexts.LocalizationLoader;
import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodels.model.decisiongraph.Answer;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.AskNode;
import edu.harvard.iq.policymodels.runtime.RuntimeEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import java.util.stream.IntStream;

/**
 * A run mode that reads answers from an ODS file and corrects transcripts according
 * to them. The ODS format is very specific to what Edyta did here:
 * 
 * https://docs.google.com/spreadsheets/d/1XUdEm43Cynz2kGS6mk4jpPS6WqROJ4Czr3wGK-pSyfM/edit#gid=568360544
 * 
 * @author michael
 */
public class UpdateAnswers extends RunMode {

    private static final int ANSWER_START_ROW=2;
    private static final int ANSWER_END_ROW=18;
    private static final int FIRST_REPO_COLUMN=2;
    
    private final Map<String, Map<String,String>> correctedAnswers = new HashMap<>();
    private List<String> questionIds;
    private final Map<String, String> fileName2RepoName = new HashMap<>();
    private PolicyModel model;
    private Localization loc;
    private Map<String,String> locAns2Ans;
    private Path transcriptFolder, resultFolder;
    
    public UpdateAnswers() {
        super("Update-answers");
    }
    
    public void go(Path modelPath, Path aTranscriptFolder, Path anOds ) throws Exception {
        transcriptFolder = aTranscriptFolder;
        resultFolder = anOds.resolveSibling("corrected");
        if ( ! Files.exists(resultFolder) ) {
            Files.createDirectory(resultFolder);
        }
        
        o.print("Loading model...");
        model = loadModel(modelPath);
        o.println(" - Done");
        o.print("Loading localization en_US...");
        loc = new LocalizationLoader().load( model, "en_US" );
        o.println(" - Done");
        
        if ( ! Files.exists(anOds) ) {
            o.println(anOds.toAbsolutePath() + " does not exist");
            System.exit(-2);
        }
        if ( ! Files.exists(aTranscriptFolder) ) {
            o.println(aTranscriptFolder.toAbsolutePath() + " does not exist");
            System.exit(-3);
        }
        
        locAns2Ans = loc.getLocalizedAnswers().stream().collect( Collectors.toMap(k->loc.localizeAnswer(k), k->k));
        
        o.println("\n\n READING correct answers");
        readCorrectedAnswers(anOds);

        o.println("\n\n RE-RUNNING questionnaires");
        var failedConversions = reanswerQuestionnaires();
        
        if ( failedConversions.isEmpty() ) {
            o.println("All files converted successfully");
        } else {
            o.println("The following files were not converted:");
            failedConversions.stream()
                .sorted()
                .map(m->"  " + m)
                .forEach(o::println);
        }
        
    }
    
    private Set<String> reanswerQuestionnaires() throws Exception {
        TranscriptParser xsptParser = new TranscriptParser(model);
        Set<String> unconversibles = new HashSet<>();
        TranscriptConverter xmlWriter = new TranscriptConverter(null,null,null);
        
        for ( String xscriptName : fileName2RepoName.keySet() ) {
            
            o.println("Re-answering " + xscriptName);
            // Re-Answer the questionnaire (if possible)
            Map<String,String> answers = correctedAnswers.get(xscriptName);
            RuntimeEngine ngn = new RuntimeEngine();
            ngn.setModel(model);
            if ( ! ngn.start() ) {
                throw new Exception("PM Engine did not start properly");
            }
            boolean isGo = true;
            boolean updateFail = false;
                
            Transcript updatedXspt = new Transcript();
            Transcript originalXspt = xsptParser.parse(transcriptFolder.resolve(xscriptName));
            Map<String,String> originalNotes = originalXspt.getAnswerList().stream()
                .filter( ans -> ans.note != null && ans.note.isPresent() && !ans.note.get().isBlank() )
                .collect( toMap(a->a.questionId, a->a.note.get()) );
                // re-build transcript
                updatedXspt.setHrName(originalXspt.getHrName());
                updatedXspt.setName(originalXspt.getName());
                updatedXspt.setModelData(originalXspt.getModelData());
            
            while ( isGo && ! updateFail ) {
                String qId = ngn.getCurrentNode().getId();
                String newAnswer = answers.get(qId);
                if ( newAnswer!=null ) {
                    final Transcript.SingleQandA sqa = new Transcript.SingleQandA();
                    sqa.questionId = qId;
                    final AskNode curAskNode = (AskNode)ngn.getCurrentNode();
                    sqa.questionText = curAskNode.getText();
                    sqa.answerText = newAnswer;
                    sqa.note = Optional.ofNullable(originalNotes.get(qId));
                    sqa.answerOrdinal = curAskNode.getAnswers().indexOf(Answer.withName(sqa.answerText));
                    sqa.normalizedAnswer = ((double)sqa.answerOrdinal)/curAskNode.getAnswers().size();
                    isGo = ngn.consume(Answer.withName(newAnswer));
                    updatedXspt.append(sqa);
                } else {
                    final String msg = xscriptName + ": Conversion failed, as there's no answer question: " + qId;
                    o.println("\n!! " + msg);
                    unconversibles.add(msg);
                    updateFail = true;
                }
            }
            
            if ( ! updateFail ) {    
                // write XML file.
                updatedXspt.setCoordinate(ngn.getCurrentValue());
                Path outPath = resultFolder.resolve(xscriptName);
                xmlWriter.writeTranscript(updatedXspt, outPath);
                o.println(xscriptName + " done");
            }
        }
        return unconversibles;
    }
    
    
    private void readCorrectedAnswers(Path anOds) throws IOException {
        SpreadSheet ods = new SpreadSheet(anOds.toFile());
        Sheet qc = ods.getSheet("answer-summary.ods");
        Range dr = qc.getDataRange();
        
        // fill the filename to reponame map
        IntStream.range(0, dr.getNumColumns()).forEach( col -> {
            String xmlName = Optional.ofNullable(dr.getCell(1, col).getValue()).map(v->v.toString()).orElse("");
            if ( ! xmlName.isBlank() ) {
                String repoName = dr.getCell(0,col).getValue().toString();
                fileName2RepoName.put( xmlName, repoName );
                o.println(xmlName + "\t ->\t" + repoName );
            }
        });
        
        // read the question ids into a list
        questionIds = IntStream.range(ANSWER_START_ROW, ANSWER_END_ROW+1)
            .mapToObj(rowNum -> dr.getCell(rowNum,0))
            .filter( v -> v != null )
            .map(    v -> v.getValue() )
            .filter( v -> v != null )  // being super-cautious here
            .map(    v -> v.toString() )
            .map(    v -> v.split(" ", 2)[0] ) // remove remarks to the id, added by Airtable.
            .collect( toList() );
        
        o.println("");
        o.println("Question Ids:");
        questionIds.forEach( i -> o.println(" - " + i));
        
        var atLeastOneLetter = Pattern.compile(".*[a-zA-Z].*");
        
        // Now do the answer maps.
        for ( int col=FIRST_REPO_COLUMN; col < fileName2RepoName.size()+FIRST_REPO_COLUMN; col++ ) {
            Map<String,String> correctedRepoAnswers = new HashMap<>();
            final Object repoNameValue = dr.getCell(1, col).getValue();
            if ( repoNameValue != null ) {
                final String repoName = dr.getCell(1, col).getValue().toString();
                for ( int row=ANSWER_START_ROW; row<=ANSWER_END_ROW; row++ ){
                    var valueOpt = getValue( dr, row, col);
                    if ( valueOpt.isPresent() ) {
                        String value = valueOpt.get().trim();
                        if ( ! value.equals("-") ) { // "-" is used to mark blank answers
                            String correctedAnswer = valueOpt.get();
                            // Ignore no-text answers (the weird date-times)
                            if ( atLeastOneLetter.matcher(correctedAnswer).matches() ) {
                                // Convert from the text in localization to the answer in code
                                correctedAnswer = locAns2Ans.getOrDefault(correctedAnswer, correctedAnswer);
                                correctedRepoAnswers.put(questionIds.get(row-ANSWER_START_ROW), correctedAnswer);
                            }
                        }
                    }
                }
                correctedAnswers.put(repoName, correctedRepoAnswers);
                o.println("");
                o.println(repoName + ":");
                questionIds.stream()
                    .filter( qid -> correctedRepoAnswers.containsKey(qid) )
                    .forEach( qid -> o.println(" " + (qid + "                    ").substring(0,40) + correctedRepoAnswers.get(qid)))
                    ;
            } // reponame != null
        } // for repo columns
        
    }
    
   private Optional<String> getValue(Range r, int row, int col) {
       var cell = r.getCell(row,col);
       if ( cell == null ) return Optional.empty();
       var val = cell.getValue();
       return (val!=null) ? Optional.of( val.toString() ) : Optional.empty();
   }
}
