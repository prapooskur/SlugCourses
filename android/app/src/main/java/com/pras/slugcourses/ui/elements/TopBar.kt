package com.pras.slugcourses.ui.elements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingLargeTopBar(
    titleText: String,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    LargeTopAppBar(
        title = {
            Text(
                modifier = Modifier,
                text = titleText,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.navigateUp()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoringNormalTopBar(
    titleText: String,
    navController: NavController,
) {
    TopAppBar(
        title = {
            Text(
                text = titleText,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.navigateUp()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
    )
}