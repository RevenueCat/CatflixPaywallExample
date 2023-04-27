package com.revenuecat.paywallSample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails

val revenueCatColor = Color(android.graphics.Color.parseColor("#f25a5a"))

@Composable
fun PaywallScreen(
    paywallItems: List<PaywallItem>,
    products: List<ProductDetails>,
    onPurchasePaywallItem: (ProductDetails, PaywallItem) -> Unit,
    onPurchaseSpecialOffer: () -> Unit,
) {
    if (paywallItems.isEmpty() || products.isEmpty()) return
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var selectedPaywallItem by remember {
        mutableStateOf(paywallItems.filter { it.productId == selectedProduct?.productId }
            .sortedBy { it.basePlan?.pricingPhases?.pricingPhaseList?.first()?.billingPeriod?.billingPeriodDays() }
            .firstOrNull())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.catflix), contentDescription = "Catflix"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(0.5f), RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            products.forEach {
                val isSelected = it.productId == selectedProduct?.productId
                val borderColor = if (isSelected) revenueCatColor else Color.Transparent
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(4f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(borderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            selectedProduct = it
                            selectedPaywallItem = paywallItems
                                .filter { it.productId == selectedProduct?.productId }
                                .sortedBy { it.basePlan?.pricingPhases?.pricingPhaseList?.first()?.billingPeriod?.billingPeriodDays() }
                                .firstOrNull()
                        }) {
                    Text(
                        text = it.name,
                        color = if (isSelected) Color.White else Color.Black,
                        style = MaterialTheme.typography.h6
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        paywallItems.filter { it.productId == selectedProduct?.productId }
            .sortedBy { it.basePlan?.pricingPhases?.pricingPhaseList?.first()?.billingPeriod?.billingPeriodDays() }
            .forEach { paywallItem ->
                SubscriptionOptionBox(title = paywallItem.title,
                    subtitle = paywallItem.subtitle,
                    isSelected = selectedPaywallItem == paywallItem,
                    onSelected = {
                        selectedPaywallItem = paywallItem
                    })
            }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = selectedPaywallItem != null,
            onClick = { onPurchasePaywallItem(selectedProduct!!, selectedPaywallItem!!) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(14.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = revenueCatColor, contentColor = Color.White
            )
        ) {
            Text(text = "Start free trial")
        }
        Spacer(modifier = Modifier.height(16.dp))

        LocationOfferScreen(onClick = { onPurchaseSpecialOffer() })
    }
}

@Composable
fun SubscriptionOptionBox(
    title: String, subtitle: String, isSelected: Boolean, onSelected: () -> Unit
) {
    val borderColor = if (isSelected) revenueCatColor else Color.LightGray
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .clickable { onSelected() }, contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(0.dp)) {
            CheckableCircle(checked = isSelected)
            Column(
                modifier = Modifier.padding(
                    PaddingValues(
                        top = 8.dp, bottom = 8.dp, end = 8.dp
                    )
                )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(PaddingValues(bottom = 4.dp))
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CheckableCircle(checked: Boolean) {
    Canvas(
        modifier = Modifier
            .padding(16.dp)
            .size(24.dp)
    ) {
        // Draw the circle
        drawCircle(
            color = if (checked) revenueCatColor else Color.LightGray,
            radius = size.minDimension / 2,
            center = Offset(size.width / 2, size.height / 2),
            style = if (checked) Fill else Stroke(width = 4f)
        )

        // Draw the checkmark if the circle is checked
        if (checked) {
            val checkmarkPath = Path().apply {
                moveTo(size.width * 0.25f, size.height * 0.55f)
                lineTo(size.width * 0.4f, size.height * 0.7f)
                lineTo(size.width * 0.75f, size.height * 0.35f)
            }
            drawPath(
                checkmarkPath, color = Color.White, style = Stroke(width = size.minDimension * 0.1f)
            )
        }
    }
}