package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodelssummary.DiffResult.AnswerDiff;
import edu.harvard.iq.policymodelssummary.DiffResult.CoordinateDiff;
import edu.harvard.iq.policymodelssummary.DiffResult.Message;
import static edu.harvard.iq.policymodelssummary.DiffResult.Subject.*;
import edu.harvard.iq.policymodelssummary.Transcript.SingleQandA;
import edu.harvard.iq.policymodelssummary.policyspace.ExpandedSlotValue;
import edu.harvard.iq.policymodelssummary.policyspace.SlotPathCollector;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
        
        for ( int i=0; i<2; i++ ) {
            files[i] = Paths.get(args[i+1]).toAbsolutePath().normalize();
            if ( ! Files.exists(files[i]) ) {
                System.err.printf("Input file %s does not exist\n", files[i].toString());
                System.exit(-2);
            }
            o.println(String.format("File %d: %s", i, files[i]));
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
        
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        cfg.setClassForTemplateLoading(getClass(), "/");
        
        String diffName = String.format("diff-%s-%s.html", fileNameNoSuffix(files[0]), fileNameNoSuffix(files[1]) );
        Path diffDest = files[1].resolveSibling(diffName);
        o.println("Diff file: " + diffDest);
        try ( Writer ow = Files.newBufferedWriter(diffDest) ) {
            Template tpl = cfg.getTemplate("/diff-report.html");
            Map<String,Object> data = new HashMap<>();
            data.put("diff", diff);
            data.put("time", LocalDateTime.now());
            data.put("fileA", files[0]);
            data.put("fileB", files[1]);
            data.put("fileNameA", fileNameNoSuffix(files[0]));
            data.put("fileNameB", fileNameNoSuffix(files[1]));
            data.put("modelFile", modelPath);
            System.out.println("modelPath = " + modelPath);
            tpl.process(data, ow);
        }
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
        if ( ! a.getModelData().equals(b.getModelData()) ) {
            if ( ! a.getModelData().id().equals(b.getModelData().id()) ) {
                res.add(new Message("ID", a.getModelData().id(), b.getModelData().id()));
            }
            if ( a.getModelData().version() != b.getModelData().version() ) {
                res.add(new Message("Version", 
                    Integer.toString(a.getModelData().version()), 
                    Integer.toString(b.getModelData().version())));
            }
            if ( ! a.getModelData().localization().equals(b.getModelData().localization()) ) {
                res.add(new Message("Localization", a.getModelData().localization(), b.getModelData().localization()));
            }
            if ( ! a.getModelData().time().equals(b.getModelData().time()) ) {
                var fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                res.add(new Message("Time",
                    a.getModelData().time().format(fmt),
                    b.getModelData().time().format(fmt)));
            }
        }
        
        // coordinate compare
        if ( ! a.getCoordinate().equals(b.getCoordinate()) ) {
            System.out.println("Coordinates are not the same");
            var spc = new SlotPathCollector();
            var slotsA = spc.collect(a.getCoordinate());
            var slotsB = spc.collect(b.getCoordinate());
            
            if ( ! slotsA.equals(slotsB) ) {
                System.out.println("Slots are not the same");
                var amb = new TreeSet<>(slotsA);
                amb.removeAll(slotsB);
                if ( ! amb.isEmpty() ) {
                    var ambl = new ArrayList<>(amb);
                    Collections.sort(ambl);
                    res.add(new Message("Slots in A and not in B", ambl.stream().collect(joining(", ")), ""));
                }
                var bma = new TreeSet<>(slotsB);
                bma.removeAll(slotsA);
                if ( ! bma.isEmpty() ) {
                    var bmal = new ArrayList<>(bma);
                    Collections.sort(bmal);
                    res.add(new Message("Slots in B and not in A", "", bmal.stream().collect(joining(", "))));
                }
            }
            
            slotsA.retainAll(slotsB);
            slotsA.forEach( path -> {
                List<String> pathList = Arrays.asList(path.split("/"));
                pathList = pathList.subList(1, pathList.size());
                var va = ExpandedSlotValue.lookup(a.getCoordinate(), pathList);
                var vb = ExpandedSlotValue.lookup(b.getCoordinate(), pathList);
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
    
    String fileNameNoSuffix(Path p ) {
        String r = p.getFileName().toString();
        if ( r.toLowerCase().endsWith(".xml") ){
            r = r.substring(0,r.length()-4);
        }
        return r;
    }
}
