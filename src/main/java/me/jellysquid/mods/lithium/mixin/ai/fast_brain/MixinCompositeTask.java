package me.jellysquid.mods.lithium.mixin.ai.fast_brain;

import me.jellysquid.mods.lithium.common.util.IIterableWeightedList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.CompositeTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WeightedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(CompositeTask.class)
public class MixinCompositeTask<E extends LivingEntity> {
    @Shadow
    @Final
    private WeightedList<Task<? super E>> tasks;

    @Shadow
    @Final
    private Set<MemoryModuleType<?>> memoriesToForgetWhenStopped;

    /**
     * @reason Replace stream code with traditional iteration
     * @author JellySquid
     */
    @Overwrite
    public boolean shouldKeepRunning(ServerWorld world, E entity, long time) {
        for (Task<? super E> task : IIterableWeightedList.cast(this.tasks)) {
            if (task.getStatus() == Task.Status.RUNNING) {
                if (task.shouldKeepRunning(world, entity, time)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @reason Replace stream code with traditional iteration
     * @author JellySquid
     */
    @Overwrite
    public void keepRunning(ServerWorld world, E entity, long time) {
        for (Task<? super E> task : IIterableWeightedList.cast(this.tasks)) {
            if (task.getStatus() == Task.Status.RUNNING) {
                task.tick(world, entity, time);
            }
        }
    }

    /**
     * @reason Replace stream code with traditional iteration
     * @author JellySquid
     */
    @Overwrite
    public void finishRunning(ServerWorld world, E entity, long time) {
        for (Task<? super E> task : IIterableWeightedList.cast(this.tasks)) {
            if (task.getStatus() == Task.Status.RUNNING) {
                task.stop(world, entity, time);
            }
        }

        Brain<?> brain = entity.getBrain();

        for (MemoryModuleType<?> module : this.memoriesToForgetWhenStopped) {
            brain.forget(module);
        }
    }

    @Mixin(targets = "net/minecraft/entity/ai/brain/task/CompositeTask$RunMode")
    private static class MixinRunMode {
        @Mixin(targets = "net/minecraft/entity/ai/brain/task/CompositeTask$RunMode$1")
        private static class MixinRunOne {
            /**
             * @reason Replace stream code with traditional iteration
             * @author JellySquid
             */
            @Overwrite
            public <E extends LivingEntity> void run(WeightedList<Task<? super E>> tasks, ServerWorld serverWorld_1, E entity, long time) {
                for (Task<? super E> task : IIterableWeightedList.cast(tasks)) {
                    if (task.getStatus() == Task.Status.STOPPED && task.tryStarting(serverWorld_1, entity, time)) {
                        break;
                    }
                }
            }
        }

        @Mixin(targets = "net/minecraft/entity/ai/brain/task/CompositeTask$RunMode$2")
        private static class MixinTryAll {
            /**
             * @reason Replace stream code with traditional iteration
             * @author JellySquid
             */
            @Overwrite
            public <E extends LivingEntity> void run(WeightedList<Task<? super E>> tasks, ServerWorld serverWorld_1, E entity, long time) {
                for (Task<? super E> task : IIterableWeightedList.cast(tasks)) {
                    if (task.getStatus() == Task.Status.STOPPED) {
                        task.tryStarting(serverWorld_1, entity, time);
                    }
                }
            }
        }
    }
}
