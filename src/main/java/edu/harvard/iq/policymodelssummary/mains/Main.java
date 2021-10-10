package edu.harvard.iq.policymodelssummary.mains;

import edu.harvard.iq.policymodelssummary.SummarizeTranscripts;
import edu.harvard.iq.policymodelssummary.TranscriptDiffMaker;
import java.nio.file.Paths;

/**
 * Entry point class for the program.
 * 
 * @author michael
 */
public class Main {
    
    public static void main(String[] args) {
        
        if ( args.length ==  0 ) {
            System.out.println("Usage: <verb> <params> ");
            System.out.println("Where: verb is one of: summerize, diff");
            System.out.println("example1: summary <path to model> <path to transcript folder>");
            System.out.println("example2: diff <path to model> <path to transcript 1> <path to transcript 2>");
                
            System.exit(-1);
        }
        
        
        try {
            switch (args[0].toLowerCase()) {
                case "summerize": 
                    new SummarizeTranscripts().go(Paths.get(args[1]), Paths.get(args[2]));
                    break;
                case "diff":
                    String[] shiftedArgs = new String[args.length-1];
                    System.arraycopy(args, 1, shiftedArgs, 0, shiftedArgs.length);
                    new TranscriptDiffMaker(shiftedArgs).go();
                    break;
            }
        } catch ( Exception e ) {
            System.out.println("Error while performing " + args[0] + ": " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }
    
}
