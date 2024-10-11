package de.theredend2000.advancedhunt.util;

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

        return sound.orElse(def);
    }

    public static XMaterial ParseMaterial(String materialString, XMaterial def) {
        if (materialString == null || materialString.isEmpty())
            return def;

        Optional<XMaterial> material = XMaterial.matchXMaterial(materialString);

        return material.orElse(def);
    }

    public static Particle ParseParticle(String particlelString, XParticle def) {
        if (particlelString == null || particlelString.isEmpty())
            return def.get();

        Optional<XParticle> particle = XParticle.of(particlelString);

        return particle.orElse(def).get();
    }
}
