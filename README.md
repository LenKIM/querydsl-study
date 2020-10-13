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
// 
```

