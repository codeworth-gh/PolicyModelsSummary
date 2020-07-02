package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodels.model.policyspace.slots.AbstractSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.AtomicSlot;
import edu.harvard.iq.policymodels.model.policyspace.values.AbstractValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodelssummary.TranscriptSummary;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static java.util.stream.Collectors.toList;

/**
 *
 * @author michael
 */
public abstract class CoordinateSummaryColumn extends TranscriptSummaryColumn {

    public CoordinateSummaryColumn(TranscriptSummary smry, String rowName) {
        super(smry, rowName);
    }
    
    public static class Max extends CoordinateSummaryColumn {
        
        public Max(TranscriptSummary aSmry, String aName ){
            super(aSmry, aName);
        }

        @Override
        String getValue(String rowKey) {
            List<String> path = Arrays.asList(rowKey.split("/"));
            List<String> slotPath = path.subList(1, path.size());
            List<AbstractValue> values = new ArrayList<>(smry.getTranscripts().size());
            
            smry.getTranscripts().forEach(tspt -> {
                values.add(CoordinateColumn.getSlotValue(tspt.getCoordinate(), slotPath));
            });
            
            values = values.stream().filter(v->v != null).collect( toList() );
            if ( values.isEmpty() ) return "";
            AbstractSlot slot = values.get(0).getSlot();
            
            if ( slot instanceof AtomicSlot ) {
                return values.stream().map( v -> (AtomicValue)v ).max( (a,b)->a.getOrdinal()-b.getOrdinal() ).get().getName();
                
            } else {
                return "";
            }
            
        }
    }

    public static class Min extends CoordinateSummaryColumn {
        
        public Min(TranscriptSummary aSmry, String aName ){
            super(aSmry, aName);
        }

        @Override
        String getValue(String rowKey) {
            List<String> path = Arrays.asList(rowKey.split("/"));
            List<String> slotPath = path.subList(1, path.size());
            List<AbstractValue> values = new ArrayList<>(smry.getTranscripts().size());
            
            smry.getTranscripts().forEach(tspt -> {
                values.add(CoordinateColumn.getSlotValue(tspt.getCoordinate(), slotPath));
            });
            
            values = values.stream().filter(v->v != null).collect( toList() );
            if ( values.isEmpty() ) return "";
            
            AbstractSlot slot = values.get(0).getSlot();
            
            if ( slot instanceof AtomicSlot ) {
                return values.stream().map( v -> (AtomicValue)v ).min( (a,b)->a.getOrdinal()-b.getOrdinal() ).get().getName();
                
            } else {
                return "";
            }
            
        }
    }
    
}
