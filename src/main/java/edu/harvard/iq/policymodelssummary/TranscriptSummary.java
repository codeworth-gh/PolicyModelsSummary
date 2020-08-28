package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodels.model.decisiongraph.Answer;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.AskNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.CallNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.ConsiderNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.ContainerNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.ContinueNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.EndNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.Node;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.PartNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.RejectNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.SectionNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.SetNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.ThroughNode;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.ToDoNode;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodels.runtime.exceptions.DataTagsRuntimeException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import static java.util.stream.Collectors.toList;
import java.util.stream.StreamSupport;

/**
 * A summary object holding multiple transcripts.
 * 
 * @author michael
 */
public class TranscriptSummary {
    
    private final Map<String, Transcript> transcripts = new HashMap<>();
    private final Path transcriptDir;
    private final PolicyModel model;

    public TranscriptSummary(Path transcriptDir, PolicyModel model) {
        this.transcriptDir = transcriptDir;
        this.model = model;
    }
    
    public void add( Transcript aTranscript ) {
        transcripts.put(aTranscript.getName(), aTranscript);
    }
    
    public List<String> getSortedNames() {
        return transcripts.keySet().stream()
            .sorted().collect(toList());
    }
    
    public Set<Transcript> getTranscripts() {
        return Collections.unmodifiableSet( new HashSet<>(transcripts.values()));
    }
    
    public Transcript get(String tsptName) {
        return transcripts.get(tsptName);
    }

    public PolicyModel getModel() {
        return model;
    }
    
    /**
     * Lists the questions, in some reasonable order. Note that since some interviews
     * won't include all questions, the returned question order may (read: probably) not
     * be the actual order that appears in an interview.
     * 
     * @return Ids of the questions, in a reasonable traversal order.
     */
    public List<String> questionOrder() {
        final List<String> out = new ArrayList<>();
        final Set<String>  visited = new TreeSet<>();
        
        Node.VoidVisitor v = new Node.VoidVisitor() {
            @Override
            public void visitImpl(ConsiderNode nd) throws DataTagsRuntimeException {
                visited.add( nd.getId() );
                for ( CompoundValue ans : nd.getAnswers() ) {
                    Node next = nd.getNodeFor(ans);
                    if ( ! visited.contains(next.getId()) ) {
                        next.accept(this);
                    }
                }
                Node elseNode = nd.getElseNode();
                if ( elseNode != null && !visited.contains(elseNode.getId()) ) {
                    elseNode.accept(this);
                }
            }

            @Override
            public void visitImpl(AskNode nd) throws DataTagsRuntimeException {
                out.add( nd.getId() );
                visited.add( nd.getId() );
                for ( Answer ans : nd.getAnswers() ) {
                    Node next = nd.getNodeFor(ans);
                    if ( ! visited.contains(next.getId()) ) {
                        next.accept(this);
                    }
                }
            }

            @Override
            public void visitImpl(SetNode nd) throws DataTagsRuntimeException {
                visitThroughNode(nd);
            }

            @Override
            public void visitImpl(SectionNode nd) throws DataTagsRuntimeException {
                visitContainerNode(nd);
                visitThroughNode(nd);
            }

            @Override
            public void visitImpl(PartNode nd) throws DataTagsRuntimeException {
                visited.add( nd.getId() );
                visitContainerNode(nd);
            }

            @Override
            public void visitImpl(RejectNode nd) throws DataTagsRuntimeException {
                visited.add( nd.getId() );
            }

            @Override
            public void visitImpl(CallNode nd) throws DataTagsRuntimeException {
                visited.add( nd.getId() );
                Node callee = nd.getCalleeNode();
                if ( ! visited.contains(callee.getId()) ) {
                    callee.accept(this);
                }
                visitThroughNode(nd);
            }

            @Override
            public void visitImpl(ToDoNode nd) throws DataTagsRuntimeException {
                visitThroughNode(nd);
            }

            @Override
            public void visitImpl(EndNode nd) throws DataTagsRuntimeException {
                visited.add( nd.getId() );
            }

            @Override
            public void visitImpl(ContinueNode nd) throws DataTagsRuntimeException {
                visited.add( nd.getId() );
            }
            
            private void visitThroughNode( ThroughNode nd ) {
                visited.add( nd.getId() );
                Node next = nd.getNextNode();
                if ( ! visited.contains(next.getId()) ) {
                    next.accept(this);
                }
            }
            
            private void visitContainerNode( ContainerNode nd ) {
                Node next = nd.getStartNode();
                if ( ! visited.contains(next.getId()) ) {
                    next.accept(this);
                }
            }
        };
        
        model.getDecisionGraph().getStart().accept(v);
        StreamSupport.stream(model.getDecisionGraph().nodes().spliterator(), false)
                .filter( n -> n instanceof PartNode )
                .forEach(n -> n.accept(v));
        
        return out;
    }
}
