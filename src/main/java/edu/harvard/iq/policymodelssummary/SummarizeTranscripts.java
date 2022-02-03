package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodelssummary.reports.CSVCoordinateReport;
import edu.harvard.iq.policymodelssummary.reports.CSVTranscriptReport;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 *
 * @author michael
 */
public class SummarizeTranscripts extends RunMode {
    
    public SummarizeTranscripts() {
        super("SUMMARY");
    }
    
    public void go( Path model, Path transcriptDir ) throws Exception {
        o.println("Loading model:");
        PolicyModel mdl = loadModel(model);
        o.println(" - Done");
        
        final TranscriptSummary smry = new TranscriptSummary(transcriptDir, mdl);
        
        TranscriptParser psr = new TranscriptParser(mdl);
        Iterator<Path> transcripts = Files.newDirectoryStream(transcriptDir).iterator();
        while ( transcripts.hasNext() ) {
            Path tspt = transcripts.next();
            o.print("Reading " + tspt.toAbsolutePath() + "...");
            if ( tspt.getFileName().toString().endsWith(".xml") ) {
                o.print("parsing..");
                smry.add(psr.parse(tspt));
                o.println("OK");
            } else {
                o.println("Ignored");
            }
        }
        
        Path smryDir = transcriptDir.resolve("summary");
        int idx = 0;
        while ( Files.exists(smryDir) ) {
            smryDir = transcriptDir.resolve("summary-" + (++idx));
        }
        
        Files.createDirectory(smryDir);
        Path smryOut = smryDir.resolve("summary-transcript.tsv");
        o.println("Writing transcript summary report to: " + smryOut.toAbsolutePath() );
        CSVTranscriptReport rp = new CSVTranscriptReport();
        try ( PrintWriter prt = new PrintWriter(Files.newBufferedWriter(smryOut)) ) {
            rp.writeReport(smry, prt);
        }
        
        smryOut = smryDir.resolve("summary-coordinate.tsv");
        o.println("Writing coordinate summary report to: " + smryOut.toAbsolutePath() );
        CSVCoordinateReport crp = new CSVCoordinateReport();
        try ( PrintWriter prt = new PrintWriter(Files.newBufferedWriter(smryOut)) ) {
            crp.writeReport(smry, prt);
        }
        
        TranscriptPrinter printer = new TranscriptPrinter();
        for ( Transcript t:smry.getTranscripts() ) {
            try ( PrintWriter prt = new PrintWriter(Files.newBufferedWriter(smryDir.resolve(t.getName() + ".txt"))) ) {
               printer.print(t, prt);
           }   
        }
        
        Path jsonSmry = smryDir.resolve("summary.json");
        o.println("Writing JSON summary to: " + jsonSmry.toAbsolutePath());
        try ( PrintWriter jsonOut = new PrintWriter(Files.newBufferedWriter(jsonSmry))) {
            JsonReportPrinter jsonPrt = new JsonReportPrinter(jsonOut);
            jsonPrt.print(mdl, smry.getTranscripts());
        }
        
    }
   
}
