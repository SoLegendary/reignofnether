package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.hud.buttons.StartButtons.ICON_SIZE;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class SurvivalClientEvents {

    public static int lastWaveNumber = 0;
    public static int waveNumber = 1;
    public static boolean isEnabled = false;
    public static WaveDifficulty difficulty = WaveDifficulty.BEGINNER;

    private static Minecraft MC = Minecraft.getInstance();

    public static int getMinutesPerDay() {
        return switch (SurvivalClientEvents.difficulty) {
            case BEGINNER -> 20;
            case EASY -> 15;
            case MEDIUM -> 12;
            case HARD -> 9;
            case EXTREME -> 6;
        };
    }

    public static void setWaveNumber(long number) {
        lastWaveNumber = waveNumber;
        waveNumber = (int) number;
    }

    public static void setRandomSeed(long number) {
        Wave.randomSeed = number;
        Wave.reseedWaves();
    }

    public static void reset() {
        isEnabled = false;
        waveNumber = 1;
    }

    public static void enable(WaveDifficulty diff) {
        if (MC.player == null)
            return;

        difficulty = diff;
        isEnabled = true;
    }

    public static Button getNextWaveButton() {
        return new Button("Next Survival Wave",
                ICON_SIZE,
                switch (SurvivalClientEvents.difficulty) {
                    case BEGINNER -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/wooden_sword.png");
                    case EASY -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/stone_sword.png");
                    case MEDIUM -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_sword.png");
                    case HARD -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/diamond_sword.png");
                    case EXTREME -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_sword.png");
                },
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    if (SurvivalClientEvents.waveNumber < 30 && ResearchClient.hasCheat("thereisnospoon")) {
                        SurvivalClientEvents.waveNumber += 1;
                        SurvivalServerboundPacket.setWaveNumber(SurvivalClientEvents.waveNumber);
                    }
                },
                () -> {
                    if (SurvivalClientEvents.waveNumber > 1 && ResearchClient.hasCheat("thereisnospoon")) {
                        SurvivalClientEvents.waveNumber -= 1;
                        SurvivalServerboundPacket.setWaveNumber(SurvivalClientEvents.waveNumber);
                    }
                },
                getWaveTooltip()
        );
    }

    private static String str(String string) {
        Wave wave = Wave.getWave(waveNumber);
        String localePrefix = switch (wave.faction) {
            case VILLAGERS -> "units.villagers.reignofnether.";
            case MONSTERS -> "units.monsters.reignofnether.";
            case PIGLINS -> "units.piglins.reignofnether.";
            case NONE, NEUTRAL -> "";
        };
        return I18n.get(localePrefix + string);
    }

    private static FormattedCharSequence slimeFcs() {
        Wave wave = Wave.getWave(waveNumber);
        String research = wave.highestUnitTier >= 6 ? (" [" + I18n.get("research.reignofnether.slime_conversion") + "]") : "";
        return FormattedCharSequence.forward(
                I18n.get("units.monsters.reignofnether.slime") + " " +
                        I18n.get("units.monsters.reignofnether.slime.size", wave.highestUnitTier) +
                        research,
                Style.EMPTY);
    }

    private static FormattedCharSequence magmaCubeFcs() {
        Wave wave = Wave.getWave(waveNumber);
        String research = wave.highestUnitTier >= 6 ? (" [" + I18n.get("research.reignofnether.cube_magma") + "]") : "";
        return FormattedCharSequence.forward(
                I18n.get("units.piglins.reignofnether.magma_cube") + " " +
                        I18n.get("units.monsters.reignofnether.slime.size", wave.highestUnitTier) +
                        research,
                Style.EMPTY);
    }

    private static String research(String string) {
        return " [" + I18n.get("research.reignofnether." + string) + "]";
    }

    private static String armoured(int plus) {
        Wave wave = Wave.getWave(waveNumber);
        String str = " (" + I18n.get("hud.units.reignofnether.armoured");
        str += new String(new char[plus]).replace("\0", "+");
        return str + ")";
    }

    private static String enchant(String string) {
        return " [" + I18n.get("hud.enchant.reignofnether." + string) + "]";
    }

    private static String faction(Faction faction) {
        return switch (faction) {
            case VILLAGERS -> I18n.get("hud.faction.reignofnether.villager");
            case MONSTERS -> I18n.get("hud.faction.reignofnether.monster");
            case PIGLINS -> I18n.get("hud.faction.reignofnether.piglin");
            case NONE, NEUTRAL -> I18n.get("hud.faction.reignofnether.random");
        };
    }

    public static List<FormattedCharSequence> getWaveTooltip() {
        ArrayList<FormattedCharSequence> tooltip = new ArrayList<>();
        Wave wave = Wave.getWave(waveNumber);

        tooltip.add(FormattedCharSequence.forward(I18n.get("survival.reignofnether.next_wave",
                wave.number, wave.highestUnitTier, faction(wave.faction)), Style.EMPTY.withBold(true)));

        tooltip.add(FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.survival5",
                SurvivalClientEvents.difficulty, SurvivalClientEvents.getMinutesPerDay()), Style.EMPTY));

        tooltip.add(FormattedCharSequence.forward("", Style.EMPTY));

        if (wave.faction == Faction.MONSTERS) {
            if (wave.highestUnitTier == 1) {
                tooltip.add(fcs(str("zombie_piglin")));
                tooltip.add(fcs(str("zombie")));
                tooltip.add(fcs(str("skeleton")));
            }
            if (wave.highestUnitTier == 2) {
                tooltip.add(fcs(str("zombie_piglin")));
                tooltip.add(fcs(str("zombie") + "/" + str("husk")));
                tooltip.add(fcs(str("skeleton") + "/" + str("stray")));
                tooltip.add(fcs(str("spider")));
                tooltip.add(slimeFcs());
            }
            if (wave.highestUnitTier == 3) {
                tooltip.add(fcs(str("zombie_piglin")));
                tooltip.add(fcs(str("husk") + "/" + str("drowned")));
                tooltip.add(fcs(str("skeleton") + "/" + str("stray")));
                tooltip.add(fcs(str("poison_spider")));
                tooltip.add(fcs(str("spider_jockey")));
                tooltip.add(fcs(str("creeper")));
                tooltip.add(slimeFcs());
            }
            if (wave.highestUnitTier == 4) {
                tooltip.add(fcs(str("husk") + "/" + str("drowned") + armoured(0)));
                tooltip.add(fcs(str("stray") + armoured(0)));
                tooltip.add(fcs(str("poison_spider")));
                tooltip.add(fcs(str("spider_jockey")));
                tooltip.add(fcs(str("creeper")));
                tooltip.add(fcs(str("enderman")));
                tooltip.add(slimeFcs());
                // spider webs
            }
            if (wave.highestUnitTier == 5) {
                tooltip.add(fcs(str("drowned") + armoured(1)));
                tooltip.add(fcs(str("stray") + armoured(1)));
                tooltip.add(fcs(str("poison_spider_jockey")));
                tooltip.add(fcs(str("creeper")));
                tooltip.add(fcs(str("enderman")));
                tooltip.add(fcs(str("warden")));
                tooltip.add(slimeFcs());
            }
            if (wave.highestUnitTier >= 6) {
                tooltip.add(fcs(str("drowned") + armoured(2)));
                tooltip.add(fcs(str("stray") + armoured(2)));
                tooltip.add(fcs(str("poison_spider_jockey")));
                tooltip.add(fcs(str("charged_creeper")));
                tooltip.add(fcs(str("enderman")));
                tooltip.add(fcs(str("warden")));
                tooltip.add(slimeFcs());
            }
        }
        else if (wave.faction == Faction.PIGLINS) {
            if (wave.highestUnitTier == 1) {
                tooltip.add(fcs(str("brute")));
                tooltip.add(fcs(str("headhunter")));
            }
            if (wave.highestUnitTier == 2) {
                tooltip.add(fcs(str("brute")));
                tooltip.add(fcs(str("headhunter")));
                tooltip.add(fcs(str("hoglin")));
                tooltip.add(magmaCubeFcs());
            }
            if (wave.highestUnitTier == 3) {
                tooltip.add(fcs(str("brute")));
                tooltip.add(fcs(str("headhunter")));
                tooltip.add(fcs(str("hoglin")));
                tooltip.add(fcs(str("blaze")));
                tooltip.add(magmaCubeFcs());
            }
            if (wave.highestUnitTier == 4) {
                tooltip.add(fcs(str("brute") + research("brute_shields") + armoured(0)));
                tooltip.add(fcs(str("headhunter") + research("heavy_tridents") + armoured(0)));
                tooltip.add(fcs(str("hoglin")));
                tooltip.add(fcs(str("hoglin_rider")));
                tooltip.add(fcs(str("blaze")));
                tooltip.add(fcs(str("wither_skeleton")));
                tooltip.add(magmaCubeFcs());
            }
            if (wave.highestUnitTier == 5) {
                tooltip.add(fcs(str("brute") + research("brute_shields") + armoured(1)));
                tooltip.add(fcs(str("headhunter") + research("heavy_tridents") + armoured(1)));
                tooltip.add(fcs(str("hoglin")));
                tooltip.add(fcs(str("hoglin_rider")));
                tooltip.add(fcs(str("blaze")));
                tooltip.add(fcs(str("wither_skeleton")));
                tooltip.add(fcs(str("ghast")));
                tooltip.add(magmaCubeFcs());
            }
            if (wave.highestUnitTier >= 6) {
                tooltip.add(fcs(str("brute") + research("brute_shields") + armoured(2)));
                tooltip.add(fcs(str("headhunter") + research("heavy_tridents") + armoured(2)));
                tooltip.add(fcs(str("hoglin")));
                tooltip.add(fcs(str("hoglin_rider")));
                tooltip.add(fcs(str("blaze")));
                tooltip.add(fcs(str("wither_skeleton")));
                tooltip.add(fcs(str("ghast") + research("soul_fireballs")));
                tooltip.add(magmaCubeFcs());
            }
        }
        else if (wave.faction == Faction.VILLAGERS) {
            if (wave.highestUnitTier == 1) {
                tooltip.add(fcs(str("militia")));
                tooltip.add(fcs(str("vindicator")));
                tooltip.add(fcs(str("pillager")));
            }
            if (wave.highestUnitTier == 2) {
                tooltip.add(fcs(str("militia")));
                tooltip.add(fcs(str("vindicator") + enchant("maiming")));
                tooltip.add(fcs(str("pillager")));
            }
            if (wave.highestUnitTier == 3) {
                tooltip.add(fcs(str("militia")));
                tooltip.add(fcs(str("vindicator") + enchant("maiming")));
                tooltip.add(fcs(str("pillager") + enchant("quickshot")));
                tooltip.add(fcs(str("iron_golem")));
            }
            if (wave.highestUnitTier == 4) {
                tooltip.add(fcs(str("militia")));
                tooltip.add(fcs(str("vindicator") + enchant("sharpness")));
                tooltip.add(fcs(str("pillager") + enchant("quickshot")));
                tooltip.add(fcs(str("iron_golem")));
                tooltip.add(fcs(str("evoker")));
            }
            if (wave.highestUnitTier == 5) {
                tooltip.add(fcs(str("vindicator") + enchant("sharpness")));
                tooltip.add(fcs(str("pillager") + enchant("multishot")));
                tooltip.add(fcs(str("iron_golem")));
                tooltip.add(fcs(str("evoker")));
                tooltip.add(fcs(str("ravager")));
            }
            if (wave.highestUnitTier >= 6) {
                tooltip.add(fcs(str("vindicator") + enchant("sharpness")));
                tooltip.add(fcs(str("pillager") + enchant("multishot")));
                tooltip.add(fcs(str("iron_golem")));
                tooltip.add(fcs(str("evoker") + enchant("vigor")));
                tooltip.add(fcs(str("ravager")));
                tooltip.add(fcs(str("ravager_artillery") + " + " + str("captain")));
            }
        }
        else if (wave.faction == Faction.NONE) {
            tooltip.add(FormattedCharSequence.forward("???", Style.EMPTY));
        }

        return tooltip;
    }
}














