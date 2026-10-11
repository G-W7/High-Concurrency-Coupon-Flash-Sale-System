package com.hmdp.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Feed流滚动分页结果")
public class ScrollResult {
    @Schema(description = "当前页数据列表")
    private List<?> list;
    @Schema(description = "本次查询的最小时间戳（下次查询的 lastId）", example = "1696000000000")
    private Long minTime;
    @Schema(description = "偏移量，本次查询返回的条数", example = "5")
    private Integer offset;
}
