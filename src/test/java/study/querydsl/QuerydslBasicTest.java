package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static study.querydsl.QMember.member;
import static study.querydsl.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory = new JPAQueryFactory(em);

    @BeforeEach
    void setUp() {

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
        queryFactory = new JPAQueryFactory(em);
    }

    @Test
    void startJPQL() {
//        member1을 찾기
        Member findMember = em.createQuery("select m from  Member  m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() {
        QMember m = new QMember("m");

        //1. 파라미터 바인딩 X preparestatement 로 동작 됨.
        //2. 문자열로 작성됨.
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    void startQuerydsl2() {

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl3() {

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember m = member; // 기본 인스턴스 사용
//        QMember m = new QMember("m"); // 별칭 직접 사용

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() {
        queryFactory = new JPAQueryFactory(em);
        QMember member = new QMember("m");


        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() {

        queryFactory = new JPAQueryFactory(em);


//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member member2 = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne();
//
//        Member member1 = queryFactory.selectFrom(QMember.member)
//                .fetchFirst();
//
//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        results.getTotal();
//
//        List<Member> a = results.getResults();

        long total = queryFactory.selectFrom(member)
                .fetchCount();

    }

    /**
     * 1. 나이 내림차 순(desc)
     * 2. 회원 이름 올린차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 nulls last
     */
    @Test
    void sort() {
        queryFactory = new JPAQueryFactory(em);


        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = fetch.get(0);
        Member member6 = fetch.get(1);
        Member memberNull = fetch.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    void paging1() {
        queryFactory = new JPAQueryFactory(em);

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(fetch.size()).isEqualTo(2);
    }

    @Test
    void paging2() {
        queryFactory = new JPAQueryFactory(em);

        QueryResults<Member> memberQueryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(memberQueryResults.getTotal()).isEqualTo(4);
        assertThat(memberQueryResults.getLimit()).isEqualTo(2);
        assertThat(memberQueryResults.getOffset()).isEqualTo(1);
        assertThat(memberQueryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    void aggregation() {
        queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory.select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min()
        ).from(member).fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구하라.
     */
    @Test
    public void group() throws Exception {
        queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    void join() {
        queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

    }

    /**
     * 세타 조인 -연관 관계가 없어도 조인이 가능하다.
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * select
     * member0_.member_id as member_i1_1_,
     * member0_.age as age2_1_,
     * member0_.team_id as team_id4_1_,
     * member0_.username as username3_1_
     * from
     * member member0_ cross
     * join
     * team team1_
     * where
     * member0_.username=team1_.name
     */
    @Test
    void theta_join() {
        queryFactory = new JPAQueryFactory(em);

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        // 모두다 가져와서 조인한다. = 세타 조인

        assertThat(result).extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     * tuple = [Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
     * tuple = [Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
     * tuple = [Member(id=5, username=member3, age=30), null]
     * tuple = [Member(id=6, username=member4, age=40), null]
     * <p>
     * select
     * member0_.member_id as member_i1_1_0_,
     * team1_.team_id as team_id1_2_1_,
     * member0_.age as age2_1_0_,
     * member0_.team_id as team_id4_1_0_,
     * member0_.username as username3_1_0_,
     * team1_.name as name2_2_1_
     * from
     * member member0_
     * left outer join
     * team team1_
     * on member0_.team_id=team1_.team_id
     * and (
     * team1_.name=?
     * )
     */
    @Test
    void join_on_filtering() {
        queryFactory = new JPAQueryFactory(em);

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        List<Tuple> result2 = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team).on(team.name.eq("teamA")) // inner join 하게 된다.
                .fetch();

        for (Tuple tuple : result2) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관 관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀이름과 같은 대상 외부 조인
     */
    @Test
    void join_on_no_relation() {
        queryFactory = new JPAQueryFactory(em);

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNo() {
        queryFactory = new JPAQueryFactory(em);

        em.flush();
        em.clear();

        Member member = queryFactory.selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();
        // 여기까지하면 멤버만 조인될 것이다. 왜냐하면 레이지 로딩이기 때문이다.

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member.getTeam());
        // 여기서 현재 엔티티가 조회된 상태인지 아닌지 여부를 판단하게 해주는 함수

        assertThat(loaded).as("폐치 조인 미적용").isFalse();
    }

    /**
     * select
     * member0_.member_id as member_i1_1_0_,
     * team1_.team_id as team_id1_2_1_,
     * member0_.age as age2_1_0_,
     * member0_.team_id as team_id4_1_0_,
     * member0_.username as username3_1_0_,
     * team1_.name as name2_2_1_
     * from
     * member member0_
     * inner join
     * team team1_
     * on member0_.team_id=team1_.team_id
     * where
     * member0_.username=?
     */
    @Test
    void fetchJoinUse() {
        queryFactory = new JPAQueryFactory(em);

        em.flush();
        em.clear();

        Member one = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(one.getTeam());
        // 여기서 현재 엔티티가 조회된 상태인지 아닌지 여부를 판단하게 해주는 함수

        assertThat(loaded).as("폐치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     * select
     * member0_.member_id as member_i1_1_,
     * member0_.age as age2_1_,
     * member0_.team_id as team_id4_1_,
     * member0_.username as username3_1_
     * from
     * member member0_
     * where
     * member0_.age=(
     * select
     * max(member1_.age)
     * from
     * member member1_
     * )
     */
    @Test
    void subQuery() {
        queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub))).fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원들
     * select
     * member0_.member_id as member_i1_1_,
     * member0_.age as age2_1_,
     * member0_.team_id as team_id4_1_,
     * member0_.username as username3_1_
     * from
     * member member0_
     * where
     * member0_.age>=(
     * select
     * avg(cast(member1_.age as double))
     * from
     * member member1_
     * )
     */
    @Test
    void subQueryGoe() {
        queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                )).fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     * select
     * member0_.member_id as member_i1_1_,
     * member0_.age as age2_1_,
     * member0_.team_id as team_id4_1_,
     * member0_.username as username3_1_
     * from
     * member member0_
     * where
     * member0_.age in (
     * select
     * member1_.age
     * from
     * member member1_
     * where
     * member1_.age>?
     * )
     */
    @Test
    void subQueryIn() {
        queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                )).fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    /**
     * select
     * member0_.username as col_0_0_,
     * (select
     * avg(cast(member1_.age as double))
     * from
     * member member1_) as col_1_0_
     * from
     * member member0_
     */
    @Test
    void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        queryFactory = new JPAQueryFactory(em);
        List<Tuple> fetch = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * select
     * case
     * when member0_.age=? then ?
     * when member0_.age=? then ?
     * else '기타'
     * end as col_0_0_
     * from
     * member member0_
     */
    @Test
    void basicCase() {
        queryFactory = new JPAQueryFactory(em);

        List<String> fetch = queryFactory
                .select(member.age.when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s => " + s);
        }
    }

    /**
     * select
     * case
     * when member0_.age between ? and ? then ?
     * when member0_.age between ? and ? then ?
     * else '기타'
     * end as col_0_0_
     * from
     * member member0_
     */
    @Test
    void complexCase() {
        queryFactory = new JPAQueryFactory(em);

        List<String> result = queryFactory.select(new CaseBuilder()
                .when(member.age.between(0, 20)).then("0~20살")
                .when(member.age.between(21, 30)).then("21~30살")
                .otherwise("기타")
        ).from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s => " + s);
        }
    }

    /**
     * select
     * member0_.username as col_0_0_
     * from
     * member member0_
     */
    @Test
    void constant() {
        queryFactory = new JPAQueryFactory(em);

        List<Tuple> a = queryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : a) {
            System.out.println("tuple => " + tuple);
        }
    }

    /**
     * select
     * ((member0_.username||?)||cast(member0_.age as char)) as col_0_0_
     * from
     * member member0_
     * where
     * member0_.username=?
     */
    @Test
    void concat() {
        queryFactory = new JPAQueryFactory(em);

        //{username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();


        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void simpleProjection() {
        List<String> members = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : members) {
            System.out.println("s - > " + s);
        }
    }

    @Test
    void tupleProjection() {
        List<Tuple> fetch = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            String s = tuple.get(member.username);
            Integer integer = tuple.get(member.age);
            System.out.println("username = " + s);
            System.out.println("age = " + in());
        }
    }

    @Test
    void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from  Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto =>" + memberDto);
        }
    }

    /**
     * Setter 접근
     */
    @Test
    void findDtoBySetter() {

        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto => " + memberDto);
        }
    }

    /**
     * 필드 직접 접근
     */
    @Test
    void findDtoByField() {

        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto => " + memberDto);
        }
    }

    /**
     * 생성자 접근
     */
    @Test
    void findDtoByConstructor() {

        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto => " + memberDto);
        }
    }

    /**
     * 별칭이 다를 때
     */
    @Test
    void findDtoByAlias() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto memberDto : result) {
            System.out.println("UserDto => " + memberDto);
        }
    }

    /**
     * 단점,
     * 1.Q 타입을 만들어야 한다.
     * 2.queryDSL의 의존성을 갖게 된다. 왜냐하면. @QueryProjection 을 붙어야 하기 때문이다.
     */
    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto > " + memberDto);
        }
    }

    @Test
    void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();

        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    void dynamicQuery_whereParam() {

        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(
//                        usernameEq(usernameCond), ageEq(ageCond)
                        allEq(usernameCond, ageCond)
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if (usernameCond == null) return null;
        return member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if (ageCond == null) return null;
        return member.age.eq(ageCond);
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }


    @Test
    void bulkUpdate() {

        /** 실행 전
         * 변경감지 개발 엔티티 건건히 나가는 것이다.
         * member1 = 10 > DB member1
         * member2 = 10 > DB member2
         * member3 = 30 > DB member3
         * member4 = 40 > DB member4
         */

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

//        그러므로 해결하기 위해서는 아래와 같이 해줍니다.
//        em.flush();
//        em.clear();
        // 실행 후
//        member1 = 10 > 1 DB 비회원
//        member2 = 10 > 2 DB 비회원
//        member3 = 30 > 3 DB member3
//        member4 = 40 > 4 DB member4

//        항상 영속성 컨테스트가 우선권을 갖는다.
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member : result) {
            System.out.println("member > " + member);
        }
    }

    @Test
    void bulkAdd() {
        long execute = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    void bulkMultiple() {
        long execute = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    void bulkDelete() {
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("S => " + s);
        }
    }
    @Test
    void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)
//                        )
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (String s : result) {
            System.out.println("S => " + s);
        }
    }
}
