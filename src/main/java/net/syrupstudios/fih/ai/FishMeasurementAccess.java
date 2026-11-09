package net.syrupstudios.fih.ai;

public interface FishMeasurementAccess {
    float fih$lengthCm();
    float fih$weightKg();
    void fih$setMeasurements(float lengthCm, float weightKg);
}
