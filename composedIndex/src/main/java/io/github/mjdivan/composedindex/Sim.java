/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.mjdivan.composedindex;

import io.github.mjdivan.composedindex.utils.InstrumentationAgent;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Scanner;
import org.ciedayap.ipd.IPD;
import org.ciedayap.ipd.exception.ProcessingException;
import org.ciedayap.ipd.utils.Sample;

/**
 *
 * @author mjdivan
 */
public class Sim {
    public static void main(String args[]) throws ProcessingException, Exception
    {                
        InstrumentationAgent ia=new InstrumentationAgent();
        Scanner reader=new Scanner(System.in);
        int opt=0;
        ArrayList<ArrayList> list;
        
        do{
            System.out.println("Simulation options:");
            System.out.println("\t1. Individual operation rate with a constant number of projects for 10 minutes with 10 projects]");
            System.out.println("\t2. Evolution of the individual operation rate when the number of projects varies [from 10 to 200 projects]");
            System.out.println("Enter your choice [1-2](Exit with 0): ");
            opt=reader.nextInt();
        
            System.out.println("Your choice: "+opt);
            switch(opt)
            {                
                case 0:
                    System.out.println("Bye bye...");
                    break;
                case 1:
                    IPD message=Sample.generateIPDMessage(1, 10);
                    list=sim1_individualOps(ia,10,message);//Minutes: 
                    Sample.store("/Users/mjdivan/Downloads/ci_simulation1.txt", list, ";");
                    break;
                case 2:
                    list=Sim.sim2_varProjects(ia, 200);
                    Sample.store("/Users/mjdivan/Downloads/ci_simulation2.txt", list, ";");
                    break;
                default:
                    System.out.println("Wrong option!");
                    Thread.sleep(1000);
            }
        }while(opt!=0);        
    }
    
    public static ArrayList sim1_individualOps(InstrumentationAgent meter,int minutes, IPD message) throws ProcessingException, Exception
    {
        if(minutes<1) throw new ProcessingException("Simulation time must be higher or equal to 1");
        if(message==null) throw new ProcessingException("The number of projects must be higher or equal to 2");        
        
        ArrayList results=new ArrayList();
        ArrayList titles=new ArrayList();
        titles.add("#Projects");
        titles.add("Datetime");
        titles.add("Elapsed (ns)");
        titles.add("Matrix Creation time (ns)");
        titles.add("Distances Computation time (ns)");
        titles.add("Matrix Size (bytes)");
        results.add(titles);
        
        //Generating the project definition
        System.out.println("Starting simulation 1. ["+ZonedDateTime.now()+"]...");
        System.out.println("Estimating Individual Operations time with a message with "+message.getProjects().length()+
                " projects for "+minutes+" minutes");
        long start=System.nanoTime();
        while((System.nanoTime()-start)<=(minutes*60000000000L))
        {                        
            results.add(Sim.measureIndividualTime(meter,start, message, false)); 
        }
        
        System.out.println("Finishing simulation 1");        
        return results;
    }
    
    public static ArrayList sim2_varProjects(InstrumentationAgent meter, int maxNofProjects) throws ProcessingException, Exception
    {
        if(maxNofProjects<10) throw new ProcessingException("The number of projects must be higher or equal to 10");        

        System.out.println("Individual Times - To Vary the Number of projects between [1;"+maxNofProjects+"]");
        System.out.println("Starting simulation 2 ["+ZonedDateTime.now()+"]...");


        
        ArrayList results=new ArrayList();
        ArrayList titles=new ArrayList();
        titles.add("#Projects");
        titles.add("Datetime");
        titles.add("Elapsed (ns)");
        titles.add("Matrix Creation time (ns)");
        titles.add("Distances Computation time (ns)");
        titles.add("Matrix Size (bytes)");
        results.add(titles);
        
        long start=System.nanoTime();
        for(int i=10;i<=maxNofProjects;i+=10)
        {                 
            System.out.println("["+i+"] "+ZonedDateTime.now());
            IPD message=Sample.generateIPDMessage(i,i);//ID, #Projects

            if(i==10)
                results.add(Sim.measureIndividualTime(meter,start, message, true));            
            else
                results.add(Sim.measureIndividualTime(meter,start, message, false));            
            
            message.realeaseResources();
            System.gc();
            Thread.sleep(1000);            
        }
        
        System.out.println("Finishing simulation 2");        
        return results;
    }
    
    /**
     * It measres the individual time for creating the instance, calculating distances, and estimating the memory sizes involved.
     * @param meter Instrumentation Agent
     * @param start Simulation begin
     * @param message The message to be analyzed
     * @param show It indicates whether or not the matrix is shown through the console.
     * @return An array with the measures in the following order: #Projects, Datetime, Elapsed Time (ns), Matrix creation time (ns),
     * Distances computation time (ns), and matrix sizes
     * @throws ProcessingException It is raised when some inconvenient occures in the message creation
     * @throws Exception It is raised when memory sizes can not be measured
     */
    public static ArrayList measureIndividualTime(InstrumentationAgent meter,long start,IPD message,boolean show) throws ProcessingException, Exception
    {
        long before,after;        
        
        ArrayList record=new ArrayList();
        
        if(!message.isDefinedProperties())
        {
            throw new ProcessingException("The message has not been created properly");
        }
        
        //#Projects
        record.add(message.getProjects().length());

        //Time
        record.add(ZonedDateTime.now());
        record.add(System.nanoTime()-start);

        //Matrix Creation
        before=System.nanoTime();
        ComposedIndex ci=new ComposedIndex(message);
        after=System.nanoTime();
        record.add(after-before);//Matrix Generation(ns);
        
        //Filling matrix with andom values
        ci.fillBehavioralValues(false);

        //Distance computation Time
        before=System.nanoTime();
        ci.updateEstimationOfComposedIndex();
        after=System.nanoTime();        
        record.add(after-before);//Distance Computation Time

        //Matrix Size
        record.add(meter.sizeDeepOf(ci));//BriefPD decompressionTime (ns)

        if(show)
        {
            ci.showProjects();
        }
        
        ci.releaseResources();
        ci=null;

        return record;             
    }
    
}
