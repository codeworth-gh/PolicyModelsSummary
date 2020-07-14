package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodels.model.policyspace.slots.AtomicSlot;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodelssummary.TranscriptSummary;
import edu.harvard.iq.policymodelssummary.policyspace.ExpandedSlotValue;
import edu.harvard.iq.policymodelssummary.policyspace.ExpandedSlotValue.Atomic;
import edu.harvard.iq.policymodelssummary.policyspace.ExpandedSlotValue.Flag;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

/**
 *
 * @author michael
 */
public abstract class CoordinateSummaryColumn extends TranscriptSummaryColumn {

    public CoordinateSummaryColumn(TranscriptSummary smry, String title) {
        super(smry, title);
    }
    
    protected Map<ExpandedSlotValue, Long> counts( String rowKey ) {
        List<String> path = Arrays.asList(rowKey.split("/"));
        List<String> slotPath = path.subList(1, path.size());
        return summary.getTranscripts().stream()
            .map( t -> t.getCoordinate() )
            .filter( t -> t != null )
            .map( t -> ExpandedSlotValue.lookup(t, slotPath) )
            .collect( groupingBy( Function.identity(), counting()));
    }
    
    public static class Max extends CoordinateSummaryColumn {
        
        public Max(TranscriptSummary aSmry, String aName ){
            super(aSmry, aName);
        }

        @Override
        String getValue(String rowKey) {
            Set<ExpandedSlotValue> found = counts(rowKey).keySet();
            Optional<ExpandedSlotValue> max = found.stream().sorted( (a,b)->b.ordinal()-a.ordinal() ).findFirst();
            return max.isPresent() ? max.get().nameString() : "";
        }
    }

    public static class Min extends CoordinateSummaryColumn {
        
        public Min(TranscriptSummary aSmry, String aName ){
            super(aSmry, aName);
        }

        @Override
        String getValue(String rowKey) {
            Set<ExpandedSlotValue> found = counts(rowKey).keySet();
            Optional<ExpandedSlotValue> min = found.stream().sorted( (a,b)->a.ordinal()-b.ordinal() ).findFirst();
            return min.isPresent() ? min.get().nameString() : "";
        }
    }
    
    public static class Counts extends CoordinateSummaryColumn { 

        public Counts(TranscriptSummary smry, String title) {
            super(smry, title);
        }
        
        @Override
        String getValue(String rowKey) {
            Map<ExpandedSlotValue, Long> found = counts(rowKey);
            
            if ( found.isEmpty() ) return "";
            ExpandedSlotValue sampleSlot = found.keySet().iterator().next();
            
            if ( sampleSlot instanceof ExpandedSlotValue.Flag ) {
                return "no:" + found.getOrDefault(Flag.NO,0l) + " yes:" + found.getOrDefault(Flag.YES,0l); 
                    
            } else if ( sampleSlot instanceof ExpandedSlotValue.Atomic ) {
                AtomicValue av = (AtomicValue) sampleSlot.get();
                if ( av == null ) {
                    System.err.println("Error getting counts: '" + rowKey + "' has null value");
                    return "";
                }
                AtomicSlot slt = av.getSlot();
                return slt.values().stream()
                    .map( v -> v.getName() + ":" + found.getOrDefault(new Atomic(v),0l) )
                    .collect( joining(" ") );
                    
                
            } else { 
                return "";
            }
            
            
        }
        
    }
    
}
