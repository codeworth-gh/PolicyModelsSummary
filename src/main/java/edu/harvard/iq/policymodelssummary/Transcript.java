package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 
 * Interview Transcript.
 * 
 * @author michael
 */
public class Transcript {
    
    public static class SingleQandA {
        String questionId;
        String questionText;
        Optional<String> note;
        String answerText;
        int answerOrdinal;
        double normalizedAnswer;

        public String getQuestionId() {
            return questionId;
        }
        
        public String getQuestionText() {
            return questionText;
        }

        public Optional<String> getNote() {
            return  (note!=null) ? note : Optional.empty();
        }

        public String getAnswerText() {
            return answerText;
        }

        public int getAnswerOrdinal() {
            return answerOrdinal;
        }

        public double getNormalizedAnswer() {
            return normalizedAnswer;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + Objects.hashCode(this.questionText);
            hash = 29 * hash + Objects.hashCode(this.answerText);
            hash = 29 * hash + this.answerOrdinal;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SingleQandA other = (SingleQandA) obj;
            
            
            return this.answerOrdinal != other.answerOrdinal &&
                Objects.equals(this.questionId, other.questionId) &&
                Objects.equals(this.note, other.note) &&
                Objects.equals(this.answerText, other.answerText) &&
                Objects.equals(this.questionText, other.questionText);
        }

        @Override
        public String toString() {
            return "[SingleQandA questionId=" + questionId + ", questionText=" + questionText.substring(0,10) + ", note=" + note + ", answer=" + answerText + " (" + answerOrdinal + "," + normalizedAnswer + ")]";
        }
    }
    
    public record ModelData( String id, int version, String localization, LocalDateTime time){}
    
    
    
    /** 
     * Transcript filename.
     */
    private String name;
    private final List<SingleQandA> answerSeq = new ArrayList<>();
    private final Map<String, SingleQandA> answerMap = new HashMap<>();
    private CompoundValue coordinate;
    private ModelData modelData;
    private Path transcriptFile;
    
    /** Human-readable name */
    private String hrName;
    
    public void append( SingleQandA sa ) {
        answerSeq.add(sa);
        answerMap.put(sa.questionId, sa);
    }
    
    public SingleQandA get(String qId) { 
        return answerMap.get(qId);
    }
    
    public List<SingleQandA> getAnswerList() {
        return answerSeq;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ModelData getModelData() {
        return modelData;
    }

    public void setModelData(ModelData modelData) {
        this.modelData = modelData;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.name);
        hash = 71 * hash + Objects.hashCode(this.answerSeq);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Transcript other = (Transcript) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.answerSeq, other.answerSeq);
    }

    public void setCoordinate(CompoundValue coordinate) {
        this.coordinate = coordinate;
    }
    
    public CompoundValue getCoordinate() {
        return coordinate;
    }

    public Path getTranscriptFile() {
        return transcriptFile;
    }

    public void setTranscriptFile(Path transcriptFile) {
        this.transcriptFile = transcriptFile;
    }

    public void setHrName(String hrName) {
        this.hrName = hrName;
    }
    
    /**
     * Gets the HR name. If it is not present returns the original (file) name.
     * @return 
     */
    public String getHrName() {
        return (hrName!=null) ? hrName : getName();
    }
    
    @Override
    public String toString() {
        return "[Transcript name:" + name + " QACount:" + answerMap.size() + "]";
    }
    
    public void dump() {
        System.out.println("Transcript " + name );
        answerSeq.forEach( System.out::println );
        System.out.println("/Transcript" );
    }
}   
