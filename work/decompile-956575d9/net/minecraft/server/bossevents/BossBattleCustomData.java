package net.minecraft.server.bossevents;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import org.slf4j.Logger;

public class BossBattleCustomData {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<Map<MinecraftKey, BossBattleCustom.a>> EVENTS_CODEC = Codec.unboundedMap(MinecraftKey.CODEC, BossBattleCustom.a.CODEC);
    private final Map<MinecraftKey, BossBattleCustom> events = Maps.newHashMap();

    public BossBattleCustomData() {}

    @Nullable
    public BossBattleCustom get(MinecraftKey minecraftkey) {
        return (BossBattleCustom) this.events.get(minecraftkey);
    }

    public BossBattleCustom create(MinecraftKey minecraftkey, IChatBaseComponent ichatbasecomponent) {
        BossBattleCustom bossbattlecustom = new BossBattleCustom(minecraftkey, ichatbasecomponent);

        this.events.put(minecraftkey, bossbattlecustom);
        return bossbattlecustom;
    }

    public void remove(BossBattleCustom bossbattlecustom) {
        this.events.remove(bossbattlecustom.getTextId());
    }

    public Collection<MinecraftKey> getIds() {
        return this.events.keySet();
    }

    public Collection<BossBattleCustom> getEvents() {
        return this.events.values();
    }

    public NBTTagCompound save(HolderLookup.a holderlookup_a) {
        Map<MinecraftKey, BossBattleCustom.a> map = SystemUtils.mapValues(this.events, BossBattleCustom::pack);

        return (NBTTagCompound) BossBattleCustomData.EVENTS_CODEC.encodeStart(holderlookup_a.createSerializationContext(DynamicOpsNBT.INSTANCE), map).getOrThrow();
    }

    public void load(NBTTagCompound nbttagcompound, HolderLookup.a holderlookup_a) {
        Map<MinecraftKey, BossBattleCustom.a> map = (Map) BossBattleCustomData.EVENTS_CODEC.parse(holderlookup_a.createSerializationContext(DynamicOpsNBT.INSTANCE), nbttagcompound).resultOrPartial((s) -> {
            BossBattleCustomData.LOGGER.error("Failed to parse boss bar events: {}", s);
        }).orElse(Map.of());

        map.forEach((minecraftkey, bossbattlecustom_a) -> {
            this.events.put(minecraftkey, BossBattleCustom.load(minecraftkey, bossbattlecustom_a));
        });
    }

    public void onPlayerConnect(EntityPlayer entityplayer) {
        for (BossBattleCustom bossbattlecustom : this.events.values()) {
            bossbattlecustom.onPlayerConnect(entityplayer);
        }

    }

    public void onPlayerDisconnect(EntityPlayer entityplayer) {
        for (BossBattleCustom bossbattlecustom : this.events.values()) {
            bossbattlecustom.onPlayerDisconnect(entityplayer);
        }

    }
}
