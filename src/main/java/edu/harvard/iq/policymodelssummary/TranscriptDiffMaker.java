package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodelssummary.DiffResult.AnswerDiff;
import edu.harvard.iq.policymodelssummary.DiffResult.CoordinateDiff;
import edu.harvard.iq.policymodelssummary.DiffResult.Message;
import static edu.harvard.iq.policymodelssummary.DiffResult.Subject.*;
import edu.harvard.iq.policymodelssummary.Transcript.SingleQandA;
import edu.harvard.iq.policymodelssummary.policyspace.ExpandedSlotValue;
import edu.harvard.iq.policymodelssummary.policyspace.SlotPathCollector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * Creates a diff report of two interview transcripts.
 * 
 * @author michael
 */
public class TranscriptDiffMaker extends RunMode {
   
    final Path[] files = new Path[2];
    final Path modelPath;

    public TranscriptDiffMaker( String[] args ) {
        super("DIFF");
        if ( args.length != 3 ) {
            System.err.println("diff expects arguments: <path to model> <path to XML transcript 1> <path to XML transcript 2>");
            System.exit(-1);
        }
        
        for ( int i=1; i<3; i++ ) {
            files[i] = Paths.get(args[i]).toAbsolutePath().normalize();
            if ( ! Files.exists(files[i]) ) {
                System.err.printf("Input file %s does not exist\n", files[i].toString());
                System.exit(-2);
            }
            System.out.printf("File %d: %s", i, files[i]);
        }
        
        if ( files[0].equals(files[1]) ) {
            System.err.println("Comparing identical files");
            System.exit(-3);
        }
        
        modelPath = Paths.get(args[0]).toAbsolutePath().normalize();
        if ( ! Files.exists(modelPath) ) {
            System.err.println("Model file does not exist at: " + modelPath );
            System.exit(-4);
        }
    }
    
    public void go() throws Exception {
        DiffResult diff = makeDiff();
        // now write the HTML report.
    }
    
    public DiffResult makeDiff() throws Exception {
        // load data
        o.println("Loading model " + modelPath );
        PolicyModel model = loadModel(modelPath);
        if ( model == null ) {
            o.println("Model loading failed. Stop.");
            System.exit(-5);
        }
        
        List<Transcript> transcripts = new ArrayList<>(2);
        var loader = new TranscriptParser( model );
        o.println("Loading transcript " + files[0]);
        transcripts.add(loader.parse(files[0]));
        o.println("Loading transcript " + files[1]);
        transcripts.add(loader.parse(files[1]));
        
        
        o.println("Diffing...");
        DiffResult res = new DiffResult(model, transcripts);
        Transcript a = res.getTranscript(A);
        Transcript b = res.getTranscript(B);
        
        // metadata compare
        if ( a.getModelData().equals(b.getModelData()) ) {
            res.add(new Message(Both, "Metadata is identical"));
        } else {
            if ( ! a.getModelData().id().equals(b.getModelData().id()) ) {
                res.add(new Message(A, "ID: " + a.getModelData().id()));
                res.add(new Message(B, "ID: " + b.getModelData().id()));
            }
            if ( a.getModelData().version() != b.getModelData().version() ) {
                res.add(new Message(A, "Version: " + a.getModelData().version()));
                res.add(new Message(B, "Version: " + b.getModelData().version()));
            }
            if ( ! a.getModelData().localization().equals(b.getModelData().localization()) ) {
                res.add(new Message(A, "Localization: " + a.getModelData().localization()));
                res.add(new Message(B, "Localization: " + b.getModelData().localization()));
            }
            if ( ! a.getModelData().time().equals(b.getModelData().time()) ) {
                res.add(new Message(A, "Time: " + a.getModelData().time()));
                res.add(new Message(B, "Time: " + b.getModelData().time()));
            }
        }
        
        // coordinate compare
        if ( ! a.getCoordinate().equals(b.getCoordinate()) ) {
            var spc = new SlotPathCollector();
            var slotsA = spc.collect(a.getCoordinate());
            var slotsB = spc.collect(b.getCoordinate());
            
            if ( ! slotsA.equals(slotsB) ) {
                var amb = new TreeSet<>(slotsA);
                amb.removeAll(slotsB);
                if ( ! amb.isEmpty() ) {
                    var ambl = new ArrayList<>(amb);
                    Collections.sort(ambl);
                    res.add(new Message(A, "Slots in A and not in B: " + ambl.stream().collect(joining(", "))));
                }
                var bma = new TreeSet<>(slotsB);
                bma.removeAll(slotsA);
                if ( ! bma.isEmpty() ) {
                    var bmal = new ArrayList<>(bma);
                    Collections.sort(bmal);
                    res.add(new Message(B, "Slots in B and not in A: " + bmal.stream().collect(joining(", "))));
                }
            }
            
            slotsA.retainAll(slotsB);
            slotsA.forEach( path -> {
                var va = ExpandedSlotValue.lookup(a.getCoordinate(), path);
                var vb = ExpandedSlotValue.lookup(b.getCoordinate(), path);
                if ( ! va.equals(vb) ) {
                    res.add( new CoordinateDiff(path, va, vb) );
                }
            });
        }
        
        // answer compare per A
        Map<String, SingleQandA> bAnswerIndex = b.getAnswerList().stream().collect( Collectors.toConcurrentMap(SingleQandA::getQuestionId, Function.identity()));
        a.getAnswerList().forEach( qnaA -> {
            var qnaB = bAnswerIndex.get(qnaA.getQuestionId());
            if ( ! qnaA.equals(qnaB) ){
                if ( qnaB == null ) {
                    res.add( new AnswerDiff(qnaA.questionId, qnaA, null));
                } else {
                    if ( qnaA.answerOrdinal != qnaB.answerOrdinal ) {
                        // Aha! different answer!
                        res.add( new AnswerDiff(qnaA.questionId, qnaA, qnaB));
                    }
                }
            }
        });
        
        // answers that were in B but not in A
        Set<String> aQuestionIds = a.getAnswerList().stream().map(SingleQandA::getQuestionId).collect( toSet() );
        b.getAnswerList().stream()
            .filter( qna -> ! aQuestionIds.contains(qna.questionId) )
            .forEach( qna -> {
                     res.add( new AnswerDiff(qna.questionId, null, qna) );
            });
        
        return res;
    }
}
