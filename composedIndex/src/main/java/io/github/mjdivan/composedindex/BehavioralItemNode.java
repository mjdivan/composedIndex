/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.mjdivan.composedindex;

import org.ciedayap.ipd.exception.ProcessingException;
import org.ciedayap.ipd.utils.StringUtils;

/**
 * It contains the basic statistic used for the behavioral distances
 * @author mjdivan
 */
public class BehavioralItemNode {
    /**
     * The project ID associated with the attributeID
     */
    private String projectID;
    /**
     * The attributeID (or contextpropertyID) depending on the value of the isContextProperty variable
     */
    private String attributeID;
    /**
     * It indicates whether the attributeID is an attribute or context property
     */
    private boolean isContextProperty;
    /**
     * The estimated sample arithmetic mean for the attributeID in the projectID
     */
    private Double mean;
    /**
     * The number of elements in the sample used for estimating the arithmetic mean and variance
     */
    private Integer n;
    /**
     * The estimated sample variance for the attributeID in the projectID
     */
    private Double var; 

    public BehavioralItemNode()
    {
        
    }
    
    /**
     * 
     * @param prjID The projectID
     * @param attID The attributeID corresponding to the project
     * @param isatt TRUE indicates an attribute, while FALSE indicates a context property
     * @param mean The estimated sample arithmetic mean
     * @param var The estimated sample variance
     * @param n The number of elements used in the sample
     * @return A new instance containing statistic details about the attribute in the project
     * @throws org.ciedayap.ipd.exception.ProcessingException It is raised when no project and attribute is identified, or when the number of items is lower than 1
     */
    public synchronized static BehavioralItemNode create(String prjID,String attID, boolean isatt,Double mean, Double var, Integer n) throws ProcessingException
    {
        if(StringUtils.isNull(attID) || StringUtils.isNull(prjID))
            throw new ProcessingException("The project or attribute are not identified");
        if(n!=null && n<1)
            throw new ProcessingException("The n parameter must be higher or equal to one");
        
        BehavioralItemNode bin=new BehavioralItemNode();
        if(bin==null) return null;
        bin.setProjectID(prjID);
        bin.setAttributeID(attID);
        bin.setN(n);
        bin.setMean(mean);
        bin.setVar(var);
        
        return bin;        
    }
    
    /**
     * @return the projectID
     */
    public String getProjectID() {
        return projectID;
    }

    /**
     * @param projectID the projectID to set
     */
    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    /**
     * @return the attributeID
     */
    public String getAttributeID() {
        return attributeID;
    }

    /**
     * @param attributeID the attributeID to set
     */
    public void setAttributeID(String attributeID) {
        this.attributeID = attributeID;
    }

    /**
     * @return the isContextProperty
     */
    public boolean isIsContextProperty() {
        return isContextProperty;
    }

    /**
     * @param isContextProperty the isContextProperty to set
     */
    public void setIsContextProperty(boolean isContextProperty) {
        this.isContextProperty = isContextProperty;
    }

    /**
     * @return the mean
     */
    public Double getMean() {
        return mean;
    }

    /**
     * @param mean the mean to set
     */
    public void setMean(Double mean) {
        this.mean = mean;
    }

    /**
     * @return the n
     */
    public Integer getN() {
        return n;
    }

    /**
     * @param n the n to set
     * @throws org.ciedayap.ipd.exception.ProcessingException it is raised when n is lower than one
     */
    public void setN(Integer n) throws ProcessingException 
    {
        if(n!=null && n<1) throw new ProcessingException("n must be higher or equal to one");
        
        this.n = n;
    }

    /**
     * @return the var
     */
    public Double getVar() {
        return var;
    }

    /**
     * @param var the var to set
     */
    public void setVar(Double var) {
        if(var==null || var<0) return;
        this.var = var;
    }

}
