/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.mjdivan.composedindex;

import java.util.concurrent.ConcurrentHashMap;
import org.ciedayap.ipd.exception.ProcessingException;

/**
 * This class implements a Runnable interface for Multi-thread computing of the composed index
 * @author mjdivan
 * @version 1.0 
 */
public class IndexEstimator implements Runnable{
    private ConcurrentHashMap<String,Node> projects;
    private ComposedSimilarityTriangularMatrix matrix;
    private double alfa;
    private double beta;
    private double gama;
    private double delta;
    private double w;
    
    /**
     * It creates a new instance for estimating the composed index
     * @param projs The set of projects
     * @param m The unidimensional array related to the triangular matrix
     * @param palfa The alfa parameter. The relative importance of entity states against transitions. A value between 0 and 1.
     * @param pbeta The beta parameter. The relative importance of entities against states. A value between 0 and 1.
     * @param pgama The gama parameter. The relative importance of scenarios against transitions. A value between 0 and 1.
     * @param pdelta The delta parameter. The relative importance of contexts against scenarios. A value between 0 and 1.
     * @param pw The w parameter. The relative importance of the internal against external distances. A value between 0 and 1. 
     * @throws ProcessingException It is raised when some parameters (i.e., alfa, beta, gamma, delta, or w) are out of range [0; 1]
     */
    public IndexEstimator(ConcurrentHashMap<String,Node> projs,ComposedSimilarityTriangularMatrix m,
            double palfa, double pbeta, double pgama, double pdelta, double pw) throws ProcessingException
    {
        if(palfa<0 || palfa>1) throw new ProcessingException("The alfa parameter is out of range [0; 1]");
        if(pbeta<0 || pbeta>1) throw new ProcessingException("The beta parameter is out of range [0; 1]");
        if(pdelta<0 || pdelta>1) throw new ProcessingException("The delta parameter is out of range [0; 1]");
        if(pgama<0 || pgama>1) throw new ProcessingException("The gama parameter is out of range [0; 1]");
        if(pw<0 || pw>1) throw new ProcessingException("The w parameter is out of range [0; 1]");
        if(projs==null) throw new ProcessingException("No projects to be processed");
        if(m==null || !m.isCreated()) throw new ProcessingException("No matrix available");
        
        this.projects=projs;
        this.matrix=m;        
        this.alfa=palfa;
        this.beta=pbeta;
        this.gama=pgama;
        this.delta=pdelta;
        this.w=pw;
    }

    /**
     * A factory method to create a new instance
     * @param projs The set of projects
     * @param m The unidimensional array related to the triangular matrix
     * @param palfa The alfa parameter. The relative importance of entity states against transitions. A value between 0 and 1.
     * @param pbeta The beta parameter. The relative importance of entities against states. A value between 0 and 1.
     * @param pgama The gama parameter. The relative importance of scenarios against transitions. A value between 0 and 1.
     * @param pdelta The delta parameter. The relative importance of contexts against scenarios. A value between 0 and 1.
     * @param pw The w parameter. The relative importance of the internal against external distances. A value between 0 and 1. 
     * @return A new instance to estimate the index
     * @throws ProcessingException It is raised when some parameters (i.e., alfa, beta, gamma, delta, or w) are out of range [0; 1]
     */
    public static synchronized IndexEstimator create(ConcurrentHashMap<String,Node> projs,ComposedSimilarityTriangularMatrix m,
            double palfa, double pbeta, double pgama, double pdelta, double pw) throws ProcessingException
    {
        return new IndexEstimator(projs, m, palfa, pbeta, pgama, pdelta, pw);
    }
    
    @Override
    public void run() {
        if(matrix!=null && projects!=null)
        {
            ComposedSimilarityNode item=matrix.nextCurrentThreadElement();
            while(item!=null)
            {
                boolean ret=false;
                try {
                    ret= ComposedIndex.composedDistance(projects, item, alfa,beta,gama,delta,w);
                } catch (ProcessingException ex) {
                    ret=false;
                }

                item=matrix.nextCurrentThreadElement();
            }
        }
        else
        {
            System.out.println("No matrix or projects to be processed");
        }
    }    
}
