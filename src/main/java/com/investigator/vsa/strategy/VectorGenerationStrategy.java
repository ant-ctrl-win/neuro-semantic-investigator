package com.investigator.vsa.strategy;

import com.investigator.vsa.HDVector;

public interface VectorGenerationStrategy {
    HDVector generate(String uri);
}