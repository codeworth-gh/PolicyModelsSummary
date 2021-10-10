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
        List<String> dimensionNames = new ArrayList<>();
        
        root.accept(new AbstractValue.Visitor<Void>() {
            
            LinkedList<String> stack = new LinkedList<>();
            
            @Override
            public Void visitToDoValue(ToDoValue v) {
                dimensionNames.add(stackToPath());
                return null;
            }

            @Override
            public Void visitAtomicValue(AtomicValue v) {
                dimensionNames.add( stackToPath() + "/" + v.getSlot().getName());
                return null;
            }

            @Override
            public Void visitAggregateValue(AggregateValue v) {
                dimensionNames.add( stackToPath() + "/" + v.getSlot().getName() );
                return null;
            }

            @Override
            public Void visitCompoundValue(CompoundValue v) {
                stack.add( v.getSlot().getName() );
                v.getNonEmptySubSlots().stream().sorted((a,b)->a.getName().compareTo(b.getName()))
                    .forEach(slt -> v.get(slt).accept(this));
                stack.removeLast();
                return null;
            }
            
            private String stackToPath() {
                return stack.stream().collect(joining("/"));
            }
        });
        Collections.sort(dimensionNames);
        return dimensionNames;
    }
    
}
