package org.selkie.kol.impl.campaign.cores

import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore
import org.selkie.kol.impl.skills.cores.DuskBossCoreSkill


class DuskBossCoreOfficerPlugin : BossCoreOfficerPlugin() {
    override val exclusiveSkill = DuskBossCoreSkill()
    override val coreItemID = BossCore.DUSK_CORE_ITEM_ID
    override val portraitSpriteName = BossCore.DUSK_CORE_PORTRAIT
}
