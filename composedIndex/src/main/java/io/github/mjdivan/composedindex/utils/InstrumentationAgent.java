/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.mjdivan.composedindex.utils;

import java.lang.instrument.Instrumentation;
import org.github.jamm.MemoryMeter;

/**
 *
 * @author mjdivan
 */
public class InstrumentationAgent {
 private MemoryMeter meter;
    
    public InstrumentationAgent()
    {
        meter=new MemoryMeter();
    }
    
    private static volatile Instrumentation globalInstrumentation;
 
    public static void premain(final String agentArgs, final Instrumentation inst) {
        globalInstrumentation = inst;
    }
 
    public static long getObjectSize(final Object object) {
        if (globalInstrumentation == null) {
            throw new IllegalStateException("Agent not initialized.");
        }
        return globalInstrumentation.getObjectSize(object);
    }    
    
    public long sizeOf(final Object object)
    {
        return meter.measure(object);
    }
    
    public long sizeDeepOf(final Object object)
    {
        return meter.measureDeep(object);
    }    
}
