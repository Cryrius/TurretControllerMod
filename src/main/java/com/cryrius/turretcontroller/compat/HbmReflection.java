package com.cryrius.turretcontroller.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

/**
 * Optional HBM adapter.  This is the only class that knows the names used by
 * HBM's turret implementation, and it knows them as strings only.
 *
 * <p>Do not replace the string lookups with imports.  An import would put HBM
 * in the constant pool and Forge would try to resolve it while loading this
 * mod, making a standalone installation fail before the optional integration
 * could be skipped.</p>
 */
public final class HbmReflection {

    private static final String TURRET_PACKAGE = "com.hbm.tileentity.turret.";
    private static final String BASE_TURRET_NAME = TURRET_PACKAGE + "TileEntityTurretBaseNT";

    private HbmReflection() {
    }

    /**
     * Checks the runtime type without loading any optional class.  The package
     * check covers all current HBM turrets and the base-name check covers
     * subclasses even if a future version moves a concrete class.
     */
    public static boolean isTurret(TileEntity tile) {
        if (tile == null) {
            return false;
        }
        Class<?> type = tile.getClass();
        while (type != null && type != Object.class) {
            String name = type.getName();
            if (BASE_TURRET_NAME.equals(name)) {
                return true;
            }
            if (name.startsWith(TURRET_PACKAGE)
                    && name.indexOf("TileEntityTurret") >= 0) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    /** Returns whether the optional base class can currently be resolved. */
    public static boolean isAvailable() {
        try {
            Class.forName(BASE_TURRET_NAME, false,
                    HbmReflection.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            // ClassNotFoundException is expected in a standalone installation.
            return false;
        }
    }

    /** Reads a defensive snapshot for a GUI or diagnostic command. */
    public static TurretSnapshot snapshot(TileEntity tile) {
        if (!isTurret(tile)) {
            return TurretSnapshot.unavailable();
        }
        Object target = getField(tile, "target");
        Object power = getField(tile, "power");
        Object maxPower = invokeNoArgs(tile, "getMaxPower");
        Object yaw = getField(tile, "rotationYaw");
        Object pitch = getField(tile, "rotationPitch");
        Object aligned = getField(tile, "aligned");
        Object enabled = getField(tile, "isOn");
        if (enabled == null) {
            enabled = invokeNoArgs(tile, "isOn");
        }

        String name = tile.getClass().getSimpleName();
        Object reportedName = invokeNoArgs(tile, "getName");
        if (reportedName instanceof String && ((String) reportedName).length() > 0) {
            name = (String) reportedName;
        }
        return new TurretSnapshot(
                true,
                asBoolean(enabled),
                asBoolean(aligned),
                target instanceof Entity ? (Entity) target : null,
                asLong(power),
                asLong(maxPower),
                asDouble(yaw),
                asDouble(pitch),
                name);
    }

    /** Sets the native on/off flag when the target implementation exposes it. */
    public static boolean setEnabled(TileEntity turret, boolean enabled) {
        if (!isTurret(turret)) {
            return false;
        }
        Object result = invokeBoolean(turret, "setOn", enabled);
        if (result != NO_RESULT) {
            return true;
        }
        if (setField(turret, "isOn", Boolean.valueOf(enabled))) {
            return true;
        }
        // A small compatibility concession for forks that renamed the field.
        return setField(turret, "enabled", Boolean.valueOf(enabled));
    }

    /**
     * Installs a target and, for HBM versions that cache a target position,
     * refreshes that cache as well.  Passing null clears the lock.
     */
    public static boolean setTarget(TileEntity turret, Entity target) {
        if (!isTurret(turret)) {
            return false;
        }
        boolean changed = setField(turret, "target", target);
        if (target == null) {
            setField(turret, "tPos", null);
        } else {
            setField(turret, "tPos", Vec3.createVectorHelper(target.posX, target.posY, target.posZ));
        }
        return changed;
    }

    public static Entity getTarget(TileEntity turret) {
        Object value = getField(turret, "target");
        return value instanceof Entity ? (Entity) value : null;
    }

    /** Applies the four native HBM target category switches if present. */
    public static void setTargetingFlags(TileEntity turret, boolean players,
                                         boolean animals, boolean mobs,
                                         boolean machines) {
        if (!isTurret(turret)) {
            return;
        }
        setField(turret, "targetPlayers", Boolean.valueOf(players));
        setField(turret, "targetAnimals", Boolean.valueOf(animals));
        setField(turret, "targetMobs", Boolean.valueOf(mobs));
        setField(turret, "targetMachines", Boolean.valueOf(machines));
    }

    public static Object getField(TileEntity object, String name) {
        if (object == null || name == null) {
            return null;
        }
        Field field = findField(object.getClass(), name);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(object);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean setField(Object object, String name, Object value) {
        Field field = findField(object.getClass(), name);
        if (field == null) {
            return false;
        }
        try {
            field.setAccessible(true);
            field.set(object, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Field findField(Class<?> type, String wanted) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(wanted);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object invokeNoArgs(Object object, String name) {
        if (object == null) {
            return null;
        }
        Class<?> type = object.getClass();
        while (type != null && type != Object.class) {
            Method[] methods;
            try {
                methods = type.getDeclaredMethods();
            } catch (Throwable ignored) {
                return null;
            }
            for (Method method : methods) {
                if (name.equals(method.getName()) && method.getParameterTypes().length == 0) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(object);
                    } catch (Throwable ignored) {
                        return null;
                    }
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static final Object NO_RESULT = new Object();

    private static Object invokeBoolean(Object object, String name, boolean value) {
        if (object == null) {
            return NO_RESULT;
        }
        Class<?> type = object.getClass();
        while (type != null && type != Object.class) {
            try {
                Method[] methods = type.getDeclaredMethods();
                for (Method method : methods) {
                    Class<?>[] parameters = method.getParameterTypes();
                    if (name.equals(method.getName()) && parameters.length == 1
                            && (parameters[0] == Boolean.TYPE || parameters[0] == Boolean.class)) {
                        method.setAccessible(true);
                        return method.invoke(object, Boolean.valueOf(value));
                    }
                }
            } catch (Throwable ignored) {
                // Try the superclass; obfuscated/final methods should not break control.
            }
            type = type.getSuperclass();
        }
        return NO_RESULT;
    }

    private static boolean asBoolean(Object value) {
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    private static long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static double asDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0D;
    }

    /** Immutable, dependency-free state returned to UI/API callers. */
    public static final class TurretSnapshot {
        public final boolean available;
        public final boolean enabled;
        public final boolean aligned;
        public final Entity target;
        public final long power;
        public final long maxPower;
        public final double yaw;
        public final double pitch;
        public final String name;

        private TurretSnapshot(boolean available, boolean enabled, boolean aligned,
                               Entity target, long power, long maxPower,
                               double yaw, double pitch, String name) {
            this.available = available;
            this.enabled = enabled;
            this.aligned = aligned;
            this.target = target;
            this.power = power;
            this.maxPower = maxPower;
            this.yaw = yaw;
            this.pitch = pitch;
            this.name = name;
        }

        private static TurretSnapshot unavailable() {
            return new TurretSnapshot(false, false, false, null, 0L, 0L,
                    0D, 0D, "Unavailable");
        }
    }
}
