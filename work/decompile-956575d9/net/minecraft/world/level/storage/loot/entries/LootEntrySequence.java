package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootEntrySequence extends LootEntryChildrenAbstract {

    public static final MapCodec<LootEntrySequence> CODEC = createCodec(LootEntrySequence::new);

    LootEntrySequence(List<LootEntryAbstract> list, List<LootItemCondition> list1) {
        super(list, list1);
    }

    @Override
    public LootEntryType getType() {
        return LootEntries.SEQUENCE;
    }

    @Override
    protected LootEntryChildren compose(List<? extends LootEntryChildren> list) {
        LootEntryChildren lootentrychildren;

        switch (list.size()) {
            case 0:
                lootentrychildren = LootEntrySequence.ALWAYS_TRUE;
                break;
            case 1:
                lootentrychildren = (LootEntryChildren) list.get(0);
                break;
            case 2:
                lootentrychildren = ((LootEntryChildren) list.get(0)).and((LootEntryChildren) list.get(1));
                break;
            default:
                lootentrychildren = (loottableinfo, consumer) -> {
                    for (LootEntryChildren lootentrychildren1 : list) {
                        if (!lootentrychildren1.expand(loottableinfo, consumer)) {
                            return false;
                        }
                    }

                    return true;
                };
        }

        return lootentrychildren;
    }

    public static LootEntrySequence.a sequential(LootEntryAbstract.a<?>... alootentryabstract_a) {
        return new LootEntrySequence.a(alootentryabstract_a);
    }

    public static class a extends LootEntryAbstract.a<LootEntrySequence.a> {

        private final ImmutableList.Builder<LootEntryAbstract> entries = ImmutableList.builder();

        public a(LootEntryAbstract.a<?>... alootentryabstract_a) {
            for (LootEntryAbstract.a<?> lootentryabstract_a : alootentryabstract_a) {
                this.entries.add(lootentryabstract_a.build());
            }

        }

        @Override
        protected LootEntrySequence.a getThis() {
            return this;
        }

        @Override
        public LootEntrySequence.a then(LootEntryAbstract.a<?> lootentryabstract_a) {
            this.entries.add(lootentryabstract_a.build());
            return this;
        }

        @Override
        public LootEntryAbstract build() {
            return new LootEntrySequence(this.entries.build(), this.getConditions());
        }
    }
}
