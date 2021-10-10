package edu.harvard.iq.policymodelssummary.policyspace;

import edu.harvard.iq.policymodels.model.policyspace.values.AbstractValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AggregateValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodels.model.policyspace.values.ToDoValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import static java.util.stream.Collectors.joining;

/**
 *
 * @author michael
 */
public class SlotPathCollector {
    
    public List<String> collect(CompoundValue root) {
        List<String> coordinateString = new ArrayList<>();
        
        root.accept(new AbstractValue.Visitor<Void>() {
            
            LinkedList<String> stack = new LinkedList<>();
            
            @Override
            public Void visitToDoValue(ToDoValue v) {
                coordinateString.add(stackToPath() + ": TODO");
                return null;
            }

            @Override
            public Void visitAtomicValue(AtomicValue v) {
                coordinateString.add( stackToPath() + "/" + v.getSlot().getName() + ": " + v.getName() );
                return null;
            }

            @Override
            public Void visitAggregateValue(AggregateValue v) {
                coordinateString.add( stackToPath() + "/" + v.getSlot().getName() + ": " 
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
        Collections.sort(coordinateString);
        return coordinateString;
    }
    
}
