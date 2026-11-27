package io.github.erinleonid3_collab;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        final int CAPACITY = 3;
        final int THREADS = 3;
        final int TASKS = 10;

        BlockingQueue<Runnable> queue = new BlockingQueue<>(CAPACITY);

        for (int i = 0; i < THREADS; i++) {
            final int workerId = i;
            Thread worker = new Thread(() -> {
                try {
                    while (true) {
                        Runnable task = queue.dequeue();
                        task.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Поток-" + workerId + " прерван");
                }
            }, "Потоке-" + i);
            worker.setDaemon(true);
            worker.start();
        }


        for (int i = 0; i < TASKS; i++) {
            final int taskId = i;
            if (queue.size() >= CAPACITY) {
                System.out.println("Очередь полна, ожидание. размер = " + queue.size() );
            }
            queue.enqueue(() -> {
                System.out.println("  задча-" + taskId + " выполняется на  " + Thread.currentThread().getName());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            System.out.println("Добавлена задача- " + taskId + ", размер = " + queue.size());
            Thread.sleep(50);
        }

        // Даём воркерам доработать оставшиеся задачи
        Thread.sleep(3000);
        System.out.println("Готово, итоговый размер очереди = " + queue.size());
    }
}