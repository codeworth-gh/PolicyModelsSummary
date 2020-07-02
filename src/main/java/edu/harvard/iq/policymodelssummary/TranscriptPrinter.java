package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.policyspace.values.AbstractValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AggregateValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodels.model.policyspace.values.ToDoValue;
import edu.harvard.iq.policymodelssummary.Transcript.SingleQandA;
import java.io.PrintWriter;
import java.util.LinkedList;
import static java.util.stream.Collectors.joining;

/**
 * Takes a transcript, outputs to a text file.
 * 
 * @author michael
 */
public class TranscriptPrinter {
    
    public void print( Transcript tspt, PrintWriter prt ){
        prt.println("Transcript: " + tspt.getName() );
        prt.println();
        
        prt.println("Questions and Answers:");
        int i=0;
        for ( SingleQandA qna: tspt.getAnswerList() ) {
            prt.printf("%d. %s:\n", ++i, qna.questionId );
            prt.println( qna.questionText );
            prt.println();
            prt.printf("Answer: %s (%.4f)\n", qna.getAnswerText(), qna.getNormalizedAnswer() );
            qna.getNote().ifPresent( note -> {
                prt.println("Note: ");
                prt.println(note);
            });
            prt.println();
        }   
        
        prt.println();
        prt.println("Results:");
        tspt.getCoordinate().accept(new AbstractValue.Visitor<Void>() {
            
            LinkedList<String> stack = new LinkedList<>();
            
            @Override
            public Void visitToDoValue(ToDoValue v) {
                prt.println( stackToPath() + ": TODO");
                return null;
            }

            @Override
            public Void visitAtomicValue(AtomicValue v) {
                prt.println( stackToPath() + "/" + v.getSlot().getName() + ": " + v.getName() );
                return null;
            }

            @Override
            public Void visitAggregateValue(AggregateValue v) {
                prt.println( stackToPath() + "/" + v.getSlot().getName() + ": " 
                                + v.getValues().stream().map( w->w.getName()).sorted().collect(joining(",")));
                return null;
            }

            @Override
            public Void visitCompoundValue(CompoundValue v) {
                stack.push( v.getSlot().getName() );
                v.getNonEmptySubSlots().stream().sorted((a,b)->a.getName().compareTo(b.getName()))
                    .forEach(slt -> v.get(slt).accept(this));
                stack.pop();
                return null;
            }
            
            private String stackToPath() {
                return stack.stream().collect(joining("/"));
            }
        });
        
    }   
    
}
