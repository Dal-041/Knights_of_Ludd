package org.selkie.kol.impl.campaign.cores

import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore
import org.selkie.kol.impl.skills.cores.ElysiaBossCoreSkill


class ElysiaBossCoreOfficerPlugin : BossCoreOfficerPlugin() {
    override val exclusiveSkill = ElysiaBossCoreSkill()
    override val coreItemID = BossCore.ELYSIAN_CORE_ITEM_ID
    override val portraitSpriteName = BossCore.ELYSIAN_CORE_PORTRAIT
}
