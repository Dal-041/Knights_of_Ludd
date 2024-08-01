package org.selkie.kol.impl.campaign.cores

import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore
import org.selkie.kol.impl.skills.cores.DawnBossCoreSkill


class DawnBossCoreOfficerPlugin : BossCoreOfficerPlugin() {
    override val exclusiveSkill = DawnBossCoreSkill()
    override val coreItemID = BossCore.DAWN_CORE_ITEM_ID
    override val portraitSpriteName = BossCore.DAWN_CORE_PORTRAIT
}
