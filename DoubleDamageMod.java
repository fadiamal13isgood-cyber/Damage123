package dev.doubledamage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class DoubleDamageMod implements ModInitializer {

    private static final float BASE = 2.0f;

    // Safety limit so the number never overflows. 2^100 is already far
    // more damage than anything in the game can survive.
    private static final int MAX_STACKS = 100;

    private record Key(UUID player, UUID mob) {}

    private static final Map<Key, Integer> STACKS = new HashMap<>();
    private static boolean applying = false;

    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (applying) return true;
            if (!(entity instanceof Mob)) return true;
            if (!(source.getEntity() instanceof Player player)) return true;
            if (!(entity.level() instanceof ServerLevel level)) return true;

            Key key = new Key(player.getUUID(), entity.getUUID());
            int count = Math.min(STACKS.getOrDefault(key, 0) + 1, MAX_STACKS);
            STACKS.put(key, count);

            float multiplier = (float) Math.pow(BASE, count);

            applying = true;
            try {
                entity.hurtServer(level, source, amount * multiplier);
            } finally {
                applying = false;
            }
            return false;
        });

        // Forget a mob's stacks once it dies.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            UUID id = entity.getUUID();
            STACKS.keySet().removeIf(k -> k.mob().equals(id));
        });
    }
}
