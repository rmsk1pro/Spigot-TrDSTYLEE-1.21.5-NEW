package org.bukkit.craftbukkit.inventory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.PotionRegistry;
import org.bukkit.Color;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.craftbukkit.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.potion.CraftPotionType;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

@DelegateDeserialization(SerializableMeta.class)
class CraftMetaPotion extends CraftMetaItem implements PotionMeta {

    @ItemMetaKey.Specific(ItemMetaKey.Specific.To.NBT)
    static final ItemMetaKeyType<PotionContents> POTION_CONTENTS = new ItemMetaKeyType<>(DataComponents.POTION_CONTENTS);
    static final ItemMetaKeyType<Float> POTION_DURATION_SCALE = new ItemMetaKeyType<>(DataComponents.POTION_DURATION_SCALE, "potion-duration-scale");
    static final ItemMetaKey POTION_EFFECTS = new ItemMetaKey("custom-effects");
    static final ItemMetaKey POTION_COLOR = new ItemMetaKey("custom-color");
    static final ItemMetaKey CUSTOM_NAME = new ItemMetaKey("custom-name");
    static final ItemMetaKey DEFAULT_POTION = new ItemMetaKey("potion-type");

    private PotionType type;
    private List<PotionEffect> customEffects;
    private Color color;
    private String customName;
    private Float potionDurationScale;

    CraftMetaPotion(CraftMetaItem meta) {
        super(meta);
        if (!(meta instanceof CraftMetaPotion potionMeta)) {
            return;
        }
        this.type = potionMeta.type;
        this.color = potionMeta.color;
        this.customName = potionMeta.customName;
        if (potionMeta.hasCustomEffects()) {
            this.customEffects = new ArrayList<>(potionMeta.customEffects);
        }
        this.potionDurationScale = potionMeta.potionDurationScale;
    }

    CraftMetaPotion(DataComponentPatch tag) {
        super(tag);
        getOrEmpty(tag, POTION_CONTENTS).ifPresent((potionContents) -> {
            potionContents.potion().ifPresent((potion) -> {
                type = CraftPotionType.minecraftHolderToBukkit(potion);
            });

            potionContents.customColor().ifPresent((customColor) -> {
                try {
                    color = Color.fromRGB(customColor);
                } catch (IllegalArgumentException ex) {
                    // Invalid colour
                }
            });

            potionContents.customName().ifPresent((name) -> {
                customName = name;
            });

            List<MobEffect> list = potionContents.customEffects();
            int length = list.size();
            customEffects = new ArrayList<>(length);

            for (int i = 0; i < length; i++) {
                MobEffect effect = list.get(i);
                PotionEffectType type = CraftPotionEffectType.minecraftHolderToBukkit(effect.getEffect());
                // SPIGOT-4047: Vanilla just disregards these
                if (type == null) {
                    continue;
                }

                int amp = effect.getAmplifier();
                int duration = effect.getDuration();
                boolean ambient = effect.isAmbient();
                boolean particles = effect.isVisible();
                boolean icon = effect.showIcon();
                customEffects.add(new PotionEffect(type, duration, amp, ambient, particles, icon));
            }
        });

        getOrEmpty(tag, POTION_DURATION_SCALE).ifPresent((potionDurationScale) -> {
            this.potionDurationScale = potionDurationScale;
        });
    }

    CraftMetaPotion(Map<String, Object> map) {
        super(map);
        String typeString = SerializableMeta.getString(map, DEFAULT_POTION.BUKKIT, true);
        if (typeString != null) {
            type = CraftPotionType.stringToBukkit(typeString);
        }

        Color color = SerializableMeta.getObject(Color.class, map, POTION_COLOR.BUKKIT, true);
        if (color != null) {
            setColor(color);
        }

        String name = SerializableMeta.getString(map, CUSTOM_NAME.BUKKIT, true);
        if (name != null) {
            setCustomName(name);
        }

        Iterable<?> rawEffectList = SerializableMeta.getObject(Iterable.class, map, POTION_EFFECTS.BUKKIT, true);
        if (rawEffectList == null) {
            return;
        }

        for (Object obj : rawEffectList) {
            Preconditions.checkArgument(obj instanceof PotionEffect, "Object (%s) in effect list is not valid", obj.getClass());
            addCustomEffect((PotionEffect) obj, true);
        }

        Float scale = SerializableMeta.getObject(Float.class, map, POTION_DURATION_SCALE.BUKKIT, true);
        if (scale != null) {
            setDurationScale(scale);
        }
    }

    @Override
    void applyToItem(CraftMetaItem.Applicator tag) {
        super.applyToItem(tag);

        if (isPotionEmpty()) {
            return;
        }

        Optional<Holder<PotionRegistry>> defaultPotion = (hasBasePotionType()) ? Optional.of(CraftPotionType.bukkitToMinecraftHolder(type)) : Optional.empty();
        Optional<Integer> potionColor = (hasColor()) ? Optional.of(this.color.asRGB()) : Optional.empty();
        Optional<String> customName = Optional.ofNullable(this.customName);

        List<MobEffect> effectList = new ArrayList<>();
        if (customEffects != null) {
            for (PotionEffect effect : customEffects) {
                effectList.add(new MobEffect(CraftPotionEffectType.bukkitToMinecraftHolder(effect.getType()), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
            }
        }

        tag.put(POTION_CONTENTS, new PotionContents(defaultPotion, potionColor, effectList, customName));

        if (hasDurationScale()) {
            tag.put(POTION_DURATION_SCALE, getDurationScale());
        }
    }

    @Override
    boolean isEmpty() {
        return super.isEmpty() && isPotionEmpty();
    }

    boolean isPotionEmpty() {
        return (type == null) && !(hasCustomEffects() || hasColor() || hasCustomName() || hasDurationScale());
    }

    @Override
    public CraftMetaPotion clone() {
        CraftMetaPotion clone = (CraftMetaPotion) super.clone();
        clone.type = type;
        if (this.customEffects != null) {
            clone.customEffects = new ArrayList<>(this.customEffects);
        }
        return clone;
    }

    @Override
    public void setBasePotionData(PotionData data) {
        setBasePotionType(CraftPotionUtil.fromBukkit(data));
    }

    @Override
    public PotionData getBasePotionData() {
        return CraftPotionUtil.toBukkit(getBasePotionType());
    }

    @Override
    public void setBasePotionType(PotionType potionType) {
        type = potionType;
    }

    @Override
    public PotionType getBasePotionType() {
        return type;
    }

    @Override
    public boolean hasBasePotionType() {
        return type != null;
    }

    @Override
    public boolean hasCustomEffects() {
        return customEffects != null && !customEffects.isEmpty();
    }

    @Override
    public List<PotionEffect> getCustomEffects() {
        if (hasCustomEffects()) {
            return ImmutableList.copyOf(customEffects);
        }
        return ImmutableList.of();
    }

    @Override
    public boolean addCustomEffect(PotionEffect effect, boolean overwrite) {
        Preconditions.checkArgument(effect != null, "Potion effect cannot be null");

        int index = indexOfEffect(effect.getType());
        if (index != -1) {
            if (overwrite) {
                PotionEffect old = customEffects.get(index);
                if (old.getAmplifier() == effect.getAmplifier() && old.getDuration() == effect.getDuration() && old.isAmbient() == effect.isAmbient()) {
                    return false;
                }
                customEffects.set(index, effect);
                return true;
            } else {
                return false;
            }
        } else {
            if (customEffects == null) {
                customEffects = new ArrayList<>();
            }
            customEffects.add(effect);
            return true;
        }
    }

    @Override
    public boolean removeCustomEffect(PotionEffectType type) {
        Preconditions.checkArgument(type != null, "Potion effect type cannot be null");

        if (!hasCustomEffects()) {
            return false;
        }

        boolean changed = false;
        Iterator<PotionEffect> iterator = customEffects.iterator();
        while (iterator.hasNext()) {
            PotionEffect effect = iterator.next();
            if (type.equals(effect.getType())) {
                iterator.remove();
                changed = true;
            }
        }
        if (customEffects.isEmpty()) {
            customEffects = null;
        }
        return changed;
    }

    @Override
    public boolean hasCustomEffect(PotionEffectType type) {
        Preconditions.checkArgument(type != null, "Potion effect type cannot be null");
        return indexOfEffect(type) != -1;
    }

    @Override
    public boolean setMainEffect(PotionEffectType type) {
        Preconditions.checkArgument(type != null, "Potion effect type cannot be null");
        int index = indexOfEffect(type);
        if (index == -1 || index == 0) {
            return false;
        }

        PotionEffect old = customEffects.get(0);
        customEffects.set(0, customEffects.get(index));
        customEffects.set(index, old);
        return true;
    }

    private int indexOfEffect(PotionEffectType type) {
        if (!hasCustomEffects()) {
            return -1;
        }

        for (int i = 0; i < customEffects.size(); i++) {
            if (customEffects.get(i).getType().equals(type)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean clearCustomEffects() {
        boolean changed = hasCustomEffects();
        customEffects = null;
        return changed;
    }

    @Override
    public boolean hasColor() {
        return color != null;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public boolean hasCustomName() {
        return this.customName != null;
    }

    @Override
    public String getCustomName() {
        return this.customName;
    }

    @Override
    public void setCustomName(String customName) {
        this.customName = customName;
    }

    @Override
    public boolean hasDurationScale() {
        return this.potionDurationScale != null;
    }

    @Override
    public float getDurationScale() {
        Preconditions.checkState(hasDurationScale(), "hasDurationScale is false");

        return this.potionDurationScale;
    }

    @Override
    public void setDurationScale(Float scale) {
        this.potionDurationScale = scale;
    }

    @Override
    int applyHash() {
        final int original;
        int hash = original = super.applyHash();
        if (type != null) {
            hash = 73 * hash + type.hashCode();
        }
        if (hasColor()) {
            hash = 73 * hash + color.hashCode();
        }
        if (hasCustomName()) {
            hash = 73 * hash + customName.hashCode();
        }
        if (hasCustomEffects()) {
            hash = 73 * hash + customEffects.hashCode();
        }
        if (hasDurationScale()) {
            hash = 73 * hash + potionDurationScale.hashCode();
        }
        return original != hash ? CraftMetaPotion.class.hashCode() ^ hash : hash;
    }

    @Override
    public boolean equalsCommon(CraftMetaItem meta) {
        if (!super.equalsCommon(meta)) {
            return false;
        }
        if (meta instanceof CraftMetaPotion) {
            CraftMetaPotion that = (CraftMetaPotion) meta;

            return Objects.equals(type, that.type)
                    && (this.hasCustomEffects() ? that.hasCustomEffects() && this.customEffects.equals(that.customEffects) : !that.hasCustomEffects())
                    && (this.hasColor() ? that.hasColor() && this.color.equals(that.color) : !that.hasColor())
                    && (this.hasCustomName() ? that.hasCustomName() && this.customName.equals(that.customName) : !that.hasCustomName())
                    && (this.hasDurationScale() ? that.hasDurationScale() && this.potionDurationScale.equals(that.potionDurationScale) : !that.hasDurationScale());
        }
        return true;
    }

    @Override
    boolean notUncommon(CraftMetaItem meta) {
        return super.notUncommon(meta) && (meta instanceof CraftMetaPotion || isPotionEmpty());
    }

    @Override
    Builder<String, Object> serialize(Builder<String, Object> builder) {
        super.serialize(builder);
        if (type != null) {
            builder.put(DEFAULT_POTION.BUKKIT, CraftPotionType.bukkitToString(type));
        }

        if (hasColor()) {
            builder.put(POTION_COLOR.BUKKIT, getColor());
        }

        if (hasCustomName()) {
            builder.put(CUSTOM_NAME.BUKKIT, getCustomName());
        }

        if (hasCustomEffects()) {
            builder.put(POTION_EFFECTS.BUKKIT, ImmutableList.copyOf(this.customEffects));
        }

        if (hasDurationScale()) {
            builder.put(POTION_DURATION_SCALE.BUKKIT, getDurationScale());
        }

        return builder;
    }
}
