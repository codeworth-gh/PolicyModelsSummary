package edu.harvard.iq.policymodelssummary.reports;

/**
 *
 * @author michael
 */
public abstract class ReportColumn {
    private final String title;

    public ReportColumn(String rowName) {
        this.title = rowName;
    }
   
    public String getName() {
        return title;
    }
    
    abstract String getValue(String rowKey);
    
}
