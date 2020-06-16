package edu.harvard.iq.policymodelssummary.reports;

/**
 *
 * @author michael
 */
public abstract class ReportColumn {
     private final String rowName;

    public ReportColumn(String rowName) {
        this.rowName = rowName;
    }
   
    public String getName() {
        return rowName;
    }
    
    abstract String getValue(String rowKey);
    
}
