package com.zkteco.recruit.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictItemRequest {

    @NotBlank(message = "请填写名称")
    @Size(min = 1, max = 20, message = "名称长度需为 1-20 位")
    private String name;

    private Integer sortOrder;
}
