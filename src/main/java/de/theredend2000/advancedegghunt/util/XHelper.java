package de.theredend2000.advancedegghunt.util;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Particle;

import java.util.Optional;

public class XHelper {
    public static XSound ParseSound(String soundString, XSound def) {
        if (soundString == null || soundString.isEmpty())
            return def;

        Optional<XSound> sound = XSound.matchXSound(soundString);

        if (sound.isEmpty())
            return def;

        return sound.get();
    }

    public static XMaterial ParseMaterial(String materialString, XMaterial def) {
        if (materialString == null || materialString.isEmpty())
            return def;

        Optional<XMaterial> material = XMaterial.matchXMaterial(materialString);

        if (material.isEmpty())
            return def;

        return material.get();
    }

    public static Particle ParseParticle(String particlelString, Particle def) {
        if (particlelString == null || particlelString.isEmpty())
            return def;

        Particle particle = XParticle.getParticle(particlelString);

        if (particle == null)
            return def;

        return particle;
    }
}
