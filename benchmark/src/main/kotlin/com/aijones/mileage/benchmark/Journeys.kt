package com.aijones.mileage.benchmark

import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val PACKAGE = "com.aijones.mileage"

fun MacrobenchmarkScope.seedDemoData() {
    device.executeShellCommand("am start -W -n $PACKAGE/.MainActivity --ez seed_demo true")
    device.wait(Until.hasObject(By.textContains("need a purpose")), 60_000)
    killProcess()
}

fun MacrobenchmarkScope.startAndWaitForHome() {
    startActivityAndWait(Intent(Intent.ACTION_MAIN).setPackage(PACKAGE).addCategory(Intent.CATEGORY_LAUNCHER))
    device.wait(Until.hasObject(By.text("Recent trips")), 5_000)
}

fun MacrobenchmarkScope.openFirstTrip() {
    device.findObject(By.textContains("Office → Acme HQ"))?.click()
    device.wait(Until.hasObject(By.text("Trip")), 5_000)
    device.pressBack()
}
