package edu.harvard.iq.policymodelssummary.reports;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author michael
 */
public class CounterColumn extends ReportColumn {
    
    private final Map<String, Integer> seen = new HashMap<>();
    
    public CounterColumn(String rowName) {
        super(rowName);
    }

    @Override
    String getValue(String rowKey) {
        if ( ! seen.containsKey(rowKey) ) {
            seen.put(rowKey, seen.size()+1);
        }
        return seen.get(rowKey).toString();
    }
    
}
