package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * A summary object of all transcripts.
 * @author michael
 */
public class TranscriptSummary {
    
    private final Set<Transcript> transcripts = new HashSet<>();
    private final Path transcriptDir;
    private final PolicyModel model;

    public TranscriptSummary(Path transcriptDir, PolicyModel model) {
        this.transcriptDir = transcriptDir;
        this.model = model;
    }
    
    public void add( Transcript aTranscript ) {
        transcripts.add(aTranscript);
    }
    
    public void dump() {
        System.out.println( "Transcript:" );
        System.out.println( "Model: " + model.getMetadata().getTitle() );
        System.out.println( "Dir: " + transcriptDir );
        transcripts.forEach( Transcript::dump );
    }
    
}
