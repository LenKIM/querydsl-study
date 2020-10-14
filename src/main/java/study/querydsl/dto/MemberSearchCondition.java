package study.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {

    // 회원명, 팀명, 나이(ageGoe, ageLoe)
    private String username;
    private String teamname;
    private Integer ageGoe;
    private Integer ageLoe;


    public MemberSearchCondition() {
    }

    public MemberSearchCondition(String username, String teamname, Integer ageGoe, Integer ageLoe) {
        this.username = username;
        this.teamname = teamname;
        this.ageGoe = ageGoe;
        this.ageLoe = ageLoe;
    }
}
