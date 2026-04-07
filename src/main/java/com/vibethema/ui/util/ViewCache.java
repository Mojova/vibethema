package com.vibethema.ui.util;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import de.saxsys.mvvmfx.FluentViewLoader;
import de.saxsys.mvvmfx.JavaView;
import de.saxsys.mvvmfx.ViewModel;
import de.saxsys.mvvmfx.ViewTuple;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A simple cache for pre-loading and reusing expensive JavaFX views. */
public class ViewCache {
    private static final Logger logger = LoggerFactory.getLogger(ViewCache.class);
    private static final Map<Class<? extends JavaView<?>>, ViewTuple<?, ?>> cache =
            new ConcurrentHashMap<>();

    /**
     * Pre-loads a view in the current thread (should be called from a background thread or at
     * startup if safe). Note: MVVMFX FluentViewLoader might have UI-thread requirements for certain
     * components.
     */
    public static <V extends JavaView<VM>, VM extends ViewModel> void warmup(Class<V> viewType) {
        if (cache.containsKey(viewType)) return;

        // We load it on the UI thread to be safe with JavaFX node creation rules
        Platform.runLater(
                () -> {
                    try {
                        if (!cache.containsKey(viewType)) {
                            logger.info("Warming up view: {}", viewType.getSimpleName());
                            ViewTuple<V, VM> tuple = FluentViewLoader.javaView(viewType).load();
                            cache.put(viewType, tuple);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to warmup view: {}", viewType.getSimpleName(), e);
                    }
                });
    }

    /**
     * Gets a cached ViewTuple or loads a new one if not cached. The returned instance is removed
     * from the cache to ensure ownership.
     */
    @SuppressWarnings("unchecked")
    public static <V extends JavaView<VM>, VM extends ViewModel> ViewTuple<V, VM> get(
            Class<V> viewType) {
        ViewTuple<V, VM> tuple = (ViewTuple<V, VM>) cache.remove(viewType);
        if (tuple == null) {
            logger.debug(
                    "Cache miss for view: {}, loading synchronously", viewType.getSimpleName());
            return FluentViewLoader.javaView(viewType).load();
        }
        logger.debug("Cache hit for view: {}", viewType.getSimpleName());
        return tuple;
    }
}
