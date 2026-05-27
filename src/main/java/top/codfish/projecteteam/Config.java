package top.codfish.projecteteam;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config
{
    public static final String CATEGORY_TEAM = "team";

    public static int emcDistributionMode;
    public static final int EMC_MODE_EQUAL = 0;
    public static final int EMC_MODE_CLEAR = 1;

    public static int knowledgeDistributionMode;
    public static final int KNOWLEDGE_MODE_KEEP = 0;
    public static final int KNOWLEDGE_MODE_CLEAR = 1;

    public static void init(File configFile)
    {
        Configuration cfg = new Configuration(configFile);

        cfg.load();

        emcDistributionMode = cfg.get(CATEGORY_TEAM, "emcDistributionMode", EMC_MODE_EQUAL,
            "EMC distribution on leave, kick, or disband:\n"
            + "  0 = equal — member gets fair share (pool / memberCount)\n"
            + "  1 = clear — member gets 0, everything stays with the team").getInt();
        if (emcDistributionMode != EMC_MODE_EQUAL && emcDistributionMode != EMC_MODE_CLEAR)
        {
            emcDistributionMode = EMC_MODE_EQUAL;
        }

        knowledgeDistributionMode = cfg.get(CATEGORY_TEAM, "knowledgeDistributionMode", KNOWLEDGE_MODE_KEEP,
            "Knowledge (unlocked items) handling on leave, kick, or disband:\n"
            + "  0 = keep (default) — member retains all learned items\n"
            + "  1 = clear — member's knowledge is wiped").getInt();
        if (knowledgeDistributionMode != KNOWLEDGE_MODE_KEEP && knowledgeDistributionMode != KNOWLEDGE_MODE_CLEAR)
        {
            knowledgeDistributionMode = KNOWLEDGE_MODE_KEEP;
        }

        if (cfg.hasChanged())
        {
            cfg.save();
        }
    }
}
