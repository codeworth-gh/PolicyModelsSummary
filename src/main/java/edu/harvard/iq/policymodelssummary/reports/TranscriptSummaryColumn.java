package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodelssummary.TranscriptSummary;
import java.util.OptionalInt;

/**
 * A column summarizing a row in multiple transcripts.
 * 
 * @author michael
 */
public abstract class TranscriptSummaryColumn extends ReportColumn {
    
    protected final TranscriptSummary summary;

    public TranscriptSummaryColumn(TranscriptSummary smry, String rowName) {
        super(rowName);
        this.summary = smry;
    }
    
    protected long countValues( String row ) {
        return summary.getTranscripts().stream()
            .map( t -> t.get(row) )
            .filter( v -> v != null )
            .mapToInt( r -> r.getAnswerOrdinal() )
            .filter( i -> i>=0 )
            .count();
    }
    
    public static class Count extends TranscriptSummaryColumn {
        
        public Count( TranscriptSummary smry ) {
            super(smry, "Count");
        }

        @Override
        String getValue(String rowKey) {
            return Long.toString(countValues(rowKey));
        }
    }
    
    public static class Max extends TranscriptSummaryColumn {
        
        public Max( TranscriptSummary smry ) {
            super(smry, "Max");
        }

        @Override
        String getValue(String rowKey) {
            OptionalInt mmax = summary.getTranscripts().stream()
            .map( t -> t.get(rowKey) )
            .filter( v -> v != null )
            .mapToInt( v -> v.getAnswerOrdinal() )
             .filter( i -> i>=0 )
            .max();
            
            return mmax.isPresent() ? Integer.toString(mmax.getAsInt()) : "";
        }
    }
    
    public static class Min extends TranscriptSummaryColumn {
        
        public Min( TranscriptSummary smry ) {
            super(smry, "Min");
        }

        @Override
        String getValue(String rowKey) {
            OptionalInt mmin = summary.getTranscripts().stream()
            .map( t -> t.get(rowKey) )
            .filter( v -> v != null )
            .mapToInt( v -> v.getAnswerOrdinal() )
            .filter( i -> i>=0 )
            .min();
            
            return mmin.isPresent() ? Integer.toString(mmin.getAsInt()) : "";
        }
    }
    
    
    public static class AverageNormalized extends TranscriptSummaryColumn {
        
        public AverageNormalized( TranscriptSummary smry ) {
            super(smry, "Normalized Average");
        }

        @Override
        String getValue(String rowKey) {
            
            long count = countValues(rowKey);
            
            double sum = summary.getTranscripts().stream()
                .map( t -> t.get(rowKey) )
                .filter( v -> v != null )
                .mapToDouble(q -> q.getNormalizedAnswer() )
                .sum();
            
            return  String.format("%.4f", sum/count); 
        }
    }
    
    public static class Average extends TranscriptSummaryColumn {
        
        public Average( TranscriptSummary smry ) {
            super(smry, "Average");
        }

        @Override
        String getValue(String rowKey) {
            
            long count = countValues(rowKey);
            
            double sum = summary.getTranscripts().stream()
                .map( t -> t.get(rowKey) )
                .filter( v -> v != null )
                .mapToDouble(q -> q.getAnswerOrdinal())
                .sum();
            
            return  String.format("%.4f", sum/count); 
        }
    }
}
