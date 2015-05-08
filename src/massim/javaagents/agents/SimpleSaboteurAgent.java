package massim.javaagents.agents;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Vector;

import apltk.interpreter.data.LogicBelief;
import apltk.interpreter.data.LogicGoal;
import eis.iilang.Action;
import eis.iilang.Percept;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import massim.javaagents.Agent;

public class SimpleSaboteurAgent extends Agent {

	public SimpleSaboteurAgent(String name, String team) {
		super(name, team);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handlePercept(Percept p) {
	}

	@Override
	public Action step() {

		Action act = null;
		
		handlePercepts();
		
		// 1. recharging
		act = planRecharge();
		if ( act != null ) return act;
		
		// 2. fight if possible
		act = planFight();
		if ( act != null ) return act;
		
		// 3. random walking
		act = planWalk();
		if ( act != null ) return act;

		return MarsUtil.skipAction();
		
	}

	private void handlePercepts() {

		String position = null;
		Vector<String> neighbors = new Vector<String>();
		
		// check percepts
		Collection<Percept> percepts = getAllPercepts();
		//if ( gatherSpecimens ) processSpecimens(percepts);
		removeBeliefs("visibleEntity");
		removeBeliefs("visibleEdge");
                removeBeliefs("lastAction");
		for ( Percept p : percepts ) {
                    /*
                    if (p.getName().equals("role")) {
                        println(this.getName() + " with role Saboteour is assigned role " + p.getParameters().get(0).toString());
                    } else 
                            */
                            if ( p.getName().equals("step") ) {
				println(p);
			}
			else if ( p.getName().equals("visibleEntity") ) {
				LogicBelief b = MarsUtil.perceptToBelief(p);
				if ( containsBelief(b) == false ) {
					//println("I perceive an edge I have not known before");
					addBelief(b);
					//broadcastBelief(b);
				}
				else {
					//println("I already knew " + b);
				}
			}
            else if ( p.getName().equals("position") ) {
				position = p.getParameters().get(0).toString();
				removeBeliefs("position");
				addBelief(new LogicBelief("position",position));
			}
			else if ( p.getName().equals("health")) {
				Integer health = new Integer(p.getParameters().get(0).toString());
				println("my health is " +health );
				if ( health.intValue() == 0 ) {
					println("my health is zero. asking for help");
					broadcastBelief(new LogicBelief("iAmDisabled", position));
				}
			}
			else if ( p.getName().equals("energy") ) {
				Integer energy = new Integer(p.getParameters().get(0).toString());
				removeBeliefs("energy");
				addBelief(new LogicBelief("energy",energy.toString()));
			}
			else if ( p.getName().equals("maxEnergy") ) {
				Integer maxEnergy = new Integer(p.getParameters().get(0).toString());
				removeBeliefs("maxEnergy");
				addBelief(new LogicBelief("maxEnergy",maxEnergy.toString()));
			}
			else if ( p.getName().equals("achievement") ) {
				println("reached achievement " + p);
			}
                        else if (p.getName().equals("lastAction")) {
                            addBelief (new LogicBelief("lastAction", p.getParameters().get(0).toString()));
                        }
		}
		
		// again for checking neighbors
		this.removeBeliefs("neighbor");
		for ( Percept p : percepts ) {
			if ( p.getName().equals("visibleEdge") ) {
				String vertex1 = p.getParameters().get(0).toString();
				String vertex2 = p.getParameters().get(1).toString();
				if ( vertex1.equals(position) ) 
					addBelief(new LogicBelief("neighbor",vertex2));
				if ( vertex2.equals(position) ) 
					addBelief(new LogicBelief("neighbor",vertex1));
			}
		}	
	}
	
	private Action planRecharge() {

		LinkedList<LogicBelief> beliefs = null;
		
		beliefs =  getAllBeliefs("energy");
		if ( beliefs.size() == 0 ) {
				println("strangely I do not know my energy");
				return null;
		}		
		int energy = new Integer(beliefs.getFirst().getParameters().firstElement()).intValue();

		beliefs =  getAllBeliefs("maxEnergy");
		if ( beliefs.size() == 0 ) {
				println("strangely I do not know my maxEnergy");
				return null;
		}		
		int maxEnergy = new Integer(beliefs.getFirst().getParameters().firstElement()).intValue();

		// if has the goal of being recharged...
                /*
		if ( goals.contains(new LogicGoal("beAtFullCharge")) ) {
			if ( maxEnergy == energy ) {
				println("I can stop recharging. I am at full charge");
				removeGoals("beAtFullCharge");
			}
			else {
				println("recharging...");
				return MarsUtil.rechargeAction();
			}
		}
		// go to recharge mode if necessary
		else {
                        */
			if ( energy < maxEnergy / 3 ) {
				println("I need to recharge");
				//goals.add(new LogicGoal("beAtFullCharge")); /* Ignoring this goal, wastes one step */
				return MarsUtil.rechargeAction();
			}
		//}	
		
		return null;
		
	}
	
	private Action planFight() {
		
		// get position
		LinkedList<LogicBelief> beliefs = null;
		beliefs =  getAllBeliefs("position");
		if ( beliefs.size() == 0 ) {
				println("strangely I do not know my position");
				return null;
		}
		String position = beliefs.getFirst().getParameters().firstElement();

		// if there is an enemy on the current position then attack or defend
		Vector<String> enemies = new Vector<String>();
                Vector<String> allies = new Vector<String>();
		beliefs = getAllBeliefs("visibleEntity");
		for ( LogicBelief b : beliefs ) {
			String name = b.getParameters().get(0);
			String pos = b.getParameters().get(1);
			String team = b.getParameters().get(2);
			if ( team.equals(getTeam()) ) {
                            allies.add(name);
                            continue;
                        }
			if ( pos.equals(position) == false ) continue;
			enemies.add(name);
		}
		if ( enemies.size() != 0 ) {
			println("there are " + enemies.size() + " enemies at my current position");
                        println("there are " + allies.size() + " allies at my current position");
			if (allies.size() >= enemies.size()) {
                            /* I will attack if there are more allies than enemies */
                            /* TODO: will want to check for enemy energy if available to decide whom to attack */
                                Collections.shuffle(enemies);
				String enemy = enemies.firstElement();
				println("I will attack " + enemy);
                                /* TODO: Add logic to notify others of the attack, so they can join in if possible */
				return MarsUtil.attackAction(enemy);
			}
			else {
                            beliefs =  getAllBeliefs("lastAction");
                            if (beliefs.size() != 0) {
                                String last = beliefs.getFirst().getParameters().firstElement();
                                if (last.equals("parry")) { 
                                    /* If I parried last time and am still surrounded, get the heck out */
                                    return null;
                                } else {
                                    /* Parry when surrounded */
                                    println("I will parry");
                                    return MarsUtil.parryAction();
                                }
                            } else {
                                /* Shouldn't happen, but if I don't know what I did last, move */
                                return null;
                            }
			}
		}
		
		// if there is an enemy on a neighboring vertex to there
		beliefs = getAllBeliefs("neighbor");
		Vector<String> neighbors = new Vector<String>();
		for ( LogicBelief b : beliefs ) {
			neighbors.add(b.getParameters().firstElement());
		}
		
		Map<String,Vector<String>> neighborEnemies = new HashMap<String, Vector<String>>();
		beliefs = getAllBeliefs("visibleEntity");
		for ( LogicBelief b : beliefs ) {
			String name = b.getParameters().get(0);
			String pos = b.getParameters().get(1);
			String team = b.getParameters().get(2);
			if ( team.equals(getTeam()) ) continue;
			if ( neighbors.contains(pos) == false ) continue;
                        if (!neighborEnemies.containsKey(pos)) {
                            Vector<String> enemiesInVertex = new Vector<String>();
                            enemiesInVertex.add(name);
                            neighborEnemies.put(pos, enemiesInVertex);
                        } else {
                            Vector<String> enemiesInVertex = neighborEnemies.get(pos);
                            enemiesInVertex.add(name);
                        }
		}
                
		if ( neighborEnemies.size() != 0 ) {
			println("there are " + neighborEnemies.size() + " enemies in adjacent vertices");
                        /* Attack the loneliest */
                        Vector <String> targetEnemies = null;
                        Integer noOfEnemies = 29; /* can't ever be more than this */ 
                        for(Entry<String, Vector<String>> entry : neighborEnemies.entrySet()) {
                            if(entry.getValue().size() < noOfEnemies) {
                                targetEnemies = entry.getValue(); // Not sure if the VM optimizes this, may want to store before instead
                            }
                        }
			Collections.shuffle(targetEnemies);
                        String enemy = targetEnemies.firstElement();
			//String vertex = vertices.firstElement();
			println("I will attack " + enemies);
                        /* TODO: Notify others of the attack so they can join in */
			//return MarsUtil.gotoAction(vertex);
                        return MarsUtil.attackAction(enemy);
		}
		
		return null;
	}
	
	private Action planWalk() {

		LinkedList<LogicBelief> beliefs = getAllBeliefs("neighbor");
		Vector<String> neighbors = new Vector<String>();
		for ( LogicBelief b : beliefs ) {
			neighbors.add(b.getParameters().firstElement());
		}
		
		if ( neighbors.size() == 0 ) {
			println("strangely I do not know any neighbors");
			return MarsUtil.rechargeAction();
		}
                
		// Contribute to map coverage
                // TODO: as attackers, we may want to go where our allies are attacking others. That needs to go before this logic.
                int amountOfNeighbors = 10; // hopefully not so small
                String nodeWithLessNeighbors;
                for (String neighbor : neighbors) {
                    int amount = mapGraph.degreeOf(neighbor);
                    if (amount < amountOfNeighbors) {
                        amountOfNeighbors = amount;
                        nodeWithLessNeighbors = neighbor;
                    }
                }
		String neighbor = neighbors.firstElement();
		println("I will go to " + neighbor);
		return MarsUtil.gotoAction(neighbor);
		
	}
}
