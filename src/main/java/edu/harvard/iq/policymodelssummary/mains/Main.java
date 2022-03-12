package edu.harvard.iq.policymodelssummary.mains;

import edu.harvard.iq.policymodelssummary.AnswerSummerizer;
import edu.harvard.iq.policymodelssummary.FilterTranscripts;
import edu.harvard.iq.policymodelssummary.SummarizeTranscripts;
import edu.harvard.iq.policymodelssummary.TranscriptConverter;
import edu.harvard.iq.policymodelssummary.TranscriptDiffMaker;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Entry point class for the program.
 * 
 * @author michael
 */
public class Main {
    
    public static void main(String[] args) {
        
        if ( args.length ==  0 ) {
            System.out.println("Usage: <verb> <params> ");
            System.out.println("Where: verb is one of: summerize, diff, filter");
            System.out.println("example: summarize <path to model> <path to transcript folder>");
            System.out.println("example: summarize-answers <path to model> <path to transcript folder> [<path to human readable name CSV>]");
            System.out.println("example: diff <path to model> <path to transcript 1> <path to transcript 2>");
            System.out.println("example: filter <path to model> <path to transcript> <path to items directory>");
            System.out.println("example: convert <path to src model> <path to destination model> <path to items directory>");
                
            System.exit(-1);
        }
        
        
        try {
            switch (args[0].toLowerCase()) {
                case "summarize" -> new SummarizeTranscripts().go(Paths.get(args[1]), Paths.get(args[2]));
                
                case "summarize-answers" -> new AnswerSummerizer().go(Paths.get(args[1]), Paths.get(args[2]),
                    Optional.ofNullable(args.length>=4 ? Paths.get(args[3]) : null));
                    
                case "diff" -> {
                    String[] shiftedArgs = new String[3];
                    for ( int i=1; i<args.length; i++ ){
                        shiftedArgs[i-1] = args[i];
                    }
                    
                    new TranscriptDiffMaker(shiftedArgs).go();
                }
                    
                case "filter" -> {
                    var ft = new FilterTranscripts(Paths.get(args[1]),Paths.get(args[2]), Paths.get(args[3]));
                    ft.go();
                }
                    
                case "convert" -> new TranscriptConverter(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3])).go();
                    
                default -> {
                    System.out.println("Unknown verb '" + args[0] + "'.");
                    System.exit(-2);
                }
            }
            System.exit(0);
            
        } catch ( Exception e ) {
            System.out.println("Error while performing " + args[0] + ": " + e.getMessage());
            e.printStackTrace(System.out);
            System.exit(-3);
        }
    }
    
}
