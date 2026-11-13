/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import cx.ring.R

@Composable
fun JamiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colorResource(R.color.colorPrimary),
            onPrimary = colorResource(R.color.background),
            secondary = colorResource(R.color.colorSecondary),
            onSecondary = colorResource(R.color.colorOnSecondary),
            background = colorResource(R.color.background),
            onBackground = colorResource(R.color.colorOnSurface),
            surface = colorResource(R.color.background),
            onSurface = colorResource(R.color.colorOnSurface),
            onSurfaceVariant = colorResource(R.color.colorOnSecondaryContainer),
            error = colorResource(R.color.colorError),
            onError = colorResource(R.color.colorOnError)
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.colorPrimary),
            onPrimary = colorResource(R.color.background),
            secondary = colorResource(R.color.colorSecondary),
            onSecondary = colorResource(R.color.colorOnSecondary),
            background = colorResource(R.color.background),
            onBackground = colorResource(R.color.colorOnSurface),
            surface = colorResource(R.color.background),
            onSurface = colorResource(R.color.colorOnSurface),
            onSurfaceVariant = colorResource(R.color.colorOnSecondaryContainer),
            error = colorResource(R.color.colorError),
            onError = colorResource(R.color.colorOnError)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
