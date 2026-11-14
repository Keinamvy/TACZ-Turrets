package com.entropy.tacz_turrets.util;

import net.minecraft.util.Mth;

public class TurretEnergyStorage {
    protected long amount;
    protected long capacity;
    protected long maxInsert;
    protected long maxExtract;

    public TurretEnergyStorage(int capacity, int maxReceive) {
        this.capacity = capacity;
        this.maxInsert = maxReceive;
        this.maxExtract = 0;
        this.amount = 0;
    }

    public void consume(int amount) {
        this.amount = Math.max(0, this.amount - amount);
    }

    public void setLimits(int capacity, int maxReceive) {
        this.capacity = capacity;
        this.maxInsert = maxReceive;
        this.amount = Math.min(this.amount, capacity);
    }

    public void setEnergy(int amount) {
        this.amount = Mth.clamp(amount, 0, (int) this.capacity);
    }

    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, this.amount);
    }

    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, this.capacity);
    }

    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        int energyReceived = Math.min((int) (this.capacity - this.amount), Math.min((int) this.maxInsert, maxReceive));
        if (!simulate) this.amount += energyReceived;
        return energyReceived;
    }

    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        int energyExtracted = Math.min((int) this.amount, Math.min((int) this.maxExtract, maxExtract));
        if (!simulate) this.amount -= energyExtracted;
        return energyExtracted;
    }

    public boolean canReceive() {
        return this.maxInsert > 0;
    }

    public boolean canExtract() {
        return this.maxExtract > 0;
    }
}
