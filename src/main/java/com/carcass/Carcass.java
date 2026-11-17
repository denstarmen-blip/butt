package com.carcass;

import com.carcass.entity.CorpseEntity;
import com.carcass.item.KnifeItem;
import com.carcass.item.ModTiers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Carcass.MODID)
public class Carcass {
    public static final String MODID = "carcass";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    // Ножі: (матеріал, тіків на розрізання (20 тіків = 1 с), шанс бонусного предмета)
    public static final RegistryObject<Item> FLINT_KNIFE = ITEMS.register("flint_knife",
            () -> new KnifeItem(ModTiers.FLINT, 80, 0.00f, new Item.Properties()));
    public static final RegistryObject<Item> IRON_KNIFE = ITEMS.register("iron_knife",
            () -> new KnifeItem(Tiers.IRON, 60, 0.15f, new Item.Properties()));
    public static final RegistryObject<Item> GOLDEN_KNIFE = ITEMS.register("golden_knife",
            () -> new KnifeItem(Tiers.GOLD, 36, 0.20f, new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_KNIFE = ITEMS.register("diamond_knife",
            () -> new KnifeItem(Tiers.DIAMOND, 48, 0.30f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_KNIFE = ITEMS.register("netherite_knife",
            () -> new KnifeItem(Tiers.NETHERITE, 30, 0.45f, new Item.Properties().fireResistant()));

    public static final RegistryObject<EntityType<CorpseEntity>> CORPSE = ENTITIES.register("corpse",
            () -> EntityType.Builder.<CorpseEntity>of(CorpseEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.4F).clientTrackingRange(8).updateInterval(3).build("corpse"));

    public Carcass() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(this::addCreative);
        MinecraftForge.EVENT_BUS.register(DeathHandler.class);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.COMBAT) {
            e.accept(FLINT_KNIFE);
            e.accept(IRON_KNIFE);
            e.accept(GOLDEN_KNIFE);
            e.accept(DIAMOND_KNIFE);
            e.accept(NETHERITE_KNIFE);
        }
    }
}
