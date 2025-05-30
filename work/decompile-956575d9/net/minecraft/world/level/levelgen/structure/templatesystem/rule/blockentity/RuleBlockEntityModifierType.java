package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.IRegistry;
import net.minecraft.core.registries.BuiltInRegistries;

public interface RuleBlockEntityModifierType<P extends RuleBlockEntityModifier> {

    RuleBlockEntityModifierType<Clear> CLEAR = register("clear", Clear.CODEC);
    RuleBlockEntityModifierType<Passthrough> PASSTHROUGH = register("passthrough", Passthrough.CODEC);
    RuleBlockEntityModifierType<AppendStatic> APPEND_STATIC = register("append_static", AppendStatic.CODEC);
    RuleBlockEntityModifierType<AppendLoot> APPEND_LOOT = register("append_loot", AppendLoot.CODEC);

    MapCodec<P> codec();

    private static <P extends RuleBlockEntityModifier> RuleBlockEntityModifierType<P> register(String s, MapCodec<P> mapcodec) {
        return (RuleBlockEntityModifierType) IRegistry.register(BuiltInRegistries.RULE_BLOCK_ENTITY_MODIFIER, s, (RuleBlockEntityModifierType) () -> {
            return mapcodec;
        });
    }
}
