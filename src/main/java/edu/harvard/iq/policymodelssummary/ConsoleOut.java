package edu.harvard.iq.policymodelssummary;

import java.io.PrintStream;

/**
 *
 * @author michael
 */
public class ConsoleOut {
    
    private String stage;
    
    private final PrintStream out;
    
    boolean continuingLine = false;
    
    public ConsoleOut(PrintStream out) {
        this.out = out;
    }
    
    public ConsoleOut() {
        this(System.out);
    }

    public ConsoleOut println( String msg ) {
        out.println( "[" + stage + "] " + msg );
        continuingLine = false;
        return this;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public ConsoleOut print(String msg) {
        if ( ! continuingLine ) {
            out.print("[" + stage + "] ");
        }
        out.print(msg);
        continuingLine = !msg.endsWith("\n");
        return this;
    }
    
    
}
