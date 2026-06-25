package com.example.game

import io.appmetrica.analytics.AppMetrica

object GameAnalytics {
    fun trackLevelStart(levelIndex: Int) {
        val params = mapOf<String, Any>("level_index" to levelIndex)
        AppMetrica.reportEvent("level_start", params)
    }

    fun trackLevelWin(levelIndex: Int, movesCount: Int, noMistakes: Boolean) {
        val params = mapOf<String, Any>(
            "level_index" to levelIndex,
            "moves_count" to movesCount,
            "no_mistakes" to noMistakes
        )
        AppMetrica.reportEvent("level_win", params)
    }

    fun trackLevelRestart(levelIndex: Int) {
        val params = mapOf<String, Any>("level_index" to levelIndex)
        AppMetrica.reportEvent("level_restart", params)
    }

    fun trackLevelSkip(levelIndex: Int) {
        val params = mapOf<String, Any>("level_index" to levelIndex)
        AppMetrica.reportEvent("level_skip", params)
    }

    fun trackCoinsSpent(amount: Int, purpose: String) {
        val params = mapOf<String, Any>(
            "amount" to amount,
            "purpose" to purpose
        )
        AppMetrica.reportEvent("coins_spent", params)
    }

    fun trackHintUsed(levelIndex: Int, type: String) {
        val params = mapOf<String, Any>(
            "level_index" to levelIndex,
            "type" to type
        )
        AppMetrica.reportEvent("hint_used", params)
    }

    fun trackAdShowed(adType: String, purpose: String) {
        val params = mapOf<String, Any>(
            "ad_type" to adType,
            "purpose" to purpose
        )
        AppMetrica.reportEvent("ad_showed", params)
    }

    fun trackAdClicked(adType: String, purpose: String) {
        val params = mapOf<String, Any>(
            "ad_type" to adType,
            "purpose" to purpose
        )
        AppMetrica.reportEvent("ad_clicked", params)
    }
}
