package me.sirius.core.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Scheduler {

    private static final Folia FOLIA = Folia.detect();

    private final Plugin plugin;

    public Scheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isFolia() {
        return FOLIA != null;
    }

    public void sync(Runnable task) {
        if (FOLIA != null) {
            FOLIA.globalExecute(plugin, task);
        } else if (plugin.getServer().isPrimaryThread()) {
            task.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public void region(Location location, Runnable task) {
        if (FOLIA != null) {
            FOLIA.regionExecute(plugin, location, task);
        } else {
            sync(task);
        }
    }

    public void entity(Entity entity, Runnable task) {
        if (FOLIA != null) {
            FOLIA.entityRun(plugin, entity, task);
        } else {
            sync(task);
        }
    }

    public void async(Runnable task) {
        if (FOLIA != null) {
            FOLIA.asyncNow(plugin, task);
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void syncLater(Runnable task, long delay) {
        if (delay <= 0) {
            sync(task);
        } else if (FOLIA != null) {
            FOLIA.globalDelayed(plugin, task, delay);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    public void asyncLater(Runnable task, long delayTicks) {
        if (FOLIA != null) {
            FOLIA.asyncDelayed(plugin, task, Math.max(1, delayTicks) * 50L);
        } else {
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    public Task syncTimer(Runnable task, long delay, long period) {
        if (FOLIA != null) {
            return FOLIA.globalTimer(plugin, task, Math.max(1, delay), Math.max(1, period));
        }
        BukkitTask handle = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, task, delay, period);
        return handle::cancel;
    }

    public Task asyncTimer(Runnable task, long delay, long period) {
        if (FOLIA != null) {
            return FOLIA.asyncTimer(plugin, task,
                    Math.max(1, delay) * 50L, Math.max(1, period) * 50L);
        }
        BukkitTask handle = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, task, delay, period);
        return handle::cancel;
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        async(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public void cancelAll() {
        if (FOLIA != null) {
            FOLIA.cancelAll(plugin);
        } else {
            plugin.getServer().getScheduler().cancelTasks(plugin);
        }
    }

    @FunctionalInterface
    public interface Task {
        void cancel();
    }

    private record Folia(
            Object globalScheduler,
            Object asyncScheduler,
            Object regionScheduler,
            Method globalExecute,
            Method globalDelayed,
            Method globalTimer,
            Method globalCancel,
            Method asyncNow,
            Method asyncDelayed,
            Method asyncTimer,
            Method asyncCancel,
            Method regionExecute,
            Method entityScheduler,
            Method entityRun,
            Method taskCancel
    ) {

        static Folia detect() {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            } catch (ClassNotFoundException notFolia) {
                return null;
            }
            try {
                var server = org.bukkit.Bukkit.getServer();
                var serverType = org.bukkit.Server.class;

                Object global = serverType.getMethod("getGlobalRegionScheduler").invoke(server);
                Object async = serverType.getMethod("getAsyncScheduler").invoke(server);
                Object region = serverType.getMethod("getRegionScheduler").invoke(server);

                Class<?> globalType = global.getClass();
                Class<?> asyncType = async.getClass();
                Class<?> regionType = region.getClass();
                Class<?> taskType = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");

                return new Folia(
                        global, async, region,
                        method(globalType, "execute", Plugin.class, Runnable.class),
                        method(globalType, "runDelayed", Plugin.class, Consumer.class, long.class),
                        method(globalType, "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class),
                        method(globalType, "cancelTasks", Plugin.class),
                        method(asyncType, "runNow", Plugin.class, Consumer.class),
                        method(asyncType, "runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class),
                        method(asyncType, "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class),
                        method(asyncType, "cancelTasks", Plugin.class),
                        method(regionType, "execute", Plugin.class, Location.class, Runnable.class),
                        Entity.class.getMethod("getScheduler"),
                        null,
                        taskType.getMethod("cancel"));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Folia fue detectado pero su API de planificación no coincide con la esperada. "
                        + "Actualiza SiriusCore.", e);
            }
        }

        private static Method method(Class<?> owner, String name, Class<?>... params)
                throws NoSuchMethodException {
            Method method = owner.getMethod(name, params);
            method.setAccessible(true);
            return method;
        }

        void globalExecute(Plugin plugin, Runnable task) {
            invoke(globalExecute, globalScheduler, plugin, task);
        }

        void globalDelayed(Plugin plugin, Runnable task, long ticks) {
            invoke(globalDelayed, globalScheduler, plugin, consumer(task), ticks);
        }

        Task globalTimer(Plugin plugin, Runnable task, long delay, long period) {
            Object handle = invoke(globalTimer, globalScheduler, plugin, consumer(task), delay, period);
            return () -> invoke(taskCancel, handle);
        }

        void asyncNow(Plugin plugin, Runnable task) {
            invoke(asyncNow, asyncScheduler, plugin, consumer(task));
        }

        void asyncDelayed(Plugin plugin, Runnable task, long millis) {
            invoke(asyncDelayed, asyncScheduler, plugin, consumer(task), millis, TimeUnit.MILLISECONDS);
        }

        Task asyncTimer(Plugin plugin, Runnable task, long delayMillis, long periodMillis) {
            Object handle = invoke(asyncTimer, asyncScheduler, plugin, consumer(task),
                    delayMillis, periodMillis, TimeUnit.MILLISECONDS);
            return () -> invoke(taskCancel, handle);
        }

        void regionExecute(Plugin plugin, Location location, Runnable task) {
            invoke(regionExecute, regionScheduler, plugin, location, task);
        }

        void entityRun(Plugin plugin, Entity entity, Runnable task) {
            try {
                Object entityScheduler = this.entityScheduler.invoke(entity);
                Method run = entityScheduler.getClass()
                        .getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                run.setAccessible(true);
                run.invoke(entityScheduler, plugin, consumer(task), null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("No se pudo planificar en la entidad", e);
            }
        }

        void cancelAll(Plugin plugin) {
            invoke(globalCancel, globalScheduler, plugin);
            invoke(asyncCancel, asyncScheduler, plugin);
        }

        private static Consumer<Object> consumer(Runnable task) {
            return ignored -> task.run();
        }

        private static Object invoke(Method method, Object target, Object... args) {
            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Acceso denegado a " + method.getName(), e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException("Falló " + method.getName(), cause);
            }
        }
    }
}
