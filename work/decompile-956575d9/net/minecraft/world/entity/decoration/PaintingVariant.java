package net.minecraft.world.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.ExtraCodecs;

public record PaintingVariant(int width, int height, MinecraftKey assetId, Optional<IChatBaseComponent> title, Optional<IChatBaseComponent> author) {

    public static final Codec<PaintingVariant> DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(ExtraCodecs.intRange(1, 16).fieldOf("width").forGetter(PaintingVariant::width), ExtraCodecs.intRange(1, 16).fieldOf("height").forGetter(PaintingVariant::height), MinecraftKey.CODEC.fieldOf("asset_id").forGetter(PaintingVariant::assetId), ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(PaintingVariant::title), ComponentSerialization.CODEC.optionalFieldOf("author").forGetter(PaintingVariant::author)).apply(instance, PaintingVariant::new);
    });
    public static final StreamCodec<RegistryFriendlyByteBuf, PaintingVariant> DIRECT_STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, PaintingVariant::width, ByteBufCodecs.VAR_INT, PaintingVariant::height, MinecraftKey.STREAM_CODEC, PaintingVariant::assetId, ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC, PaintingVariant::title, ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC, PaintingVariant::author, PaintingVariant::new);
    public static final Codec<Holder<PaintingVariant>> CODEC = RegistryFileCodec.<Holder<PaintingVariant>>create(Registries.PAINTING_VARIANT, PaintingVariant.DIRECT_CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<PaintingVariant>> STREAM_CODEC = ByteBufCodecs.holder(Registries.PAINTING_VARIANT, PaintingVariant.DIRECT_STREAM_CODEC);

    public int area() {
        return this.width() * this.height();
    }
}
