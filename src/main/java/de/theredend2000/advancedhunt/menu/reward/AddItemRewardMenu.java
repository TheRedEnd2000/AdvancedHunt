package de.theredend2000.advancedhunt.menu.reward;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.menu.Menu;
import de.theredend2000.advancedhunt.menu.common.SingleItemInputMenu;
import de.theredend2000.advancedhunt.model.Reward;
import de.theredend2000.advancedhunt.model.RewardType;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A menu for adding an item reward to a treasure.
 * Player places an item in the center slot, then confirms to add it as a reward.
 */
public class AddItemRewardMenu extends SingleItemInputMenu {

    private final RewardsMenu parentMenu;
    private final Menu lastMenu;
    private final boolean edit;
    private final int rewardIndex;

    public AddItemRewardMenu(Player player, Main plugin, RewardsMenu parentMenu, Menu lastMenu, boolean edit, int rewardIndex) {
        super(player, plugin, lastMenu, true);
        this.parentMenu = parentMenu;
        this.lastMenu = lastMenu;
        this.edit = edit;
        this.rewardIndex = rewardIndex;
    }

    @Override
    protected String getGuiKeyBase() {
        return "gui.rewards.add.item";
    }

    @Override
    protected String getNoItemErrorKey() {
        return "error.rewards.add.item.no_item";
    }

    @Override
    protected void onConfirm(ItemStack itemSnapshot) {
        if (edit) {
            String serialized = ItemSerializer.serialize(itemSnapshot);
            parentMenu.updateRewardValue(rewardIndex, serialized);
            playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.updated",
                    "%type%", plugin.getMessageManager().getMessage("common.value", false)));
            parentMenu.open();
            return;
        }

        String serialized = ItemSerializer.serialize(itemSnapshot);
        Reward reward = new Reward(RewardType.ITEM, 100.0, null, null, serialized);
        parentMenu.addReward(reward);
        playerMenuUtility.sendMessage(plugin.getMessageManager().getMessage("feedback.rewards.added"));
        parentMenu.open();
    }

    @Override
    protected void onCancel() {
        lastMenu.open();
    }
}
