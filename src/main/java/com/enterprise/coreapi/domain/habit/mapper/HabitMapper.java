package com.enterprise.coreapi.domain.habit.mapper;

import com.enterprise.coreapi.domain.habit.dto.HabitResponse;
import com.enterprise.coreapi.domain.habit.entity.Habit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HabitMapper {

    @Mapping(target = "identityPublicId", source = "identity.publicId")
    @Mapping(target = "identityName", source = "identity.name")
    @Mapping(target = "category", expression = "java(habit.getCategory().name())")
    @Mapping(target = "completedToday", ignore = true)
    HabitResponse toResponse(Habit habit);

    List<HabitResponse> toResponseList(List<Habit> habits);
}
