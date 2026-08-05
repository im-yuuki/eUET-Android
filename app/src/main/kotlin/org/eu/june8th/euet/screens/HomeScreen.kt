package org.eu.june8th.euet.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.eu.june8th.euet.ExpressiveAppTheme
import org.eu.june8th.euet.icons.filled.Add

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    Toast.makeText(
                        context, "Đã nhấn nút Expressive", Toast.LENGTH_SHORT,
                    ).show()
                },
                icon = { Icon(imageVector = Add, contentDescription = null) },
                text = { Text("Tạo mới") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Material 3 Expressive",
                style = typography.displaySmall,
            )

            Text(
                text = "Màu sắc, shape, typography và motion đều lấy từ MaterialExpressiveTheme.",
                style = typography.bodyLarge,
            )

            HorizontalDivider()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Shape", style = typography.titleLarge)
                    Text(
                        text = "Thang hình dạng mở rộng với largeIncreased giúp thể hiện thương hiệu.",
                        style = typography.bodyMedium
                    )
                }
            }

            LoadingIndicator(
                modifier = Modifier
                    .size(72.dp)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.primary,
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = "Typography", style = typography.titleLarge)
                    Text(
                        text = "Các kiểu chữ được tùy chỉnh tại Theme.kt.",
                        style = typography.bodyMedium,
                    )
                    Button(onClick = {
                        Toast.makeText(context, "Đã nhấn nút", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Thử ngay")
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    ExpressiveAppTheme {
        HomeScreen()
    }
}