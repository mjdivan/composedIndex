/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.mjdivan.composedindex;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.ciedayap.ipd.MeasurementProject;
import org.ciedayap.ipd.context.ContextProperties;
import org.ciedayap.ipd.context.ContextProperty;
import org.ciedayap.ipd.exception.ProcessingException;
import org.ciedayap.ipd.requirements.Attribute;
import org.ciedayap.ipd.requirements.Attributes;

/**
 * It contains the statistical information for a given project to be used in the behavioral similarity
 * @author mjdivan
 */
public class BehavioralNode {
    private ArrayList<BehavioralItemNode> attributes;
    private ArrayList<BehavioralItemNode> contextProperties;
    
    public BehavioralNode()
    {
        attributes=null;
        contextProperties=null;
    }
    
    /**
     * It creates a new instance based on a given measurement project definition
     * @param mp The measurement project to be considered
     * @throws ProcessingException It is raised when the project definition is incomplete or not accessible
     */
    public BehavioralNode(MeasurementProject mp) throws ProcessingException
    {
        if(mp==null) throw new ProcessingException("The Measurement Project is null");
        if(!mp.isDefinedProperties()) throw new ProcessingException("[MeasurementProject] Incomplete instance");
        
        if(mp.getInfneed()==null || mp.getInfneed().getEntityCategory()==null ||
                mp.getInfneed().getEntityCategory().getAttributes()==null ||
                mp.getInfneed().getEntityCategory().getAttributes().getAttributes()==null )
                    throw new ProcessingException("[MeasurementProject] Attributes are not accessible");
                
        Attributes atList=mp.getInfneed().getEntityCategory().getAttributes();
        attributes=new ArrayList(atList.getAttributes().size());        
        
        for(Attribute at:atList.getAttributes())
        {
            attributes.add(BehavioralItemNode.create(mp.getID(), at.getID(), true, null, null, null));
        }
        
        if(mp.getInfneed().getContext()!=null)
        {//The context definition is optional
            if(mp.getInfneed().getContext().getContextProperties()==null ||
                    mp.getInfneed().getContext().getContextProperties().getContextProperties()==null)
            {
                throw new ProcessingException("[MeasurementProject] Context properties are not accessible but they seem to be defined");
            }
            
            ContextProperties cpList=mp.getInfneed().getContext().getContextProperties(); 
            contextProperties=new ArrayList(cpList.getContextProperties().size());
            for(ContextProperty cp:cpList.getContextProperties())
            {
                contextProperties.add(BehavioralItemNode.create(mp.getID(), cp.getID(), false, null, null, null));
            }
        }
    }

    /**
     * It creates an instance based on a measurement project definition
     * @param mp The project definition to be considered
     * @return A new instance containing the statistic information with a null. It must be completed after the creation. 
     * @throws ProcessingException It is raised when the project definition is incomplete or not accessible
     */
    public static synchronized BehavioralNode create(MeasurementProject mp) throws ProcessingException
    {
        return new BehavioralNode(mp);
    }
    
    /**
     * @return the attributes
     */
    public ArrayList<BehavioralItemNode> getAttributes() {
        return attributes;
    }

    /**
     * It returns the same attributes under a ConcurrentHashMap organized by the attribute ID
     * @return A ConcurrentHashMap organized by AttributeID as key
     */    
    public ConcurrentHashMap<String,BehavioralItemNode> getAttributesAsHashMap()
    {
        if(attributes==null || attributes.isEmpty()) return null;
    
        ConcurrentHashMap<String,BehavioralItemNode> map=new ConcurrentHashMap();
        for(BehavioralItemNode mynode:attributes)
        {
            map.put(mynode.getAttributeID(), mynode);
        }
        
        return map;
    }

    
    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(ArrayList<BehavioralItemNode> attributes) {
        this.attributes = attributes;
    }

    /**
     * @return the contextProperties
     */
    public ArrayList<BehavioralItemNode> getContextProperties() {
        return contextProperties;
    }

    /**
     * It returns the same attributes under a ConcurrentHashMap organized by the attribute ID
     * @return A ConcurrentHashMap organized by AttributeID as key
     */    
    public ConcurrentHashMap<String,BehavioralItemNode> getContextPropertiesAsHashMap()
    {
        if(contextProperties==null || contextProperties.isEmpty()) return null;
        
        ConcurrentHashMap<String,BehavioralItemNode> map=new ConcurrentHashMap();
        for(BehavioralItemNode mynode:contextProperties)
        {
            map.put(mynode.getAttributeID(), mynode);
        }
        
        return map;
    }
    
    /**
     * @param contextProperties the contextProperties to set
     */
    public void setContextProperties(ArrayList<BehavioralItemNode> contextProperties) {
        this.contextProperties = contextProperties;
    }
}
