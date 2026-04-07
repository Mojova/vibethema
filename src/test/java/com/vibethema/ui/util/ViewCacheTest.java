package com.vibethema.ui.util;

import com.vibethema.ui.MainView;
import com.vibethema.viewmodel.MainViewModel;
import de.saxsys.mvvmfx.ViewTuple;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ViewCacheTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Toolkit already started
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    void testWarmupAndGet() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        // 1. Warm up the view
        ViewCache.warmup(MainView.class);
        
        // 2. Wait for the warmup to potentially complete (one pulse)
        Platform.runLater(() -> {
            // 3. Retrieve from cache
            ViewTuple<MainView, MainViewModel> tuple = ViewCache.get(MainView.class);
            assertNotNull(tuple, "Should retrieve cached tuple");
            assertNotNull(tuple.getView(), "View should be instantiated");
            assertNotNull(tuple.getViewModel(), "ViewModel should be instantiated");
            
            // 4. Verify cache is now empty for this type (single ownership)
            // Note: we can't easily check internal cache size, but a second get should load fresh
            // but for this test we focus on the first success.
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Test timed out");
    }

    @Test
    void testGetFallback() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            // Ensure cache is clear for this call
            ViewCache.get(MainView.class); 
            
            // Should still return a tuple (freshly loaded)
            ViewTuple<MainView, MainViewModel> tuple = ViewCache.get(MainView.class);
            assertNotNull(tuple, "Fallback should return a fresh tuple");
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Test timed out");
    }
}
