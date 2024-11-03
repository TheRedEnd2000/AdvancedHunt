package de.theredend2000.advancedhunt.managers;

import com.cryptomorin.xseries.XEntityType;
import de.theredend2000.advancedhunt.Main;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class ExtraManager{

    private ArrayList<UUID> fireworkUUID;

    public ExtraManager(){
        fireworkUUID = new ArrayList<>();
    }

    public void spawnFireworkRocket(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, XEntityType.FIREWORK_ROCKET.get());
        firework.setSilent(true);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        Random random = Main.getInstance().getRandom();
        FireworkEffect effect = FireworkEffect.builder().flicker(random.nextBoolean()).withColor(getColor(random.nextInt(17) + 1)).withFade(getColor(random.nextInt(17) + 1)).with(FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)]).trail(random.nextBoolean()).build();
        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(random.nextInt(2) + 1);
        firework.setFireworkMeta(fireworkMeta);
        fireworkUUID.add(firework.getUniqueId());
    }

    public Color getColor(int i) {
        switch (i) {
            case 1:
                return Color.AQUA;
            case 2:
                return Color.BLACK;
            case 3:
                return Color.BLUE;
            case 4:
                return Color.FUCHSIA;
            case 5:
                return Color.GRAY;
            case 6:
                return Color.GREEN;
            case 7:
                return Color.LIME;
            case 8:
                return Color.MAROON;
            case 9:
                return Color.NAVY;
            case 10:
                return Color.OLIVE;
            case 11:
                return Color.ORANGE;
            case 12:
                return Color.PURPLE;
            case 13:
                return Color.RED;
            case 14:
                return Color.SILVER;
            case 15:
                return Color.TEAL;
            case 16:
                return Color.WHITE;
            case 17:
                return Color.YELLOW;
        }
        return null;
    }

    public String decimalToFraction(double decimal) {
        BigDecimal bd = BigDecimal.valueOf(decimal);

        int denominator = (int) Math.pow(10, bd.scale());

        BigDecimal numeratorBd = bd.multiply(BigDecimal.valueOf(denominator));
        long numerator = numeratorBd.longValue();

        long gcd = gcd(numerator, denominator);
        numerator /= gcd;
        denominator /= gcd;
        StringBuilder result = new StringBuilder();
        result.append(numerator);
        result.append("/").append(denominator);
        return result.toString();
    }

    private long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public ArrayList<UUID> getFireworkUUID() {
        return fireworkUUID;
    }
}
