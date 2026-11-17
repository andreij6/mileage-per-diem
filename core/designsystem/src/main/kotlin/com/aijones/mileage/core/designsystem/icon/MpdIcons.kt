package com.aijones.mileage.core.designsystem.icon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.R

object MpdIcons {
    val ArrowLeft = R.drawable.ic_arrow_left
    val BatteryMedium = R.drawable.ic_battery_medium
    val BedDouble = R.drawable.ic_bed_double
    val Bell = R.drawable.ic_bell
    val Building2 = R.drawable.ic_building_2
    val Calendar = R.drawable.ic_calendar
    val Car = R.drawable.ic_car
    val ChartColumn = R.drawable.ic_chart_column
    val Check = R.drawable.ic_check
    val ChevronDown = R.drawable.ic_chevron_down
    val ChevronLeft = R.drawable.ic_chevron_left
    val ChevronRight = R.drawable.ic_chevron_right
    val Circle = R.drawable.ic_circle
    val CircleCheck = R.drawable.ic_circle_check
    val Clock = R.drawable.ic_clock
    val CloudOff = R.drawable.ic_cloud_off
    val EllipsisVertical = R.drawable.ic_ellipsis_vertical
    val ExternalLink = R.drawable.ic_external_link
    val FileSpreadsheet = R.drawable.ic_file_spreadsheet
    val FileText = R.drawable.ic_file_text
    val Globe = R.drawable.ic_globe
    val History = R.drawable.ic_history
    val Info = R.drawable.ic_info
    val List = R.drawable.ic_list
    val Lock = R.drawable.ic_lock
    val MapPin = R.drawable.ic_map_pin
    val Moon = R.drawable.ic_moon
    val Navigation = R.drawable.ic_navigation
    val Pencil = R.drawable.ic_pencil
    val Play = R.drawable.ic_play
    val Plus = R.drawable.ic_plus
    val Receipt = R.drawable.ic_receipt
    val RotateCcw = R.drawable.ic_rotate_ccw
    val Route = R.drawable.ic_route
    val ScrollText = R.drawable.ic_scroll_text
    val Search = R.drawable.ic_search
    val Settings = R.drawable.ic_settings
    val Share2 = R.drawable.ic_share_2
    val ShieldCheck = R.drawable.ic_shield_check
    val Square = R.drawable.ic_square
    val Sun = R.drawable.ic_sun
    val Trash2 = R.drawable.ic_trash_2
    val TriangleAlert = R.drawable.ic_triangle_alert
    val UserRound = R.drawable.ic_user_round
    val Utensils = R.drawable.ic_utensils
    val X = R.drawable.ic_x
    val Zap = R.drawable.ic_zap
}

@Composable
fun MpdIcon(@DrawableRes icon: Int, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp, contentDescription: String? = null) {
    Icon(painterResource(icon), contentDescription, modifier.size(size), tint = tint)
}
