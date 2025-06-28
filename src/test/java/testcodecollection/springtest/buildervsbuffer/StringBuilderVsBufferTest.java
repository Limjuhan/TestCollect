package testcodecollection.springtest.buildervsbuffer;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest // Boot에서 테스트 환경을 세팅해줌.
public class StringBuilderVsBufferTest {

    private static final int THREAD_CNT = 1000; // 총 몇개의 스레드를 만들지
    private static final int APPEND_CNT = 100; // 각 스레드가 몇 번 문자열을 붙일지

    @Test
    public void testStringBuilderMultiThreadUnsafe() throws InterruptedException {

        StringBuilder builder = new StringBuilder();
        // CountDownLatch는 모든 스레드가 끝날 때까지 기다리게 해주는 도구(초기값 = 스레드 수(THREAD_CNT))
        // 스레드마다 .countDown()을 호출하면 숫자가 줄어들고
        // 메인 스레드는 .await()로 그 숫자가 0이 될 때까지 기다린다.
        CountDownLatch latch = new CountDownLatch(THREAD_CNT);

        Runnable task = () -> {
            for (int i = 0; i < APPEND_CNT; i++) {
                builder.append('a');// 이게 동시에 실행되면 충돌 위험!
            }
            latch.countDown();// 이 스레드 끝났다는 신호
        };

        for (int i = 0; i < THREAD_CNT; i++) {
            new Thread(task).start(); // 1000개의 스레드를 실행시킴
        }
        // 모든 스레드가 끝날 때까지 기다림. 즉, 모든 스레드가 append() 작업을 끝낼 때까지 대기
        latch.await();

//        System.out.println("StringBuilder length: " + builder.length());
        assertNotEquals(THREAD_CNT * APPEND_CNT, builder.length());
    }

    @Test
    public void testStringBufferMultiThreadSafe() throws InterruptedException {
        StringBuffer buffer = new StringBuffer();
        CountDownLatch latch = new CountDownLatch(THREAD_CNT);

        Runnable task = () -> {
            for (int i = 0; i < APPEND_CNT; i++) {
                buffer.append("a"); // synchronized 처리되어 안전
            }
            latch.countDown();
        };

        for (int i = 0; i < THREAD_CNT; i++) {
            new Thread(task).start();// 1000개의 스레드 실행
        }
        // 모든 스레드가 끝날 때까지 기다림. 즉, 모든 스레드가 append() 작업을 끝낼 때까지 대기
        latch.await();

//        System.out.println("StringBuffer length: " + buffer.length());
        assertEquals(THREAD_CNT * APPEND_CNT, buffer.length()); // 항상 성공해야 함
    }

    // builder를 동기화방식으로 감싸서 실행해보기
    // buffer는 append한번당 락을 걸고 풀어야해서 비용이 스레드수 * append횟수로 굉장이 많음.
    // syncronized(buffer) : for문당 락을걸어서 스레수만큼 락비용이 들음
    @Test
    public void testStringBuilderSyncronize() throws InterruptedException {
        StringBuilder builder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(THREAD_CNT);

        Runnable task = () -> {
            synchronized (builder) {
                for (int i = 0; i < APPEND_CNT; i++) {
                    builder.append("a");
                }
            }
            latch.countDown();
        };
        for (int i = 0; i < THREAD_CNT; i++) {
            new Thread(task).start();
        }

        latch.await();

//        System.out.println("StringBuffer length: " + builder.length());
        assertEquals(THREAD_CNT * APPEND_CNT, builder.length());
    }
}
