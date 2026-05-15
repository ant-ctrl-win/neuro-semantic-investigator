package com.investigator.vsa.strategy;

import com.investigator.vsa.HDVector;
import com.investigator.vsa.HDVectorMapB;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RandomGenerationStrategy implements VectorGenerationStrategy {
    @Override
    public HDVector generate(String uri) {
        long seed = UUID.nameUUIDFromBytes(uri.getBytes(StandardCharsets.UTF_8)).getMostSignificantBits();
        return HDVectorMapB.generateSeeded(seed);
    }
}