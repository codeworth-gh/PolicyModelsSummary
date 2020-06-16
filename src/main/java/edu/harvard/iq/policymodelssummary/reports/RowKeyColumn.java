package edu.harvard.iq.policymodelssummary.reports;

/**
 *
 * @author michael
 */
public class RowKeyColumn extends ReportColumn {

    public RowKeyColumn(String rowName) {
        super(rowName);
    }
    
    @Override
    public String getValue(String rowKey) {
        return rowKey;
    }
    
}
