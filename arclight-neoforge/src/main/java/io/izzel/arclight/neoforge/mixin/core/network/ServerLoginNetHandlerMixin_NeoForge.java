package io.izzel.arclight.neoforge.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.login.ServerLoginNetHandlerBridge;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetHandlerMixin_NeoForge implements ServerLoginNetHandlerBridge {

    @Override
    public Thread bridge$newHandleThread(String name, Runnable runnable) {
        return new Thread(SidedThreadGroups.SERVER, runnable, name);
    }
}
