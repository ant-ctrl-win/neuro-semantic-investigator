package com.investigator.vsa;

public interface HDVector {
    HDVector bind(HDVector other);
    HDVector bundle(HDVector other);
    HDVector permute(int shift);
    double similarity(HDVector other);
}