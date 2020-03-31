package me.gramman75.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {
    private String username;
    private int age;
    private String teamname;

    @QueryProjection
    public MemberDto(String username, int age){
        this.username = username;
        this.age = age;
    }

    @QueryProjection
    public MemberDto(String username, int age, String teamname) {
        this.username = username;
        this.age = age;
        this.teamname = teamname;
    }
}
