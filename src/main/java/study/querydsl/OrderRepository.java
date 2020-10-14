package study.querydsl;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<AOrder, Long> {

}