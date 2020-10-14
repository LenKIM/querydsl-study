# Querydsl

[참고 자료]

http://www.querydsl.com/static/querydsl/4.0.1/reference/ko-KR/html_single/#intro

Querydsl 정적 타입을 이용해서 SQL과 같은 쿼리를 생성할 수 있도록 해 주는 프레임워크다. 문자열로 작성하거나 XML 파일에 쿼리를 작성하는 대신, Querydsl이 제공하는 플루언트(Fluent) API를 이용해서 쿼리를 생성할 수 있다.

단순 문자열과 비교해서 Fluent API를 사용할 때의 장점은 다음과 같다.

- IDE의 코드 자동 완성 기능 사용

- 문법적으로 잘못된 쿼리를 허용하지 않음

- 도메인 타입과 프로퍼티를 안전하게 참조할 수 있음

- 도메인 타입의 리팩토링을 더 잘 할 수 있음



## 1. Querydsl 설정하기

```java
plugins {
    id 'org.springframework.boot' version '2.3.4.RELEASE'
    id 'io.spring.dependency-management' version '1.0.10.RELEASE'

    //querydsl 추가
    id "com.ewerk.gradle.plugins.querydsl" version "1.0.10"

    id 'java'
}

group = 'study'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '1.8'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'

    //querydsl 추가
    implementation 'com.querydsl:querydsl-jpa'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation('org.springframework.boot:spring-boot-starter-test') {
        exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
    }
}

test {
    useJUnitPlatform()
}

//querydsl 추가 시작
def querydslDir = "$buildDir/generated/querydsl"

querydsl {
    jpa = true
    querydslSourcesDir = querydslDir
}

sourceSets {
    main.java.srcDir querydslDir
}

configurations {
    querydsl.extendsFrom compileClasspath
}

compileQuerydsl {
    options.annotationProcessorPath = configurations.querydsl
}
// 설정 종료
```



**gradle 에서 의존관계 살펴보기**

`./gradlew dependencies --configuration compileClasspath`

- Querydsl Q타입은 무엇인가?





---

## 예제 모델 을 만드는 엔티티 코드

![image-20201014104113097](https://tva1.sinaimg.cn/large/007S8ZIlgy1gjom7xpy1zj30iw0fijsg.jpg)



```java

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") //외래키 이름.
    private Team team;

    public Member(String username) {
        this(username, 0);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null){
            changeTeam(team);
        }
        this.team = team;
    }

    private void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
```

```java
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "name"})
public class Team {

    @Id
    @GeneratedValue
    @Column(name = "team_id")
    private Long id;
    private String name;

    @OneToMany(mappedBy = "team")
    List<Member> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }
}
```



## 1. Querydsl 동작되는 테스트 코드 작성

```java
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

    @Autowired
    EntityManager em;

    @Test
    void contextLoads() {

        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QHello qHello = QHello.hello;

        Hello result = query.selectFrom(qHello).fetchOne();

        Assertions.assertThat(result).isEqualTo(hello);
        Assertions.assertThat(result.getId()).isEqualTo(hello.getId());
    }
}
```

## 2. JPQL vs Querydsl

- `EntityManager` 로 `JPAQueryFactory` 생성
- Querydsl은 JPQL 빌더
- JPQL: 문자(실행 시점 오류), Querydsl: 코드(컴파일 시점 오류) 
- JPQL: 파라미터 바인딩 직접, Querydsl: 파라미터 바인딩 자동 처리



## 동시성 문제는?

JPAQueryFactory를 필드로 제공하면 동시성 문제는 어떻게 될까?  
동시성 문제는 JPAQueryFactory를 생성할 때 제공하는 EntityManager(em)에 달려있다. 스프링 프레임워크는 여러 쓰레드에서 동시에 같은 EntityManager에 접근해도, 트랜잭션 마다 별도의 영속성 컨텍스트를 제공하기 때문에, 동시성 문제는 걱정하지 않아도 된다.



![image-20201014105234384](https://tva1.sinaimg.cn/large/007S8ZIlgy1gjomjope21j30ej0570t4.jpg)

## 3. 검색 조건 쿼리

*JPQL이 제공하는 모든 검색 조건 제공*

```java
member.username.eq("member1") // username = 'member1'
member.username.ne("member1") //username != 'member1'
member.username.eq("member1").not() // username != 'member1'
member.username.isNotNull() //이름이 is not null
member.age.in(10, 20) // age in (10,20)
member.age.notIn(10, 20) // age not in (10, 20)
member.age.between(10,30) //between 10, 30
member.age.goe(30) // age >= 30
member.age.gt(30) // age > 30
member.age.loe(30) // age <= 30
member.age.lt(30) // age < 30
member.username.like("member%") //like 검색
member.username.contains("member") // like ‘%member%’ 검색
member.username.startsWith("member") //like ‘member%’ 검색
```

```java
@Test
public void searchAndParam() {
 List<Member> result1 = queryFactory
 .selectFrom(member)
 .where(member.username.eq("member1"),
 member.age.eq(10))
 .fetch();
 assertThat(result1.size()).isEqualTo(1);
}
```

- where() 에 파라미터로 검색조건을 추가하면 AND 조건이 추가됨 

- 이 경우 null 값은 무시 메서드 추출을 활용해서 동적 쿼리를 깔끔하게 만들 수 있음

```java
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

        Member findMember2 = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember2.getUsername()).isEqualTo("member1");
    }

```

```java
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

```

```java
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

```

## 4. 결과 조회 

```java
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
```

**Count 시, 쿼리가 2번 날라간다는 사실을 명심할 것!**

## 5. 정렬

```java

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
```

## 6. 페이징

```java
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
```

## 7. 집합 

```java
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
```

```java
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

// 그룹화된 결과를 제한하려면 having()
```

## 8. 조인 - 기본 조인 

```java
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
```

```java
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

```

```java
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

```

## 9. 조인 - on절 

```java
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

```

## 10. 조인 - 페치 조인 

```java
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
```

## 11. 서브 쿼리 

`com.querydsl.jpa.JPAExpressions`

```java

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
                                .from(memberSub)
                )).fetch();

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

        assertThat(result).extracting("age").containsExactly(30);
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
```

그러나, from 절의 서브쿼리는 한계가 있다.

JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원 X, Querydsl도 X

하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다. querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.

*from 절의 서브쿼리 해결방안* 

1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.) 
2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다. 
3. nativeSQL을 사용한다.

## 12. Case 문 

select, 조건절(where), AOrder by 에서 사용 가능.

```java

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

```



## 13. 상수, 문자 더하기

```java
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
```



