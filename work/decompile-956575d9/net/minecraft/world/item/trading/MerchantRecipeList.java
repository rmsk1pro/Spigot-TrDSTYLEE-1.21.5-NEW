package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public class MerchantRecipeList extends ArrayList<MerchantRecipe> {

    public static final Codec<MerchantRecipeList> CODEC = MerchantRecipe.CODEC.listOf().optionalFieldOf("Recipes", List.of()).xmap(MerchantRecipeList::new, Function.identity()).codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, MerchantRecipeList> STREAM_CODEC = MerchantRecipe.STREAM_CODEC.apply(ByteBufCodecs.collection(MerchantRecipeList::new));

    public MerchantRecipeList() {}

    private MerchantRecipeList(int i) {
        super(i);
    }

    private MerchantRecipeList(Collection<MerchantRecipe> collection) {
        super(collection);
    }

    @Nullable
    public MerchantRecipe getRecipeFor(ItemStack itemstack, ItemStack itemstack1, int i) {
        if (i > 0 && i < this.size()) {
            MerchantRecipe merchantrecipe = (MerchantRecipe) this.get(i);

            return merchantrecipe.satisfiedBy(itemstack, itemstack1) ? merchantrecipe : null;
        } else {
            for (int j = 0; j < this.size(); ++j) {
                MerchantRecipe merchantrecipe1 = (MerchantRecipe) this.get(j);

                if (merchantrecipe1.satisfiedBy(itemstack, itemstack1)) {
                    return merchantrecipe1;
                }
            }

            return null;
        }
    }

    public MerchantRecipeList copy() {
        MerchantRecipeList merchantrecipelist = new MerchantRecipeList(this.size());

        for (MerchantRecipe merchantrecipe : this) {
            merchantrecipelist.add(merchantrecipe.copy());
        }

        return merchantrecipelist;
    }
}
