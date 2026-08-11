package nl.pixento.remindforge.ui.settings.vibration

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.pixento.remindforge.R
import nl.pixento.remindforge.domain.model.VibrationPatternType

@StringRes
fun VibrationPatternType.labelRes(): Int = when (this) {
    VibrationPatternType.SHORT_PULSE -> R.string.vibration_pattern_short_pulse
    VibrationPatternType.LONG_PULSE -> R.string.vibration_pattern_long_pulse
    VibrationPatternType.DOUBLE_PULSE -> R.string.vibration_pattern_double_pulse
    VibrationPatternType.TRIPLE_PULSE -> R.string.vibration_pattern_triple_pulse
    VibrationPatternType.LONG_SHORT_SHORT -> R.string.vibration_pattern_long_short_short
    VibrationPatternType.SHORT_SHORT_LONG -> R.string.vibration_pattern_short_short_long
    VibrationPatternType.SHORT_LONG_SHORT -> R.string.vibration_pattern_short_long_short
    VibrationPatternType.LONG_SHORT_LONG -> R.string.vibration_pattern_long_short_long
    VibrationPatternType.SILENT -> R.string.silent_label
}

val VibrationPatternType.label: String
    @Composable get() = stringResource(labelRes())
