package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodels.model.decisiongraph.nodes.AskNode;
import edu.harvard.iq.policymodels.model.policyspace.slots.AggregateSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.AtomicSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.CompoundSlot;
import edu.harvard.iq.policymodels.model.policyspace.values.AggregateValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static java.util.stream.Collectors.toList;
import java.util.stream.StreamSupport;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Takes a transcript file from the server, and parses it.
 * 
 * @author michael
 */
public class TranscriptParser implements ContentHandler {
    
    private Path curPath;
    private final StringBuilder charAggregator = new StringBuilder();
    private Transcript result;
    private Transcript.SingleQandA curQnA;
    
    private final PolicyModel model;
    private final Map<String, List<String>> answerIndex = new HashMap<>();
    private final LinkedList<String> stack = new LinkedList<>();
    private Transcript.ModelData modelData = new Transcript.ModelData(null, 0, null, null);
    boolean inModelMetadata = false;
    
    public TranscriptParser(PolicyModel model) {
        this.model = model;
        prepareAnswerIndex();
    }
  
    public Transcript parse( Path transcriptFile ) throws IOException, SAXException, ParserConfigurationException {
        curPath = transcriptFile;
        XMLReader rdr = SAXParserFactory.newDefaultInstance().newSAXParser().getXMLReader();
        rdr.setContentHandler(this);
        try ( Reader fileRdr = Files.newBufferedReader(transcriptFile) ) {
            rdr.parse( new InputSource(fileRdr));        
        }
        result.setModelData(modelData);
        return result;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
        result = new Transcript();
        result.setName( curPath.getFileName().toString() );
        result.setCoordinate( model.getSpaceRoot().createInstance() );
    }

    @Override
    public void endDocument() throws SAXException {
        charAggregator.setLength(0);
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {}

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {}

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        switch (qName) {
            case "Text" -> charAggregator.setLength(0);
            case "question" -> {
                curQnA = new Transcript.SingleQandA();
                curQnA.questionId = atts.getValue("id");
            }
            case "compound" -> stack.push(atts.getValue("slot"));
            case "aggregate", "aggreate" -> stack.push(atts.getValue("slot"));
            case "atomic" -> addAtomicValue(atts.getValue("slot"), atts.getValue("ordinal"));
            case "model"  -> inModelMetadata = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "question" -> {
                result.append(curQnA);
                curQnA = null;
            }
                
            case "text" -> curQnA.questionText = consumeFreeText().trim();
            case "model"  -> inModelMetadata = false;    
            case "answer" -> {
                curQnA.answerText = consumeFreeText().trim();
                List<String> answers = answerIndex.get(curQnA.questionId);
                curQnA.normalizedAnswer=-1.0;
                if ( answers==null ) {
                    System.out.printf("WARNING: no answers found for question %s\n", curQnA.questionId );
                    curQnA.answerOrdinal = -2;
                    
                } else {
                    curQnA.answerOrdinal = answers.indexOf(curQnA.answerText);
                    if ( curQnA.answerOrdinal==-1 ) {
                        System.out.printf("WARNING: answer '%s' not found for question %s\n", curQnA.answerText, curQnA.questionId );
                    } else {
                        curQnA.normalizedAnswer = ((double)curQnA.answerOrdinal)/(answers.size()-1);
                    }
                }
            }
                
            case "note" -> curQnA.note = Optional.of(consumeFreeText().trim());
            case "value" -> addAggregateValue(consumeFreeText().trim());
            case "atomic" -> consumeFreeText(); // just flush it
            case "compound", "aggregate", "aggreate" -> stack.pop();
            case "id" -> {
                if ( inModelMetadata ) {
                    modelData = new Transcript.ModelData(consumeFreeText().trim(), modelData.version(), modelData.localization(), modelData.time() );
                } else {
                    consumeFreeText();
                }
            }
            case "version" -> {
                if ( inModelMetadata ) {
                    modelData = new Transcript.ModelData(modelData.id(), Integer.parseInt(consumeFreeText().trim()), modelData.localization(), modelData.time() );
                } else {
                    consumeFreeText();
                }
            }
            case "localization" -> {
                if ( inModelMetadata ) {
                    modelData = new Transcript.ModelData(modelData.id(), modelData.version(), consumeFreeText().trim().trim(), modelData.time() );
                } else {
                    consumeFreeText();
                }
            }
            case "time" -> {
                if ( inModelMetadata ) {
                    String raw = consumeFreeText().trim();
                    long millies = Long.parseLong(raw);
                    LocalDateTime tme = LocalDateTime.ofEpochSecond(millies/1000, 0, ZoneOffset.UTC);
                    modelData = new Transcript.ModelData(modelData.id(), modelData.version(), modelData.localization(), tme );
                } else {
                    consumeFreeText();
                }
            }
        }
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        charAggregator.append(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {}

    @Override
    public void processingInstruction(String target, String data) throws SAXException {}

    @Override
    public void skippedEntity(String name) throws SAXException {}
    
    private void prepareAnswerIndex(){
        StreamSupport.stream(model.getDecisionGraph().nodes().spliterator(), false)
            .filter( n -> n instanceof AskNode )
            .map( n -> (AskNode)n )
            .forEach( n -> {
                List<String> texts = n.getAnswers().stream().map( a -> a.getAnswerText() ).collect(toList());
                answerIndex.put( n.getId(), texts);
            });
    }
    
    private String consumeFreeText() {
        String retVal = charAggregator.toString();
        charAggregator.setLength(0);
        return retVal;
    }

    private void addAtomicValue(String slotName, String slotValueOrdinal) {
        // build compound value hierarchy
        Iterator<String> path = stack.descendingIterator();
        path.next(); // root is known
        CompoundValue curVal = result.getCoordinate();
        while ( path.hasNext() ) {
            String nextName = path.next();
            CompoundSlot subSlotType = (CompoundSlot) curVal.getSlot().getSubSlot(nextName);
            CompoundValue nextVal = (CompoundValue) curVal.get(subSlotType);
            if ( nextVal == null ) {
                nextVal = subSlotType.createInstance();
                curVal.put(nextVal);
            }
            curVal = nextVal;
        }
        
        // add atomic value
        AtomicSlot atSlt = (AtomicSlot) curVal.getSlot().getSubSlot(slotName);
        int valueOrd = Integer.parseInt(slotValueOrdinal);
        Iterator<AtomicValue> valueItr = atSlt.values().iterator();
        while ( valueOrd > 0 ) {
            valueItr.next();
            valueOrd--;
        }
        curVal.put(valueItr.next());
    }
    
    private void addAggregateValue( String value ) {
         // build compound value hierarchy
        Iterator<String> path = stack.descendingIterator();
        int toGo = stack.size()-1;
        path.next(); // root is known
        CompoundValue curVal = result.getCoordinate();
        while ( --toGo > 0 ) {
            String nextName = path.next();
            CompoundSlot subSlotType = (CompoundSlot) curVal.getSlot().getSubSlot(nextName);
            CompoundValue nextVal = (CompoundValue) curVal.get(subSlotType);
            if ( nextVal == null ) {
                nextVal = subSlotType.createInstance();
                curVal.put(nextVal);
            }
            curVal = nextVal;
        }
        
        // Generate/get aggregate value
        AggregateSlot aggSlt = (AggregateSlot) curVal.getSlot().getSubSlot(path.next());
        AggregateValue aggVal = (AggregateValue) curVal.get(aggSlt);
        if ( aggVal == null ) {
            aggVal = aggSlt.createInstance();
            curVal.put(aggVal);
        }
        aggVal.add( aggSlt.getItemType().valueOf(value) );
    }
    
}
