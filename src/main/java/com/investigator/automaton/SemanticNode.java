package com.investigator.automaton;

import org.apache.jena.rdf.model.Resource;
import com.investigator.vsa.HDVector;

public class SemanticNode {
    private Resource jenaResource;
    private HDVector currentVector;
    private double energy;

    public SemanticNode(Resource resource, HDVector baseVector) {
        this.jenaResource = resource;
        this.currentVector = baseVector;
        this.energy = 0.0;
    }

    public Resource getJenaResource() { return jenaResource; }
    public HDVector getCurrentVector() { return currentVector; }
    public double getEnergy() { return energy; }

    public void addEnergy(double amount) { this.energy += amount; }
    public void reduceEnergy(double amount) { this.energy = Math.max(0, this.energy - amount); }
    public void resetEnergy() { this.energy = 0.0; }
}