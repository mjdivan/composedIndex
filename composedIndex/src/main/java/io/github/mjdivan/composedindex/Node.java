/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.mjdivan.composedindex;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.ciedayap.ipd.MeasurementProject;
import org.ciedayap.ipd.exception.ProcessingException;
import org.ciedayap.ipd.states.ECState;
import org.ciedayap.ipd.states.Scenario;
import org.ciedayap.ipd.states.StateTransition;

/**
 *
 * @author mjdivan
 */
public class Node {
    /**
     * The measurement project definitioin
     */    
    private MeasurementProject project;
    /**
     * A node containing attributes and context properties jointly with the statistic information (when it is available)
     */
    private BehavioralNode beh;
    /**
     * The entity's states of the measurement project when they are available
     */
    private ArrayList<ECState> ecstateList;
    /**
     * The transition model for the entity's states when they are available
     */
    private ArrayList<StateTransition> ecstateTrList;
    /**
     * The scenarios for the defined context when they are available
     */
    private ArrayList<Scenario> scenarioList;
    /**
     * The transition model for scenarios when they are available
     */
    private ArrayList<StateTransition> scenarioTrList;         
    
    /**
     * It creates the node based on the project definition
     * 
     * @param mp The measurement project definition
     * @throws ProcessingException it is raised when the measurement project definition is incomplete or not accessible
     */
    public Node(MeasurementProject mp) throws ProcessingException
    {
        this.project=mp;
        beh=BehavioralNode.create(mp);
        try{
            if(mp.getInfneed()!=null && mp.getInfneed().getEntityCategory()!=null)
            {
                if(mp.getInfneed().getEntityCategory().getEcStates()!=null &&
                    mp.getInfneed().getEntityCategory().getEcStates().getEcstates()!=null)
                {
                    ecstateList=mp.getInfneed().getEntityCategory().getEcStates().getEcstates();
                }
                else
                {
                    ecstateList=null;
                }
                
                if( mp.getInfneed().getEntityCategory().getStateTransitionModel()!=null &&
                         mp.getInfneed().getEntityCategory().getStateTransitionModel().getTransitions()!=null &&
                         mp.getInfneed().getEntityCategory().getStateTransitionModel().getTransitions().getTransitions()!=null)
                {                         
                   ecstateTrList=mp.getInfneed().getEntityCategory().getStateTransitionModel().getTransitions().getTransitions();
                }
                else
                {
                   ecstateTrList=null;
                }
            }
            else
            {
                ecstateList=null;
                ecstateTrList=null;
            }
                
            if(mp.getInfneed()!=null && mp.getInfneed().getContext()!=null)
            {
                if(mp.getInfneed().getContext().getScenarios()!=null &&
                        mp.getInfneed().getContext().getScenarios().getScenarios()!=null)
                {
                    scenarioList=mp.getInfneed().getContext().getScenarios().getScenarios();
                }
                else
                {
                    scenarioList=null;
                }
                
                if(mp.getInfneed().getContext().getStateTransitionModel()!=null &&
                        mp.getInfneed().getContext().getStateTransitionModel().getTransitions()!=null &&
                        mp.getInfneed().getContext().getStateTransitionModel().getTransitions().getTransitions()!=null)
                {
                    scenarioTrList=mp.getInfneed().getContext().getStateTransitionModel().getTransitions().getTransitions();
                }
                else
                {
                    scenarioTrList=null;
                }
            }
            else
            {
             scenarioList=null;
             scenarioTrList=null;
            }
            
        }catch(Exception e)
        {
            throw new ProcessingException("The information need is not accessible. Message: "+e.getMessage());
        }
        
    }

    /**
     * 
     * @param mp The measurement project based on which the node instance will be created
     * @return A new Node instance
     * @throws ProcessingException it is raised when the measurement project definition is incomplete or not accessible
     */
    public synchronized static Node create(MeasurementProject mp) throws ProcessingException
    {
        return new Node(mp);
    }
    
    /**
     * @return the project
     */
    public MeasurementProject getProject() {
        return project;
    }

    /**
     * @return the beh
     */
    public BehavioralNode getBeh() {
        return beh;
    }

    /**
     * @return the ecstateList
     */
    public ArrayList<ECState> getEcstateList() {
        return ecstateList;
    }
    
    /**
     * It returns the list of states as a Concurrent Hash map organized by the state ID
     * @return A concurrent hash map containing the states organized by state ID
     */
    public ConcurrentHashMap<String,ECState> getEcstateListAsHashMap()
    {
       if(ecstateList==null || ecstateList.isEmpty()) return null;
        
        ConcurrentHashMap<String,ECState> map=new ConcurrentHashMap();
        for(ECState mynode:ecstateList)
        {
            map.put(mynode.getID(), mynode);
        }
        
        return map;        
    }

    /**
     * @return the ecstateTrList
     */
    public ArrayList<StateTransition> getEcstateTrList() {
        return ecstateTrList;
    }

    /**
     * It returns the list of state transitions as a Concurrent Hash map organized by the state transition unique ID
     * @return A concurrent hash map containing the state transitions organized by state transition unique ID
     */
    public ConcurrentHashMap<String,StateTransition> getEcstateTrListAsHashMap()
    {
       if(ecstateTrList==null || ecstateTrList.isEmpty()) return null;
        
        ConcurrentHashMap<String,StateTransition> map=new ConcurrentHashMap();
        for(StateTransition mynode:ecstateTrList)
        {
            map.put(mynode.getUniqueID(), mynode);
        }
        
        return map;        
    }
    
    /**
     * @return the scenarioList
     */
    public ArrayList<Scenario> getScenarioList() {
        return scenarioList;
    }

    /**
     * It returns the list of scenarios as a Concurrent Hash map organized by the scenario ID
     * @return A concurrent hash map containing the scenarios organized by scenario ID
     */
    public ConcurrentHashMap<String,Scenario> getScenarioListAsHashMap()
    {
       if(scenarioList==null || scenarioList.isEmpty()) return null;
        
        ConcurrentHashMap<String,Scenario> map=new ConcurrentHashMap();
        for(Scenario mynode:scenarioList)
        {
            map.put(mynode.getID(), mynode);
        }
        
        return map;        
    }
    
    /**
     * @return the scenarioTrList
     */
    public ArrayList<StateTransition> getScenarioTrList() {
        return scenarioTrList;
    }
    
    /**
     * It returns the list of scenario transitions as a Concurrent Hash map organized by the scenario transition unique ID
     * @return A concurrent hash map containing the scenario transitions organized by scenario transition unique ID
     */
    public ConcurrentHashMap<String,StateTransition> getScenarioTrAsHashMap()
    {
       if(scenarioTrList==null || scenarioTrList.isEmpty()) return null;
        
        ConcurrentHashMap<String,StateTransition> map=new ConcurrentHashMap();
        for(StateTransition mynode:scenarioTrList)
        {
            map.put(mynode.getUniqueID(), mynode);
        }
        
        return map;        
    }
    
}
