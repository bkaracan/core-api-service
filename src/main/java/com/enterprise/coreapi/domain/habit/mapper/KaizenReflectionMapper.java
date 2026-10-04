package com.enterprise.coreapi.domain.habit.mapper;

import com.enterprise.coreapi.domain.habit.dto.KaizenReflectionResponse;
import com.enterprise.coreapi.domain.habit.entity.KaizenReflection;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KaizenReflectionMapper {

    KaizenReflectionResponse toResponse(KaizenReflection reflection);
}
