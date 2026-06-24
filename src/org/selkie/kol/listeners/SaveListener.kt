package org.selkie.kol.listeners

interface SaveListener {
    fun beforeGameSave()
    fun onGameLoad()
    fun afterGameSave()
    fun onGameSaveFailed()
}