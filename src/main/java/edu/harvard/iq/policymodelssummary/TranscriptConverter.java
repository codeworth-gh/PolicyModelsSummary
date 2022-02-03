package edu.harvard.iq.policymodelssummary;

import edu.harvard.iq.policymodels.model.PolicyModel;
import edu.harvard.iq.policymodels.model.decisiongraph.Answer;
import edu.harvard.iq.policymodels.model.policyspace.values.AbstractValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AggregateValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodels.model.policyspace.values.ToDoValue;
import edu.harvard.iq.policymodels.runtime.RuntimeEngine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author michael
 */
public class TranscriptConverter extends RunMode {
    
    private final DocumentBuilderFactory bldFactory = DocumentBuilderFactory.newInstance();
    private final DocumentBuilder docBuilder;
    private final Path srcModelPath;
    private final Path dstModelPath;
    private final Path transcriptsPath;
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private final Transformer transformer;
    private TranscriptParser tspParser;
    private RuntimeEngine modelRunner;
    
    
    public TranscriptConverter(Path srcModelPath, Path dstModelPath, Path transcriptsPath) throws Exception {
        super("CONVERT");
        this.srcModelPath = srcModelPath;
        this.dstModelPath = dstModelPath;
        this.transcriptsPath = transcriptsPath;
        docBuilder = bldFactory.newDocumentBuilder();
        transformer = transformerFactory.newTransformer();
    }
    
    public void go() throws Exception {
        o.println(String.format("src model\t%s", srcModelPath.toAbsolutePath()));
        o.println(String.format("dst model\t%s", dstModelPath.toAbsolutePath()));
        o.println(String.format("transcripts\t%s", transcriptsPath.toAbsolutePath()));
        
        // load models
        var srcModel = loadModel(srcModelPath);
        tspParser = new TranscriptParser(srcModel);
        var dstModel = loadModel(dstModelPath);
        modelRunner = new RuntimeEngine();
        modelRunner.setModel(dstModel);
        o.println("Parser and Runtime OK");
        
        // process tspts
        Path dstDir = transcriptsPath.resolveSibling(transcriptsPath.getFileName() + "-cnv");
        o.println(String.format("Destination dir:\t%s", dstDir.toAbsolutePath()));
        Files.createDirectory(dstDir);
        Files.list(transcriptsPath)
            .filter( p -> Files.isRegularFile(p) ).filter( p -> p.getFileName().toString().toLowerCase().endsWith(".xml") )
            .forEach( p -> {
                var dstPath = dstDir.resolve(p.getFileName());
                convertTranscript(p, dstPath, srcModel, dstModel);
            });
    }
    
    private void convertTranscript( Path tsptPath, Path dstPath, PolicyModel src, PolicyModel dst ) {
        try {
            o.println("Converting " + tsptPath.getFileName() );
            Transcript tspt = tspParser.parse(tsptPath);
            modelRunner.restart();
            List<Answer> answers = tspt.getAnswerList().stream().map( qa -> Answer.withName(qa.getAnswerText())).toList();
            modelRunner.consumeAll(answers);
            CompoundValue result = modelRunner.getCurrentValue();
            Element converted = createXml(tspt, result);
                    
            var ds = new DOMSource(converted);
            try( var out = Files.newBufferedWriter(dstPath) ) {
                transformer.transform(ds, new StreamResult(out) );            
            }
            o.println("done");
            
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private Element createXml( Transcript tspt, CompoundValue newRes ) {
        var doc = docBuilder.newDocument();
        Element model = doc.createElement("model");
        model.appendChild(doc.createElement("id")).setTextContent(tspt.getModelData().id());
        model.appendChild(doc.createElement("version")).setTextContent(Integer.toString(tspt.getModelData().version()));
        model.appendChild(doc.createElement("localization")).setTextContent(tspt.getModelData().localization());
        model.appendChild(doc.createElement("time"))
            .setTextContent(Long.toString(tspt.getModelData().time().toEpochSecond(ZoneOffset.UTC)));
        Element metadata = doc.createElement("metadata");
        metadata.appendChild(model);
        metadata.appendChild( doc.createComment("Converted at: " + System.currentTimeMillis()));
        
        Element result = doc.createElement("result");
        fillResultElement(newRes, result, doc);
        
        Element transcript = doc.createElement("transcript");
        fillTranscriptElement(tspt, transcript, doc);
        
        Element interview = doc.createElement("interview");
        interview.appendChild(metadata);
        interview.appendChild(result);
        interview.appendChild(transcript);
        
        return interview;
    }
    
    private void fillResultElement( CompoundValue resRoot, Element resultEmt, Document doc ) {
        resultEmt.setAttribute("status", (resRoot != null) ? "Accept" : "Reject");
        if ( resRoot != null ) {
            resRoot.accept(new AbstractValue.Visitor<Void>() {

                LinkedList<Element> stack = new LinkedList<>(List.of(resultEmt));

                @Override
                public Void visitToDoValue(ToDoValue v) {
                    Element n = (Element) stack.peek().appendChild(doc.createElement("todo"));
                    n.setAttribute("slot", v.getSlot().getName());
                    return null;
                }

                @Override
                public Void visitAtomicValue(AtomicValue v) {
                    Element n = (Element) stack.peek().appendChild(doc.createElement("atomic"));
                    n.setAttribute("slot", v.getSlot().getName());
                    n.setAttribute("ordinal", Integer.toString(v.getOrdinal()));
                    n.setAttribute("outOf", Integer.toString(v.getSlot().values().size()));
                    n.setTextContent( v.getName() );
                    return null;
                }

                @Override
                public Void visitAggregateValue(AggregateValue v) {
                    final Element n = (Element) stack.peek().appendChild(doc.createElement("aggregate"));
                    n.setAttribute("slot", v.getSlot().getName());
                    var values = new ArrayList<>(v.getValues());
                    Collections.sort(values, (a,b)->a.getName().compareTo(b.getName()));
                    
                    values.forEach( av -> n.appendChild(doc.createElement("value")).setTextContent(av.getName()));
                    
                    return null;
                }

                @Override
                public Void visitCompoundValue(CompoundValue v) {
                    var emt = doc.createElement("compound");
                    emt.setAttribute("slot", v.getSlot().getName());
                    stack.peek().appendChild(emt);
                    stack.push( emt );
                    
                    v.getNonEmptySubSlots().stream().sorted((a,b)->a.getName().compareTo(b.getName()))
                        .forEach(slt -> v.get(slt).accept(this));
                    stack.pop();
                    return null;
                }
            });
        }
    }
    
    private void fillTranscriptElement( Transcript tspt, Element tsptEmt, Document doc ) {
        for ( var sqa : tspt.getAnswerList() ) {
            var qEmt = (Element)tsptEmt.appendChild(doc.createElement("question"));
            qEmt.setAttribute("id", sqa.getQuestionId());
            qEmt.appendChild(doc.createElement("text"))
                    .appendChild(doc.createCDATASection(sqa.getQuestionText()));
            qEmt.appendChild(doc.createElement("answer"))
                    .setTextContent(sqa.getAnswerText());
            if ( sqa.getNote().isPresent() ) {
                qEmt.appendChild(doc.createElement("note"))
                        .appendChild(doc.createCDATASection(sqa.getNote().get()));
            }
        }
    }
}
