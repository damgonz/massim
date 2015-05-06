package massim.javaagents.agents;

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import apltk.interpreter.data.LogicBelief;
import apltk.interpreter.data.LogicGoal;
import apltk.interpreter.data.Message;
import eis.iilang.Action;
import eis.iilang.Percept;
import java.util.LinkedList;
import massim.javaagents.Agent;

public class SimpleRepairerAgent extends Agent {

    int rechargeSteps = 0;
    Vector<String> needyAgents = new Vector<String>();

    public SimpleRepairerAgent(String name, String team) {
        super(name, team);
    }

    @Override
    public void handlePercept(Percept p) {
    }

    @Override
    public Action step() {
        
        handleMessages();
        handlePercepts();

        Action act = null;

        // 1. recharging
        act = planRecharge();
        if (act != null) {
            return act;
        }

        // 2. repair if necessary
        act = planRepair();
        if (act != null) {
            return act;
        }
        
        // 3. buying battery with a certain probability
        act = planBuyBattery();
        if (act != null) {
            return act;
        }
                
        // 4. surveying if necessary
        act = planSurvey();
        if (act != null) {
            return act;
        }

        // 5. (almost) random walking
        act = planRandomWalk();
        if (act != null) {
            return act;
        }

        return MarsUtil.skipAction();

    }

    private void handleMessages() {

        Collection<Message> messages = getMessages();
        needyAgents.clear();
        for (Message msg : messages) {
            if (((LogicBelief) msg.value).getPredicate().equals("iAmDisabled")) {
                needyAgents.add(msg.sender);
            }
        }

        if (needyAgents.size() == 0) {
            println("nobody needs my help");
        } else {
            println("some poor souls need my help " + needyAgents);
        }
    }

    private void handlePercepts() {

        String position = null;
        Vector<String> neighbors = new Vector<String>();

        // check percepts
        Collection<Percept> percepts = getAllPercepts();
        //if ( gatherSpecimens ) processSpecimens(percepts);
        removeBeliefs("visibleEntity");
        removeBeliefs("visibleEdge");
        // may want to check for "edges" and "vertices,
        // to know if we've completed the map.
        for (Percept p : percepts) {
            /*
             if (p.getName().equals("role")) {
             println(this.getName() + " with role Explorer is assigned role " + p.getParameters().get(0).toString());
             } else 
             */
            if (p.getName().equals("step")) {
                println(p);
            } else if (p.getName().equals("visibleEntity")) {
                LogicBelief b = MarsUtil.perceptToBelief(p);
                if (containsBelief(b) == false) {
                    addBelief(b);
                } else {
                }
            } else if (p.getName().equals("visibleEdge")) {
                LogicBelief b = MarsUtil.perceptToBelief(p);
                if (containsBelief(b) == false) {
                    addBelief(b);
                } else {
                }
            } else if (p.getName().equals("probedVertex")) {
                LogicBelief b = MarsUtil.perceptToBelief(p);
                if (containsBelief(b) == false) {
                    println("I perceive the value of a vertex that I have not known before");
                    addBelief(b);
                    broadcastBelief(b);
                    // so, it looks like here I can add values to vertex,
                    // only the explorer can do it.
                } else {
                    //println("I already knew " + b);
                }
            } else if (p.getName().equals("surveyedEdge")) {
                // this is where we add values of edges to the map
                LogicBelief b = MarsUtil.perceptToBelief(p);
                if (containsBelief(b) == false) {
                    println("I perceive the weight of an edge that I have not known before");
                    addBelief(b);
                    broadcastBelief(b);
                } else {
                    //println("I already knew " + b);
                }
            } else if (p.getName().equals("health")) {
                Integer health = new Integer(p.getParameters().get(0).toString());
                println("my health is " + health);
                if (health.intValue() == 0) {
                    println("my health is zero. asking for help");
                    broadcastBelief(new LogicBelief("iAmDisabled"));
                }
            } else if (p.getName().equals("position")) {
                position = p.getParameters().get(0).toString();
                removeBeliefs("position");
                addBelief(new LogicBelief("position", position));
                // This is where we start our calculation of how to get somewhere.
            } else if (p.getName().equals("energy")) {
                Integer energy = new Integer(p.getParameters().get(0).toString());
                removeBeliefs("energy");
                addBelief(new LogicBelief("energy", energy.toString()));
            } else if (p.getName().equals("maxEnergy")) {
                Integer maxEnergy = new Integer(p.getParameters().get(0).toString());
                removeBeliefs("maxEnergy");
                addBelief(new LogicBelief("maxEnergy", maxEnergy.toString()));
            } else if (p.getName().equals("money")) {
                Integer money = new Integer(p.getParameters().get(0).toString());
                removeBeliefs("money");
                addBelief(new LogicBelief("money", money.toString()));
            } else if (p.getName().equals("achievement")) {
                println("reached achievement " + p);
            }
            // may want to check for ranking and/or score,
            // in case we wanted to shift to agressive
            // or conservative strategies.
            // strenght could affect whether to attack or not.
        }

        // again for checking neighbors
        this.removeBeliefs("neighbor");
        // here, neighbor vertices are added to our beliefs.
        for (Percept p : percepts) {
            if (p.getName().equals("visibleEdge")) {
                String vertex1 = p.getParameters().get(0).toString();
                String vertex2 = p.getParameters().get(1).toString();
                if (vertex1.equals(position)) {
                    addBelief(new LogicBelief("neighbor", vertex2));
                }
                if (vertex2.equals(position)) {
                    addBelief(new LogicBelief("neighbor", vertex1));
                }
            }
        }
    }

    private Action planRecharge() {

        LinkedList<LogicBelief> beliefs = null;

        beliefs = getAllBeliefs("energy");
        if (beliefs.size() == 0) {
            println("strangely I do not know my energy");
            return MarsUtil.skipAction();
        }
        int energy = new Integer(beliefs.getFirst().getParameters().firstElement()).intValue();

        beliefs = getAllBeliefs("maxEnergy");
        if (beliefs.size() == 0) {
            println("strangely I do not know my maxEnergy");
            return MarsUtil.skipAction();
        }
        int maxEnergy = new Integer(beliefs.getFirst().getParameters().firstElement()).intValue();

        // if has the goal of being recharged...
        if (goals.contains(new LogicGoal("beAtFullCharge"))) {
            if (maxEnergy == energy) {
                println("I can stop recharging. I am at full charge");
                removeGoals("beAtFullCharge");
            } else {
                println("recharging...");
                return MarsUtil.rechargeAction();
            }
        } // go to recharge mode if necessary
        else {
            if (energy < maxEnergy / 3) {
                println("I need to recharge");
                goals.add(new LogicGoal("beAtFullCharge"));
                return MarsUtil.rechargeAction();
            }
        }

        return null;

    }

    private Action planSurvey() {

        println("I know " + getAllBeliefs("visibleEdge").size() + " visible edges");
        println("I know " + getAllBeliefs("surveyedEdge").size() + " surveyed edges");

        // get all neighbors
        LinkedList<LogicBelief> visible = getAllBeliefs("visibleEdge");
        LinkedList<LogicBelief> surveyed = getAllBeliefs("surveyedEdge");

        String position = getAllBeliefs("position").get(0).getParameters().firstElement();

        int unsurveyedNum = 0;
        int adjacentNum = 0;

        for (LogicBelief v : visible) {

            String vVertex0 = v.getParameters().elementAt(0);
            String vVertex1 = v.getParameters().elementAt(1);

            boolean adjacent = false;
            if (vVertex0.equals(position) || vVertex1.equals(position)) {
                adjacent = true;
            }

            if (adjacent == false) {
                continue;
            }
            adjacentNum++;

            boolean isSurveyed = false;
            for (LogicBelief s : surveyed) {
                String sVertex0 = s.getParameters().elementAt(0);
                String sVertex1 = s.getParameters().elementAt(1);
                if (sVertex0.equals(vVertex0) && sVertex1.equals(vVertex1)) {
                    isSurveyed = true;
                    break;
                }
                if (sVertex0.equals(vVertex1) && sVertex1.equals(vVertex0)) {
                    isSurveyed = true;
                    break;
                }
            }
            if (isSurveyed == false) {
                unsurveyedNum++;
            }

        }

        println("" + unsurveyedNum + " out of " + adjacentNum + " adjacent edges are unsurveyed");

        if (unsurveyedNum > 0) {
            println("I will survey");
            return MarsUtil.surveyAction();
        }

        return null;

    }

    /**
     * Buy a battery with a given probability
     *
     * @return
     */
    private Action planBuyBattery() {

        LinkedList<LogicBelief> beliefs = this.getAllBeliefs("money");
        if (beliefs.size() == 0) {
            println("strangely I do not know our money.");
            return null;
        }

        LogicBelief moneyBelief = beliefs.get(0);
        int money = new Integer(moneyBelief.getParameters().get(0)).intValue();

        if (money < 10) {
            println("we do not have enough money.");
            return null;
        }
        println("we do have enough money.");

        //double r = Math.random();
        //if ( r < 1.0 ) {
        //	println("I am not going to buy a battery");
        //	return null;
        //}
        println("I am going to buy a battery");

        return MarsUtil.buyAction("battery");

    }

    private Action planRepair() {
        LinkedList<LogicBelief> beliefs = null;

        beliefs = getAllBeliefs("position");
        if (beliefs.size() == 0) {
            println("strangely I do not know my position");
            return MarsUtil.skipAction();
        }
        String position = beliefs.getFirst().getParameters().firstElement();

        beliefs.clear();
        beliefs = getAllBeliefs("visibleEntity");
        // a needy one on the same vertex
        for (LogicBelief b : beliefs) {
            String ePos = b.getParameters().get(1);
            String eName = b.getParameters().get(0);
            if (ePos.equals(position) && needyAgents.contains(eName)) {
                println("I am going to repair " + eName);
                return MarsUtil.repairAction(eName);
            }
        }

        beliefs.clear();
        beliefs = getAllBeliefs("visibleEdge");
        // maybe on an adjacent vertex?
        Vector<String> neighbors = new Vector<String>();
        for (LogicBelief b : beliefs) {
            String vertex1 = b.getParameters().get(0);
            String vertex2 = b.getParameters().get(1);
            if (vertex1.equals(position)) {
                neighbors.add(vertex2);
            }
            if (vertex2.equals(position)) {
                neighbors.add(vertex1);
            }
        }
        beliefs.clear();
        beliefs = getAllBeliefs("visibleEntity");
        for (LogicBelief b : beliefs) {
            String ePos = b.getParameters().get(1);
            String eName = b.getParameters().get(0);
            if (neighbors.contains(ePos) && needyAgents.contains(eName)) {
                println("I am going to repair " + eName + ". move to " + ePos + " first.");
                return MarsUtil.gotoAction(ePos);
            }
        }

        // goto neighbors
        if (neighbors.size() == 0) {
            println("Strangely I do not know my neighbors");
            return MarsUtil.skipAction();
        }

        return null;
    }

    private Action planRandomWalk() {

        LinkedList<LogicBelief> beliefs = getAllBeliefs("neighbor");
        Vector<String> neighbors = new Vector<String>();
        for (LogicBelief b : beliefs) {
            neighbors.add(b.getParameters().firstElement());
        }

        if (neighbors.size() == 0) {
            println("strangely I do not know any neighbors");
            return MarsUtil.skipAction();
        }

        // goto neighbors
        Collections.shuffle(neighbors);
        String neighbor = neighbors.firstElement();
        println("I will go to " + neighbor);
        return MarsUtil.gotoAction(neighbor);

    }
}
