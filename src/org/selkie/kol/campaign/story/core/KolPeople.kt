package org.selkie.kol.campaign.story.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin

/**
 * The story's people: lookup by id, generated people remembered by id, and approval (each person's own standing).
 * Features register resolvers for people they generate (`bench:<slot>`, `inquisitor`, `agent`), at each load.
 */
object KolPeople {
    private val resolvers = HashMap<String, (String) -> PersonAPI?>()

    /** An important person by id. */
    fun get(id: String): PersonAPI? = Global.getSector().importantPeople.getPerson(id)

    /** Registers a resolver for `<kind>:<arg>` and the bare alias `<kind>` (arg ""). Transient: register at each load. */
    fun resolver(kind: String, resolve: (String) -> PersonAPI?) {
        resolvers[kind] = resolve
    }

    /** A story person by id, by `<kind>:<arg>` (a registered resolver), or by a bare resolver alias. */
    fun byId(id: String): PersonAPI? {
        val colon = id.indexOf(':')
        if (colon > 0) return resolvers[id.substring(0, colon)]?.invoke(id.substring(colon + 1))
        resolvers[id]?.let { return it("") }
        return get(id)
    }

    /**
     * The important person with this id, created with [factory] and registered the first time it's asked for.
     * [market] is the person's home market; [listed] also adds them to that market's people.
     */
    fun remembered(id: String, market: MarketAPI? = null, listed: Boolean = false, factory: () -> PersonAPI): PersonAPI {
        get(id)?.let { return it }
        val person = factory()
        person.id = id
        Global.getSector().importantPeople.addPerson(person)
        if (market != null) {
            person.market = market
            if (listed) market.addPerson(person)
        }
        return person
    }

    /**
     * A named character's approval of the player: their own standing, the vanilla per-person relationship (shown on
     * their card), changed through vanilla's reputation plugin. With no text panel, vanilla reports it as a campaign
     * message (portrait, "Relationship with ... improved", the standing) rather than a line in the conversation.
     * [points] are percent of the scale (2-5 typical; senior people move less; up to 20 for dramatic moments).
     * Nothing reads it yet. [text] is unused (kept so callers in dialogs stay unchanged).
     */
    fun approve(person: PersonAPI?, points: Int, @Suppress("UNUSED_PARAMETER") text: TextPanelAPI?) {
        if (person == null || points == 0) return
        val impact = CoreReputationPlugin.CustomRepImpact()
        impact.delta = points * 0.01f
        impact.limit = if (points > 0) RepLevel.COOPERATIVE else RepLevel.VENGEFUL
        Global.getSector().adjustPlayerReputation(CoreReputationPlugin.RepActionEnvelope(
            CoreReputationPlugin.RepActions.CUSTOM, impact, null, null, false), person)
    }
}
