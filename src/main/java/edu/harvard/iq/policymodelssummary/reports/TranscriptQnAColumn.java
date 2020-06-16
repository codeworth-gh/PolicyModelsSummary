package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodelssummary.Transcript;

/**
 *
 * @author michael
 */ 
public abstract class TranscriptQnAColumn extends ReportColumn {
    
    protected final Transcript trpt;

    public TranscriptQnAColumn(Transcript trpt, String rowName) {
        super(rowName);
        this.trpt = trpt;
    }
    
    
    public static class Text extends TranscriptQnAColumn {
        public Text(Transcript trpt) {
            super(trpt, trpt.getName() + " Answer Text");
        }

        @Override
        String getValue(String rowKey) {
            Transcript.SingleQandA qna = trpt.get(rowKey);
            return ( qna != null ) ? qna.getAnswerText() : null;
        }
    }
    
    public static class Score extends TranscriptQnAColumn {
        public Score(Transcript trpt) {
            super(trpt, trpt.getName() + " Answer Score");
        }

        @Override
        String getValue(String rowKey) {
            Transcript.SingleQandA qna = trpt.get(rowKey);
            return ( qna != null ) ? Integer.toString(qna.getAnswerOrdinal()) : null;
        }
    }
    
    public static class ScaledScore extends TranscriptQnAColumn {
        public ScaledScore(Transcript trpt) {
            super(trpt, trpt.getName() + " Answer Scaled");
        }

        @Override
        String getValue(String rowKey) {
            Transcript.SingleQandA qna = trpt.get(rowKey);
            return ( qna != null ) ? String.format("%.4f", qna.getNormalizedAnswer()) : null;
        }
    }
}
