package com.shelf.feature.book.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.shelf.shared.core.strings.Res
import com.shelf.shared.core.strings.book_rating_clear
import com.shelf.shared.core.strings.book_rating_label
import com.shelf.shared.core.strings.book_rating_none
import com.shelf.shared.core.strings.book_rating_out_of_range
import com.shelf.shared.core.strings.book_rating_star_a11y
import com.shelf.shared.core.strings.book_rating_value
import com.shelf.shared.designsystem.DreamTheme
import com.shelf.shared.designsystem.components.Button
import com.shelf.shared.designsystem.components.ButtonVariant
import com.shelf.shared.designsystem.components.IconButton
import org.jetbrains.compose.resources.stringResource

internal const val MAX_RATING = 5

/** Form rating selector: five star toggles (radio semantics), a value text and a Clear control. */
@Composable
internal fun BookRatingSelector(
    visible: Boolean,
    rating: Int?,
    hasError: Boolean,
    onRatingChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        val label = stringResource(Res.string.book_rating_label)
        val valueText = if (rating == null) {
            stringResource(Res.string.book_rating_none)
        } else {
            stringResource(Res.string.book_rating_value, rating.toString())
        }
        val errorText = stringResource(Res.string.book_rating_out_of_range)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (hasError) liveRegion = LiveRegionMode.Assertive },
            verticalArrangement = Arrangement.spacedBy(DreamTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .semantics { contentDescription = label },
                horizontalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (star in 1..MAX_RATING) {
                    val filled = rating != null && star <= rating
                    val starDescription = stringResource(Res.string.book_rating_star_a11y, star.toString())
                    IconButton(
                        onClick = { onRatingChange(star) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = starDescription
                                role = Role.RadioButton
                                selected = rating == star
                            },
                    ) {
                        Icon(
                            imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            Button(
                text = stringResource(Res.string.book_rating_clear),
                onClick = { onRatingChange(null) },
                enabled = rating != null,
                variant = ButtonVariant.Tertiary,
            )
            if (hasError) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { error(errorText) },
                )
            }
        }
    }
}

/** Inline range error shown under Status when the selector is hidden. */
@Composable
internal fun BookRatingErrorText(modifier: Modifier = Modifier) {
    val errorText = stringResource(Res.string.book_rating_out_of_range)
    Text(
        text = errorText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.semantics {
            error(errorText)
            liveRegion = LiveRegionMode.Assertive
        },
    )
}

/** Detail rating row: label + "n of 5" text plus decorative stars, read as one element. */
@Composable
internal fun BookRatingRow(rating: Int, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.book_rating_label)
    val valueText = stringResource(Res.string.book_rating_value, rating.toString())
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$label, $valueText" },
        horizontalArrangement = Arrangement.spacedBy(DreamTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Row {
            for (star in 1..MAX_RATING) {
                Icon(
                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (star <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
