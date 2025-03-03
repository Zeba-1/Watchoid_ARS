package fr.uge.android.watchoid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import fr.uge.android.watchoid.entity.test.ServiceTest

import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.uge.android.watchoid.DAO.ServiceTestDao


@Composable
fun ServiceTestList(serviceTests: List<ServiceTest>, onServiceTestClick: (ServiceTest) -> Unit = {}) {
    LazyColumn {
        items(serviceTests) { serviceTest ->
            ServiceTestCard(serviceTest, onServiceTestClick)
        }
    }
}

@Composable
fun ServiceTestCard(serviceTest: ServiceTest, onClick: (ServiceTest) -> Unit = {}) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .clickable(onClick = { onClick(serviceTest) })
    ) {
        Text(
            text = "${serviceTest.name} (${serviceTest.type.name})",
            fontSize = 20.sp
        )
        Text(
            text = serviceTest.target,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
