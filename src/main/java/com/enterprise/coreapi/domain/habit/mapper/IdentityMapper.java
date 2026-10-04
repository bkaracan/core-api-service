package com.enterprise.coreapi.domain.habit.mapper;

import com.enterprise.coreapi.domain.habit.dto.IdentityResponse;
import com.enterprise.coreapi.domain.habit.entity.Identity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IdentityMapper {

    IdentityResponse toResponse(Identity identity);

    List<IdentityResponse> toResponseList(List<Identity> identities);
}
