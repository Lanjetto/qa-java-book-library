package library.jvm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Пример virtual threads (Java 21, Project Loom, t10/Б21).
 *
 * <p>Идея: виртуальные потоки «ездят» на пуле потоков-носителей (~число ядер); когда виртуальный
 * поток блокируется (I/O, sleep), он освобождает носитель, и на нём стартует другой. Поэтому
 * десятки/сотни блокирующих задач выполняются за время ~одной, а не последовательно.
 *
 * <p>Тест доказывает это фактом: 50 задач по 100 мс блокировки в сумме — 5 секунд «в лоб»;
 * на {@code newVirtualThreadPerTaskExecutor()} они перекрываются и укладываются в единицы секунд.
 * Класс изолирован (общего состояния нет), поэтому помечен {@code @Execution(CONCURRENT)} —
 * может бежать параллельно с другими такими же тестами (настройка — junit-platform.properties).
 *
 * <p>Границы virtual threads: для CPU-нагрузки они НЕ ускоряют (ядер не прибавляют); а
 * {@code synchronized} + блокирующий вызов «прибивает» поток к носителю (pinning) — для блокировок
 * используют {@code ReentrantLock}. Это разбирается на занятии, здесь — только «потоки работают».
 */
@Execution(ExecutionMode.CONCURRENT)
class VirtualThreadsTest {

    /** «Блокирующая работа» пользователя: 100 мс, не занимая CPU (как сетевой/дисковый I/O). */
    private static int blockingWork(int i) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
        return i;
    }

    @Test
    @DisplayName("50 блокирующих задач на virtual threads занимают ~время одной, а не 50 × 100 мс")
    void manyBlockingTasksOverlapOnVirtualThreads() throws Exception {
        long startNanos = System.nanoTime();

        // По одной виртуальной задаче на «пользователя»; executor закрывается после всех задач.
        List<Integer> results;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = IntStream.range(0, 50)
                    .mapToObj(i -> executor.submit(() -> blockingWork(i)))
                    .toList();
            results = new java.util.ArrayList<>(futures.size());
            for (Future<Integer> f : futures) {
                results.add(f.get(10, TimeUnit.SECONDS));   // не упасть по таймауту, если что-то не так
            }
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        // Результаты корректны: каждое значение вернулось ровно один раз.
        assertThat(results).containsExactlyInAnyOrderElementsOf(
                IntStream.range(0, 50).boxed().toList());
        // «В лоб» 50 × 100 мс = 5000 мс; параллель на virtual threads должна быть заметно быстрее.
        // Порог берём с запасом (>=10× меньше последовательного), чтобы не флакать на слабой машине.
        assertThat(elapsedMs).as("50 блокирующих задач по 100 мс должны перекрыться на virtual threads")
                .isLessThan(3000);
    }
}
