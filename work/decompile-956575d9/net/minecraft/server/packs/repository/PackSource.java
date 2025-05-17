package net.minecraft.server.packs.repository;

import java.util.function.UnaryOperator;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.IChatBaseComponent;

public interface PackSource {

    UnaryOperator<IChatBaseComponent> NO_DECORATION = UnaryOperator.identity();
    PackSource DEFAULT = create(PackSource.NO_DECORATION, true);
    PackSource BUILT_IN = create(decorateWithSource("pack.source.builtin"), true);
    PackSource FEATURE = create(decorateWithSource("pack.source.feature"), false);
    PackSource WORLD = create(decorateWithSource("pack.source.world"), true);
    PackSource SERVER = create(decorateWithSource("pack.source.server"), true);

    IChatBaseComponent decorate(IChatBaseComponent ichatbasecomponent);

    boolean shouldAddAutomatically();

    static PackSource create(final UnaryOperator<IChatBaseComponent> unaryoperator, final boolean flag) {
        return new PackSource() {
            @Override
            public IChatBaseComponent decorate(IChatBaseComponent ichatbasecomponent) {
                return (IChatBaseComponent) unaryoperator.apply(ichatbasecomponent);
            }

            @Override
            public boolean shouldAddAutomatically() {
                return flag;
            }
        };
    }

    private static UnaryOperator<IChatBaseComponent> decorateWithSource(String s) {
        IChatBaseComponent ichatbasecomponent = IChatBaseComponent.translatable(s);

        return (ichatbasecomponent1) -> {
            return IChatBaseComponent.translatable("pack.nameAndSource", ichatbasecomponent1, ichatbasecomponent).withStyle(EnumChatFormat.GRAY);
        };
    }
}
