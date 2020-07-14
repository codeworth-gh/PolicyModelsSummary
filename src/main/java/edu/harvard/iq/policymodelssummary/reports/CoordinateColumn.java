package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodels.model.policyspace.slots.AbstractSlot;
import edu.harvard.iq.policymodels.model.policyspace.values.AbstractValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodelssummary.Transcript;
import edu.harvard.iq.policymodelssummary.policyspace.ExpandedSlotValue;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author michael
 */
public abstract class CoordinateColumn extends ReportColumn {
    
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
            List<String> path = tail(Arrays.asList(rowKey.split("/")));
            return ExpandedSlotValue.lookup(tspt.getCoordinate(), path).nameString();
        }
    }
    
    public static class OrdinalValue extends CoordinateColumn {
        
        public OrdinalValue(Transcript tspt, String rowName) {
            super(tspt, rowName);
        }
        
        @Override
        String getValue(String rowKey) {
            List<String> path = tail(Arrays.asList(rowKey.split("/")));
            return Integer.toString(ExpandedSlotValue.lookup(tspt.getCoordinate(), path).ordinal());
        }
    }
    
    public static class ScaledValue extends CoordinateColumn {
        
        public ScaledValue(Transcript tspt, String rowName) {
            super(tspt, rowName);
        }
        
        @Override
        String getValue(String rowKey) {
            List<String> path = tail(Arrays.asList(rowKey.split("/")));
            ExpandedSlotValue<?> value = ExpandedSlotValue.lookup(tspt.getCoordinate(), path);
            
            
            if ( value instanceof ExpandedSlotValue.Atomic ) {
                AtomicValue v = (AtomicValue) value.get();
                if ( v == null ) {                
                    // warn, and take the lowest value
                    System.err.println("WARNING: missing slot " + rowKey + " in transcript " + tspt.getName() );
                    return "1";
                }
                double scaled = ((double)v.getOrdinal()/v.getSlot().values().size());
                return String.format("%.4f", scaled*9+1);                    

                
            } else if ( value instanceof ExpandedSlotValue.Flag ) {
                return Integer.toString(value.ordinal()*9+1);
                
            } else {
                return "0";
            }
            
        }
    }
    
    private static List<String> tail( List<String> lst ) {
        if ( lst.isEmpty() ) return lst;
        return lst.subList(1, lst.size());
    }
}
