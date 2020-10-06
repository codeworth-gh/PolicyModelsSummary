/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.policymodelssummary.reports;

import edu.harvard.iq.policymodels.model.policyspace.slots.AggregateSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.AtomicSlot;
import edu.harvard.iq.policymodels.model.policyspace.slots.CompoundSlot;
import edu.harvard.iq.policymodels.model.policyspace.values.AggregateValue;
import edu.harvard.iq.policymodels.model.policyspace.values.CompoundValue;
import edu.harvard.iq.policymodelssummary.Transcript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;

/**
 *
 * @author michael
 */
public class CoordinateColumnTest {
    
    static CompoundSlot policyModelRoot;
    static AtomicSlot otf;
    static AtomicSlot ott;
    static AggregateSlot flgs;
    
    @BeforeAll
    public static void setUpClass() {
        otf = new AtomicSlot("oneToFive","");
        otf.registerValue("one", "");
        otf.registerValue("two", "");
        otf.registerValue("three", "");
        otf.registerValue("four", "");
        otf.registerValue("five", "");
        
        ott = new AtomicSlot("oneToTen","");
        ott.registerValue("one", "");
        ott.registerValue("two", "");
        ott.registerValue("three", "");
        ott.registerValue("four", "");
        ott.registerValue("five", "");
        ott.registerValue("six", "");
        ott.registerValue("seven", "");
        ott.registerValue("eight", "");
        ott.registerValue("nine", "");
        ott.registerValue("ten", "");
        
        flgs = new AggregateSlot("flags","", new AtomicSlot("flags_item",""));
        flgs.getItemType().registerValue("flag", "");
        
        policyModelRoot = new CompoundSlot("root","");
        policyModelRoot.addSubSlot(ott);
        policyModelRoot.addSubSlot(otf);
        policyModelRoot.addSubSlot(flgs);
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of getSlotValue method, of class CoordinateColumn.
     */
    @org.junit.jupiter.api.Test
    public void testScaledValueAtomic_MaxVals() {
        
        CompoundValue result = policyModelRoot.createInstance();
        
        result.put( otf.valueOf("five") );
        result.put( ott.valueOf("ten") );
        AggregateValue flagsVal = flgs.createInstance();
        flagsVal.add(flgs.getItemType().valueOf("flag"));
        result.put(flagsVal);
        
        Transcript mockTspt = new Transcript();
        mockTspt.setCoordinate(result);
        
        CoordinateColumn.ScaledValue sut = new CoordinateColumn.ScaledValue(mockTspt, "MAX VALUES");
         
        assertEquals("10.0000", sut.getValue("oneToTen"));
        assertEquals("10.0000", sut.getValue("oneToFive"));
        assertEquals("10", sut.getValue("flags/flag"));
    }

    @org.junit.jupiter.api.Test
    public void testScaledValueAtomic_MinVals() {
        
        CompoundValue result = policyModelRoot.createInstance();
        
        result.put( otf.valueOf("one") );
        result.put( ott.valueOf("one") );
        AggregateValue flagsVal = flgs.createInstance();
        result.put(flagsVal);
        
        Transcript mockTspt = new Transcript();
        mockTspt.setCoordinate(result);
        
        CoordinateColumn.ScaledValue sut = new CoordinateColumn.ScaledValue(mockTspt, "MIN VALUES");
         
        assertEquals("1.0000", sut.getValue("oneToTen"));
        assertEquals("1.0000", sut.getValue("oneToFive"));
        assertEquals("1", sut.getValue("flags/flag"));
    }
    
}
