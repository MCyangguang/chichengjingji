package com.chichengjingji.blockentity;

import com.chichengjingji.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class CashRegisterBE extends BlockEntity {
    private UUID ownerUUID;
    private String ownerName;

    public CashRegisterBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CASH_REGISTER.get(), pos, state);
    }

    public void setOwner(UUID uuid, String name) {
        this.ownerUUID = uuid;
        this.ownerName = name;
        setChanged();
    }

    public boolean isOwner(UUID uuid) {
        return ownerUUID != null && ownerUUID.equals(uuid);
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
            tag.putString("OwnerName", ownerName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
            ownerName = tag.getString("OwnerName");
        }
    }
}