package de.theredend2000.advancedhunt.menu.cron;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.model.ActRule;
import de.theredend2000.advancedhunt.model.Collection;

import java.util.concurrent.CompletableFuture;

final class ActRuleCronHolder implements CronExpressionHolder {

    private final Main plugin;
    private final Collection collection;
    private final ActRule actRule;

    ActRuleCronHolder(Main plugin, Collection collection, ActRule actRule) {
        this.plugin = plugin;
        this.collection = collection;
        this.actRule = actRule;
    }

    @Override
    public String getExpression() {
        return actRule.getCronExpression();
    }

    @Override
    public void setExpression(String expression) {
        actRule.setCronExpression(expression);
    }

    @Override
    public void clear() {
        actRule.setCronExpression(CronEditPolicy.SPECIAL_NONE);
    }

    @Override
    public CompletableFuture<Void> save() {
        return plugin.getCollectionManager().saveCollection(collection);
    }
}
