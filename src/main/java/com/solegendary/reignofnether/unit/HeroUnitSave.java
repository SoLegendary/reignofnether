package com.solegendary.reignofnether.unit;

public class HeroUnitSave {

    public String uuid;
    public String name; // only used for revive data,
    public String ownerName; // only used for revive data
    public int experience;
    public int skillPoints;
    public int charges; // tracks specific hero states like necromancer souls
    public int ability1Rank;
    public int ability2Rank;
    public int ability3Rank;
    public int ability4Rank;

    public HeroUnitSave(String uuid, String name, String ownerName, int experience, int skillPoints, int charges,
                        int ability1Rank, int ability2Rank, int ability3Rank, int ability4Rank) {
        this.uuid = uuid;
        this.name = name;
        this.ownerName = ownerName;
        this.experience = experience;
        this.skillPoints = skillPoints;
        this.charges = charges;
        this.ability1Rank = ability1Rank;
        this.ability2Rank = ability2Rank;
        this.ability3Rank = ability3Rank;
        this.ability4Rank = ability4Rank;
    }
}
