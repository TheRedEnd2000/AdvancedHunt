package de.theredend2000.advancedhunt.menu.cron;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for anything that stores a cron-like schedule value.
 * Mirrors the RewardHolder pattern so the same cron menus can edit different targets
 * (e.g. progress reset cron or ACT rule cron).
 */
public interface CronExpressionHolder {

    /**
     * @return current expression value (may be null)
     */
    String getExpression();

    /**
     * Updates the stored value in-memory.
     */
    void setExpression(String expression);

    /**
     * Clears the stored value according to the holder's semantics.
     */
    void clear();

    /**
     * Persists the underlying owning object.
     */
    CompletableFuture<Void> save();
}
