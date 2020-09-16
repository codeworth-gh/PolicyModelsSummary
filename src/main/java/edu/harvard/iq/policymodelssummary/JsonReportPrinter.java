package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodels.model.policyspace.slots.AbstractSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.AggregateSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.AtomicSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.CompoundSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.ToDoSlot;
import edu.harvard.iq.policymodels.model.policyspace.values.AbstractValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AggregateValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodels.model.policyspace.values.ToDoValue;
import java.io.PrintWriter;
import java.util.Collection;
import static java.util.stream.Collectors.joining;
import java.util.stream.Stream;

/**
 * Writes the report results to JSON. Report includes the expanded policy space,
 * and the results for each included transcript file.
 * 
 * @author michael
 */
public class JsonReportPrinter {
    
    private final StringBuilder indent = new StringBuilder();
    private final PrintWriter out;
    private boolean afterValue = false;

    public JsonReportPrinter(PrintWriter out) {
        this.out = out;
    }
    
    public void print(PolicyModel mdl, Collection<Transcript> transcripts) {
        pushObj();
        printSpace(mdl.getSpaceRoot());
        printTranscripts(transcripts);
        popObj();
    }
    
    protected void printSpace(CompoundSlot root) {
        pushObj("space");
        root.accept(new AbstractSlot.VoidVisitor(){
            
            @Override
            public void visitAtomicSlotImpl(AtomicSlot t) {
                printPair( t.getName(), t.values().stream().map(AtomicValue::getName) );
            }

            @Override
            public void visitAggregateSlotImpl(AggregateSlot t) {
                pushObj(t.getName());
                t.getItemType().values().forEach(sv -> {
                    printPair( sv.getName(), Stream.of("no","yes") );
                });
                popObj();
            }

            @Override
            public void visitCompoundSlotImpl(CompoundSlot t) {
                pushObj(t.getName());
                t.getSubSlotsInDeclaraionOrder().forEach( subSlot -> subSlot.accept(this) );
                popObj();
            }

            @Override
            public void visitTodoSlotImpl(ToDoSlot t) {}
            
        });
        
        popObj();
    }
    
    private void printTranscripts(Collection<Transcript> transcripts) {
        pushObj("transcripts");
        transcripts.forEach( tpt -> {
            String name = tpt.getName();
            if (name.toLowerCase().endsWith(".xml")) {
                name = name.substring(0, name.length()-4);
            }
            pushObj( name );
            tpt.getCoordinate().accept(new AbstractValue.Visitor<Void>(){
                @Override
                public Void visitToDoValue(ToDoValue v) {
                    return null;
                }

                @Override
                public Void visitAtomicValue(AtomicValue v) {
                    printPair(v.getSlot().getName(), v.getName());
                    return null;
                }

                @Override
                public Void visitAggregateValue(AggregateValue v) {
                    pushObj(v.getSlot().getName());
                    v.getSlot().getItemType().values().forEach( expDim -> {
                        boolean has = v.getValues().contains(expDim);
                        printPair(expDim.getName(), has?"yes":"no");
                    });
                    popObj();
                    return null;
                }

                @Override
                public Void visitCompoundValue(CompoundValue v) {
                    pushObj(v.getSlot().getName());
                    v.getNonEmptySubSlots().forEach(ness->v.get(ness).accept(this));
                    popObj();
                    return null;
                }
            });
            popObj();
        });
        popObj();
    }
    
    private void printPair(String key, String value) {
        if ( afterValue ) {
            out.print(",");
        }
        out.println();
        indent();
        out.print("\"" + esc(key) + "\":\"" + esc(value) + "\"");
        afterValue = true;
    }

    private void printPair(String key, Stream<String> values) {
        if ( afterValue ) {
            out.print(",");
        }
        out.println();
        indent();
        out.print("\"" + esc(key) + "\":[" + values.map(this::esc).map(s->"\""+s+"\"").collect(joining(",")) + "]");
        afterValue = true;
    }

    private String esc(String in){
        return in.replaceAll("\"", "\\\\\"");
    }
    
    private void pushObj(String name) {
        if ( afterValue ) {
            out.print(",");
        }
        out.println();
        out.print(indent + "\"" + esc(name) + "\": {");
        indent.append(" ");
        afterValue = false;
    }
    
    private void pushObj() {
        out.print(indent);
        out.print("{");
        indent.append(" ");
        afterValue = false;
    }
    
    private void popObj() {
        indent.setLength(indent.length()-1);
        out.println();
        out.print(indent);
        out.print("}");
        afterValue = true;
    }
    
    private void indent() {
        out.print( indent.toString() );
    }
}
