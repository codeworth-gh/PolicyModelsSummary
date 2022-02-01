package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.io.StringMapFormat;
import edu.harvard.iq.policymodels.model.decisiongraph.Answer;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodels.runtime.RuntimeEngine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 *
 * @author michael
 */
public class TranscriptConverter extends RunMode {
    
    private final Path srcModelPath;
    private final Path dstModelPath;
    private final Path transcriptsPath;
    private TranscriptParser tspParser;
    private RuntimeEngine modelRunner;
    
    public TranscriptConverter(Path srcModelPath, Path dstModelPath, Path transcriptsPath) {
        super("CONVERT");
        this.srcModelPath = srcModelPath;
        this.dstModelPath = dstModelPath;
        this.transcriptsPath = transcriptsPath;
    }
    
    public void go() throws Exception {
        o.println(String.format("src model\t%s", srcModelPath.toAbsolutePath()));
        o.println(String.format("dst model\t%s", dstModelPath.toAbsolutePath()));
        o.println(String.format("transcripts\t%s", transcriptsPath.toAbsolutePath()));
        
        // load models
        var srcModel = loadModel(srcModelPath);
        tspParser = new TranscriptParser(srcModel);
        var dstModel = loadModel(dstModelPath);
        modelRunner = new RuntimeEngine();
        modelRunner.setModel(dstModel);
        o.println("Parser and Runtime OK");
        
        // process tspts
        Path destDir = transcriptsPath.resolveSibling(transcriptsPath.getFileName() + "-cnv");
        o.println(String.format("Destination dir:\t%s", destDir.toAbsolutePath()));
        Files.createDirectory(destDir);
        Files.list(transcriptsPath)
            .filter( p -> Files.isRegularFile(p) ).filter( p -> p.getFileName().toString().toLowerCase().endsWith(".xml") )
            .forEach( p -> convertTranscript(p) );
    }
    
    private void convertTranscript( Path tsptPath ) throws Exception {
        o.println("Converting " + tsptPath.getFileName() );
        Transcript tspt = tspParser.parse(tsptPath);
        modelRunner.restart();
        List<Answer> answers = tspt.getAnswerList().stream().map( qa -> Answer.withName(qa.getAnswerText())).toList();
        modelRunner.consumeAll(answers);
        CompoundValue result = modelRunner.getCurrentValue();
        var smf = new StringMapFormat();
        smf.format(result).entrySet().forEach((e) -> {
            o.println(String.format(" - %s = %s", e.getKey(), e.getValue()));
        });
        o.println("done");
    }
}
