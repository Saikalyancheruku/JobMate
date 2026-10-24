# Commented skeleton — `SheetsSyncWorker.kt`

```kotlin
// TODO: CoroutineWorker with network constraint and exponential backoff.
// TODO: Transactionally claim queue; UUID idempotent upsert.
// TODO: Conflicts visible; never sync secrets/resumes/screenshots.
class SheetsSyncWorker(/* TODO: Context, WorkerParameters */) :
    androidx.work.CoroutineWorker(/* TODO */) {
    // TODO: override suspend fun doWork(): Result
}
```
