/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodelssummary.reports.CoordinateDifference;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author michael
 */
public record FilterResult(
    Transcript threshold,
    Path repos,
    Set<Transcript> accepted,
    Set<Transcript> refused,
    Map<String,List<CoordinateDifference>> explanations
) {
    
    public String thresholdFilename() {
        return threshold.getTranscriptFile().getFileName().toString();
    }
    
    public List<String> refusedKeys() {
        return refused.stream().map(t->t.getName()).sorted().toList();
    }
    
    public boolean isHasAccepted(){
        return !accepted.isEmpty();
    }
    
    public boolean isHasRejected(){
        return !refused.isEmpty();
    }
    
    public List<Transcript> acceptedList() {
        return accepted.stream().sorted( (t1, t2)-> t1.getName().compareTo(t2.getName())).toList();
    }
    
    public List<CoordinateDifference> explain( String key ) {
        return explanations.get(key);
    }
}
