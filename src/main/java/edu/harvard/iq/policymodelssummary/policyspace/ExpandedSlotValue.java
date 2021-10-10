package edu.harvard.iq.policymodelssummary.policyspace;

import edu.harvard.iq.policymodels.model.policyspace.slots.AbstractSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.AggregateSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.AtomicSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.CompoundSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.ToDoSlot;
import edu.harvard.iq.policymodels.model.policyspace.values.AggregateValue;
import edu.harvard.iq.policymodels.model.policyspace.values.AtomicValue;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodels.model.policyspace.values.ToDoValue;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Value in the expended policy model space. This can be an {@link AtomicValue},
 * or a {@link Boolean} value, when the slot is an {@link AggregateSlot}.
 * 
 * This is because {@code AggregateSlot}s are really a grouping of binary 
 * dimensions.
 * 
 * @author michael
 */
public abstract class ExpandedSlotValue<T> {
    
    public final static ExpandedSlotValue<Void> NOT_FOUND = new ExpandedSlotValue<>(null){
        @Override
        public String nameString() {
            return "(not found)";
        }
        @Override
        public int ordinal() {
            return -1;
        }
    }; 
    
    protected final T value;
    
    protected ExpandedSlotValue( T aValue ) {
        value = aValue;
    }
    
    public T get() {
        return value;
    }
    
    public abstract String nameString();
    public abstract int    ordinal();
    
    public static class Atomic extends ExpandedSlotValue<AtomicValue> {
        public Atomic( AtomicValue av ) {
            super(av);
        }

        @Override
        public String nameString() {
            return (get()!=null) ? get().getName() : "";
        }

        @Override
        public int ordinal() {
            return (get()!=null) ? get().getOrdinal() : -1;
        }
        
    }
    
    public static class Flag extends ExpandedSlotValue<Boolean> {
        
        public static final Flag YES = new Flag(true);
        public static final Flag NO  = new Flag(false);
        
        public static Flag forValue( boolean b ) {
            return b ? YES : NO;
        }
        
        private Flag( Boolean value ) {
            super(value);
        }
        
        @Override
        public String nameString() {
            return get() ? "yes" : "no";
        }
        
        @Override
        public int ordinal() {
            return get() ? 1 : 0;
        }
    }
    
    public static class ToDo extends ExpandedSlotValue<ToDoValue>{
        public ToDo(ToDoValue aValue) {
            super(aValue);
        }
        
        @Override
        public String nameString() {
            return "(not implemented)";
        }
        @Override
        public int ordinal() {
            return -2;
        }
    };
    
    public static ExpandedSlotValue<?> lookup( CompoundValue root, String path ) {
        String[] comps = path.split("/");
        return lookup(root, Arrays.asList(comps) );
    }
    
    public static ExpandedSlotValue<?> lookup( CompoundValue root, List<String> path ) {
        CompoundSlot rootSlot = root.getSlot();
        AbstractSlot nextSlot = rootSlot.getSubSlot(path.get(0));
        
        if ( nextSlot == null ) return NOT_FOUND;
        if ( nextSlot instanceof AtomicSlot ) return path.size()==1 ? new Atomic((AtomicValue)root.get(nextSlot)) : NOT_FOUND;
        if ( nextSlot instanceof AggregateSlot ) {
            if ( path.size() != 2 ) return NOT_FOUND;
            String expandedSlotName = path.get(1);
            AggregateSlot agSlt = (AggregateSlot) nextSlot;
            try {
                AtomicValue boolSlot = agSlt.getItemType().valueOf(expandedSlotName);
                AggregateValue aggValue = (AggregateValue) root.get(nextSlot);
                return ( aggValue == null ) 
                    ? Flag.forValue(false)
                    : Flag.forValue(aggValue.getValues().contains(boolSlot));
                
            } catch ( IllegalArgumentException _iex ) {
                return NOT_FOUND;
            }
        }
        if ( nextSlot instanceof ToDoSlot ) {
            return new ToDo((ToDoValue) root.get(nextSlot));
        }
        if ( nextSlot instanceof CompoundSlot ) {
            return lookup((CompoundValue) root.get(nextSlot), path.subList(1, path.size()));
        }
        throw new IllegalArgumentException("Encountered a value of an unkown type: " + root );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.value);
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
        final ExpandedSlotValue<?> other = (ExpandedSlotValue<?>) obj;
        return Objects.equals(this.value, other.value);
    }
    
    @Override
    public String toString(){
        return "[ExpandedSlotValue: " + nameString() + "]";
    }
}
