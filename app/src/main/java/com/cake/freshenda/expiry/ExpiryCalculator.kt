package com.cake.freshenda.expiry

import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.model.AddBatchDraft
import com.cake.freshenda.model.DateBasis
import com.cake.freshenda.model.DeadlineCandidate
import com.cake.freshenda.model.DeadlineKind
import com.cake.freshenda.model.ExpiryResult
import com.cake.freshenda.model.FoodPhysicalState
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.model.MaturityState
import com.cake.freshenda.model.PackagingState
import com.cake.freshenda.model.StorageLocation
import com.cake.freshenda.model.StorageProfile
import com.cake.freshenda.model.StorageWindow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object ExpiryCalculator {
    fun suggestedWindow(draft: AddBatchDraft, profile: StorageProfile): StorageWindow? {
        val window = when (draft.storageLocation) {
            StorageLocation.REFRIGERATED -> {
                if (draft.packagingState == PackagingState.OPENED) {
                    profile.openedRefrigerated ?: return null
                } else {
                    profile.refrigerated
                }
            }
            StorageLocation.FROZEN -> profile.frozen
            StorageLocation.PANTRY -> profile.pantry
        }
        if (window.defaultValue == null || window.unit == null) return null
        if (window.status !in setOf("SUPPORTED", "CONDITIONAL")) return null
        if (!stateMatches(profile.id, draft, window)) return null
        return window
    }

    fun deadlineFromWindow(anchorEpochMillis: Long, zoneId: String, window: StorageWindow): Long {
        val anchor = Instant.ofEpochMilli(anchorEpochMillis).atZone(ZoneId.of(zoneId))
        val deadline = when (window.unit) {
            "DAY" -> anchor.plusDays(window.defaultValue!!.toLong())
            "WEEK" -> anchor.plusWeeks(window.defaultValue!!.toLong())
            "MONTH" -> anchor.plusMonths(window.defaultValue!!.toLong())
            else -> error("不支持的期限单位：${window.unit}")
        }
        return deadline.toInstant().toEpochMilli()
    }

    fun labelDeadline(labelEpochDay: Long, zoneId: String): Long =
        LocalDate.ofEpochDay(labelEpochDay)
            .plusDays(1)
            .atStartOfDay(ZoneId.of(zoneId))
            .toInstant()
            .toEpochMilli()

    fun customDeadline(dateEpochDay: Long, zoneId: String): Long =
        LocalDate.ofEpochDay(dateEpochDay)
            .plusDays(1)
            .atStartOfDay(ZoneId.of(zoneId))
            .toInstant()
            .toEpochMilli()

    fun result(batch: FoodBatchEntity, nowEpochMillis: Long, normalAdvanceDays: Int = 1, frozenAdvanceDays: Int = 7): ExpiryResult {
        val deadline = batch.effectiveDeadlineEpochMillis
        val anchor = batch.effectiveAnchorEpochMillis
        if (deadline == null || anchor == null) {
            return ExpiryResult(null, if (batch.requiresDateReview) FreshnessStatus.REVIEW else FreshnessStatus.UNKNOWN, 0f, if (batch.requiresDateReview) "日期待确认" else "未设日期")
        }

        val kind = batch.effectiveDeadlineKind?.let(DeadlineKind::valueOf) ?: DeadlineKind.CUSTOM
        val candidate = DeadlineCandidate(
            kind = kind,
            anchorEpochMillis = anchor,
            deadlineEpochMillis = deadline,
            displayDateEpochDay = batch.effectiveDisplayDateEpochDay ?: displayDate(deadline, batch.businessZoneId),
            description = reasonText(kind),
        )
        val zone = ZoneId.of(batch.businessZoneId)
        val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()
        val target = LocalDate.ofEpochDay(candidate.displayDateEpochDay)
        val status = when {
            nowEpochMillis >= deadline -> FreshnessStatus.OVERDUE
            target == today -> FreshnessStatus.TODAY
            target == today.plusDays(1) -> FreshnessStatus.DUE_SOON
            nowEpochMillis >= deadline - ChronoUnit.DAYS.duration.toMillis() *
                if (batch.storageLocation == StorageLocation.FROZEN.name) frozenAdvanceDays else normalAdvanceDays -> FreshnessStatus.DUE_SOON
            else -> FreshnessStatus.OK
        }
        val fraction = if (deadline <= anchor) 0f else ((deadline - nowEpochMillis).toDouble() / (deadline - anchor))
            .coerceIn(0.0, 1.0).toFloat()
        return ExpiryResult(candidate, status, fraction, statusText(status, target, today, kind))
    }

    fun chooseEffective(candidates: List<DeadlineCandidate>): DeadlineCandidate? =
        candidates.minWithOrNull(compareBy<DeadlineCandidate> { it.deadlineEpochMillis }.thenBy { it.kind.ordinal })

    fun displayDate(deadlineEpochMillis: Long, zoneId: String): Long =
        Instant.ofEpochMilli(deadlineEpochMillis - 1)
            .atZone(ZoneId.of(zoneId))
            .toLocalDate()
            .toEpochDay()

    private fun stateMatches(profileId: String, draft: AddBatchDraft, window: StorageWindow): Boolean {
        if (window.status == "CONDITIONAL" && window.preparation != "NONE" && draft.completedPreparation != window.preparation) return false
        if (profileId in RIPE_REQUIRED && draft.maturityState != MaturityState.RIPE) return false
        if (profileId in UNOPENED_REQUIRED && draft.packagingState != PackagingState.UNOPENED) return false
        if (profileId == "ground" && draft.physicalState != FoodPhysicalState.GROUND) return false
        if (profileId == "red_stew" && draft.physicalState != FoodPhysicalState.CUT) return false
        if (profileId in WHOLE_RAW_REQUIRED && draft.physicalState != FoodPhysicalState.WHOLE) return false
        if (profileId in COOKED_REQUIRED && draft.physicalState != FoodPhysicalState.COOKED) return false
        return true
    }

    private fun reasonText(kind: DeadlineKind) = when (kind) {
        DeadlineKind.LABEL -> "包装到期日"
        DeadlineKind.SUGGESTED -> "储存参考"
        DeadlineKind.OPENED -> "开封后期限"
        DeadlineKind.CUSTOM -> "自己设定"
        DeadlineKind.THAWED -> "解冻后参考"
    }

    private fun statusText(status: FreshnessStatus, target: LocalDate, today: LocalDate, kind: DeadlineKind): String = when (status) {
        FreshnessStatus.OVERDUE -> if (kind == DeadlineKind.LABEL) "包装日期已过" else "已过建议日期"
        FreshnessStatus.TODAY -> if (kind == DeadlineKind.LABEL) "今天到期" else "今天安排"
        FreshnessStatus.DUE_SOON -> if (target == today.plusDays(1)) "明天到期" else "临期 · ${ChronoUnit.DAYS.between(today, target).coerceAtLeast(1)}天"
        FreshnessStatus.OK -> "${ChronoUnit.DAYS.between(today, target).coerceAtLeast(1)}天后"
        FreshnessStatus.UNKNOWN -> "未设日期"
        FreshnessStatus.REVIEW -> "日期待确认"
    }

    fun formatFraction(result: ExpiryResult): String = "${(result.fractionRemaining * 100).roundToInt()}%"

    private val RIPE_REQUIRED = setOf("pear", "peach", "nectarine", "plum", "apricot", "kiwi", "mango", "papaya", "banana_ripe", "avocado", "guava")
    private val UNOPENED_REQUIRED = setOf("milk", "yogurt", "hard_cheese")
    private val WHOLE_RAW_REQUIRED = setOf("red_whole", "chicken_whole", "egg_shell")
    private val COOKED_REQUIRED = setOf("egg_boiled", "leftover_meat", "soup")
}
