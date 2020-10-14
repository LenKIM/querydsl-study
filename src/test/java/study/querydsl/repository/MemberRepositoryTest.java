package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import study.querydsl.Member;
import study.querydsl.Team;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

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

    @Test
    void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeGoe(40);
        condition.setTeamname("teamB");
        // 만약에 데이터가 아무것도 없을 경우, 모든 쿼리를 다 날립니다. eager와 동일한 형태를 띕니다.
        // 가급적이면 페이징 쿼리와 같이 해주는게 바람직하다.

//        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        List<MemberTeamDto> result = memberRepository.search(condition);

        assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    void searchTestPage() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);
//        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);

        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }


}