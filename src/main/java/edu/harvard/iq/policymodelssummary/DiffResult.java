package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodelssummary.Transcript.SingleQandA;
import edu.harvard.iq.policymodelssummary.policyspace.ExpandedSlotValue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author michael
 */
public class DiffResult {
    
    public enum Subject {
        A, B, Both
    }
    
    public record Message(Subject subject, String message ){};
    public record CoordinateDiff( String path, ExpandedSlotValue<?> valueA, ExpandedSlotValue<?> valueB ){};
    public record AnswerDiff( String id, SingleQandA qnaA, SingleQandA qnaB ){};
    
    private final PolicyModel model;
    private final List<Transcript> transcripts = new ArrayList<>(2);
    private final List<Message> messages = new ArrayList<>();
    private final List<CoordinateDiff> coordinateDiffs = new ArrayList<>();
    private final List<AnswerDiff> answerDiffs = new ArrayList<>();

    public DiffResult(PolicyModel model, Iterable<Transcript> someTranscripts) {
        this.model = model;
        final Iterator<Transcript> iterator = someTranscripts.iterator();
        transcripts.add(iterator.next());
        transcripts.add(iterator.next());
    }
    
    public void add(Message m) {
        messages.add(m);
    }
    
    public void add(CoordinateDiff m) {
        coordinateDiffs.add(m);
    }

    public void add(AnswerDiff m) {
        answerDiffs.add(m);
    }

    public List<Message> getMessages() {
        return messages;
    }

    public PolicyModel getModel() {
        return model;
    }

    public List<Transcript> getTranscripts() {
        return transcripts;
    }

    public List<CoordinateDiff> getCoordinateDiffs() {
        return coordinateDiffs;
    }

    public List<AnswerDiff> getAnswerDiffs() {
        return answerDiffs;
    }
    
    public Transcript getTranscript(Subject s){
        switch (s) {
            case A -> { return transcripts.get(0); }
            case B -> { return transcripts.get(1); }
            default -> throw new IllegalArgumentException("Transcript is either A of B");
        }
    }
    
    
}
