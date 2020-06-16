package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodels.model.policyspace.slots.AbstractSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.AggregateSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.AtomicSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.CompoundSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.ToDoSlot;
import edu.harvard.iq.policymodelssummary.TranscriptSummary;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import static java.util.stream.Collectors.joining;

/**
 *
 * @author michael
 */
public class CSVCoordinateReport {
    
    final List<ReportColumn> columns = new ArrayList<>();
    final List<String> rows = new ArrayList<>();
    
    public void writeReport(TranscriptSummary smry, PrintWriter out ) {
        createRows(smry);
        createColumns(smry);
        
          // headers
        out.println(
            columns.stream().map( c -> c.getName() ).collect( joining("\t") )
        );
        
        // rows
        rows.stream()
            .map( id -> columns.stream()
                               .map(c->c.getValue(id))
                               .map(v-> (v!=null) ? v : "" )
                               .collect(joining("\t")) )
            .forEach( out::println );
    }
    
    private void createRows( TranscriptSummary smry ) {
        rows.clear();
        
        smry.getModel().getSpaceRoot().accept(new AbstractSlot.VoidVisitor(){
            
            LinkedList<String> stack = new LinkedList<>();
            
            @Override
            public void visitAtomicSlotImpl(AtomicSlot t) {
                stack.add(t.getName());
                addRow();
                stack.removeLast();
            }

            @Override
            public void visitAggregateSlotImpl(AggregateSlot t) {
                stack.add(t.getName());
                addRow();
                stack.removeLast();
            }

            @Override
            public void visitCompoundSlotImpl(CompoundSlot t) {
                stack.add(t.getName());
                t.getSubSlots().forEach( slt -> slt.accept(this) );
                stack.removeLast();
            }

            @Override
            public void visitTodoSlotImpl(ToDoSlot t) {}
            
            void addRow() {
                rows.add( stack.stream().collect(joining("/")) );
            }
        });
        
        
        
    }
    
    private void createColumns( TranscriptSummary smry ) {
        columns.clear();
        
        columns.add( new CounterColumn("row #") );
        columns.add( new RowKeyColumn("slot") );
        
        smry.getSortedNames().stream().map( n -> smry.get(n) )
            .forEach( tspt -> {
                columns.add( new CoordinateColumn.TextValue(tspt, tspt.getName() + " Text") );
                columns.add( new CoordinateColumn.OrdinalValue(tspt, tspt.getName() + " Ordinal") );
            });
    }
}
