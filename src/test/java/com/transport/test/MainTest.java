package com.transport.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={MainConfig.class}, loader=AnnotationConfigContextLoader.class)
public class MainTest {

    @Autowired
    private PersonServiceTransport personService;

    @Autowired
    private ClientServiceTransport clientService;

    @Test
    public void testMethods() {
        Runnable runnable = () -> {
            Integer id = personService.add("James Carr", "james@zapier.com", null).withTimeout(10_000).onModule("main.server").executeSync();
            System.out.printf("Resulting id is %s", id);
            System.out.println();
            Person person = personService.get(id).onModule("main.server").executeSync();
            System.out.println(person);
            personService.lol().executeSync();
            personService.lol2("kek").executeSync();
            System.out.println("Name: " + personService.getName().executeSync());
            clientService.lol3("test3").onModule("main.server").executeSync();
            clientService.lol4("test4").onModule("main.server").executeSync();
            clientService.lol4("test4").onModule("main.server").executeAsync(UUID.randomUUID().toString(), ServiceCallback.class);
            personService.get(id).onModule("main.server").executeAsync(UUID.randomUUID().toString(), PersonCallback.class);
            personService.lol2("kek").executeSync();
            try {
                personService.testError().onModule("main.server").executeSync();
            } catch (Exception e) {
                System.out.println("Exception during sync call");
                e.printStackTrace();
            }
            personService.testError().onModule("main.server").executeAsync(UUID.randomUUID().toString(), PersonCallback.class);

            id = personService.add("James Carr", "james@zapier.com", null).withTimeout(10_000).onModule("test.server").executeSync();
            System.out.printf("Resulting id is %s", id);
            System.out.println();
            person = personService.get(id).onModule("test.server").executeSync();
            System.out.println(person);
            personService.lol().executeSync();
            personService.lol2("kek").executeSync();
            System.out.println("Name: " + personService.getName().executeSync());
            clientService.lol3("test3").onModule("test.server").executeSync();
            clientService.lol4("test4").onModule("test.server").executeSync();
            clientService.lol4("test4").onModule("test.server").executeAsync(UUID.randomUUID().toString(), ServiceCallback.class);
            personService.get(id).onModule("test.server").executeAsync(UUID.randomUUID().toString(), PersonCallback.class);
            personService.lol2("kek").executeSync();
            try {
                personService.testError().onModule("test.server").executeSync();
            } catch (Exception e) {
                System.out.println("Exception during sync call");
                e.printStackTrace();
            }
            personService.testError().onModule("test.server").executeAsync(UUID.randomUUID().toString(), PersonCallback.class);
        };

        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);
        Thread thread3 = new Thread(runnable);

        thread1.start();
        thread2.start();
        thread3.start();

        try{
            thread1.join();
            thread2.join();
            thread3.join();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
