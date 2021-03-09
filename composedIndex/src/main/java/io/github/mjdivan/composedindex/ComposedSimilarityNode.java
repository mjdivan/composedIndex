/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.mjdivan.composedindex;

import org.ciedayap.ipd.exception.ProcessingException;
import org.ciedayap.utils.StringUtils;

/**
 *
 * @author mjdivan
 */
public class ComposedSimilarityNode {
    /**
     * The project ID associated with the matrix row
     */
    private String projectID_row;    
    /**
     * The project ID associated with the matrix column
     */
    private String projectID_col;
    /**
     * The structural similarity between a pair of entities (Equation 1)
     */
    private double eq01_sim_str_ent;
    /**
     * The structural similarity between entity states (Equation 2)
     */
    private double eq02_sim_sc_st;
    /**
     * Internal structural distance (See equation 3)
     */
    private double eq03_idist_str;
    /**
     * The structural similarity between a pair of contexts (See equation 4)
     */
    private double eq04_sim_ctx;
    /**
     * The structural similarity between scenarios (See equation 5)
     */
    private double eq05_sim_str_sc;
    /**
     * External structural distance (See equation 6)
     */
    private double eq06_edist_str;
    /**
     * The internal behavioral distance (See equation 10)
     */
    private double idist_beh;
    /**
     * The external behavioral distance (See equation 13)
     */
    private double edist_beh;
    /**
     * The internal distance (See equation 14)
     */
    private double idist;
    /**
     * The external distance (See equation 15)
     */
    private double edist;
    /**
     * The composed distance (See equation 16)
     */
    private double cdist;

    public ComposedSimilarityNode()
    {
        
    }
    
    /**
     * It creates a new instance indicating the projects ID
     * @param prj_row The projectID located at the row
     * @param prj_col The projectID located at the col
     * @throws ProcessingException It is raised when some IDs is null or empty
     */
    public ComposedSimilarityNode(String prj_row, String prj_col) throws ProcessingException
    {
        if(StringUtils.isEmpty(prj_row) || StringUtils.isEmpty(prj_col)) throw new ProcessingException("Row or Col ProjectID is null or empty");        
        this.projectID_row=prj_row;
        this.projectID_col=prj_col;
    }
    
    @Override
    public ComposedSimilarityNode clone() throws CloneNotSupportedException
    {
        super.clone();
        
        ComposedSimilarityNode sec=new ComposedSimilarityNode();
        sec.cdist=this.cdist;
        sec.edist=this.edist;
        sec.edist_beh=this.edist_beh;
        sec.eq01_sim_str_ent=this.eq01_sim_str_ent;
        sec.eq02_sim_sc_st=this.eq02_sim_sc_st;
        sec.eq03_idist_str=this.eq03_idist_str;
        sec.eq04_sim_ctx=this.eq04_sim_ctx;
        sec.eq05_sim_str_sc=this.eq05_sim_str_sc;
        sec.eq06_edist_str=this.eq06_edist_str;
        sec.idist=this.idist;
        sec.idist_beh=this.idist_beh;
        sec.projectID_col=this.projectID_col;
        sec.projectID_row=this.projectID_row;
        
        return sec;
    }
    
    /**
     * Factory method to create a new instance, indicating a pair of project IDs
     * @param prjRow The project ID row
     * @param prjCol The project ID col 
     * @return A new instance is created incorporating the indicated IDs
     * @throws ProcessingException It is raised when some ID is null or empty
     */
    public synchronized static ComposedSimilarityNode create(String prjRow,String prjCol) throws ProcessingException
    {
        if(StringUtils.isEmpty(prjRow) || StringUtils.isEmpty(prjCol)) throw new ProcessingException("Row or Col ProjectID is null or empty");
        
        return new ComposedSimilarityNode(prjRow,prjCol);
        
    }
    /**
     * @return the projectID_row
     */
    public String getProjectID_row() {
        return projectID_row;
    }

    /**
     * @param projectID_row the projectID_row to set
     */
    public void setProjectID_row(String projectID_row) {
        this.projectID_row = projectID_row;
    }

    /**
     * @return the projectID_col
     */
    public String getProjectID_col() {
        return projectID_col;
    }

    /**
     * @param projectID_col the projectID_col to set
     */
    public void setProjectID_col(String projectID_col) {
        this.projectID_col = projectID_col;
    }

    /**
     * @return the eq01_sim_str_ent
     */
    public double getEq01_sim_str_ent() {
        return eq01_sim_str_ent;
    }

    /**
     * @param eq01_sim_str_ent the eq01_sim_str_ent to set
     */
    public void setEq01_sim_str_ent(double eq01_sim_str_ent) {
        this.eq01_sim_str_ent = eq01_sim_str_ent;
    }

    /**
     * @return the eq02_sim_sc_st
     */
    public double getEq02_sim_sc_st() {
        return eq02_sim_sc_st;
    }

    /**
     * @param eq02_sim_sc_st the eq02_sim_sc_st to set
     */
    public void setEq02_sim_sc_st(double eq02_sim_sc_st) {
        this.eq02_sim_sc_st = eq02_sim_sc_st;
    }

    /**
     * @return the eq03_idist_str
     */
    public double getEq03_idist_str() {
        return eq03_idist_str;
    }

    /**
     * @param eq03_idist_str the eq03_idist_str to set
     */
    public void setEq03_idist_str(double eq03_idist_str) {
        this.eq03_idist_str = eq03_idist_str;
    }

    /**
     * @return the eq04_sim_ctx
     */
    public double getEq04_sim_ctx() {
        return eq04_sim_ctx;
    }

    /**
     * @param eq04_sim_ctx the eq04_sim_ctx to set
     */
    public void setEq04_sim_ctx(double eq04_sim_ctx) {
        this.eq04_sim_ctx = eq04_sim_ctx;
    }

    /**
     * @return the eq05_sim_str_sc
     */
    public double getEq05_sim_str_sc() {
        return eq05_sim_str_sc;
    }

    /**
     * @param eq05_sim_str_sc the eq05_sim_str_sc to set
     */
    public void setEq05_sim_str_sc(double eq05_sim_str_sc) {
        this.eq05_sim_str_sc = eq05_sim_str_sc;
    }

    /**
     * @return the eq06_edist_str
     */
    public double getEq06_edist_str() {
        return eq06_edist_str;
    }

    /**
     * @param eq06_edist_str the eq06_edist_str to set
     */
    public void setEq06_edist_str(double eq06_edist_str) {
        this.eq06_edist_str = eq06_edist_str;
    }

    /**
     * @return the idist_beh
     */
    public double getIdist_beh() {
        return idist_beh;
    }

    /**
     * @param idist_beh the idist_beh to set
     */
    public void setIdist_beh(double idist_beh) {
        this.idist_beh = idist_beh;
    }

    /**
     * @return the edist_beh
     */
    public double getEdist_beh() {
        return edist_beh;
    }

    /**
     * @param edist_beh the edist_beh to set
     */
    public void setEdist_beh(double edist_beh) {
        this.edist_beh = edist_beh;
    }

    /**
     * @return the idist
     */
    public double getIdist() {
        return idist;
    }

    /**
     * @param idist the idist to set
     */
    public void setIdist(double idist) {
        this.idist = idist;
    }

    /**
     * @return the edist
     */
    public double getEdist() {
        return edist;
    }

    /**
     * @param edist the edist to set
     */
    public void setEdist(double edist) {
        this.edist = edist;
    }

    /**
     * @return the cdist
     */
    public double getCdist() {
        return cdist;
    }

    /**
     * @param cdist the cdist to set
     */
    public void setCdist(double cdist) {
        this.cdist = cdist;
    }
    
}
