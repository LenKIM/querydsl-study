package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;
    
    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        Member save = memberRepository.save(member);

        Member findMember = memberRepository.findById(save.getId()).get();
        assertThat(findMember).isEqualTo(save);

        List<Member> all = memberRepository.findAll();
        assertThat(all).containsExactly(member);

        List<Member> member1 = memberRepository.findByUsername("member1");
        assertThat(member1).containsExactly(member);

//        List<Member> allQueryDsl = memberRepository.findAll_Querydsl();
//        assertThat(allQueryDsl).containsExactly(member);

//        List<Member> memberQueryDsl = memberRepository.findByUsername_Querydsl("member1");
//        assertThat(memberQueryDsl).containsExactly(member);
    }
}