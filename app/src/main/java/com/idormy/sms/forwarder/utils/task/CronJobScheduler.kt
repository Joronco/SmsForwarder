package com.idormy.sms.forwarder.utils.task

import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.idormy.sms.forwarder.database.entity.Task
import com.idormy.sms.forwarder.workers.CronWorker
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class CronJobScheduler {

    companion object {

        private const val TAG: String = "CronJobScheduler"

        fun scheduleTask(task: Task) {
            val currentTimeMillis = System.currentTimeMillis()
            val delayInMillis = task.nextExecTime.time / 1000 * 1000 - currentTimeMillis
            val inputData = Data.Builder().putLong("taskId", task.id).build()
            val taskRequest = if (delayInMillis <= 0L) {
                Log.d(TAG, "任务${task.id}：立即执行，delayInMillis = $delayInMillis")
                OneTimeWorkRequestBuilder<CronWorker>()
                    .setInputData(inputData)
                    .build()
            } else {
                Log.d(TAG, "任务${task.id}：延迟 $delayInMillis 毫秒执行")
                OneTimeWorkRequestBuilder<CronWorker>()
                    .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build()
            }

            // 确保同一个任务 ID 的 Worker 在同一时间只会执行一个实例
            val uniqueTaskName = "$TAG-${task.id}"
            WorkManager.getInstance().beginUniqueWork(
                uniqueTaskName, // 给任务设置一个唯一的名称
                ExistingWorkPolicy.KEEP, // 设置任务存在时的策略
                taskRequest
            ).enqueue()
        }

        fun cancelTask(taskId: Long) {
            val uniqueTaskName = "$TAG-$taskId"
            WorkManager.getInstance().cancelUniqueWork(uniqueTaskName)
        }
    }
}
