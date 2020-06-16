package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodels.model.policyspace.slots.AbstractSlot;
import edu.harvard.iq.policymodels.model.policyspace.values.AbstractValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AggregateValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodels.model.policyspace.values.ToDoValue;
import edu.harvard.iq.policymodelssummary.Transcript;
import java.util.Arrays;
import java.util.List;
import static java.util.stream.Collectors.joining;

/**
 *
 * @author michael
 */
public abstract class CoordinateColumn extends ReportColumn {
    
    private static final AbstractValue.Visitor<String> VALUE_PTR = new AbstractValue.Visitor<>(){
        @Override
        public String visitToDoValue(ToDoValue v) {
            return "TODO";
        }

        @Override
        public String visitAtomicValue(AtomicValue v) {
            return v.getName();
        }

        @Override
        public String visitAggregateValue(AggregateValue v) {
            return v.getValues().stream().map( itm -> itm.getName() ).sorted().collect( joining(",") );
        }

        @Override
        public String visitCompoundValue(CompoundValue v) {
            System.err.println("Attempt to print a compound value of slot " + v.getSlot().getName() );
            return "ERR";
        }
    };
    
    private static final AbstractValue.Visitor<String> ORDINAL_PTR = new AbstractValue.Visitor<>(){
        @Override
        public String visitToDoValue(ToDoValue v) {
            return "TODO";
        }

        @Override
        public String visitAtomicValue(AtomicValue v) {
            return Integer.toString(v.getOrdinal());
        }

        @Override
        public String visitAggregateValue(AggregateValue v) {
            return v.getValues().stream().map( itm -> Integer.toString(itm.getOrdinal())).sorted().collect( joining(",") );
        }

        @Override
        public String visitCompoundValue(CompoundValue v) {
            System.err.println("Attempt to print a compound value of slot " + v.getSlot().getName() );
        }
    };


    final Transcript tspt;
    
    
    public CoordinateColumn(Transcript tspt, String rowName) {
        super(rowName);
        this.tspt = tspt;
    }
    
    
    protected AbstractValue getValue( CompoundValue root, List<String> path ) {
        AbstractSlot nextSlot = root.getSlot().getSubSlot(path.get(0));
        if ( nextSlot == null ) return null;
        if ( path.size() == 1 ) {
            return root.get( nextSlot );
            
        } else {
            return getValue((CompoundValue) root.get(nextSlot), path.subList(1, path.size()));
        }
    }
    
    public static class TextValue extends CoordinateColumn {
        
        public TextValue(Transcript tspt, String rowName) {
            super(tspt, rowName);
        }
        
        @Override
        String getValue(String rowKey) {
            List<String> path = Arrays.asList(rowKey.split("/"));

            AbstractValue abv = getValue(tspt.getCoordinate(), path.subList(1, path.size()));
            return (abv!=null) ? abv.accept(VALUE_PTR) : "";

        }
    }
    
    public static class OrdinalValue extends CoordinateColumn {
        
        public OrdinalValue(Transcript tspt, String rowName) {
            super(tspt, rowName);
        }
        
        @Override
        String getValue(String rowKey) {
            List<String> path = Arrays.asList(rowKey.split("/"));

            AbstractValue abv = getValue(tspt.getCoordinate(), path.subList(1, path.size()));
            return (abv!=null) ? abv.accept(ORDINAL_PTR) : "";

        }
    }
}
