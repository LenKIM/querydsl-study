package study.querydsl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@SpringBootTest
class CustomerTest {

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        Customer customer1 = new Customer();
        Customer customer2 = new Customer();
        Customer customer3 = new Customer();
        Customer customer4 = new Customer();
        customerRepository.save(customer1);
        customerRepository.save(customer2);
        customerRepository.save(customer3);
        customerRepository.save(customer4);
        Optional<Customer> byId1 = customerRepository.findById(1L);
        Optional<Customer> byId2 = customerRepository.findById(2L);
        Optional<Customer> byId3 = customerRepository.findById(3L);
        Optional<Customer> byId4 = customerRepository.findById(4L);
        for (int i = 0; i < 5; i++) {
            AOrder AOrder = new AOrder((long) i, i + "A", byId1.get());
            orderRepository.saveAndFlush(AOrder);
            AOrder BOrder = new AOrder((long) i, i + "B", byId2.get());
            orderRepository.saveAndFlush(BOrder);
            AOrder COrder = new AOrder((long) i, i + "C", byId3.get());
            orderRepository.saveAndFlush(COrder);
            AOrder DOrder = new AOrder((long) i, i + "D", byId4.get());
            orderRepository.saveAndFlush(DOrder);
        }
    }

//    @Test
//    @Transactional
//    void name() {
//        List<Customer> all = customerRepository.findAll();
//        all.stream().forEach(a -> {
//            a.getAOrders().stream().forEach(b -> System.out.println(b.getName()));
//        });
//    }
}