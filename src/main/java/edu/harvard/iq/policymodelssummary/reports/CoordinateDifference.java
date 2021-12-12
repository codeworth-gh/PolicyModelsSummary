package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodelssummary.policyspace.ExpandedSlotValue;

/**
 * 
 */
public record CoordinateDifference(String path, ExpandedSlotValue threshold, ExpandedSlotValue actual) {
    
}
