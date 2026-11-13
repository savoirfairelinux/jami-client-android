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
package cx.ring.about.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.ring.R
import cx.ring.ui.theme.JamiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    uiState: AboutUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onLogoClick: () -> Unit = {},
    onCreditsClick: () -> Unit = {},
    onContributeClick: () -> Unit = {},
    onLicenseClick: () -> Unit = {},
    onSubmitIssueClick: () -> Unit = {},
    onSflLogoClick: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.menu_item_about)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.menu_item_about),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            // Logo
            Image(
                painter = painterResource(R.drawable.ic_jami),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(top = 5.dp)
                    .clickable(onClick = onLogoClick)
            )

            // Description
            Text(
                text = stringResource(R.string.description),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 12.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Credits Button
            Button(
                onClick = onCreditsClick,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            ) {
                Text(text = stringResource(R.string.credits))
            }

            // Version Section
            AboutInfoRow(
                iconRes = R.drawable.ic_ring_logo_white,
                contentDescription = stringResource(R.string.version_section),
                title = stringResource(R.string.version_section),
                subtitle = uiState.releaseVersion
            )

            // Contribute Container
            AboutClickableRow(
                iconRes = R.drawable.baseline_person_add_24,
                contentDescription = stringResource(R.string.contribute_section),
                title = stringResource(R.string.contribute_section),
                subtitle = stringResource(R.string.app_website_contribute),
                onClick = onContributeClick
            )

            // Copyright Section
            AboutInfoRow(
                iconRes = R.drawable.baseline_copyright_24,
                contentDescription = stringResource(R.string.copyright_section),
                title = stringResource(R.string.copyright_section),
                subtitle = stringResource(R.string.copyright)
            )

            // License Container
            val licenseAnnotatedString = buildAnnotatedString {
                append("This software is provided \"as is\", without warranty of any kind. For details, see ")
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append("GNU General Public License version 3 or later")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLicenseClick)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.baseline_insert_drive_file_24),
                    contentDescription = stringResource(R.string.copyright_section),
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Spacer(modifier = Modifier.width(32.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.section_license),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = licenseAnnotatedString,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
            }

            // Submit Issue Container
            AboutClickableRow(
                iconRes = R.drawable.baseline_feedback_24,
                contentDescription = null,
                title = stringResource(R.string.submit_issue_title),
                subtitle = stringResource(R.string.submit_issue_action_description),
                onClick = onSubmitIssueClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Developed by / Sponsor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.sponsor_section),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painterResource(R.drawable.logo_sfl_coul_rgb),
                    contentDescription = stringResource(R.string.app_author),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(56.dp)
                        .clickable(onClick = onSflLogoClick)
                )
            }
        }
    }
}

@Composable
fun AboutInfoRow(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )
        Spacer(modifier = Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AboutClickableRow(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUnderlined: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )
        Spacer(modifier = Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                textDecoration = if (isUnderlined) TextDecoration.Underline else null
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun AboutScreenPreviewLight() {
    JamiTheme(darkTheme = false) {
        AboutScreen(
            uiState = AboutUiState(releaseVersion = "Version 20261106-01")
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun AboutScreenPreviewDark() {
    JamiTheme(darkTheme = true) {
        AboutScreen(
            uiState = AboutUiState(releaseVersion = "Version 20261106-01")
        )
    }
}
