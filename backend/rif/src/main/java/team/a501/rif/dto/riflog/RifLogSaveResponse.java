package team.a501.rif.dto.riflog;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class RifLogSaveResponse {

    private String name;
    private Integer point;
    private Integer exp;
    private LocalDateTime createdAt;

    @Builder
    public RifLogSaveResponse(String name, Integer point, Integer exp, LocalDateTime createdAt) {
        this.name = name;
        this.point = point;
        this.exp = exp;
        this.createdAt = createdAt;
    }
}
