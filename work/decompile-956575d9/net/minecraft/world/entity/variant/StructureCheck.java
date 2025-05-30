package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;

public record StructureCheck(HolderSet<Structure> requiredStructures) implements SpawnCondition {

    public static final MapCodec<StructureCheck> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(RegistryCodecs.homogeneousList(Registries.STRUCTURE).fieldOf("structures").forGetter(StructureCheck::requiredStructures)).apply(instance, StructureCheck::new);
    });

    public boolean test(SpawnContext spawncontext) {
        return spawncontext.level().getLevel().structureManager().getStructureWithPieceAt(spawncontext.pos(), this.requiredStructures).isValid();
    }

    @Override
    public MapCodec<StructureCheck> codec() {
        return StructureCheck.MAP_CODEC;
    }
}
