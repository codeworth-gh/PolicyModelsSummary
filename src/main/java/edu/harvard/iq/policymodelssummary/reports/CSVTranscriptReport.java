package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodelssummary.TranscriptSummary;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.joining;

/**
 *
 * @author michael
 */
public class CSVTranscriptReport {
    
    final List<ReportColumn> columns = new ArrayList<>();
    
    public void writeReport(TranscriptSummary smry, PrintWriter out ) {
        
        createColumns(smry);
        
        // headers
        out.println(
            columns.stream().map( c -> c.getName() ).collect( joining("\t") )
        );
        
        // rows
        smry.questionOrder().stream()
            .map( id -> columns.stream()
                               .map(c->c.getValue(id))
                               .map(v-> (v!=null) ? v : "" )
                               .collect(joining("\t")) )
            .forEach( out::println );
    }

    private void createColumns(TranscriptSummary smry) {
        columns.clear();
        
        columns.add( new CounterColumn("row #") );
        columns.add( new RowKeyColumn("identifier") );
        smry.getSortedNames().stream().map( n -> smry.get(n) )
            .forEach( tspt -> {
                columns.add( new TranscriptQnAColumn.Text(tspt) );
                columns.add( new TranscriptQnAColumn.Score(tspt) );
                columns.add( new TranscriptQnAColumn.ScaledScore(tspt) );
            });
        columns.add( new TranscriptSummaryColumn.Count(smry) );
        columns.add( new TranscriptSummaryColumn.Min(smry) );
        columns.add( new TranscriptSummaryColumn.Average(smry) );
        columns.add( new TranscriptSummaryColumn.AverageNormalized(smry) );
        columns.add( new TranscriptSummaryColumn.Max(smry) );
        
    }
    
}
