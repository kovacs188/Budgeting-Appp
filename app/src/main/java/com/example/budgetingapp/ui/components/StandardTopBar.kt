package com.example.budgetingapp.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class MenuItemData(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardTopBar(
    title: String,
    menuItems: List<MenuItemData> = emptyList(),
    onMenuClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            if (menuItems.isNotEmpty()) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardDrawerMenu(
    menuItems: List<MenuItemData>,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(
        initialValue = if (isOpen) DrawerValue.Open else DrawerValue.Closed
    )

    // Sync drawer state with isOpen parameter
    LaunchedEffect(isOpen) {
        if (isOpen) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    // Handle drawer dismissal
    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed && isOpen) {
            onDismiss()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.text) },
                        selected = false,
                        onClick = {
                            item.onClick()
                            onDismiss()
                        }
                    )
                }
            }
        }
    ) {
        content()
    }
}