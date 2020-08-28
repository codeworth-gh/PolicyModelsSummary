package edu.harvard.iq.policymodelssummary.mains;

import edu.harvard.iq.policymodels.io.PolicyModelDataParser;
import edu.harvard.iq.policymodels.io.PolicyModelLoadingException;
import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodels.model.metadata.PolicyModelData;
import edu.harvard.iq.policymodels.parser.PolicyModelLoadResult;
import edu.harvard.iq.policymodels.parser.PolicyModelLoader;
import edu.harvard.iq.policymodelssummary.JsonReportPrinter;
import edu.harvard.iq.policymodelssummary.Transcript;
import edu.harvard.iq.policymodelssummary.TranscriptParser;
import edu.harvard.iq.policymodelssummary.TranscriptPrinter;
import edu.harvard.iq.policymodelssummary.TranscriptSummary;
import edu.harvard.iq.policymodelssummary.reports.CSVCoordinateReport;
import edu.harvard.iq.policymodelssummary.reports.CSVTranscriptReport;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 *
 * @author michael
 */
public class SummarizeTranscripts {
    
    public static void main(String[] args) throws Exception {
        
        if ( args.length ==  0 ) {
            System.out.println("Usage: summery <path to model> <path to transcript folder>");
            System.exit(-1);
        }
        
        new SummarizeTranscripts().summarize(Paths.get(args[0]), Paths.get(args[1]));
    }
    
    public void summarize( Path model, Path transcriptDir ) throws Exception {
        
        System.out.println("Loading mode:");
        PolicyModel mdl = loadModel(model);
        System.out.println(" - Done");
        
        final TranscriptSummary smry = new TranscriptSummary(transcriptDir, mdl);
        
        TranscriptParser psr = new TranscriptParser(mdl);
        Iterator<Path> transcripts = Files.newDirectoryStream(transcriptDir).iterator();
        while ( transcripts.hasNext() ) {
            Path tspt = transcripts.next();
            System.out.print("Reading " + tspt.toAbsolutePath() + "...");
            if ( tspt.getFileName().toString().endsWith(".xml") ) {
                System.out.print("parsing..");
                smry.add(psr.parse(tspt));
                System.out.println("OK");
            } else {
                System.out.println("Ignored");
            }
        }
        
        Path smryDir = transcriptDir.resolve("summary");
        int idx = 0;
        while ( Files.exists(smryDir) ) {
            smryDir = transcriptDir.resolve("summary-" + (++idx));
        }
        
        Files.createDirectory(smryDir);
        Path smryOut = smryDir.resolve("summary-transcript.tsv");
        System.out.println("Writing transcript summary report to: " + smryOut.toAbsolutePath() );
        CSVTranscriptReport rp = new CSVTranscriptReport();
        try ( PrintWriter prt = new PrintWriter(Files.newBufferedWriter(smryOut)) ) {
            rp.writeReport(smry, prt);
        }
        
        smryOut = smryDir.resolve("summary-coordinate.tsv");
        System.out.println("Writing coordinate summary report to: " + smryOut.toAbsolutePath() );
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
        System.out.println("Writing JSON summary to: " + jsonSmry.toAbsolutePath());
        try ( PrintWriter jsonOut = new PrintWriter(Files.newBufferedWriter(jsonSmry))) {
            JsonReportPrinter jsonPrt = new JsonReportPrinter(jsonOut);
            jsonPrt.print(mdl, smry.getTranscripts());
        }
        
    }
    
    private PolicyModel loadModel( Path mpdelPath ) throws PolicyModelLoadingException {
        PolicyModelDataParser pmdParser = new PolicyModelDataParser();
        final PolicyModelData modelData = pmdParser.read(mpdelPath);

        if ( modelData == null ) {
            System.out.println("Model loading failed.");
            return null;
        }

        PolicyModelLoadResult loadRes = PolicyModelLoader.verboseLoader().load(modelData);

        if ( loadRes.isSuccessful() ) {
            System.out.printf("Model '%s' loaded\n", loadRes.getModel().getMetadata().getTitle());
        } else {
            System.out.println("Failed to load model");
        }
        return loadRes.getModel();
    }
}
