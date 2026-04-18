package com.investigator.vsa.strategy;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;

public class RandomGenerationStrategy implements VectorGenerationStrategy {
    @Override
    public HDVector generate(String uri) {
        return HDVectorMapB.generateRandom();
    }
}