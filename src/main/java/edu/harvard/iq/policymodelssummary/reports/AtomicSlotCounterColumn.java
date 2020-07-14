package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodels.model.policyspace.slots.AbstractSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.AtomicSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.CompoundSlot;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodelssummary.TranscriptSummary;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static java.util.function.Function.identity;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;

/**
 * Outputs a count of each value
 * 
 * @author michael
 */
public class AtomicSlotCounterColumn extends TranscriptSummaryColumn {
    
    private final AtomicSlot slot;
    private final CompoundSlot root;

    public AtomicSlotCounterColumn(TranscriptSummary smry, AtomicSlot aSlot) {
        super(smry, aSlot.getName() + " counts");
        slot = aSlot;
        root = smry.getTranscripts().iterator().next().getCoordinate().getSlot();
    }
    
   
    @Override
    String getValue(String rowKey) {
        List<String> path = Arrays.asList(rowKey.split("/"));
        List<String> slotPath = path.subList(1, path.size());
        AbstractSlot absSlt = root.getSubSlot(slotPath);
        if ( absSlt instanceof AtomicSlot ) {
            final Map<AtomicValue, Long> counts = summary.getTranscripts().stream()
                .map(tspt -> CoordinateColumn.getSlotValue(tspt.getCoordinate(), slotPath))
                .filter( v -> v != null )
                .map( v -> (AtomicValue)v )
                .collect( Collectors.groupingBy(identity(), Collectors.counting()));
            
            return slot.values().stream().map( v -> v.getName() + ":" + counts.getOrDefault(v, 0l) )
                        .collect( joining(", ") );
            
        } else {
            return "";
        }
    }
    
}
