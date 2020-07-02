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
    
    static final AbstractValue.Visitor<String> VALUE_PTR = new AbstractValue.Visitor<>(){
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
    
    static final AbstractValue.Visitor<String> ORDINAL_PTR = new AbstractValue.Visitor<>(){
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
    
    static final AbstractValue.Visitor<Double> SCALED_PTR = new AbstractValue.Visitor<>(){
        @Override
        public Double visitToDoValue(ToDoValue v) {
            return -1d;
        }

        @Override
        public Double visitAtomicValue(AtomicValue v) {
            return ((double)v.getOrdinal()/v.getSlot().values().size());
        }

        @Override
        public Double visitAggregateValue(AggregateValue v) {
            return ((double)v.getValues().size()/v.getSlot().getItemType().values().size());
        }

        @Override
        public Double visitCompoundValue(CompoundValue v) {
            System.err.println("Attempt to print a compound value of slot " + v.getSlot().getName() );
            return -2d;
        }
    };


    final Transcript tspt;
    
    
    public CoordinateColumn(Transcript tspt, String rowName) {
        super(rowName);
        this.tspt = tspt;
    }
    
    
    public static AbstractValue getSlotValue( CompoundValue root, List<String> path ) {
        AbstractSlot nextSlot = root.getSlot().getSubSlot(path.get(0));
        if ( nextSlot == null ) return null;
        if ( path.size() == 1 ) {
            return root.get( nextSlot );
            
        } else {
            return getSlotValue((CompoundValue) root.get(nextSlot), path.subList(1, path.size()));
        }
    }
    
    public static class TextValue extends CoordinateColumn {
        
        public TextValue(Transcript tspt, String rowName) {
            super(tspt, rowName);
        }
        
        @Override
        String getValue(String rowKey) {
            List<String> path = Arrays.asList(rowKey.split("/"));

            AbstractValue abv = getSlotValue(tspt.getCoordinate(), path.subList(1, path.size()));
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

            AbstractValue abv = getSlotValue(tspt.getCoordinate(), path.subList(1, path.size()));
            return (abv!=null) ? abv.accept(ORDINAL_PTR) : "";

        }
    }
    
    public static class ScaledValue extends CoordinateColumn {
        
        public ScaledValue(Transcript tspt, String rowName) {
            super(tspt, rowName);
        }
        
        @Override
        String getValue(String rowKey) {
            List<String> path = Arrays.asList(rowKey.split("/"));

            AbstractValue abv = getSlotValue(tspt.getCoordinate(), path.subList(1, path.size()));
            return (abv!=null) ? String.format("%.4f", abv.accept(SCALED_PTR)*9+1) : "0";
        }
    }
    
}
