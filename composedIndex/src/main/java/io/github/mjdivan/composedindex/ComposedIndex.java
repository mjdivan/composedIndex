/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.mjdivan.composedindex;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.ciedayap.ipd.IPD;
import org.ciedayap.ipd.MeasurementProject;
import org.ciedayap.ipd.exception.ProcessingException;
import org.ciedayap.ipd.states.ECState;
import org.ciedayap.ipd.states.Scenario;
import org.ciedayap.ipd.states.StateTransition;
import org.ciedayap.ipd.utils.StringUtils;

/**
 * It is responible for computing the composed index based on a BriefPD message
 * 
 * @author mjdivan
 */
public class ComposedIndex{
   /**
     * The original IPD message
    */
   private IPD message;
   /**
    * A pre-processed local cache based on the IPD message to make easy the similarity calculus. It is organized by projectID.
    */
   private ConcurrentHashMap<String,Node> projects;
   /**
    * A triangular matrix containing the detail about the similarity calculus
    */
   private ComposedSimilarityTriangularMatrix matrix;
   /**
    * It represents the relative importance between states and transitions (See equations 2 and 3)
    */
   private double alfa=0.5;
   /**
    * It represents the relative importance between the entity and states (See equation 3)
    */
   private double beta=0.5;
   /**
    * It indicates the relative importance of the context against the defined scenarios (See equations 5 and 6)
    */
   private double gama=0.5; 
   /**
    * It indicates the relative importance between the context and scenarios (See equation 6)
    */
   private double delta=0.5;
   /**
    * It indicates the relative importance between the internal and the external distances (See equation 15)
    */
   private double w=0.5;

   /**
    * The constructor creates the instance based on a BriefPD message expressed as a string.
    */   
   public ComposedIndex()
   {
       
   }
   
   /**
    * The constructor creates the instance based on a BriefPD message expressed as a string.
    * @param briefpd
    * @throws ProcessingException 
    */
   public ComposedIndex(String briefpd) throws ProcessingException
   {
        if(StringUtils.isNull(briefpd)) throw new ProcessingException("The indicated briefPD message is null");
        IPD pmessage=IPD.readFromSt(briefpd);
        
        if(pmessage==null) throw new ProcessingException("No projects to be processed");
        if(pmessage.getProjects()==null || pmessage.getProjects().length()<=1)
            throw new ProcessingException("There not exist enough projects to compare (Lenght: "+((pmessage.getProjects()==null?0:pmessage.getProjects().length()))+")");

        setMessage(pmessage);
   }
   
    /**
     * The Constructor creates the instance based on an IPD instance
     * @param pmessage The IPD instance (BriefPD message)
     * @throws ProcessingException It is raised when there is not a message, or when there is a project in the message.
     */
    public ComposedIndex(IPD pmessage) throws ProcessingException
   {
        if(pmessage==null) throw new ProcessingException("No projects to be processed");
        if(pmessage.getProjects()==null || pmessage.getProjects().length()<=1)
            throw new ProcessingException("There not exist enough projects to compare (Lenght: "+((pmessage.getProjects()==null?0:pmessage.getProjects().length()))+")");

        setMessage(pmessage);
   }

    /**
     * @return the IPD message
     */
    public IPD getMessage() {
        return message;
    }

    /**
     * It changes the IPD message when it is possible
     * @param pmessage the IPD message to set
     * @throws org.ciedayap.ipd.exception.ProcessingException It is raised when there is not a message, or when there is a project in the message.
     */
    public final synchronized void setMessage(IPD pmessage) throws ProcessingException {
        if(pmessage==null) throw new ProcessingException("No projects to be processed");
        if(pmessage.getProjects()==null || pmessage.getProjects().length()<=1)
            throw new ProcessingException("There not exist enough projects to compare (Lenght: "+((pmessage.getProjects()==null?0:pmessage.getProjects().length()))+")");

        if(message!=null) this.message.realeaseResources();
        
        this.message = pmessage;
        
        if(projects!=null) projects.clear();
        projects=new ConcurrentHashMap(pmessage.getProjects().length());
        
        for(MeasurementProject mp:pmessage.getProjects().getProjects())
        {
            Node node=Node.create(mp);
            if(node!=null)
                projects.put(node.getProject().getID(), node);
        }
        
        if(projects.size()!=pmessage.getProjects().length())
        {
            throw new ProcessingException("[ConcurrentHashMap] there is a divergence in the length between projects and the IPD message");
        }
        
        try{
            if(matrix!=null) matrix.releaseResources();
            
            matrix=ComposedSimilarityTriangularMatrix.createSimilarityTriangularMatrix(projects.size());
            if(!fillMatrix()) throw new Exception("The matrix has not been filled");
        }catch(Exception e)
        {
            throw new ProcessingException("[TriangularMatrix] Problems creating the matrix. Message: "+e.getMessage());
        }
    }        
    
    public final boolean fillBehavioralValues(boolean equal) throws ProcessingException
    {
        if(projects==null) return false;
        Random r=new Random();
        for(Node item:projects.values())
        {
            for(BehavioralItemNode att:item.getBeh().getAttributes())
            {
                //All the same values-It is only for control
                if(equal)
                {
                    att.setN(100);
                    att.setMean(120.0);
                    att.setVar(10.0);
                }
                else
                {
                    att.setN(100);
                    att.setMean(r.nextDouble()*120);
                    att.setVar(10.0);

                    if(att.getAttributeID().startsWith("SBP"))
                    {
                        att.setMean(r.nextGaussian()*120.00);
                        att.setVar(Math.abs(r.nextGaussian())*10.00);
                    }

                    if(att.getAttributeID().startsWith("DBP"))
                    {
                        att.setMean(r.nextGaussian()*75.00);
                        att.setVar(Math.abs(r.nextGaussian())*10.00);
                    }

                    if(att.getAttributeID().startsWith("HB"))
                    {
                        att.setMean(r.nextGaussian()*80.00);
                        att.setVar(Math.abs(r.nextGaussian())*10.00);                            
                    }

                    if(att.getAttributeID().startsWith("CT"))
                    {
                        att.setMean(r.nextGaussian()*30.00);
                        att.setVar(Math.abs(r.nextGaussian())*5.00);                                                            
                    }
                }
            }
            
            
            for(BehavioralItemNode cp:item.getBeh().getContextProperties())
            {
                if(equal)
                {
                    cp.setN(100);
                    cp.setMean(30.0);
                    cp.setVar(5.0);                    
                }
                else
                {
                    cp.setN(100);
                    cp.setMean(r.nextDouble()*30);
                    cp.setVar(5.0);

                    if(cp.getAttributeID().startsWith("CP-HUM"))
                    {
                        cp.setMean(r.nextGaussian()*60.00);
                        cp.setVar(Math.abs(r.nextGaussian())*5.00);                                                            
                    }
                    if(cp.getAttributeID().startsWith("CP-ETEM"))
                    {
                        cp.setMean(r.nextGaussian()*26.00);
                        cp.setVar(Math.abs(r.nextGaussian())*5.00);                                                            
                    }
                    if(cp.getAttributeID().startsWith("CP-EP"))
                    {
                        cp.setMean(r.nextGaussian()*120.00);
                        cp.setVar(Math.abs(r.nextGaussian())*5.00);                                                            
                    }
                }                
            }
            
        }
        
        return true;
    }
    
    public final boolean fillMatrix() throws ProcessingException
    {
        if(matrix==null || projects==null) return false;
        if(!matrix.isCreated()) return false;
        if(matrix.getDim()!=projects.size()) throw new ProcessingException("There is not matching between the matrix dimmension and the nnumber of projects");
        
        ArrayList<Node> items = new ArrayList(projects.values());
        for(int i=0;i<items.size();i++)
           for(int j=i;j<items.size();j++)
           {
               ComposedSimilarityNode mynode=ComposedSimilarityNode.create(items.get(i).getProject().getID(), items.get(j).getProject().getID());
               matrix.set(i, j, mynode);
           }
        
        return true;
    }
    
    /**
     * @return the alfa
     */
    public double getAlfa() {
        return alfa;
    }

    /**
     * @param alfa the alfa to set
     */
    public void setAlfa(double alfa) {
        if(alfa<0 || alfa>1) return;
        this.alfa = alfa;
    }

    /**
     * @return the beta
     */
    public double getBeta() {
        return beta;
    }

    /**
     * @param beta the beta to set
     */
    public void setBeta(double beta) {
        if(beta<0 || beta>1) return;
        this.beta = beta;
    }

    /**
     * @return the gama
     */
    public double getGama() {
        return gama;
    }

    /**
     * @param gama the gama to set
     */
    public void setGama(double gama) {
        if(gama<0 || gama>1) return;
        this.gama = gama;
    }

    /**
     * @return the delta
     */
    public double getDelta() {
        return delta;
    }

    /**
     * @param delta the delta to set
     */
    public void setDelta(double delta) {
        if(delta<0 || delta>1) return;        
        this.delta = delta;
    }

    /**
     * @return the w
     */
    public double getW() {
        return w;
    }

    /**
     * @param w the w to set
     */
    public void setW(double w) {
        this.w = w;
    }
    
    /**
     * It returns the behavioral perspective of attributes and context properties to be updated
     * @param projectID The project ID 
     * @return The behavioral perspective when the project exists, null otherwise
     */
    public BehavioralNode getBehavioralPerspective(String projectID)
    {
        if(StringUtils.isNull(projectID)) return null;
        Node node=projects.get(projectID);
        if(node==null) return null;
        
        return node.getBeh();
    }
    
    
    public void showProjects() throws ProcessingException
    {               
        Collection<Node> col=projects.values();
        Node a,b;
        a=b=null;
        
       DecimalFormat df = new DecimalFormat("#.00");
       String offset=null;
       for(int i=0;i<matrix.getDim();i++)
       {           
           if(offset==null) offset="\t";
           else offset=offset+"\t";
           
           for(int j=i;j<matrix.getDim();j++)
           {
               ComposedSimilarityNode item=matrix.get(i, j);
               if(j==i)
               {
                   System.out.print("\n"+item.getProjectID_row()+offset);
               }
               
               System.out.print(df.format(item.getCdist())+"\t");
           }
       }
       System.out.println();
    }
    
    /**
     * It computes the composed similarity, storing the results in the node instance.It uses the informed attributes of projectID_row and projectID_col to compute the mentioned distance.
     * @param projects Information about the projects metadata loaded from the briefPD message
     * @param node The instance from where the project IDs will be taken and where results will be stored.
     * @param palfa The alfa parameter. The relative importance of entity states against transitions. A value between 0 and 1.
     * @param pbeta The beta parameter. The relative importance of entities against states. A value between 0 and 1.
     * @param pgama The gama parameter. The relative importance of scenarios against transitions. A value between 0 and 1.
     * @param pdelta The delta parameter. The relative importance of contexts against scenarios. A value between 0 and 1.
     * @param pw The w parameter. The relative importance of the internal against external distances. A value between 0 and 1. 
     * @return TRUE when the distance could be computed and stored in the node instance, FALSE otherwise
     * @throws ProcessingException It is raised when the project IDs are not informed, or the node is null (or the IDs are not contained within the instance).
     */
    public static boolean composedDistance(ConcurrentHashMap<String,Node> projects,ComposedSimilarityNode node,
            double palfa,double pbeta,double pgama,double pdelta,double pw) throws ProcessingException
    {
        if(node==null) throw new ProcessingException("The informed node is null");
        if(palfa<0 || palfa>1) throw new ProcessingException("The alfa parameter is out of range [0; 1]");
        if(pbeta<0 || pbeta>1) throw new ProcessingException("The beta parameter is out of range [0; 1]");
        if(pdelta<0 || pdelta>1) throw new ProcessingException("The delta parameter is out of range [0; 1]");
        if(pgama<0 || pgama>1) throw new ProcessingException("The gama parameter is out of range [0; 1]");
        if(pw<0 || pw>1) throw new ProcessingException("The w parameter is out of range [0; 1]");
        
        if(StringUtils.isNull(node.getProjectID_row()) || 
                StringUtils.isNull(node.getProjectID_col())) throw new ProcessingException("There are not enough projects ID to process");
        
       Node prjRow=projects.get(node.getProjectID_row());
       if(prjRow==null) throw new ProcessingException("The projectID "+node.getProjectID_row()+" are not available.");
       
       Node prjCol=projects.get(node.getProjectID_col());
       if(prjCol==null) throw new ProcessingException("The projectID "+node.getProjectID_col()+" are not available.");
       
       //Attributes (Intersection and Union)
       ArrayList<BehavioralItemNode> ent_common_atts=new ArrayList();
       ArrayList<BehavioralItemNode> ent_union_atts=new ArrayList();
       
       parsingEntityAttributes(prjRow,prjCol,ent_common_atts,ent_union_atts);
       if(ent_union_atts.isEmpty()) throw new ProcessingException("No attributes in the union");
       
       double eq01_sim_str_ent=((double)ent_common_atts.size())/((double)ent_union_atts.size());
       node.setEq01_sim_str_ent(eq01_sim_str_ent);
       
       //States
       ArrayList<ECState> ent_common_states=new ArrayList();
       ArrayList<ECState> ent_union_states=new ArrayList();
       ComposedIndex.parsingEntityStates(prjRow, prjCol, ent_common_states, ent_union_states);
              
       //State Transitions
       ArrayList<StateTransition> ent_common_statetransitions=new ArrayList();
       ArrayList<StateTransition> ent_union_statetransitions=new ArrayList();
       ComposedIndex.parsingEntityStateTransitions(prjRow, prjCol, ent_common_statetransitions, ent_union_statetransitions);
       
       double pstates;
       if(ent_union_states.isEmpty())
           pstates=0.0;
       else
           pstates=(((double)ent_common_states.size())/((double)ent_union_states.size()));

       double pstatestransitions;
       if(ent_union_statetransitions.isEmpty())
           pstatestransitions=0.0;
       else
           pstatestransitions=(((double)ent_common_statetransitions.size())/((double)ent_union_statetransitions.size()));

       double eq02_sim_str_sc=(palfa*pstates)+((1-palfa)*pstatestransitions);
       node.setEq02_sim_sc_st(eq02_sim_str_sc);
       
       //Computing the internal structural distance
       double eq03_idist_str=1-(pbeta*eq01_sim_str_ent+((1-pbeta)*eq02_sim_str_sc));
       node.setEq03_idist_str(eq03_idist_str);
       
       //Contexts
       ArrayList<BehavioralItemNode> ent_common_ctx=new ArrayList();
       ArrayList<BehavioralItemNode> ent_union_ctx=new ArrayList();
       parsingContexts(prjRow,prjCol,ent_common_ctx,ent_union_ctx);
        
       double eq04_sim_str_ctx;
       if(ent_union_ctx.isEmpty())
           eq04_sim_str_ctx=0;
       else
           eq04_sim_str_ctx=((double)ent_common_ctx.size())/((double)ent_union_ctx.size());
       node.setEq04_sim_ctx(eq04_sim_str_ctx);
       
       //Scenarios
       ArrayList<Scenario> ent_common_scenarios=new ArrayList();
       ArrayList<Scenario> ent_union_scenarios=new ArrayList();
       parsingScenarios(prjRow,prjCol,ent_common_scenarios,ent_union_scenarios);
       
       double pscenarios;
       if(ent_union_scenarios.isEmpty()) pscenarios=0.0;
       else pscenarios=((double)ent_common_scenarios.size())/((double)ent_union_scenarios.size());
       
       //Scenario Transitions
       ArrayList<StateTransition> ent_common_scenariotransitions=new ArrayList();
       ArrayList<StateTransition> ent_union_scenariotransitions=new ArrayList();       
       parsingScenrioStateTransitions(prjRow,prjCol,ent_common_scenariotransitions,ent_union_scenariotransitions);
       double pscenariosTransitions;
       if(ent_union_scenariotransitions.isEmpty()) pscenariosTransitions=0;
       else pscenariosTransitions=((double)ent_common_scenariotransitions.size())/((double)ent_union_scenariotransitions.size());
       
       double eq05_sim_str_sc=(pgama*pscenarios)+((1-pgama)*pscenariosTransitions);
       node.setEq05_sim_str_sc(eq05_sim_str_sc);
       
       //Computing the external Structural Distance
       double eq06_edist_str=1-((pdelta*eq04_sim_str_ctx)+((1-pdelta)*eq05_sim_str_sc));
       node.setEq06_edist_str(eq06_edist_str);
       
       //Computing the internal behavioral distance
       double idist_beh=eq10_idist_beh(prjRow,prjCol,ent_common_atts);
       node.setIdist_beh(idist_beh);
       
       //Computing the external behavioral distance
       double edist_beh=eq13_edist_beh(prjRow,prjCol,ent_common_ctx);
       node.setEdist_beh(edist_beh);
       
       //Computing the internal distance
       double idist=((eq03_idist_str+idist_beh)/2);
       node.setIdist(idist);
       
       //Computing the external distance
       double edist=((eq06_edist_str+edist_beh)/2);
       node.setEdist(edist);
       
       //Computing the composed distance
       double cdist=((pw*idist+((1-pw)*edist)));
       node.setCdist(cdist);
       
       //Clear all the ArrayList before ending this function
       ent_common_atts.clear();
       ent_union_atts.clear();
       ent_common_states.clear();
       ent_union_states.clear();
       ent_common_statetransitions.clear();
       ent_union_statetransitions.clear();
       ent_common_ctx.clear();
       ent_union_ctx.clear();
       ent_common_scenarios.clear();
       ent_union_scenarios.clear();
       ent_common_scenariotransitions.clear();
       ent_union_scenariotransitions.clear();
       
       return true;
    }
    
    /**
     * It is responsible to fill the common attributes (intersection) between two projects, but also to fill
     * the union of the attributes among them.
     * 
     * @param prj1 The first measurement project to be compared
     * @param prj2 The second measurement project to be compared
     * @param ent_common_atts The list to be filled with common attributes. It should be created (It should not be null).
     * @param ent_union_atts The list to be filled with the union of attributes. It should be created (It should not be null)
     * @return TRUE when the lists could be filled, FALSE otherwise
     * @throws ProcessingException  It is raised when no projects are present or there not exist attributes
     */
    public static boolean parsingEntityAttributes(Node prj1,Node prj2,
            ArrayList<BehavioralItemNode> ent_common_atts, ArrayList<BehavioralItemNode> ent_union_atts) throws ProcessingException
    {
        if(prj1==null || prj2==null)
            throw new ProcessingException("The project 1 or 2 are null");
        if(prj1.getBeh()==null || prj1.getBeh().getAttributes()==null)
            throw new ProcessingException("No attributes for the project ID "+prj1.getProject().getID());
        if(prj2.getBeh()==null || prj2.getBeh().getAttributes()==null)
            throw new ProcessingException("No attributes for the project ID "+prj2.getProject().getID());
        if(ent_common_atts==null || ent_union_atts==null)
            throw new ProcessingException("One or both attributes' list is null");
        else
        {//The lists are cleaned because they could store previous results
            ent_common_atts.clear();
            ent_union_atts.clear();
        }
        
        ConcurrentHashMap<String,BehavioralItemNode> prj1Map=prj1.getBeh().getAttributesAsHashMap();
        if(prj1Map==null) throw new ProcessingException("No attributes as a hash map in project ID "+prj1.getProject().getID());
        ConcurrentHashMap<String,BehavioralItemNode> prj2Map=prj2.getBeh().getAttributesAsHashMap();
        if(prj2Map==null) throw new ProcessingException("No attributes as a hash map in project ID "+prj2.getProject().getID());
        
        for(BehavioralItemNode node:prj1Map.values())
        {
            BehavioralItemNode node2=prj2Map.remove(node.getAttributeID());
            if(node2==null)
            {//Node is only present in prj1
                ent_union_atts.add(node);
            }
            else
            {//Node is common
                ent_union_atts.add(node);
                ent_common_atts.add(node);
            }                        
        }
        
        if(!prj2Map.isEmpty())
        {//Attributes in prj2 that are not present in prj1
            ent_union_atts.addAll(prj2Map.values());
        }
        
        prj2Map.clear();
        prj1Map.clear();
        
        return true;
    }
    
    /**
     * It is responsible to fill the common entity states (intersection) between two projects, but also to fill
     * the union of the entity states among them. Be careful, a project could not define entity states. It is an
     * optional aspect of the measurement project.
     * 
     * @param prj1 The first measurement project to be compared
     * @param prj2 The second measurement project to be compared
     * @param ent_common_states The list to be filled with common entity states. It should be created (It should not be null).
     * @param ent_union_states The list to be filled with the union of entity states. It should be created (It should not be null)
     * @return TRUE when the lists could be filled or they are fully empty, FALSE otherwise
     * @throws ProcessingException  It is raised when no projects are present. 
     */
    public static boolean parsingEntityStates(Node prj1,Node prj2,
            ArrayList<ECState> ent_common_states, ArrayList<ECState> ent_union_states) throws ProcessingException
    {
        if(prj1==null || prj2==null)
            throw new ProcessingException("The project 1 or 2 are null");
        if(ent_common_states==null || ent_union_states==null)
            throw new ProcessingException("One or both entity states' list is null");
        else
        {//The lists are cleaned because they could store previous results
            ent_common_states.clear();
            ent_union_states.clear();
        }
               
        ConcurrentHashMap<String,ECState> prj1Map=prj1.getEcstateListAsHashMap();
        ConcurrentHashMap<String,ECState> prj2Map=prj2.getEcstateListAsHashMap();
        
        if(prj1Map==null)
        {
            if(prj2Map==null)
            {//Both lists will have size=0
                return true;
            }
            else
            {
                ent_union_states.addAll(prj2Map.values());
                return true;
            }
        }

        if(prj2Map==null)
        {
            ent_union_states.addAll(prj1Map.values());
            return true;

        }
        
        //In this situation, both (prjMap1 and prjMap2 are not null)
        
        for(ECState node:prj1Map.values())
        {
            ECState node2=prj2Map.remove(node.getUniqueID());
            if(node2==null)
            {//Node is only present in prj1
                ent_union_states.add(node);
            }
            else
            {//Node is common
                ent_union_states.add(node);
                ent_common_states.add(node);
            }                        
        }
        
        if(!prj2Map.isEmpty())
        {//Attributes in prj2 that are not present in prj1
            ent_union_states.addAll(prj2Map.values());
        }
        
        prj2Map.clear();
        prj1Map.clear();
        
        return true;
    }
    
    /**
     * It is responsible to fill the common entity state transitions (intersection) between two projects, but also to fill
     * the union of the entity state transitions among them. Be careful, a project could not define entity state transitions. It is an
     * optional aspect of the measurement project.
     * 
     * @param prj1 The first measurement project to be compared
     * @param prj2 The second measurement project to be compared
     * @param ent_common_statetransitions The list to be filled with common entity state transitions. It should be created (It should not be null).
     * @param ent_union_statetransitions The list to be filled with the union of entity state transitions. It should be created (It should not be null)
     * @return TRUE when the lists could be filled or they are fully empty, FALSE otherwise
     * @throws ProcessingException  It is raised when no projects are present. 
     */
    public static boolean parsingEntityStateTransitions(Node prj1,Node prj2,
            ArrayList<StateTransition> ent_common_statetransitions, ArrayList<StateTransition> ent_union_statetransitions) throws ProcessingException
    {
        if(prj1==null || prj2==null)
            throw new ProcessingException("The project 1 or 2 are null");
        if(ent_common_statetransitions==null || ent_union_statetransitions==null)
            throw new ProcessingException("One or both entity states' list is null");
        else
        {//The lists are cleaned because they could store previous results
            ent_common_statetransitions.clear();
            ent_union_statetransitions.clear();
        }
               
        ConcurrentHashMap<String,StateTransition> prj1Map=prj1.getEcstateTrListAsHashMap();
        ConcurrentHashMap<String,StateTransition> prj2Map=prj2.getEcstateTrListAsHashMap();
        
        if(prj1Map==null)
        {
            if(prj2Map==null)
            {//Both lists will have size=0
                return true;
            }
            else
            {
                ent_union_statetransitions.addAll(prj2Map.values());
                return true;
            }
        }

        if(prj2Map==null)
        {
            ent_union_statetransitions.addAll(prj1Map.values());
            return true;

        }
        
        //In this situation, both (prjMap1 and prjMap2 are not null)
        
        for(StateTransition node:prj1Map.values())
        {
            StateTransition node2=prj2Map.remove(node.getUniqueID());
            if(node2==null)
            {//Node is only present in prj1
                ent_union_statetransitions.add(node);
            }
            else
            {//Node is common
                ent_union_statetransitions.add(node);
                ent_common_statetransitions.add(node);
            }                        
        }
        
        if(!prj2Map.isEmpty())
        {//StateTransitions in prj2 that are not present in prj1
            ent_union_statetransitions.addAll(prj2Map.values());
        }
        
        prj2Map.clear();
        prj1Map.clear();
        
        return true;
    }

    /**
     * It is responsible to fill the common contexts (intersection) between two projects, but also to fill
     * the union of the contexts among them. Be careful, a project could not define contexts. It is an
     * optional aspect of the measurement project.
     * 
     * @param prj1 The first measurement project to be compared
     * @param prj2 The second measurement project to be compared
     * @param ent_common_ctx The list to be filled with common contexts. It should be created (It should not be null).
     * @param ent_union_ctx The list to be filled with the union of contexts. It should be created (It should not be null)
     * @return TRUE when the lists could be filled or they are fully empty, FALSE otherwise
     * @throws ProcessingException  It is raised when no projects are present. 
     */
    public static boolean parsingContexts(Node prj1,Node prj2,
            ArrayList<BehavioralItemNode> ent_common_ctx, ArrayList<BehavioralItemNode> ent_union_ctx) throws ProcessingException
    {
        if(prj1==null || prj2==null)
            throw new ProcessingException("The project 1 or 2 are null");
        if(ent_common_ctx==null || ent_union_ctx==null)
            throw new ProcessingException("One or both context properties' list is null");
        else
        {//The lists are cleaned because they could store previous results
            ent_common_ctx.clear();
            ent_union_ctx.clear();
        }
               
        ConcurrentHashMap<String,BehavioralItemNode> prj1Map=prj1.getBeh().getContextPropertiesAsHashMap();
        ConcurrentHashMap<String,BehavioralItemNode> prj2Map=prj2.getBeh().getContextPropertiesAsHashMap();
        
        if(prj1Map==null)
        {
            if(prj2Map==null)
            {//Both lists will have size=0
                return true;
            }
            else
            {
                ent_union_ctx.addAll(prj2Map.values());
                return true;
            }
        }

        if(prj2Map==null)
        {
            ent_union_ctx.addAll(prj1Map.values());
            return true;

        }
        
        //In this situation, both (prjMap1 and prjMap2 are not null)
        
        for(BehavioralItemNode node:prj1Map.values())
        {
            BehavioralItemNode node2=prj2Map.remove(node.getAttributeID());
            if(node2==null)
            {//Node is only present in prj1
                ent_union_ctx.add(node);
            }
            else
            {//Node is common
                ent_union_ctx.add(node);
                ent_common_ctx.add(node);
            }                        
        }
        
        if(!prj2Map.isEmpty())
        {//StateTransitions in prj2 that are not present in prj1
            ent_union_ctx.addAll(prj2Map.values());
        }
        
        prj2Map.clear();
        prj1Map.clear();
        
        return true;
    }
 
    /**
     * It is responsible to fill the common scenarios (intersection) between two projects, but also to fill
     * the union of the scenarios among them. Be careful, a project could not define scenarios. It is an
     * optional aspect of the measurement project.
     * 
     * @param prj1 The first measurement project to be compared
     * @param prj2 The second measurement project to be compared
     * @param ent_common_scenarios The list to be filled with common scenarios. It should be created (It should not be null).
     * @param ent_union_scenarios The list to be filled with the union of scenarios. It should be created (It should not be null)
     * @return TRUE when the lists could be filled or they are fully empty, FALSE otherwise
     * @throws ProcessingException  It is raised when no projects are present. 
     */
    public static boolean parsingScenarios(Node prj1,Node prj2,
            ArrayList<Scenario> ent_common_scenarios, ArrayList<Scenario> ent_union_scenarios) throws ProcessingException
    {
        if(prj1==null || prj2==null)
            throw new ProcessingException("The project 1 or 2 are null");
        if(ent_common_scenarios==null || ent_union_scenarios==null)
            throw new ProcessingException("One or both scenarios' list is null");
        else
        {//The lists are cleaned because they could store previous results
            ent_common_scenarios.clear();
            ent_union_scenarios.clear();
        }
               
        ConcurrentHashMap<String,Scenario> prj1Map=prj1.getScenarioListAsHashMap();
        ConcurrentHashMap<String,Scenario> prj2Map=prj2.getScenarioListAsHashMap();
        
        if(prj1Map==null)
        {
            if(prj2Map==null)
            {//Both lists will have size=0
                return true;
            }
            else
            {
                ent_union_scenarios.addAll(prj2Map.values());
                return true;
            }
        }

        if(prj2Map==null)
        {
            ent_union_scenarios.addAll(prj1Map.values());
            return true;

        }
        
        //In this situation, both (prjMap1 and prjMap2 are not null)
        
        for(Scenario node:prj1Map.values())
        {
            Scenario node2=prj2Map.remove(node.getUniqueID());
            if(node2==null)
            {//Node is only present in prj1
                ent_union_scenarios.add(node);
            }
            else
            {//Node is common
                ent_union_scenarios.add(node);
                ent_common_scenarios.add(node);
            }                        
        }
        
        if(!prj2Map.isEmpty())
        {//Scenario in prj2 that are not present in prj1
            ent_union_scenarios.addAll(prj2Map.values());
        }
        
        prj2Map.clear();
        prj1Map.clear();
        
        return true;
    }
 
    /**
     * It is responsible to fill the common scenario transitions (intersection) between two projects, but also to fill
     * the union of the scenario transitions among them. Be careful, a project could not define scenario transitions. It is an
     * optional aspect of the measurement project.
     * 
     * @param prj1 The first measurement project to be compared
     * @param prj2 The second measurement project to be compared
     * @param ent_common_scenariotransitions The list to be filled with common scenario transitions. It should be created (It should not be null).
     * @param ent_union_scenariotransitions The list to be filled with the union of scenario transitions. It should be created (It should not be null)
     * @return TRUE when the lists could be filled or they are fully empty, FALSE otherwise
     * @throws ProcessingException  It is raised when no projects are present. 
     */
    public static boolean parsingScenrioStateTransitions(Node prj1,Node prj2,
            ArrayList<StateTransition> ent_common_scenariotransitions, ArrayList<StateTransition> ent_union_scenariotransitions) throws ProcessingException
    {
        if(prj1==null || prj2==null)
            throw new ProcessingException("The project 1 or 2 are null");
        if(ent_common_scenariotransitions==null || ent_union_scenariotransitions==null)
            throw new ProcessingException("One or both scenario states' list is null");
        else
        {//The lists are cleaned because they could store previous results
            ent_common_scenariotransitions.clear();
            ent_union_scenariotransitions.clear();
        }
               
        ConcurrentHashMap<String,StateTransition> prj1Map=prj1.getScenarioTrAsHashMap();
        ConcurrentHashMap<String,StateTransition> prj2Map=prj2.getScenarioTrAsHashMap();
        
        if(prj1Map==null)
        {
            if(prj2Map==null)
            {//Both lists will have size=0
                return true;
            }
            else
            {
                ent_union_scenariotransitions.addAll(prj2Map.values());
                return true;
            }
        }

        if(prj2Map==null)
        {
            ent_union_scenariotransitions.addAll(prj1Map.values());
            return true;

        }
        
        //In this situation, both (prjMap1 and prjMap2 are not null)
        
        for(StateTransition node:prj1Map.values())
        {
            StateTransition node2=prj2Map.remove(node.getUniqueID());
            if(node2==null)
            {//Node is only present in prj1
                ent_union_scenariotransitions.add(node);
            }
            else
            {//Node is common
                ent_union_scenariotransitions.add(node);
                ent_common_scenariotransitions.add(node);
            }                        
        }
        
        if(!prj2Map.isEmpty())
        {//StateTransitions in prj2 that are not present in prj1
            ent_union_scenariotransitions.addAll(prj2Map.values());
        }
        
        prj2Map.clear();
        prj1Map.clear();
        
        return true;
    }
    
    /**
     * It implements the equation 8/11 to implement a Z-test for the same attribute in two different projects
     * @param p1_att1 The behavioral information about the attribute 1 in the project 1
     * @param p2_att1 The behavioral information about the attribute 1 in the project 2
     * @return The Z value when all the numerical values are available (mean, variance, and n for both projects), null otherwise.
     */
    protected static Double eq08_11_zpi(BehavioralItemNode p1_att1,BehavioralItemNode p2_att1)
    {
        if(p1_att1==null || p2_att1==null) return null;
        if(p1_att1.getMean()==null || p1_att1.getVar()==null || p1_att1.getN()==null) return null;
        if(p1_att1.getMean().isNaN() || p1_att1.getVar().isNaN()) return null;
        if(p2_att1.getMean()==null || p2_att1.getVar()==null || p2_att1.getN()==null) return null;
        if(p2_att1.getMean().isNaN() || p2_att1.getVar().isNaN()) return null;
                
        return (p1_att1.getMean()-p2_att1.getMean())/
                (Math.sqrt((p1_att1.getVar()/p1_att1.getN().doubleValue())+(p2_att1.getVar()/p2_att1.getN().doubleValue())));
    }
    
    /**
     * It implements the equation 9/12 to implement the Z-test interpretation for the same attribute in two different projects
     * @param p1_att1 The behavioral information about the attribute 1 in the project 1
     * @param p2_att1 The behavioral information about the attribute 1 in the project 2
     * @return The interpretation of Z value based on equations 9/12 when all the numerical values are available (mean, variance, and n for both projects), null otherwise.
     */
    protected static Double eq09_12_izpi(BehavioralItemNode p1_att1,BehavioralItemNode p2_att1)
    {
        Double eq08_11_zpi=eq08_11_zpi(p1_att1,p2_att1);
        
        if(eq08_11_zpi==null || eq08_11_zpi.isNaN()) return null;

        if(Math.abs(eq08_11_zpi)<2) return 1.0;
        if(Math.abs(eq08_11_zpi)>3) return 0.0;
        
        return 3.0-Math.abs(eq08_11_zpi);
    }
    
    /**
     * It computes the internal behavioral distance between project 1 and 2, using equation 10
     * @param prj1 The project 1 
     * @param prj2 The project 2
     * @param ent_common_atts The common attributes between projects
     * @return A distance is computed between 0 (too close) and 1 (too far) when it is possible.
     * @throws ProcessingException It is raised when no information about the projects are available.
     */
    public static double eq10_idist_beh(Node prj1,Node prj2,ArrayList<BehavioralItemNode> ent_common_atts) throws ProcessingException
    {
        if(prj1==null || prj2==null)
            throw new ProcessingException("The project 1 or 2 are null");
        if(ent_common_atts==null || ent_common_atts.isEmpty()) return 1.0;
        ConcurrentHashMap<String,BehavioralItemNode> prj1Map=prj1.getBeh().getAttributesAsHashMap();
        ConcurrentHashMap<String,BehavioralItemNode> prj2Map=prj2.getBeh().getAttributesAsHashMap();
        if(prj1Map==null || prj2Map==null) return 1.0;
        
        double acu=0.0;
        int nwithValues=0;
        for(BehavioralItemNode item:ent_common_atts)
        {
            BehavioralItemNode p1=prj1Map.get(item.getAttributeID());
            BehavioralItemNode p2=prj2Map.get(item.getAttributeID());
            
            if(p1==null || p2==null)
                throw new ProcessingException("The attribute ID ("+item.getAttributeID()+" is indicated as common but it is not present in one of the compared projects");
            
            Double ret=eq09_12_izpi(p1,p2);
                        
            if(ret!=null && !ret.isNaN())
            {
                acu+=ret;
                nwithValues++;
            }            
        }
        prj1Map.clear();
        prj2Map.clear();
        
        if(nwithValues==0) return 1.0;
        
       
        return 1-(acu/((double)ent_common_atts.size()));        
    }
    
    /**
     * It computes the external behavioral distance between project 1 and 2, using equation 10
     * @param prj1 The project 1 
     * @param prj2 The project 2
     * @param ent_common_ctx The common context properties between project contexts
     * @return A distance is computed between 0 (too close) and 1 (too far) when it is possible.
     * @throws ProcessingException It is raised when no information about the projects are available.
     */
    public static double eq13_edist_beh(Node prj1,Node prj2,ArrayList<BehavioralItemNode> ent_common_ctx) throws ProcessingException
    {
        if(prj1==null || prj2==null)
            throw new ProcessingException("The project 1 or 2 are null");
        if(ent_common_ctx==null || ent_common_ctx.isEmpty()) return 1.0;
        ConcurrentHashMap<String,BehavioralItemNode> prj1Map=prj1.getBeh().getContextPropertiesAsHashMap();
        ConcurrentHashMap<String,BehavioralItemNode> prj2Map=prj2.getBeh().getContextPropertiesAsHashMap();
        if(prj1Map==null || prj2Map==null) return 1.0;
        
        double acu=0.0;
        int nwithValues=0;
        for(BehavioralItemNode item:ent_common_ctx)
        {
            BehavioralItemNode p1=prj1Map.get(item.getAttributeID());
            BehavioralItemNode p2=prj2Map.get(item.getAttributeID());
            
            if(p1==null || p2==null)
                throw new ProcessingException("The context property ID ("+item.getAttributeID()+" is indicated as common but it is not present in one of the compared projects");
            
            Double ret=eq09_12_izpi(p1,p2);
            if(ret!=null && !ret.isNaN())
            {
                acu+=ret;
                nwithValues++;
            }            
        }
        prj1Map.clear();
        prj2Map.clear();
        
        if(nwithValues==0) return 1.0;

        return 1-(acu/((double)ent_common_ctx.size()));        
    }

   
    /**
     * It updates the composed index estimation based on the current values of 
     * the alfa, beta, gama, delta, and w parameters.
     * @return TRUE when the estimation has been completed, FALSE otherwise.
     * @throws ProcessingException It is raised when does not exist a matrix or projects.
     */
    public boolean updateEstimationOfComposedIndex() throws ProcessingException
    {
        if(matrix==null) throw new ProcessingException("No matrix to be processed");
        if(projects==null || projects.isEmpty()) throw new ProcessingException("No projects available");
        matrix.restartThreadPosition();        
        
        int nthreads=1;
        int factor=matrix.unidimensionalLength()/5;
        if(factor>1)
        {
            nthreads=nthreads*factor;
        }
        
        ExecutorService pool=Executors.newFixedThreadPool(nthreads);
        for(int i=0;i<nthreads;i++)
        {
            pool.execute(IndexEstimator.create(projects,matrix,alfa,beta,gama,delta,w));
        }

        pool.shutdown();
        
        while(!pool.isTerminated()){}
        
        return true;
    }
    
    public void releaseResources() throws ProcessingException
    {
        if(matrix!=null) matrix.releaseResources();
        if(projects!=null) projects.clear();        
    }
}
